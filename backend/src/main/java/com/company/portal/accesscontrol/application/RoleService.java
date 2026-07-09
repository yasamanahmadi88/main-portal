package com.company.portal.accesscontrol.application;

import com.company.portal.accesscontrol.domain.PermissionEntity;
import com.company.portal.accesscontrol.domain.RoleEntity;
import com.company.portal.accesscontrol.repository.PermissionRepository;
import com.company.portal.accesscontrol.repository.RoleRepository;
import com.company.portal.accesscontrol.web.PermissionDto;
import com.company.portal.accesscontrol.web.RoleDto;
import com.company.portal.audit.api.AuditContext;
import com.company.portal.audit.api.AuditOutcome;
import com.company.portal.audit.api.AuditService;
import com.company.portal.audit.api.AuditSeverityLevel;
import com.company.portal.shared.error.PortalException;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD and permission-assignment operations on roles. Enforces RBAC policy via
 * {@link AuthorizationDecisionService} and emits audit events for every write.
 */
@Service
public class RoleService {

    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final AuthorizationDecisionService authorization;
    private final RbacService rbacService;
    private final AuditService auditService;

    public RoleService(RoleRepository roles,
                       PermissionRepository permissions,
                       AuthorizationDecisionService authorization,
                       RbacService rbacService,
                       AuditService auditService) {
        this.roles = roles;
        this.permissions = permissions;
        this.authorization = authorization;
        this.rbacService = rbacService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<RoleDto> list(Pageable pageable) {
        return roles.findAll(pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<RoleDto> listAll() {
        return roles.findAllOrdered().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public RoleDto findById(UUID id) {
        return toDto(mustLoad(id));
    }

    @Transactional
    public RoleDto create(UUID actorId, String code, String name, String description) {
        if (roles.existsByCode(code)) {
            throw new PortalException.Conflict("Role code already exists");
        }
        RoleEntity role = new RoleEntity(UUID.randomUUID(), code, name);
        role.setDescription(description);
        role.setSystemRole(false);
        roles.save(role);
        auditRoleWrite(actorId, "ROLE_CREATED", role);
        return toDto(role);
    }

    @Transactional
    public RoleDto update(UUID actorId, UUID id, String name, String description) {
        RoleEntity role = mustLoad(id);
        authorization.checkCanModifyRole(actorId, role, false);
        if (name != null) role.setName(name);
        if (description != null) role.setDescription(description);
        role.setUpdatedAt(OffsetDateTime.now());
        auditRoleWrite(actorId, "ROLE_UPDATED", role);
        return toDto(role);
    }

    @Transactional
    public void delete(UUID actorId, UUID id) {
        RoleEntity role = mustLoad(id);
        if (role.isSystemRole()) {
            throw new PortalException.Forbidden("System role cannot be deleted");
        }
        authorization.checkCanModifyRole(actorId, role, true);
        roles.delete(role);
        rbacService.invalidateAll();
        auditRoleWrite(actorId, "ROLE_DELETED", role);
    }

    @Transactional(readOnly = true)
    public List<PermissionDto> permissionsOfRole(UUID roleId) {
        RoleEntity role = mustLoad(roleId);
        return role.getPermissions().stream().map(RoleService::toPermissionDto).toList();
    }

    @Transactional
    public List<PermissionDto> replacePermissions(UUID actorId, UUID roleId, Set<String> permissionCodes) {
        RoleEntity role = mustLoad(roleId);
        authorization.checkCanAssignPermissions(actorId, role, permissionCodes);
        List<PermissionEntity> newPerms = permissions.findByCodes(permissionCodes);
        if (newPerms.size() != permissionCodes.size()) {
            throw new PortalException.Validation("One or more permission codes are unknown");
        }
        role.setPermissions(new HashSet<>(newPerms));
        role.setUpdatedAt(OffsetDateTime.now());
        rbacService.invalidateAll();
        auditService.append(AuditContext.builder()
                .eventType("ROLE_PERMISSIONS_REPLACED")
                .category("RBAC")
                .severity(AuditSeverityLevel.NOTICE)
                .outcome(AuditOutcome.SUCCESS)
                .actorType("USER")
                .actorId(actorId)
                .action("REPLACE_ROLE_PERMISSIONS")
                .targetType("ROLE")
                .targetId(role.getId().toString())
                .targetDisplay(role.getCode())
                .addPayload("permissions", permissionCodes)
                .build());
        return role.getPermissions().stream().map(RoleService::toPermissionDto).toList();
    }

    private RoleEntity mustLoad(UUID id) {
        return roles.findById(id).orElseThrow(() -> new PortalException.NotFound("Role not found"));
    }

    private void auditRoleWrite(UUID actorId, String eventType, RoleEntity role) {
        auditService.append(AuditContext.builder()
                .eventType(eventType)
                .category("RBAC")
                .severity(AuditSeverityLevel.NOTICE)
                .outcome(AuditOutcome.SUCCESS)
                .actorType("USER")
                .actorId(actorId)
                .action(eventType)
                .targetType("ROLE")
                .targetId(role.getId().toString())
                .targetDisplay(role.getCode())
                .build());
    }

    public RoleDto toDto(RoleEntity r) {
        return new RoleDto(
                r.getId(), r.getCode(), r.getName(), r.getDescription(),
                r.isSystemRole(),
                r.getPermissions().stream().map(RoleService::toPermissionDto).toList(),
                r.getCreatedAt(), r.getUpdatedAt());
    }

    public static PermissionDto toPermissionDto(PermissionEntity p) {
        return new PermissionDto(
                normalizeCode(p.getCode()), p.getResource(), p.getAction(),
                p.getDescription(), p.isSystemPermission());
    }

    /**
     * The OpenAPI contract exposes UPPER_SNAKE_CASE permission codes while
     * the DB stores {@code resource:action}. This method flips the format for
     * outbound payloads only.
     */
    private static String normalizeCode(String dbCode) {
        return dbCode == null ? null : dbCode.replace(':', '_').toUpperCase();
    }
}
