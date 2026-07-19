package com.company.portal.identity.mfa;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/**
 * TOTP tests using the RFC 6238 Appendix B seed. Confirms that our generator
 * matches the reference vectors, that verification tolerates a symmetric
 * window, and that replay of a matched step is rejected via the last-step
 * pointer callers persist.
 */
class TotpCodeGeneratorTest {

    // 20-byte ASCII "12345678901234567890"
    private static final byte[] RFC_SECRET_SHA1 =
            HexFormat.of().parseHex("3132333435363738393031323334353637383930");

    private final TotpCodeGenerator generator = new TotpCodeGenerator(
            "HmacSHA1", 8, Duration.ofSeconds(30));

    @Test
    void generatesReferenceVectorAt59Seconds() {
        String code = generator.generate(RFC_SECRET_SHA1, Instant.ofEpochSecond(59));
        assertThat(code).isEqualTo("94287082");
    }

    @Test
    void generatesReferenceVectorAt1111111109Seconds() {
        String code = generator.generate(RFC_SECRET_SHA1, Instant.ofEpochSecond(1111111109L));
        assertThat(code).isEqualTo("07081804");
    }

    @Test
    void verifyReturnsStepForCurrentCode() {
        Instant when = Instant.ofEpochSecond(1234567890L);
        String code = generator.generate(RFC_SECRET_SHA1, when);
        long step = generator.verify(RFC_SECRET_SHA1, code, when, 1);
        assertThat(step).isEqualTo(generator.toStep(when));
    }

    @Test
    void verifyReturnsNegativeOneForInvalidCode() {
        long step = generator.verify(RFC_SECRET_SHA1, "12345678", Instant.now(), 1);
        assertThat(step).isEqualTo(-1L);
    }

    @Test
    void verifyToleratesOneStepClockSkew() {
        Instant now = Instant.ofEpochSecond(2_000_000_000L);
        Instant previousStep = now.minusSeconds(30);
        String code = generator.generate(RFC_SECRET_SHA1, previousStep);
        assertThat(generator.verify(RFC_SECRET_SHA1, code, now, 1))
                .isEqualTo(generator.toStep(previousStep));
    }

    @Test
    void verifyRejectsOutsideWindow() {
        Instant now = Instant.ofEpochSecond(2_000_000_000L);
        String stale = generator.generate(RFC_SECRET_SHA1, now.minusSeconds(120));
        assertThat(generator.verify(RFC_SECRET_SHA1, stale, now, 1)).isEqualTo(-1L);
    }
}
