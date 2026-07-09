package com.company.portal.identity.mfa;

/**
 * Minimal Base32 (RFC 4648) codec used to render TOTP secrets in the format
 * authenticator apps expect. Uppercase output, no padding on encode, tolerant
 * of casing and padding on decode.
 */
public final class Base32 {

    private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
    private static final int[] REVERSE = new int[128];

    static {
        for (int i = 0; i < REVERSE.length; i++) REVERSE[i] = -1;
        for (int i = 0; i < ALPHABET.length; i++) REVERSE[ALPHABET[i]] = i;
    }

    private Base32() { }

    public static String encode(byte[] data) {
        if (data == null || data.length == 0) return "";
        StringBuilder sb = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xff);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0x1f;
                sb.append(ALPHABET[index]);
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1f;
            sb.append(ALPHABET[index]);
        }
        return sb.toString();
    }

    public static byte[] decode(String s) {
        if (s == null || s.isEmpty()) return new byte[0];
        String cleaned = s.replace("=", "").replace(" ", "").toUpperCase();
        int outputLen = cleaned.length() * 5 / 8;
        byte[] out = new byte[outputLen];
        int buffer = 0;
        int bitsLeft = 0;
        int idx = 0;
        for (char c : cleaned.toCharArray()) {
            if (c >= REVERSE.length || REVERSE[c] < 0) {
                throw new IllegalArgumentException("Invalid Base32 character: " + c);
            }
            buffer = (buffer << 5) | REVERSE[c];
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out[idx++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xff);
                bitsLeft -= 8;
            }
        }
        return out;
    }
}
