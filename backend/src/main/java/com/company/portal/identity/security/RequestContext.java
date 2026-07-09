package com.company.portal.identity.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.slf4j.MDC;

/**
 * Small helpers for pulling per-request context (IP, user agent, correlation
 * id) out of the current {@link HttpServletRequest} in a way that respects
 * the framework-forwarded headers.
 */
public final class RequestContext {

    private RequestContext() { }

    public static String clientIp(HttpServletRequest request) {
        if (request == null) return null;
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }

    public static String userAgent(HttpServletRequest request) {
        return request == null ? null : truncate(request.getHeader("User-Agent"), 512);
    }

    public static String correlationId() {
        return MDC.get("correlation_id");
    }

    public static String requestId() {
        return MDC.get("request_id");
    }

    public static String traceId() {
        return MDC.get("trace_id");
    }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
