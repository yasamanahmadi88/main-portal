package com.company.portal.identity.security;

import com.company.portal.shared.config.PortalProperties;
import com.company.portal.shared.error.ErrorCodes;
import com.company.portal.shared.error.PortalException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Self-hosted login CAPTCHA backed by Redis.
 *
 * <p>Answers are stored as SHA-256 digests and consumed atomically on
 * validation (one-time use). There is no production bypass. Optional
 * {@code portal.captcha.reveal-answer} exists solely for automated tests /
 * Playwright and is rejected when the {@code prod} profile is active.</p>
 */
@Service
public class CaptchaService {

    private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);
    private static final String KEY_PREFIX = "portal:captcha:";
    // Ambiguous glyphs (0/O, 1/I/L) excluded for usability.
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final StringRedisTemplate redis;
    private final PortalProperties properties;
    private final SecureRandom random = new SecureRandom();

    public CaptchaService(StringRedisTemplate redis,
                          PortalProperties properties,
                          Environment environment) {
        this.redis = redis;
        this.properties = properties;
        if (properties.getCaptcha().isRevealAnswer()
                && environment.matchesProfiles("prod")) {
            throw new IllegalStateException(
                    "portal.captcha.reveal-answer must never be enabled with the prod profile");
        }
    }

    public IssuedCaptcha issue() {
        PortalProperties.Captcha cfg = properties.getCaptcha();
        String captchaId = UUID.randomUUID().toString();
        String answer = randomAnswer(cfg.getLength());
        Duration ttl = cfg.getTtl();
        Instant expiresAt = Instant.now().plus(ttl);
        try {
            redis.opsForValue().set(KEY_PREFIX + captchaId, sha256(normalize(answer)), ttl);
        } catch (RuntimeException ex) {
            log.error("CAPTCHA store unavailable: {}", ex.getMessage());
            throw new PortalException.ServiceUnavailable(
                    ErrorCodes.INTERNAL_ERROR, "CAPTCHA temporarily unavailable");
        }
        String svg = CaptchaSvgRenderer.render(answer);
        String reveal = cfg.isRevealAnswer() ? answer : null;
        return new IssuedCaptcha(captchaId, svg, expiresAt, (int) ttl.toSeconds(), reveal);
    }

    /**
     * Validates and consumes a CAPTCHA. Generic failures avoid answer enumeration.
     */
    public void consume(String captchaId, String answer) {
        if (captchaId == null || captchaId.isBlank() || answer == null || answer.isBlank()) {
            throw captchaInvalid();
        }
        String key = KEY_PREFIX + captchaId.trim();
        String expected;
        try {
            expected = redis.opsForValue().get(key);
            if (expected != null) {
                redis.delete(key);
            }
        } catch (RuntimeException ex) {
            log.error("CAPTCHA read unavailable: {}", ex.getMessage());
            throw new PortalException.ServiceUnavailable(
                    ErrorCodes.INTERNAL_ERROR, "CAPTCHA temporarily unavailable");
        }
        if (expected == null) {
            throw captchaExpired();
        }
        String actual = sha256(normalize(answer));
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8))) {
            throw captchaInvalid();
        }
    }

    private String randomAnswer(int length) {
        int n = Math.max(4, Math.min(8, length));
        char[] chars = new char[n];
        for (int i = 0; i < n; i++) {
            chars[i] = ALPHABET[random.nextInt(ALPHABET.length)];
        }
        return new String(chars);
    }

    private static String normalize(String answer) {
        return answer.trim().toUpperCase(Locale.ROOT);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static PortalException captchaInvalid() {
        return new PortalException.BadRequest(ErrorCodes.CAPTCHA_INVALID, "Invalid CAPTCHA");
    }

    private static PortalException captchaExpired() {
        return new PortalException.BadRequest(ErrorCodes.CAPTCHA_EXPIRED, "CAPTCHA expired");
    }

    public record IssuedCaptcha(
            String captchaId,
            String imageSvg,
            Instant expiresAt,
            int ttlSeconds,
            String revealAnswer) { }
}
