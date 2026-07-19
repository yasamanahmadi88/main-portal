package com.company.portal.administration.web;

import com.company.portal.accesscontrol.web.RoleDto;
import com.company.portal.shared.security.CurrentUserAccessor;
import com.company.portal.identity.application.MfaService;
import com.company.portal.identity.application.PasswordService;
import com.company.portal.identity.application.SessionService;
import com.company.portal.identity.application.UserAdminService;
import com.company.portal.identity.application.UserAdminService.CreateUserCommand;
import com.company.portal.identity.domain.UserStatus;
import com.company.portal.identity.web.UserDto;
import com.company.portal.shared.web.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserAdminController {

    private final UserAdminService userAdminService;
    private final MfaService mfaService;
    private final SessionService sessionService;
    private final PasswordService passwordService;
    private final CurrentUserAccessor currentUser;

    public UserAdminController(UserAdminService userAdminService,
                               MfaService mfaService,
                               SessionService sessionService,
                               PasswordService passwordService,
                               CurrentUserAccessor currentUser) {
        this.userAdminService = userAdminService;
        this.mfaService = mfaService;
        this.sessionService = sessionService;
        this.passwordService = passwordService;
        this.currentUser = currentUser;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('user:read')")
    public PageResponse<UserDto> list(@RequestParam(required = false) String q,
                                      @RequestParam(required = false) String status,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        UserStatus filter = null;
        if (status != null) {
            filter = switch (status) {
                case "ACTIVE" -> UserStatus.ACTIVE;
                case "INACTIVE" -> UserStatus.DISABLED;
                case "LOCKED" -> UserStatus.LOCKED;
                case "PENDING" -> UserStatus.PENDING_VERIFICATION;
                default -> null;
            };
        }
        return PageResponse.of(userAdminService.list(q, filter,
                PageRequest.of(page, Math.min(size, 200),
                        Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user:write')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@RequestBody @Valid CreateUserRequest request) {
        return userAdminService.create(currentUser.currentUserIdOrThrow(),
                new CreateUserCommand(request.username(), request.email(),
                        request.displayName(), request.roleIds() == null ? List.of() : request.roleIds(),
                        request.sendInvite() == null ? true : request.sendInvite()));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('user:read')")
    public UserDto get(@PathVariable UUID userId) {
        return userAdminService.get(userId);
    }

    @PatchMapping("/{userId}")
    @PreAuthorize("hasAuthority('user:write')")
    public UserDto update(@PathVariable UUID userId, @RequestBody @Valid UpdateUserRequest request) {
        return userAdminService.update(currentUser.currentUserIdOrThrow(), userId,
                request.email(), request.displayName(), request.status());
    }

    @PostMapping("/{userId}/activate")
    @PreAuthorize("hasAuthority('user:write')")
    public UserDto activate(@PathVariable UUID userId,
                            @RequestBody(required = false) Map<String, Object> body) {
        return userAdminService.activate(currentUser.currentUserIdOrThrow(), userId);
    }

    @PostMapping("/{userId}/deactivate")
    @PreAuthorize("hasAuthority('user:write')")
    public UserDto deactivate(@PathVariable UUID userId,
                              @RequestBody(required = false) Map<String, Object> body) {
        return userAdminService.deactivate(currentUser.currentUserIdOrThrow(), userId);
    }

    @PostMapping("/{userId}/unlock")
    @PreAuthorize("hasAuthority('user:write')")
    public UserDto unlock(@PathVariable UUID userId,
                          @RequestBody(required = false) Map<String, Object> body) {
        return userAdminService.unlock(currentUser.currentUserIdOrThrow(), userId);
    }

    @PostMapping("/{userId}/password-reset")
    @PreAuthorize("hasAuthority('user:write')")
    public Map<String, Object> triggerPasswordReset(@PathVariable UUID userId) {
        UserDto user = userAdminService.get(userId);
        passwordService.requestPasswordReset(user.email(), null, null);
        return Map.of("message", "Password reset email queued.");
    }

    @PostMapping("/{userId}/mfa-reset")
    @PreAuthorize("hasAuthority('mfa:manage')")
    public Map<String, Object> resetMfa(@PathVariable UUID userId) {
        mfaService.adminResetMfa(userId);
        return Map.of("message", "MFA credentials reset.");
    }

    @PostMapping("/{userId}/sessions/revoke")
    @PreAuthorize("hasAuthority('session:revoke')")
    public Map<String, Object> revokeSessions(@PathVariable UUID userId,
                                              @RequestBody(required = false) Map<String, Object> body) {
        int revoked = sessionService.adminRevokeAll(userId, "ADMIN_REVOKED");
        return Map.of("revokedCount", revoked);
    }

    @GetMapping("/{userId}/roles")
    @PreAuthorize("hasAuthority('role:read')")
    public List<RoleDto> getRoles(@PathVariable UUID userId) {
        return userAdminService.get(userId).roles();
    }

    @PutMapping("/{userId}/roles")
    @PreAuthorize("hasAuthority('role:write')")
    public List<RoleDto> replaceRoles(@PathVariable UUID userId,
                                      @RequestBody @Valid AssignRolesRequest request) {
        return userAdminService.replaceRoles(currentUser.currentUserIdOrThrow(),
                userId, Set.copyOf(request.roleIds()));
    }

    public record CreateUserRequest(
            @NotBlank @Size(min = 3, max = 80) String username,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 1, max = 160) String displayName,
            List<UUID> roleIds,
            Boolean sendInvite) { }

    public record UpdateUserRequest(
            @Email String email,
            @Size(min = 1, max = 160) String displayName,
            String status) { }

    public record AssignRolesRequest(List<UUID> roleIds) { }
}
