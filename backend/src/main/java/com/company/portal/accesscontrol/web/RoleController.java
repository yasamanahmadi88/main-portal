package com.company.portal.accesscontrol.web;

import com.company.portal.accesscontrol.application.RoleService;
import com.company.portal.shared.security.CurrentUserAccessor;
import com.company.portal.shared.web.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleService roleService;
    private final CurrentUserAccessor currentUser;

    public RoleController(RoleService roleService, CurrentUserAccessor currentUser) {
        this.roleService = roleService;
        this.currentUser = currentUser;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('role:read')")
    public PageResponse<RoleDto> list(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(roleService.list(PageRequest.of(page, Math.min(size, 200),
                Sort.by(Sort.Direction.ASC, "code"))));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('role:write')")
    @ResponseStatus(HttpStatus.CREATED)
    public RoleDto create(@RequestBody @Valid RoleWriteRequest request) {
        return roleService.create(currentUser.currentUserIdOrThrow(),
                request.code(), request.name(), request.description());
    }

    @GetMapping("/{roleId}")
    @PreAuthorize("hasAuthority('role:read')")
    public RoleDto get(@PathVariable UUID roleId) {
        return roleService.findById(roleId);
    }

    @PatchMapping("/{roleId}")
    @PreAuthorize("hasAuthority('role:write')")
    public RoleDto update(@PathVariable UUID roleId, @RequestBody @Valid RolePatchRequest request) {
        return roleService.update(currentUser.currentUserIdOrThrow(),
                roleId, request.name(), request.description());
    }

    @DeleteMapping("/{roleId}")
    @PreAuthorize("hasAuthority('role:write')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID roleId) {
        roleService.delete(currentUser.currentUserIdOrThrow(), roleId);
    }

    @GetMapping("/{roleId}/permissions")
    @PreAuthorize("hasAuthority('role:read')")
    public List<PermissionDto> permissions(@PathVariable UUID roleId) {
        return roleService.permissionsOfRole(roleId);
    }

    @PutMapping("/{roleId}/permissions")
    @PreAuthorize("hasAuthority('permission:write')")
    public List<PermissionDto> replacePermissions(@PathVariable UUID roleId,
                                                  @RequestBody @Valid AssignPermissionsRequest request) {
        return roleService.replacePermissions(currentUser.currentUserIdOrThrow(),
                roleId, Set.copyOf(request.permissionCodes()));
    }

    public record RoleWriteRequest(
            @NotBlank @Pattern(regexp = "^[A-Z0-9_]+$") @Size(max = 64) String code,
            @NotBlank @Size(min = 1, max = 120) String name,
            @Size(max = 500) String description) { }

    public record RolePatchRequest(
            @Size(min = 1, max = 120) String name,
            @Size(max = 500) String description) { }

    public record AssignPermissionsRequest(@Valid List<@NotBlank String> permissionCodes) { }
}
