package com.company.portal.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.portal.shared.error.ErrorCodes;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

/**
 * Directly invokes the security-filter error handlers to confirm they emit
 * problem+json bodies with the expected {@code code} field. The full CSRF
 * flow lives in an integration test guarded by Docker availability.
 */
class ProblemDetailAuthErrorsTest {

    @Test
    void authenticationEntryPointWritesProblemJson() throws Exception {
        ProblemDetailAuthenticationEntryPoint entryPoint = new ProblemDetailAuthenticationEntryPoint();
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/me");
        MockHttpServletResponse res = new MockHttpServletResponse();

        entryPoint.commence(req, res, new AuthenticationException("no session") { });

        assertThat(res.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(res.getContentType()).contains("application/problem+json");
        String body = res.getContentAsString();
        assertThat(body).contains("\"status\":401");
        assertThat(body).contains("\"code\":\"" + ErrorCodes.UNAUTHENTICATED + "\"");
        assertThat(body).contains("\"instance\":\"/api/v1/me\"");
    }

    @Test
    void accessDeniedHandlerFlagsCsrfMessages() throws Exception {
        ProblemDetailAccessDeniedHandler handler = new ProblemDetailAccessDeniedHandler();
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/users");
        MockHttpServletResponse res = new MockHttpServletResponse();

        handler.handle(req, res, new AccessDeniedException("Invalid CSRF token"));

        assertThat(res.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
        String body = res.getContentAsString();
        assertThat(body).contains(ErrorCodes.CSRF_INVALID);
    }

    @Test
    void accessDeniedHandlerFallsBackToForbiddenCode() throws Exception {
        ProblemDetailAccessDeniedHandler handler = new ProblemDetailAccessDeniedHandler();
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/roles");
        MockHttpServletResponse res = new MockHttpServletResponse();

        handler.handle(req, res, new AccessDeniedException("Access is denied"));

        String body = res.getContentAsString();
        assertThat(body).contains(ErrorCodes.FORBIDDEN);
    }
}
