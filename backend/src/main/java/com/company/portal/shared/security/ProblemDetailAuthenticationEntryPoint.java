package com.company.portal.shared.security;

import com.company.portal.shared.error.ErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Emits an RFC 9457 problem details response for 401 so unauthenticated
 * requests get a JSON body consistent with our exception handler instead of
 * an opaque 401 with no body.
 */
@Component
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        String correlationId = MDC.get("correlation_id");
        response.getWriter().write("{"
                + "\"type\":\"https://portal.company.com/problems/" + ErrorCodes.UNAUTHENTICATED + "\","
                + "\"title\":\"Unauthorized\","
                + "\"status\":401,"
                + "\"code\":\"" + ErrorCodes.UNAUTHENTICATED + "\","
                + "\"detail\":\"Authentication required\","
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
