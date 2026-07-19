package com.company.portal.identity.application;

import com.company.portal.audit.api.AuditContext;
import com.company.portal.audit.api.AuditOutcome;
import com.company.portal.audit.api.AuditService;
import com.company.portal.audit.api.AuditSeverityLevel;
import com.company.portal.identity.domain.PasswordResetTokenEntity;
import com.company.portal.identity.domain.UserEntity;
import com.company.portal.identity.repository.PasswordResetTokenRepository;
import com.company.portal.identity.repository.UserRepository;
import com.company.portal.identity.security.RateLimiter;
import com.company.portal.notification.api.NotificationService;
import com.company.portal.shared.config.PortalProperties;
import com.company.portal.shared.error.ErrorCodes;
import com.company.portal.shared.error.PortalException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Password change and recovery. Recovery tokens are stored hashed (SHA-256)
 * so a database compromise cannot immediately hijack accounts. All flows
 * respond identically whether the account exists — enumeration protection.
 */
@Service
public class PasswordService {

    private static final Logger log = LoggerFactory.getLogger(PasswordService.class);
    private static final Duration RESET_TTL = Duration.ofMinutes(30);
    private static final int TOKEN_BYTES = 32;

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notifications;
    private final AuditService auditService;
    private final PortalProperties portalProperties;
    private final RateLimiter rateLimiter;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordService(UserRepository users,
                           PasswordResetTokenRepository tokens,
                           PasswordEncoder passwordEncoder,
                           NotificationService notifications,
                           AuditService auditService,
                           PortalProperties portalProperties,
                           RateLimiter rateLimiter) {
        this.users = users;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.notifications = notifications;
        this.auditService = auditService;
        this.portalProperties = portalProperties;
        this.rateLimiter = rateLimiter;
    }

    @Transactional
    public void requestPasswordReset(String rawEmail, String ip, String userAgent) {
        String normalized = rawEmail == null ? "" : rawEmail.trim().toLowerCase();
        if (ip != null && !ip.isBlank()) {
            RateLimiter.Decision d = rateLimiter.allow(
                    "password-reset:ip", ip, portalProperties.getRateLimit().getPasswordReset());
            if (!d.allowed()) {
                throw new PortalException.RateLimited("Too many password reset requests", d.retryAfterSeconds());
            }
        }
        if (!normalized.isBlank()) {
            RateLimiter.Decision d = rateLimiter.allow(
                    "password-reset:email", normalized, portalProperties.getRateLimit().getPasswordReset());
            if (!d.allowed()) {
                throw new PortalException.RateLimited("Too many password reset requests", d.retryAfterSeconds());
            }
        }
        Optional<UserEntity> userOpt = users.findByEmailNormalized(normalized);
        if (userOpt.isEmpty()) {
            // Do NOT signal presence to caller. Log for diagnostics.
            log.debug("Password reset requested for unknown email");
            return;
        }
        UserEntity user = userOpt.get();

        String rawToken = randomTokenUrlSafe();
        String tokenHash = sha256Hex(rawToken);
        OffsetDateTime expires = OffsetDateTime.now().plus(RESET_TTL);

        PasswordResetTokenEntity token = new PasswordResetTokenEntity(
                UUID.randomUUID(), user.getId(), tokenHash, expires);
        token.setRequestIp(ip);
        token.setRequestUserAgent(userAgent);
        tokens.save(token);

        auditService.append(AuditContext.builder()
                .eventType("PASSWORD_RESET_REQUESTED")
                .category("AUTH")
                .severity(AuditSeverityLevel.NOTICE)
                .outcome(AuditOutcome.SUCCESS)
                .actorType("USER")
                .actorId(user.getId())
                .targetType("USER")
                .targetId(user.getId().toString())
                .ipAddress(ip)
                .userAgent(userAgent)
                .build());

        String resetUrl = portalProperties.getPublicBaseUrl()
                + "/reset-password?token=" + rawToken;
        notifications.enqueueEmail("password_reset", user.getEmail(),
                user.getLocale(), Map.of(
                        "resetUrl", resetUrl,
                        "expiresAt", expires.toString(),
                        "displayName", user.getDisplayName()));
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        validatePasswordStrength(newPassword);
        String hash = sha256Hex(rawToken);
        PasswordResetTokenEntity token = tokens.findByTokenHash(hash)
                .orElseThrow(() -> new PortalException.Validation("Invalid reset token"));
        if (token.getConsumedAt() != null) {
            throw new PortalException.Validation("Reset token already used");
        }
        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new PortalException.Validation("Reset token expired");
        }
        UserEntity user = users.findById(token.getUserId())
                .orElseThrow(() -> new PortalException.Validation("Invalid reset token"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setLastPasswordChangedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        user.setFailedLoginCount(0);
        user.setLockoutUntil(null);

        token.setConsumedAt(OffsetDateTime.now());
        tokens.save(token);
        tokens.invalidateActiveForUser(user.getId(), OffsetDateTime.now());

        auditService.append(AuditContext.builder()
                .eventType("PASSWORD_RESET_COMPLETED")
                .category("AUTH")
                .severity(AuditSeverityLevel.NOTICE)
                .outcome(AuditOutcome.SUCCESS)
                .actorType("USER")
                .actorId(user.getId())
                .targetType("USER")
                .targetId(user.getId().toString())
                .build());

        notifications.enqueueEmail("password_changed", user.getEmail(),
                user.getLocale(), Map.of(
                        "changedAt", OffsetDateTime.now().toString(),
                        "displayName", user.getDisplayName()));
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        validatePasswordStrength(newPassword);
        UserEntity user = users.findById(userId)
                .orElseThrow(() -> new PortalException.NotFound("User not found"));
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new PortalException.Validation("Current password is incorrect");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new PortalException.Validation("New password must differ from current");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setLastPasswordChangedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());

        tokens.invalidateActiveForUser(user.getId(), OffsetDateTime.now());

        auditService.append(AuditContext.builder()
                .eventType("PASSWORD_CHANGED")
                .category("AUTH")
                .severity(AuditSeverityLevel.NOTICE)
                .outcome(AuditOutcome.SUCCESS)
                .actorType("USER")
                .actorId(user.getId())
                .targetType("USER")
                .targetId(user.getId().toString())
                .build());

        notifications.enqueueEmail("password_changed", user.getEmail(),
                user.getLocale(), Map.of(
                        "changedAt", OffsetDateTime.now().toString(),
                        "displayName", user.getDisplayName()));
    }

    private String randomTokenUrlSafe() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 12) {
            throw new PortalException.Validation("Password must be at least 12 characters",
                    Map.of("code", ErrorCodes.PASSWORD_POLICY));
        }
        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSymbol = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSymbol = true;
        }
        int classes = (hasUpper ? 1 : 0) + (hasLower ? 1 : 0)
                + (hasDigit ? 1 : 0) + (hasSymbol ? 1 : 0);
        if (classes < 3) {
            throw new PortalException.Validation(
                    "Password must include at least three of: uppercase, lowercase, digit, symbol",
                    Map.of("code", ErrorCodes.PASSWORD_POLICY));
        }
    }
}
