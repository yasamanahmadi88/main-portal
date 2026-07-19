package com.company.portal.identity.application;

import com.company.portal.accesscontrol.api.EffectiveAuthorities;
import com.company.portal.accesscontrol.api.RbacQueryPort;
import com.company.portal.audit.api.AuditContext;
import com.company.portal.audit.api.AuditOutcome;
import com.company.portal.audit.api.AuditService;
import com.company.portal.audit.api.AuditSeverityLevel;
import com.company.portal.identity.domain.LoginAttemptEntity;
import com.company.portal.identity.domain.UserEntity;
import com.company.portal.identity.domain.UserStatus;
import com.company.portal.identity.repository.LoginAttemptRepository;
import com.company.portal.identity.repository.UserRepository;
import com.company.portal.identity.security.CaptchaService;
import com.company.portal.identity.security.PortalUserDetails;
import com.company.portal.identity.security.RateLimiter;
import com.company.portal.identity.security.RequestContext;
import com.company.portal.observability.AuthMetrics;
import com.company.portal.securityevent.api.SecurityEventPublisher;
import com.company.portal.securityevent.api.SecurityEventPublisher.SecurityEventLevel;
import com.company.portal.shared.config.PortalProperties;
import com.company.portal.shared.error.ErrorCodes;
import com.company.portal.shared.error.PortalException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.SessionFixationProtectionEvent;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the login flow.
 *
 * <p>Guarantees:
 * <ul>
 *   <li>Generic failure responses — no email enumeration.</li>
 *   <li>Per-IP and per-email rate limiting via Redis.</li>
 *   <li>Progressive lockout: {@code failed_login_count} increments on each
 *       failure and locks the account once the threshold is reached.</li>
 *   <li>Session fixation defence: on success the servlet session is
 *       invalidated and a fresh one is created ({@code changeSessionId()}).</li>
 *   <li>Audit + security-event emission for every attempt.</li>
 * </ul>
 * </p>
 */
@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);
    private static final int LOCK_AFTER_FAILURES = 8;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final LoginAttemptRepository loginAttempts;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiter rateLimiter;
    private final CaptchaService captchaService;
    private final PortalProperties properties;
    private final AuditService auditService;
    private final SecurityEventPublisher securityEvents;
    private final RbacQueryPort rbac;
    private final LoginChallengeStore challengeStore;
    private final SessionService sessionService;
    private final ApplicationEventPublisher eventPublisher;
    private final SecurityContextRepository securityContextRepository;
    private final AuthMetrics authMetrics;

    public AuthenticationService(UserRepository userRepository,
                                 LoginAttemptRepository loginAttempts,
                                 PasswordEncoder passwordEncoder,
                                 RateLimiter rateLimiter,
                                 CaptchaService captchaService,
                                 PortalProperties properties,
                                 AuditService auditService,
                                 SecurityEventPublisher securityEvents,
                                 RbacQueryPort rbac,
                                 LoginChallengeStore challengeStore,
                                 SessionService sessionService,
                                 ApplicationEventPublisher eventPublisher,
                                 SecurityContextRepository securityContextRepository,
                                 AuthMetrics authMetrics) {
        this.userRepository = userRepository;
        this.loginAttempts = loginAttempts;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiter = rateLimiter;
        this.captchaService = captchaService;
        this.properties = properties;
        this.auditService = auditService;
        this.securityEvents = securityEvents;
        this.rbac = rbac;
        this.challengeStore = challengeStore;
        this.sessionService = sessionService;
        this.eventPublisher = eventPublisher;
        this.securityContextRepository = securityContextRepository;
        this.authMetrics = authMetrics;
    }

    /**
     * Outcome of a login attempt.
     *
     * <p>{@code status} is either {@code AUTHENTICATED} — session created and
     * user available — or {@code MFA_REQUIRED} — a challenge id was issued
     * and the caller must complete verification.</p>
     */
    public record LoginOutcome(Status status, UserEntity user, String challengeId) {
        public enum Status { AUTHENTICATED, MFA_REQUIRED }
    }

    @Transactional
    public LoginOutcome login(String rawEmail, String password,
                              String captchaId, String captchaAnswer,
                              HttpServletRequest request, HttpServletResponse response) {
        String ip = RequestContext.clientIp(request);
        String ua = RequestContext.userAgent(request);
        String emailNormalized = RequestContext.normalizeEmail(rawEmail);

        enforceRateLimit(ip, emailNormalized);
        // CAPTCHA must succeed before credential verification (anti credential-stuffing).
        captchaService.consume(captchaId, captchaAnswer);
        authMetrics.loginAttempt();

        Optional<UserEntity> userOpt = emailNormalized == null || emailNormalized.isBlank()
                ? Optional.empty()
                : userRepository.findByEmailNormalized(emailNormalized);

        if (userOpt.isEmpty()) {
            recordFailure(null, emailNormalized, ip, ua, "USER_NOT_FOUND");
            authMetrics.loginFailure("user_not_found");
            throw genericInvalidCredentials();
        }

        UserEntity user = userOpt.get();

        if (user.getLockoutUntil() != null && user.getLockoutUntil().isAfter(OffsetDateTime.now())) {
            // Same client-facing failure as invalid credentials to avoid account
            // existence / lock-state enumeration. Internal security events remain specific.
            recordFailure(user.getId(), emailNormalized, ip, ua, "LOCKED");
            securityEvents.publish("AUTH_LOGIN_ON_LOCKED_ACCOUNT", SecurityEventLevel.HIGH,
                    user.getId(), ip, ua, RequestContext.correlationId(), null,
                    Map.of("email", emailNormalized));
            authMetrics.loginFailure("locked");
            throw genericInvalidCredentials();
        }
        if (user.getStatus() == UserStatus.DISABLED || user.getStatus() == UserStatus.DELETED) {
            recordFailure(user.getId(), emailNormalized, ip, ua, "DISABLED");
            authMetrics.loginFailure("disabled");
            throw genericInvalidCredentials();
        }
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(password, user.getPasswordHash())) {
            registerFailure(user, emailNormalized, ip, ua);
            authMetrics.loginFailure("bad_credentials");
            throw genericInvalidCredentials();
        }

        if (user.isMfaEnabled()) {
            recordAttempt(user.getId(), emailNormalized, ip, ua, "REQUIRES_MFA", null, true, false);
            authMetrics.mfaChallenge();
            auditService.append(AuditContext.builder()
                    .eventType("AUTH_MFA_CHALLENGE_ISSUED")
                    .category("AUTH")
                    .severity(AuditSeverityLevel.INFO)
                    .actorType("USER").actorId(user.getId())
                    .targetType("USER").targetId(user.getId().toString())
                    .ipAddress(ip).userAgent(ua)
                    .correlationId(RequestContext.correlationId())
                    .build());
            return new LoginOutcome(LoginOutcome.Status.MFA_REQUIRED, user,
                    challengeStore.issue(user.getId()));
        }

        establishAuthenticatedSession(user, request, response, ip, ua, false);
        recordAttempt(user.getId(), emailNormalized, ip, ua, "SUCCESS", null, false, false);
        authMetrics.loginSuccess();
        auditService.append(AuditContext.builder()
                .eventType("AUTH_LOGIN_SUCCESS")
                .category("AUTH")
                .severity(AuditSeverityLevel.NOTICE)
                .actorType("USER").actorId(user.getId()).actorDisplay(user.getEmailNormalized())
                .targetType("USER").targetId(user.getId().toString())
                .ipAddress(ip).userAgent(ua)
                .correlationId(RequestContext.correlationId())
                .build());
        return new LoginOutcome(LoginOutcome.Status.AUTHENTICATED, user, null);
    }

    @Transactional
    public UserEntity completeMfaLogin(String challengeId, boolean mfaOk,
                                       HttpServletRequest request, HttpServletResponse response) {
        UUID userId = challengeStore.consume(challengeId);
        if (userId == null) {
            throw new PortalException.Unauthorized(ErrorCodes.MFA_INVALID, "Invalid or expired MFA challenge");
        }
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new PortalException.Unauthorized(ErrorCodes.MFA_INVALID, "Invalid MFA challenge"));

        String ip = RequestContext.clientIp(request);
        String ua = RequestContext.userAgent(request);

        RateLimiter.Decision mfaLimit = rateLimiter.allow(
                "mfa:verify", userId.toString(), properties.getRateLimit().getMfaVerify());
        if (!mfaLimit.allowed()) {
            throw new PortalException.RateLimited("Too many MFA attempts", mfaLimit.retryAfterSeconds());
        }

        if (!mfaOk) {
            registerFailure(user, user.getEmailNormalized(), ip, ua);
            recordAttempt(user.getId(), user.getEmailNormalized(), ip, ua, "MFA_FAILED",
                    "INVALID_CODE", true, false);
            securityEvents.publish("AUTH_MFA_FAILED", SecurityEventLevel.MEDIUM,
                    user.getId(), ip, ua, RequestContext.correlationId(), null,
                    Map.of("email", user.getEmailNormalized()));
            authMetrics.loginFailure("mfa_invalid");
            throw new PortalException.Unauthorized(ErrorCodes.MFA_INVALID, "Invalid MFA code");
        }

        establishAuthenticatedSession(user, request, response, ip, ua, true);
        recordAttempt(user.getId(), user.getEmailNormalized(), ip, ua, "SUCCESS", null, true, true);
        authMetrics.loginSuccess();
        auditService.append(AuditContext.builder()
                .eventType("AUTH_MFA_SUCCESS")
                .category("AUTH")
                .severity(AuditSeverityLevel.NOTICE)
                .actorType("USER").actorId(user.getId())
                .targetType("USER").targetId(user.getId().toString())
                .ipAddress(ip).userAgent(ua)
                .correlationId(RequestContext.correlationId())
                .build());
        return user;
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContext empty = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.clearContext();
        if (request != null) {
            securityContextRepository.saveContext(empty, request, response);
            if (request.getSession(false) != null) {
                request.getSession(false).invalidate();
            }
        }
    }

    private void enforceRateLimit(String ip, String emailNormalized) {
        PortalProperties.Bucket loginBucket = properties.getRateLimit().getLogin();
        if (ip != null) {
            RateLimiter.Decision d = rateLimiter.allow("login:ip", ip, loginBucket);
            if (!d.allowed()) {
                throw new PortalException.RateLimited("Too many login attempts", d.retryAfterSeconds());
            }
        }
        if (emailNormalized != null && !emailNormalized.isBlank()) {
            RateLimiter.Decision d = rateLimiter.allow("login:email", emailNormalized, loginBucket);
            if (!d.allowed()) {
                throw new PortalException.RateLimited("Too many login attempts", d.retryAfterSeconds());
            }
        }
    }

    private void registerFailure(UserEntity user, String email, String ip, String ua) {
        user.setFailedLoginCount(user.getFailedLoginCount() + 1);
        if (user.getFailedLoginCount() >= LOCK_AFTER_FAILURES) {
            user.setLockoutUntil(OffsetDateTime.now().plus(LOCK_DURATION));
            user.setStatus(UserStatus.LOCKED);
            securityEvents.publish("AUTH_ACCOUNT_LOCKED", SecurityEventLevel.HIGH,
                    user.getId(), ip, ua, RequestContext.correlationId(), null,
                    Map.of("failedCount", user.getFailedLoginCount()));
        }
        user.setUpdatedAt(OffsetDateTime.now());
        recordFailure(user.getId(), email, ip, ua, "INVALID_CREDENTIALS");
    }

    private void recordFailure(UUID userId, String email, String ip, String ua, String reason) {
        recordAttempt(userId, email, ip, ua, "FAILURE", reason, false, false);
        auditService.append(AuditContext.builder()
                .eventType("AUTH_LOGIN_FAILED")
                .category("AUTH")
                .severity(AuditSeverityLevel.WARN)
                .outcome(AuditOutcome.FAILURE)
                .actorType(userId == null ? "ANONYMOUS" : "USER")
                .actorId(userId)
                .action("LOGIN_FAILED")
                .ipAddress(ip).userAgent(ua)
                .correlationId(RequestContext.correlationId())
                .addPayload("reason", reason)
                .build());
    }

    private void recordAttempt(UUID userId, String emailNormalized, String ip, String ua,
                               String outcome, String reason, boolean mfaChallenged, boolean mfaPassed) {
        LoginAttemptEntity entity = new LoginAttemptEntity(UUID.randomUUID(),
                emailNormalized == null ? "" : emailNormalized, outcome);
        entity.setUserId(userId);
        entity.setIpAddress(ip);
        entity.setUserAgent(ua);
        entity.setFailureReason(reason);
        entity.setMfaChallenged(mfaChallenged);
        entity.setMfaPassed(mfaPassed);
        entity.setCorrelationId(RequestContext.correlationId());
        loginAttempts.save(entity);
    }

    private void establishAuthenticatedSession(UserEntity user,
                                               HttpServletRequest request,
                                               HttpServletResponse response,
                                               String ip, String ua, boolean mfaVerified) {
        EffectiveAuthorities authorities = rbac.loadEffectiveAuthorities(user.getId());
        PortalUserDetails principal = new PortalUserDetails(user,
                authorities.roleCodes(), authorities.permissionCodes());
        Authentication auth = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        String oldSessionId = null;
        String newSessionId = null;
        if (request != null) {
            var existing = request.getSession(false);
            if (existing != null) {
                oldSessionId = existing.getId();
                try {
                    newSessionId = request.changeSessionId();
                } catch (IllegalStateException ex) {
                    log.debug("changeSessionId failed, falling back to invalidate+create: {}", ex.getMessage());
                    existing.invalidate();
                    newSessionId = request.getSession(true).getId();
                }
            } else {
                newSessionId = request.getSession(true).getId();
            }
            // Spring Security 6+ does not auto-persist a SecurityContext that was
            // set outside AuthenticationFilter — save explicitly into the Redis session.
            securityContextRepository.saveContext(context, request, response);
        }
        if (newSessionId != null && oldSessionId != null && !newSessionId.equals(oldSessionId)) {
            eventPublisher.publishEvent(new SessionFixationProtectionEvent(auth, oldSessionId, newSessionId));
        }

        if (newSessionId != null) {
            sessionService.recordSession(user.getId(), newSessionId, ip, ua, mfaVerified);
        }
        user.setLastLoginAt(OffsetDateTime.now());
        user.setFailedLoginCount(0);
        user.setLockoutUntil(null);
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            // First login also verifies the address.
            user.setStatus(UserStatus.ACTIVE);
        }
        user.setUpdatedAt(OffsetDateTime.now());
    }

    private static PortalException.Unauthorized genericInvalidCredentials() {
        return new PortalException.Unauthorized(ErrorCodes.UNAUTHENTICATED,
                "Invalid credentials");
    }
}
