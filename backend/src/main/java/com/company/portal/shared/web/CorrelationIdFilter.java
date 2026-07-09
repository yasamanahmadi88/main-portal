package com.company.portal.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads {@code X-Correlation-ID} from the request (or generates one) and puts
 * it, plus a per-request id, into the SLF4J {@link MDC} for structured logs.
 *
 * <p>The filter runs early (order = HIGHEST_PRECEDENCE + 10) so subsequent
 * filters and controllers see MDC values populated.</p>
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter implements Ordered {

    public static final String HEADER = "X-Correlation-ID";
    public static final String REQUEST_HEADER = "X-Request-ID";
    public static final String MDC_CORRELATION = "correlation_id";
    public static final String MDC_REQUEST = "request_id";
    private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = sanitize(request.getHeader(HEADER));
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        String requestId = sanitize(request.getHeader(REQUEST_HEADER));
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        try {
            MDC.put(MDC_CORRELATION, correlationId);
            MDC.put(MDC_REQUEST, requestId);
            response.setHeader(HEADER, correlationId);
            response.setHeader(REQUEST_HEADER, requestId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_CORRELATION);
            MDC.remove(MDC_REQUEST);
        }
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return SAFE.matcher(trimmed).matches() ? trimmed : null;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
