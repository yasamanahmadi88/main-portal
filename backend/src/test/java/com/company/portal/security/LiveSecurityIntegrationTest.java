package com.company.portal.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.portal.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Live security checks against Testcontainers PostgreSQL + Redis (when Docker is available).
 * Complements shell scripts that run against docker compose in CI.
 */
@EnabledIf(
    value = "com.company.portal.support.DockerAvailability#isAvailable",
    disabledReason = "Docker unavailable for Testcontainers")
@AutoConfigureMockMvc
class LiveSecurityIntegrationTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;

  @DynamicPropertySource
  static void bootstrapAdmin(DynamicPropertyRegistry registry) {
    registry.add("portal.bootstrap.enabled", () -> "true");
    registry.add("portal.bootstrap.admin-email", () -> "admin@example.com");
    registry.add("portal.bootstrap.admin-password", () -> "ChangeMeNow!123");
    registry.add("portal.bootstrap.admin-display-name", () -> "Test Admin");
  }

  @Test
  void csrfIsRequiredForLogin() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"username":"nobody@example.com","password":"x","rememberDevice":false}
                    """))
        .andExpect(status().isForbidden());
  }

  @Test
  void loginSetsHttpOnlySameSiteSessionCookie() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"username":"admin@example.com","password":"ChangeMeNow!123","rememberDevice":false}
                    """))
        .andExpect(status().isOk())
        .andExpect(cookie().exists("PORTAL_SESSION"))
        .andExpect(cookie().httpOnly("PORTAL_SESSION", true))
        .andExpect(cookie().sameSite("PORTAL_SESSION", "Lax"));
  }

  @Test
  void loginPersistsSecurityContextForSubsequentMeRequest() throws Exception {
    var login =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"username":"admin@example.com","password":"ChangeMeNow!123","rememberDevice":false}
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("AUTHENTICATED"))
            .andReturn();

    String sessionCookie = login.getResponse().getCookie("PORTAL_SESSION").getValue();
    mockMvc
        .perform(get("/api/v1/me").cookie(new jakarta.servlet.http.Cookie("PORTAL_SESSION", sessionCookie)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("admin@example.com"));
  }

  @Test
  void anonymousCannotAccessUsersApi() throws Exception {
    mockMvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized());
  }

  @Test
  void loginFailureDoesNotDiscloseAccountExistence() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"username":"missing-user@example.com","password":"wrong-password","rememberDevice":false}
                    """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.detail").value("Invalid credentials"));
  }

  @Test
  void bootstrapAdminPasswordIsArgon2id() {
    String hash =
        jdbcTemplate.queryForObject(
            "select password_hash from users where email = ?", String.class, "admin@example.com");
    assertThat(hash).contains("$argon2id$");
  }

  @Test
  void auditEventsCarryHashChainFieldsWhenPresent() {
    Integer count = jdbcTemplate.queryForObject("select count(*) from audit_events", Integer.class);
    if (count != null && count > 0) {
      Integer missing =
          jdbcTemplate.queryForObject(
              """
              select count(*) from audit_events
              where current_hash is null or length(current_hash) = 0
                 or sequence_number is null
              """,
              Integer.class);
      assertThat(missing).isZero();
    }
  }

  @Test
  void csrfEndpointReturnsTokenMetadata() throws Exception {
    mockMvc
        .perform(get("/api/v1/auth/csrf"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
        .andExpect(jsonPath("$.token").isNotEmpty());
  }
}
