package com.company.portal.identity.mfa;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Pure-Java RFC 6238 TOTP generator/verifier. No external crypto dependency
 * — HMAC-SHA1/256/512 provided by JDK's {@link javax.crypto.Mac}.
 *
 * <p>Verification supports a symmetric time-step window (±window) to tolerate
 * modest client clock skew. Callers should record the last successfully
 * verified step and reject any code from a step that is ≤ that step to
 * prevent replay within the window.</p>
 */
public final class TotpCodeGenerator {

    private final String hmacAlgorithm;
    private final int digits;
    private final Duration timeStep;

    public TotpCodeGenerator(String algorithm, int digits, Duration timeStep) {
        if (digits < 6 || digits > 8) {
            throw new IllegalArgumentException("digits must be 6, 7 or 8");
        }
        if (timeStep == null || timeStep.isZero() || timeStep.isNegative()) {
            throw new IllegalArgumentException("timeStep must be positive");
        }
        this.hmacAlgorithm = mapAlgorithm(algorithm);
        this.digits = digits;
        this.timeStep = timeStep;
    }

    public String generate(byte[] key, Instant when) {
        long step = toStep(when);
        return format(hotp(key, step));
    }

    /**
     * Verifies {@code candidate} against the current time step and up to
     * {@code window} steps on either side. Returns the matched step (as
     * absolute step index) or {@code -1} for no match.
     */
    public long verify(byte[] key, String candidate, Instant when, int window) {
        if (candidate == null) return -1L;
        String normalized = candidate.trim();
        if (normalized.length() != digits) return -1L;
        long baseStep = toStep(when);
        for (int offset = -window; offset <= window; offset++) {
            long step = baseStep + offset;
            if (step < 0) continue;
            String expected = format(hotp(key, step));
            if (constantTimeEquals(expected, normalized)) {
                return step;
            }
        }
        return -1L;
    }

    public long toStep(Instant when) {
        return when.getEpochSecond() / timeStep.getSeconds();
    }

    private int hotp(byte[] key, long counter) {
        try {
            Mac mac = Mac.getInstance(hmacAlgorithm);
            mac.init(new SecretKeySpec(key, hmacAlgorithm));
            byte[] counterBytes = ByteBuffer.allocate(Long.BYTES).putLong(counter).array();
            byte[] hash = mac.doFinal(counterBytes);
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            int modulus = 1;
            for (int i = 0; i < digits; i++) {
                modulus *= 10;
            }
            return binary % modulus;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC not available: " + hmacAlgorithm, e);
        }
    }

    private String format(int code) {
        String s = Integer.toString(code);
        StringBuilder sb = new StringBuilder(digits);
        for (int i = s.length(); i < digits; i++) sb.append('0');
        sb.append(s);
        return sb.toString();
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }

    private static String mapAlgorithm(String algorithm) {
        if (algorithm == null) return "HmacSHA1";
        return switch (algorithm) {
            case "HmacSHA1", "HmacSHA256", "HmacSHA512" -> algorithm;
            case "SHA1" -> "HmacSHA1";
            case "SHA256" -> "HmacSHA256";
            case "SHA512" -> "HmacSHA512";
            default -> throw new IllegalArgumentException("Unsupported TOTP algorithm: " + algorithm);
        };
    }
}
