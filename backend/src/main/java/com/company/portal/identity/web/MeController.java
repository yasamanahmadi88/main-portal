package com.company.portal.identity.web;

import com.company.portal.identity.application.CurrentUserService;
import com.company.portal.identity.application.MeService;
import com.company.portal.identity.application.MfaService;
import com.company.portal.identity.application.PasswordService;
import com.company.portal.identity.application.PreferenceService;
import com.company.portal.identity.application.PreferenceService.PreferencesSnapshot;
import com.company.portal.identity.application.SessionService;
import com.company.portal.identity.application.UserMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Self-service endpoints for the currently authenticated user. All endpoints
 * require {@code self:read} or {@code self:write} authorities plus a valid
 * session cookie.
 */
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final CurrentUserService currentUser;
    private final MeService meService;
    private final UserMapper userMapper;
    private final PreferenceService preferenceService;
    private final PasswordService passwordService;
    private final SessionService sessionService;
    private final MfaService mfaService;

    public MeController(CurrentUserService currentUser,
                        MeService meService,
                        UserMapper userMapper,
                        PreferenceService preferenceService,
                        PasswordService passwordService,
                        SessionService sessionService,
                        MfaService mfaService) {
        this.currentUser = currentUser;
        this.meService = meService;
        this.userMapper = userMapper;
        this.preferenceService = preferenceService;
        this.passwordService = passwordService;
        this.sessionService = sessionService;
        this.mfaService = mfaService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('self:read')")
    public UserDto me() {
        return meService.currentProfile(currentUserId());
    }

    @PatchMapping
    @PreAuthorize("hasAuthority('self:write')")
    public UserDto updateMe(@RequestBody @Valid UpdateMeRequest request) {
        return meService.updateProfile(currentUserId(), request.displayName(), request.email());
    }

    @GetMapping("/preferences")
    @PreAuthorize("hasAuthority('self:read')")
    public PreferencesDto getPreferences() {
        PreferencesSnapshot snapshot = preferenceService.getPreferences(currentUserId());
        return new PreferencesDto(snapshot.language(), snapshot.theme(),
                snapshot.timezone(), snapshot.density());
    }

    @PatchMapping("/preferences")
    @PreAuthorize("hasAuthority('self:write')")
    public PreferencesDto patchPreferences(@RequestBody @Valid PreferencesPatchRequest request) {
        PreferencesSnapshot snapshot = preferenceService.patchPreferences(
                currentUserId(), request.language(), request.theme(),
                request.timezone(), request.density());
        return new PreferencesDto(snapshot.language(), snapshot.theme(),
                snapshot.timezone(), snapshot.density());
    }

    @PostMapping("/password")
    @PreAuthorize("hasAuthority('self:write')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@RequestBody @Valid ChangePasswordRequest request) {
        passwordService.changePassword(currentUserId(),
                request.currentPassword(), request.newPassword());
    }

    @GetMapping("/sessions")
    @PreAuthorize("hasAuthority('self:read')")
    public List<SessionDto> listMySessions() {
        String currentSid = currentUser.currentSessionId().orElse(null);
        String username = meService.usernameForUserId(currentUserId()).orElse(null);
        return sessionService.listSessions(currentUserId()).stream()
                .map(s -> userMapper.toDto(s, currentSid, username))
                .toList();
    }

    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAuthority('self:write')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeMySession(@PathVariable String sessionId) {
        sessionService.revokeSession(currentUserId(), sessionId, "USER_REVOKED");
    }

    @PostMapping("/sessions/revoke-others")
    @PreAuthorize("hasAuthority('self:write')")
    public Map<String, Object> revokeOtherSessions() {
        int revoked = sessionService.revokeOtherSessions(currentUserId(),
                currentUser.currentSessionId().orElse(null));
        return Map.of("revokedCount", revoked);
    }

    @GetMapping("/mfa")
    @PreAuthorize("hasAuthority('self:read')")
    public List<MfaFactorDto> listMfa() {
        return mfaService.listFactors(currentUserId()).stream()
                .map(userMapper::toDto).toList();
    }

    @PostMapping("/mfa/totp/setup")
    @PreAuthorize("hasAuthority('self:write')")
    public MfaSetupDto setupTotp(@RequestBody(required = false) @Valid MfaSetupRequest request) {
        String label = request == null || request.name() == null ? "Authenticator app" : request.name();
        MfaService.EnrollmentResult r = mfaService.beginTotpEnrollment(currentUserId(), label);
        return new MfaSetupDto(r.factorId(), "TOTP", r.secretBase32(),
                r.otpAuthUri(), null, OffsetDateTime.now().plusMinutes(10));
    }

    @PostMapping("/mfa/totp/confirm")
    @PreAuthorize("hasAuthority('self:write')")
    public MfaEnrollmentResultDto confirmTotp(@RequestBody @Valid MfaConfirmRequest request) {
        List<String> codes = mfaService.confirmTotpEnrollment(currentUserId(),
                request.factorId(), request.code());
        var factor = mfaService.listFactors(currentUserId()).stream()
                .filter(f -> f.getId().equals(request.factorId()))
                .findFirst().orElseThrow();
        return new MfaEnrollmentResultDto(userMapper.toDto(factor), codes);
    }

    @GetMapping("/mfa/recovery-codes")
    @PreAuthorize("hasAuthority('self:read')")
    public Map<String, Object> recoveryStatus() {
        return Map.of("remainingCodes", mfaService.remainingRecoveryCodes(currentUserId()));
    }

    @PostMapping("/mfa/recovery-codes/regenerate")
    @PreAuthorize("hasAuthority('self:write')")
    public Map<String, Object> regenerateRecoveryCodes() {
        return Map.of("recoveryCodes", mfaService.regenerateRecoveryCodes(currentUserId()));
    }

    @DeleteMapping("/mfa/{factorId}")
    @PreAuthorize("hasAuthority('self:write')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFactor(@PathVariable UUID factorId) {
        mfaService.deleteFactor(currentUserId(), factorId);
    }

    private UUID currentUserId() {
        return currentUser.currentUserIdOrThrow();
    }

    public record UpdateMeRequest(@Size(min = 1, max = 160) String displayName,
                                  String email) { }

    public record PreferencesPatchRequest(String language, String theme, String timezone, String density) { }

    public record ChangePasswordRequest(@NotBlank String currentPassword,
                                        @NotBlank @Size(min = 12, max = 200) String newPassword) { }

    public record MfaSetupRequest(@Size(max = 128) String name) { }

    public record MfaConfirmRequest(UUID factorId,
                                    @NotBlank @Size(min = 6, max = 8) String code) { }

    public record MfaSetupDto(UUID factorId, String type, String secret,
                              String otpAuthUri, String qrCodeSvg, OffsetDateTime expiresAt) { }

    public record MfaEnrollmentResultDto(MfaFactorDto factor, List<String> recoveryCodes) { }
}
