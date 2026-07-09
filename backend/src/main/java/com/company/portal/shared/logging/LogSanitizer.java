package com.company.portal.shared.logging;

import java.util.regex.Pattern;

/**
 * Utilities for scrubbing values before they hit logs or client-facing payloads.
 * Removes CR/LF (to prevent log-injection) and obviously sensitive tokens.
 */
public final class LogSanitizer {

    private LogSanitizer() { }

    private static final Pattern CRLF = Pattern.compile("[\\r\\n]");
    private static final Pattern BEARER = Pattern.compile("(?i)(bearer\\s+)[A-Za-z0-9._~+/=-]+");
    private static final Pattern PASSWORD_LIKE =
            Pattern.compile("(?i)(password|passwd|secret|token|api[-_]?key)\\s*[=:]\\s*[^\\s,;]+");
    private static final int MAX_LEN = 2048;

    public static String safe(String input) {
        if (input == null) {
            return "";
        }
        String value = input;
        if (value.length() > MAX_LEN) {
            value = value.substring(0, MAX_LEN) + "…";
        }
        value = CRLF.matcher(value).replaceAll(" ");
        value = BEARER.matcher(value).replaceAll("$1[REDACTED]");
        value = PASSWORD_LIKE.matcher(value).replaceAll("$1=[REDACTED]");
        return value;
    }

    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return "*@" + (at >= 0 ? email.substring(at + 1) : "");
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
