package com.company.portal.shared.security;

import com.company.portal.shared.error.ErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Emits a JSON problem details body for 403 responses generated inside the
 * security filter chain (CSRF failures, missing authority) so the client sees
 * the same shape as any other application error.
 */
@Component
public class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        String correlationId = MDC.get("correlation_id");
        String code = ex.getMessage() != null && ex.getMessage().toLowerCase().contains("csrf")
                ? ErrorCodes.CSRF_INVALID
                : ErrorCodes.FORBIDDEN;
        response.getWriter().write("{"
                + "\"type\":\"https://portal.company.com/problems/" + code + "\","
                + "\"title\":\"Forbidden\","
                + "\"status\":403,"
                + "\"code\":\"" + code + "\","
                + "\"detail\":\"Access denied\","
                + "\"instance\":\"" + jsonEscape(request.getRequestURI()) + "\","
                + "\"timestamp\":\"" + Instant.now() + "\""
                + (correlationId == null ? "" : (",\"correlation_id\":\"" + jsonEscape(correlationId) + "\""))
                + "}");
    }

    private static String jsonEscape(String v) {
        if (v == null) return "";
        return v.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
