package com.company.portal.shared.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.portal.shared.config.PortalProperties;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/**
 * Exercises {@link SecretEncryptionService} in isolation so we can guarantee
 * AES-GCM round-trip and per-message IV freshness — the two properties that
 * the MFA service relies on to keep TOTP seeds safe at rest.
 */
class SecretEncryptionServiceTest {

    @Test
    void encryptDecryptRoundTrip() {
        SecretEncryptionService svc = initService();
        byte[] plain = "s3cr3t-seed-value".getBytes(StandardCharsets.UTF_8);
        SecretEncryptionService.Encrypted encrypted = svc.encrypt(plain);
        assertThat(encrypted.iv()).hasSize(12);
        assertThat(encrypted.tag()).hasSize(16);
        assertThat(svc.decrypt(encrypted)).containsExactly(plain);
    }

    @Test
    void differentIvsForRepeatedEncryption() {
        SecretEncryptionService svc = initService();
        byte[] plain = "another-seed".getBytes(StandardCharsets.UTF_8);
        SecretEncryptionService.Encrypted a = svc.encrypt(plain);
        SecretEncryptionService.Encrypted b = svc.encrypt(plain);
        assertThat(a.iv()).isNotEqualTo(b.iv());
        assertThat(a.ciphertext()).isNotEqualTo(b.ciphertext());
    }

    @Test
    void stringRoundTripPreservesUnicode() {
        SecretEncryptionService svc = initService();
        String encoded = svc.encryptToString("سلام Portal 42");
        assertThat(svc.decryptToString(encoded)).isEqualTo("سلام Portal 42");
    }

    private static SecretEncryptionService initService() {
        PortalProperties properties = new PortalProperties();
        String key = Base64.getEncoder().encodeToString(new byte[32]);
        properties.getMfa().getEncryption().setActiveKeyId("k1");
        properties.getMfa().getEncryption().getKeys().put("k1", key);
        SecretEncryptionService service = new SecretEncryptionService(properties);
        service.initialize();
        return service;
    }
}
