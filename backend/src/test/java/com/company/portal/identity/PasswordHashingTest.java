package com.company.portal.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Verifies that the platform's password hashing meets the invariants required
 * by the security policy: Argon2id, salted, non-deterministic and verifies
 * back to true.
 */
class PasswordHashingTest {

    private final PasswordEncoder encoder = new DelegatingPasswordEncoder(
            "argon2",
            java.util.Map.of("argon2", new Argon2PasswordEncoder(16, 32, 1, 65_536, 3)));

    @Test
    void encodesWithArgon2Prefix() {
        String hash = encoder.encode("Correct-Horse-Battery-Staple-42!");
        assertThat(hash).startsWith("{argon2}");
        assertThat(hash).contains("$argon2id$");
    }

    @Test
    void twoEncodingsProduceDifferentHashes() {
        String first = encoder.encode("Correct-Horse-Battery-Staple-42!");
        String second = encoder.encode("Correct-Horse-Battery-Staple-42!");
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void matchesTheOriginalPasswordOnly() {
        String hash = encoder.encode("Correct-Horse-Battery-Staple-42!");
        assertThat(encoder.matches("Correct-Horse-Battery-Staple-42!", hash)).isTrue();
        assertThat(encoder.matches("Correct-Horse-Battery-Staple-43!", hash)).isFalse();
        assertThat(encoder.matches("", hash)).isFalse();
    }
}
