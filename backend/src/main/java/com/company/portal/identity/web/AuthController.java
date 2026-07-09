package com.company.portal.identity.web;

import com.company.portal.identity.application.AuthenticationService;
import com.company.portal.identity.application.AuthenticationService.LoginOutcome;
import com.company.portal.identity.application.MfaService;
import com.company.portal.identity.application.PasswordService;
import com.company.portal.identity.application.UserMapper;
import com.company.portal.identity.security.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Public authentication endpoints. All paths under {@code /api/v1/auth/**}
 * are permitted in {@link com.company.portal.shared.security.SecurityConfig}
 * so unauthenticated callers can log in and recover credentials.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authService;
    private final PasswordService passwordService;
    private final MfaService mfaService;
    private final UserMapper userMapper;
    private final com.company.portal.identity.application.LoginChallengeStore challengeStore;

    public AuthController(AuthenticationService authService,
                          PasswordService passwordService,
                          MfaService mfaService,
                          UserMapper userMapper,
                          com.company.portal.identity.application.LoginChallengeStore challengeStore) {
        this.authService = authService;
        this.passwordService = passwordService;
        this.mfaService = mfaService;
        this.userMapper = userMapper;
        this.challengeStore = challengeStore;
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LoginResponse login(@RequestBody @Valid LoginRequest request, HttpServletRequest http) {
        LoginOutcome outcome = authService.login(request.username(), request.password(), http);
        if (outcome.status() == LoginOutcome.Status.MFA_REQUIRED) {
            return LoginResponse.mfaRequired(new MfaChallenge(
                    outcome.challengeId(), List.of("TOTP", "RECOVERY_CODE"),
                    OffsetDateTime.now().plusMinutes(5)));
        }
        return LoginResponse.authenticated(userMapper.toDto(outcome.user()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authService.logout(request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping(value = "/password/forgot", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> forgot(@RequestBody @Valid ForgotPasswordRequest request,
                                                       HttpServletRequest http) {
        passwordService.requestPasswordReset(request.email(),
                RequestContext.clientIp(http), RequestContext.userAgent(http));
        return ResponseEntity.accepted().body(Map.of(
                "message", "If the account exists, a reset link has been sent."));
    }

    @PostMapping(value = "/password/reset", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> reset(@RequestBody @Valid ResetPasswordRequest request) {
        passwordService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password has been reset."));
    }

    @PostMapping(value = "/mfa/verify", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LoginResponse verifyMfa(@RequestBody @Valid MfaVerifyRequest request, HttpServletRequest http) {
        java.util.UUID userId = challengeStore.peek(request.challengeId());
        boolean ok = userId != null && mfaService.verifyTotp(userId, request.code());
        var user = authService.completeMfaLogin(request.challengeId(), ok, http);
        return LoginResponse.authenticated(userMapper.toDto(user));
    }

    @PostMapping(value = "/mfa/recovery", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LoginResponse verifyRecoveryCode(@RequestBody @Valid MfaRecoveryRequest request,
                                            HttpServletRequest http) {
        java.util.UUID userId = challengeStore.peek(request.challengeId());
        boolean ok = userId != null && mfaService.consumeRecoveryCode(userId, request.recoveryCode());
        var user = authService.completeMfaLogin(request.challengeId(), ok, http);
        return LoginResponse.authenticated(userMapper.toDto(user));
    }

    @GetMapping("/csrf")
    public CsrfTokenResponse csrf(@RequestAttribute(name = "_csrf", required = false) CsrfToken token) {
        // Always advertise the Angular/OpenAPI header name. Spring Security 7's
        // CookieCsrfTokenRepository defaults to X-XSRF-TOKEN; keep the response
        // contract stable even if a deferred token reports a different name.
        if (token == null) {
            return new CsrfTokenResponse("X-XSRF-TOKEN", "");
        }
        return new CsrfTokenResponse("X-XSRF-TOKEN", token.getToken());
    }

    public record LoginRequest(@NotBlank @Email String username,
                               @NotBlank @Size(min = 8, max = 200) String password,
                               boolean rememberDevice) { }

    public record LoginResponse(String status, UserDto user, Object session, MfaChallenge mfaChallenge) {
        public static LoginResponse authenticated(UserDto user) {
            return new LoginResponse("AUTHENTICATED", user, null, null);
        }
        public static LoginResponse mfaRequired(MfaChallenge challenge) {
            return new LoginResponse("MFA_REQUIRED", null, null, challenge);
        }
    }

    public record MfaChallenge(String challengeId, List<String> methods, OffsetDateTime expiresAt) { }

    public record ForgotPasswordRequest(@NotBlank @Email String email) { }

    public record ResetPasswordRequest(@NotBlank String token,
                                       @NotBlank @Size(min = 12, max = 200) String newPassword) { }

    public record MfaVerifyRequest(@NotBlank String challengeId,
                                   @NotBlank @Size(min = 6, max = 8) String code,
                                   boolean rememberDevice) { }

    public record MfaRecoveryRequest(@NotBlank String challengeId,
                                     @NotBlank String recoveryCode,
                                     boolean rememberDevice) { }

    public record CsrfTokenResponse(String headerName, String token) { }
}
