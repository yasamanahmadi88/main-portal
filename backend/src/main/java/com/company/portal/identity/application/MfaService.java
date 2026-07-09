package com.company.portal.identity.application;

import com.company.portal.identity.domain.MfaCredentialEntity;
import com.company.portal.identity.domain.MfaRecoveryCodeEntity;
import com.company.portal.identity.domain.UserEntity;
import com.company.portal.identity.mfa.Base32;
import com.company.portal.identity.mfa.TotpCodeGenerator;
import com.company.portal.identity.repository.MfaCredentialRepository;
import com.company.portal.identity.repository.MfaRecoveryCodeRepository;
import com.company.portal.identity.repository.UserRepository;
import com.company.portal.shared.config.PortalProperties;
import com.company.portal.shared.crypto.SecretEncryptionService;
import com.company.portal.shared.error.PortalException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TOTP-based MFA enrolment, verification and recovery-code management.
 *
 * <p>Design notes:
 * <ul>
 *   <li>TOTP seeds are stored as AES-GCM ciphertext via
 *       {@link SecretEncryptionService}. Plaintext never touches disk.</li>
 *   <li>Recovery codes are argon2-hashed with the same encoder as passwords
 *       and marked single-use.</li>
 *   <li>Verification records the last verified step and refuses to reuse it,
 *       blocking replay within the tolerance window.</li>
 * </ul>
 * </p>
 */
@Service
public class MfaService {

    private static final String TOTP = "TOTP";
    private static final int TOTP_SECRET_BYTES = 20; // 160 bits per RFC 6238 §5.1

    private final MfaCredentialRepository credentials;
    private final MfaRecoveryCodeRepository recoveryCodes;
    private final UserRepository users;
    private final SecretEncryptionService encryption;
    private final PasswordEncoder passwordEncoder;
    private final PortalProperties properties;
    private final TotpCodeGenerator totp;
    private final SecureRandom secureRandom = new SecureRandom();

    public MfaService(MfaCredentialRepository credentials,
                      MfaRecoveryCodeRepository recoveryCodes,
                      UserRepository users,
                      SecretEncryptionService encryption,
                      PasswordEncoder passwordEncoder,
                      PortalProperties properties) {
        this.credentials = credentials;
        this.recoveryCodes = recoveryCodes;
        this.users = users;
        this.encryption = encryption;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        PortalProperties.Totp totpCfg = properties.getMfa().getTotp();
        this.totp = new TotpCodeGenerator("HmacSHA1", totpCfg.getDigits(),
                Duration.ofSeconds(totpCfg.getTimeStepSeconds()));
    }

    public record EnrollmentResult(UUID factorId, String secretBase32, String otpAuthUri) { }

    @Transactional
    public EnrollmentResult beginTotpEnrollment(UUID userId, String label) {
        UserEntity user = users.findById(userId)
                .orElseThrow(() -> new PortalException.NotFound("User not found"));

        credentials.findByUserIdAndType(userId, TOTP).ifPresent(existing -> {
            if (existing.isActivated()) {
                throw new PortalException.Conflict("A TOTP factor is already active");
            }
            credentials.delete(existing);
        });

        byte[] secret = new byte[TOTP_SECRET_BYTES];
        secureRandom.nextBytes(secret);
        SecretEncryptionService.Encrypted enc = encryption.encrypt(secret);

        MfaCredentialEntity entity = new MfaCredentialEntity(UUID.randomUUID(), userId, TOTP);
        entity.setLabel(label);
        entity.setSecretCiphertext(enc.ciphertext());
        entity.setSecretIv(enc.iv());
        entity.setSecretTag(enc.tag());
        entity.setEncryptionKeyId(enc.keyId());
        entity.setDigits((short) properties.getMfa().getTotp().getDigits());
        entity.setPeriodSeconds((short) properties.getMfa().getTotp().getTimeStepSeconds());
        entity.setAlgorithm("HmacSHA1");
        credentials.save(entity);

        String base32 = Base32.encode(secret);
        String otpAuthUri = buildOtpAuthUri(user, base32);
        java.util.Arrays.fill(secret, (byte) 0);
        return new EnrollmentResult(entity.getId(), base32, otpAuthUri);
    }

    @Transactional
    public List<String> confirmTotpEnrollment(UUID userId, UUID factorId, String code) {
        MfaCredentialEntity entity = credentials.findById(factorId)
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new PortalException.NotFound("MFA factor not found"));
        if (entity.isActivated()) {
            throw new PortalException.Conflict("MFA factor already activated");
        }
        long step = verifyTotpInternal(entity, code);
        entity.setLastVerifiedStep(step);
        entity.setActivated(true);
        entity.setActivatedAt(OffsetDateTime.now());
        entity.setLastUsedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        credentials.save(entity);

        UserEntity user = users.findById(userId).orElseThrow();
        user.setMfaEnabled(true);
        user.setUpdatedAt(OffsetDateTime.now());

        return regenerateRecoveryCodesInternal(userId);
    }

    /**
     * Verifies a TOTP code at MFA challenge time. Returns true on success and
     * updates the last-verified step to prevent replay.
     */
    @Transactional
    public boolean verifyTotp(UUID userId, String code) {
        Optional<MfaCredentialEntity> opt = credentials
                .findByUserIdAndTypeAndActivatedTrue(userId, TOTP);
        if (opt.isEmpty()) return false;
        MfaCredentialEntity entity = opt.get();
        try {
            long step = verifyTotpInternal(entity, code);
            entity.setLastVerifiedStep(step);
            entity.setLastUsedAt(OffsetDateTime.now());
            entity.setUpdatedAt(OffsetDateTime.now());
            credentials.save(entity);
            return true;
        } catch (PortalException.Validation e) {
            return false;
        }
    }

    private long verifyTotpInternal(MfaCredentialEntity entity, String code) {
        byte[] secret = decryptSecret(entity);
        try {
            long step = totp.verify(secret, code, Instant.now(),
                    properties.getMfa().getTotp().getWindow());
            if (step < 0) {
                throw new PortalException.Validation("Invalid MFA code");
            }
            if (entity.getLastVerifiedStep() != null && step <= entity.getLastVerifiedStep()) {
                throw new PortalException.Validation("MFA code already used");
            }
            return step;
        } finally {
            java.util.Arrays.fill(secret, (byte) 0);
        }
    }

    @Transactional
    public boolean consumeRecoveryCode(UUID userId, String code) {
        if (code == null || code.isBlank()) return false;
        String normalized = code.trim().replace("-", "").toLowerCase();
        List<MfaRecoveryCodeEntity> unused = recoveryCodes.findUnusedByUserId(userId);
        for (MfaRecoveryCodeEntity candidate : unused) {
            if (passwordEncoder.matches(normalized, candidate.getCodeHash())) {
                candidate.setUsedAt(OffsetDateTime.now());
                recoveryCodes.save(candidate);
                return true;
            }
        }
        return false;
    }

    @Transactional
    public List<String> regenerateRecoveryCodes(UUID userId) {
        return regenerateRecoveryCodesInternal(userId);
    }

    private List<String> regenerateRecoveryCodesInternal(UUID userId) {
        recoveryCodes.deleteAllForUser(userId);
        PortalProperties.RecoveryCodes cfg = properties.getMfa().getRecoveryCodes();
        List<String> plain = new ArrayList<>();
        for (int i = 0; i < cfg.getCount(); i++) {
            String raw = randomAlphanumeric(cfg.getLength());
            plain.add(formatRecoveryCode(raw));
            MfaRecoveryCodeEntity entity = new MfaRecoveryCodeEntity(
                    UUID.randomUUID(), userId, passwordEncoder.encode(raw));
            recoveryCodes.save(entity);
        }
        return plain;
    }

    @Transactional(readOnly = true)
    public long remainingRecoveryCodes(UUID userId) {
        return recoveryCodes.countByUserIdAndUsedAtIsNull(userId);
    }

    @Transactional(readOnly = true)
    public List<MfaCredentialEntity> listFactors(UUID userId) {
        return credentials.findByUserId(userId);
    }

    @Transactional
    public void deleteFactor(UUID userId, UUID factorId) {
        MfaCredentialEntity entity = credentials.findById(factorId)
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new PortalException.NotFound("MFA factor not found"));
        credentials.delete(entity);
        if (credentials.findByUserId(userId).isEmpty()) {
            recoveryCodes.deleteAllForUser(userId);
            UserEntity user = users.findById(userId).orElseThrow();
            user.setMfaEnabled(false);
            user.setUpdatedAt(OffsetDateTime.now());
        }
    }

    @Transactional
    public void adminResetMfa(UUID targetUserId) {
        credentials.deleteAllForUser(targetUserId);
        recoveryCodes.deleteAllForUser(targetUserId);
        users.findById(targetUserId).ifPresent(u -> {
            u.setMfaEnabled(false);
            u.setUpdatedAt(OffsetDateTime.now());
        });
    }

    private byte[] decryptSecret(MfaCredentialEntity entity) {
        SecretEncryptionService.Encrypted enc = new SecretEncryptionService.Encrypted(
                entity.getEncryptionKeyId(),
                entity.getSecretIv(),
                entity.getSecretCiphertext(),
                entity.getSecretTag());
        return encryption.decrypt(enc);
    }

    private String buildOtpAuthUri(UserEntity user, String base32Secret) {
        String issuer = properties.getMfa().getTotp().getIssuer();
        String label = URLEncoder.encode(issuer, StandardCharsets.UTF_8) + ":"
                + URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8);
        return "otpauth://totp/" + label
                + "?secret=" + base32Secret
                + "&issuer=" + URLEncoder.encode(issuer, StandardCharsets.UTF_8)
                + "&algorithm=SHA1"
                + "&digits=" + properties.getMfa().getTotp().getDigits()
                + "&period=" + properties.getMfa().getTotp().getTimeStepSeconds();
    }

    private String randomAlphanumeric(int length) {
        char[] alphabet = "23456789abcdefghjkmnpqrstuvwxyz".toCharArray();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet[secureRandom.nextInt(alphabet.length)]);
        }
        return sb.toString();
    }

    private static String formatRecoveryCode(String raw) {
        if (raw.length() <= 5) return raw;
        int mid = raw.length() / 2;
        return raw.substring(0, mid) + "-" + raw.substring(mid);
    }
}
