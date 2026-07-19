package com.company.portal.shared.crypto;

import com.company.portal.shared.config.PortalProperties;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Envelope-style symmetric encryption for at-rest secrets (MFA seeds, recovery
 * codes, other small blobs). Uses AES-GCM with a 96-bit random IV per message
 * and a 128-bit auth tag. Keys are addressed by ID so we can rotate: the key
 * ID is stored alongside the ciphertext.
 *
 * <p>In production the key material would come from a KMS (AWS KMS, GCP KMS,
 * Vault Transit). Here we load Base64-encoded 32-byte keys from
 * {@code portal.mfa.encryption.keys.<id>}. Never log the plaintext or the key.
 * </p>
 */
@Component
public class SecretEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(SecretEncryptionService.class);

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;
    private static final int AES_KEY_BYTES = 32;

    private final PortalProperties properties;
    private final Map<String, SecretKey> keyRing = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    private String activeKeyId;

    public SecretEncryptionService(PortalProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void initialize() {
        PortalProperties.Encryption cfg = properties.getMfa().getEncryption();
        if (cfg == null || cfg.getKeys() == null || cfg.getKeys().isEmpty()) {
            throw new IllegalStateException(
                    "portal.mfa.encryption.keys is empty; at least one key must be configured");
        }
        cfg.getKeys().forEach((id, encoded) -> keyRing.put(id, decodeKey(id, encoded)));
        this.activeKeyId = Objects.requireNonNull(cfg.getActiveKeyId(),
                "portal.mfa.encryption.active-key-id must be set");
        if (!keyRing.containsKey(activeKeyId)) {
            throw new IllegalStateException("Active key id '" + activeKeyId
                    + "' is not present in portal.mfa.encryption.keys");
        }
        log.info("Secret encryption initialized: activeKeyId={}, keys={}",
                activeKeyId, keyRing.keySet());
    }

    public String getActiveKeyId() {
        return activeKeyId;
    }

    public Encrypted encrypt(byte[] plaintext) {
        return encrypt(plaintext, null);
    }

    public Encrypted encrypt(byte[] plaintext, byte[] aad) {
        Objects.requireNonNull(plaintext, "plaintext");
        SecretKey key = requireKey(activeKeyId);
        byte[] iv = new byte[GCM_IV_BYTES];
        secureRandom.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            if (aad != null) {
                cipher.updateAAD(aad);
            }
            byte[] combined = cipher.doFinal(plaintext);
            int tagLenBytes = GCM_TAG_BITS / 8;
            byte[] ciphertext = new byte[combined.length - tagLenBytes];
            byte[] tag = new byte[tagLenBytes];
            System.arraycopy(combined, 0, ciphertext, 0, ciphertext.length);
            System.arraycopy(combined, ciphertext.length, tag, 0, tagLenBytes);
            return new Encrypted(activeKeyId, iv, ciphertext, tag);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Encryption failure", e);
        }
    }

    public byte[] decrypt(Encrypted encrypted) {
        return decrypt(encrypted, null);
    }

    public byte[] decrypt(Encrypted encrypted, byte[] aad) {
        Objects.requireNonNull(encrypted, "encrypted");
        SecretKey key = requireKey(encrypted.keyId());
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, encrypted.iv()));
            if (aad != null) {
                cipher.updateAAD(aad);
            }
            byte[] combined = new byte[encrypted.ciphertext().length + encrypted.tag().length];
            System.arraycopy(encrypted.ciphertext(), 0, combined, 0, encrypted.ciphertext().length);
            System.arraycopy(encrypted.tag(), 0, combined, encrypted.ciphertext().length,
                    encrypted.tag().length);
            return cipher.doFinal(combined);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Decryption failure", e);
        }
    }

    public String encryptToString(String plaintext) {
        Encrypted e = encrypt(plaintext.getBytes(StandardCharsets.UTF_8));
        Base64.Encoder b64 = Base64.getEncoder().withoutPadding();
        return e.keyId() + ":" + b64.encodeToString(e.iv())
                + ":" + b64.encodeToString(e.ciphertext())
                + ":" + b64.encodeToString(e.tag());
    }

    public String decryptToString(String encoded) {
        String[] parts = encoded.split(":", 4);
        if (parts.length != 4) {
            throw new IllegalArgumentException("Malformed encrypted value");
        }
        Base64.Decoder b64 = Base64.getDecoder();
        Encrypted e = new Encrypted(parts[0], b64.decode(parts[1]),
                b64.decode(parts[2]), b64.decode(parts[3]));
        return new String(decrypt(e), StandardCharsets.UTF_8);
    }

    private SecretKey requireKey(String keyId) {
        SecretKey key = keyRing.get(keyId);
        if (key == null) {
            throw new IllegalStateException("No encryption key configured for id '" + keyId + "'");
        }
        return key;
    }

    private static SecretKey decodeKey(String id, String encoded) {
        Objects.requireNonNull(encoded, () -> "encryption key '" + id + "' has null value");
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(encoded.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Encryption key '" + id + "' is not valid Base64", e);
        }
        if (raw.length != AES_KEY_BYTES) {
            throw new IllegalStateException("Encryption key '" + id + "' must decode to "
                    + AES_KEY_BYTES + " bytes (AES-256), got " + raw.length);
        }
        return new SecretKeySpec(raw, "AES");
    }

    /**
     * Immutable ciphertext envelope. {@code keyId} identifies which key
     * decrypts it, {@code iv} is the 96-bit GCM nonce, {@code ciphertext}
     * and {@code tag} are the AES-GCM outputs separated for storage.
     */
    public record Encrypted(String keyId, byte[] iv, byte[] ciphertext, byte[] tag) { }
}
