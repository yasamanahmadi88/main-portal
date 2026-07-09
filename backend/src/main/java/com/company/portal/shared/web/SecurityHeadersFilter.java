package com.company.portal.shared.web;

import com.company.portal.shared.config.PortalProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Adds hardening HTTP response headers on every response. CSP, Referrer-Policy,
 * Permissions-Policy, X-Content-Type-Options, X-Frame-Options (defensive; CSP
 * frame-ancestors is the authoritative control), and — when the request is TLS
 * — Strict-Transport-Security.
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter implements Ordered {

    private final PortalProperties properties;

    public SecurityHeadersFilter(PortalProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        PortalProperties.Security sec = properties.getSecurity();
        setIfAbsent(response, "Content-Security-Policy", sec.getCsp());
        setIfAbsent(response, "Referrer-Policy", sec.getReferrerPolicy());
        setIfAbsent(response, "Permissions-Policy", sec.getPermissionsPolicy());
        setIfAbsent(response, "X-Content-Type-Options", "nosniff");
        setIfAbsent(response, "X-Frame-Options", "DENY");
        setIfAbsent(response, "Cross-Origin-Opener-Policy", "same-origin");
        setIfAbsent(response, "Cross-Origin-Resource-Policy", "same-origin");
        if (request.isSecure() && sec.getHstsMaxAgeSeconds() > 0) {
            setIfAbsent(response, "Strict-Transport-Security",
                    "max-age=" + sec.getHstsMaxAgeSeconds() + "; includeSubDomains");
        }
        filterChain.doFilter(request, response);
    }

    private static void setIfAbsent(HttpServletResponse response, String name, String value) {
        if (value != null && !value.isBlank() && !response.containsHeader(name)) {
            response.setHeader(name, value);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
