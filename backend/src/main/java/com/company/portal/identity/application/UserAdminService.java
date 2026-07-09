package com.company.portal.identity.application;

import com.company.portal.accesscontrol.application.AuthorizationDecisionService;
import com.company.portal.accesscontrol.application.RbacService;
import com.company.portal.accesscontrol.domain.RoleEntity;
import com.company.portal.accesscontrol.domain.UserRoleEntity;
import com.company.portal.accesscontrol.repository.RoleRepository;
import com.company.portal.accesscontrol.repository.UserRoleRepository;
import com.company.portal.accesscontrol.web.RoleDto;
import com.company.portal.audit.api.AuditContext;
import com.company.portal.audit.api.AuditOutcome;
import com.company.portal.audit.api.AuditService;
import com.company.portal.audit.api.AuditSeverityLevel;
import com.company.portal.identity.domain.UserEntity;
import com.company.portal.identity.domain.UserStatus;
import com.company.portal.identity.repository.UserPreferencesRepository;
import com.company.portal.identity.repository.UserRepository;
import com.company.portal.identity.web.UserDto;
import com.company.portal.notification.api.NotificationService;
import com.company.portal.shared.config.PortalProperties;
import com.company.portal.shared.error.PortalException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administrative operations on users. Deliberately does not physically delete
 * rows — status transitions to {@code DISABLED} or {@code DELETED} instead.
 */
@Service
public class UserAdminService {

    private final UserRepository users;
    private final UserPreferencesRepository preferences;
    private final RoleRepository roles;
    private final UserRoleRepository userRoles;
    private final AuthorizationDecisionService authorization;
    private final RbacService rbacService;
    private final AuditService auditService;
    private final NotificationService notifications;
    private final PortalProperties portalProperties;
    private final UserMapper userMapper;

    public UserAdminService(UserRepository users,
                            UserPreferencesRepository preferences,
                            RoleRepository roles,
                            UserRoleRepository userRoles,
                            AuthorizationDecisionService authorization,
                            RbacService rbacService,
                            AuditService auditService,
                            NotificationService notifications,
                            PortalProperties portalProperties,
                            UserMapper userMapper) {
        this.users = users;
        this.preferences = preferences;
        this.roles = roles;
        this.userRoles = userRoles;
        this.authorization = authorization;
        this.rbacService = rbacService;
        this.auditService = auditService;
        this.notifications = notifications;
        this.portalProperties = portalProperties;
        this.userMapper = userMapper;
    }

    public record CreateUserCommand(String username, String email, String displayName,
                                    List<UUID> roleIds, boolean sendInvite) { }

    @Transactional(readOnly = true)
    public Page<UserDto> list(String query, UserStatus status, Pageable pageable) {
        return users.search(query, status, pageable).map(userMapper::toDto);
    }

    @Transactional(readOnly = true)
    public UserDto get(UUID id) {
        UserEntity user = users.findById(id)
                .orElseThrow(() -> new PortalException.NotFound("User not found"));
        return userMapper.toDto(user);
    }

    @Transactional
    public UserDto create(UUID actorId, CreateUserCommand cmd) {
        String normalized = cmd.email() == null ? "" : cmd.email().trim().toLowerCase();
        if (users.existsByEmailNormalized(normalized)) {
            throw new PortalException.Conflict("Email already registered");
        }
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity(userId, cmd.email(), normalized, cmd.displayName());
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user.setCreatedBy(actorId);
        user.setUpdatedBy(actorId);
        users.save(user);
        preferences.save(new com.company.portal.identity.domain.UserPreferencesEntity(
                UUID.randomUUID(), userId));

        List<RoleEntity> assignedRoles = cmd.roleIds() == null || cmd.roleIds().isEmpty()
                ? List.of(mustDefaultRole())
                : roles.findAllById(cmd.roleIds());
        authorization.checkCanAssignRoles(actorId, userId, assignedRoles);
        for (RoleEntity role : assignedRoles) {
            userRoles.save(new UserRoleEntity(userId, role.getId(), actorId));
        }
        rbacService.invalidate(userId);

        auditService.append(AuditContext.builder()
                .eventType("USER_CREATED")
                .category("USER_ADMIN")
                .severity(AuditSeverityLevel.NOTICE)
                .outcome(AuditOutcome.SUCCESS)
                .actorType("USER").actorId(actorId)
                .targetType("USER").targetId(userId.toString())
                .targetDisplay(cmd.email())
                .build());

        if (cmd.sendInvite()) {
            notifications.enqueueEmail("invitation", cmd.email(), user.getLocale(),
                    Map.of("displayName", cmd.displayName(),
                            "portalUrl", portalProperties.getPublicBaseUrl()));
        }
        return userMapper.toDto(user);
    }

    @Transactional
    public UserDto update(UUID actorId, UUID targetId, String email, String displayName, String status) {
        UserEntity user = users.findById(targetId)
                .orElseThrow(() -> new PortalException.NotFound("User not found"));
        if (email != null) {
            String normalized = email.trim().toLowerCase();
            if (!normalized.equals(user.getEmailNormalized())
                    && users.existsByEmailNormalized(normalized)) {
                throw new PortalException.Conflict("Email already registered");
            }
            user.setEmail(email);
            user.setEmailNormalized(normalized);
            user.setEmailVerified(false);
        }
        if (displayName != null) user.setDisplayName(displayName);
        if (status != null) {
            if ("INACTIVE".equals(status) || "LOCKED".equals(status)) {
                authorization.checkCanDisableOrDeleteUser(targetId);
            }
            applyApiStatus(user, status);
        }
        user.setUpdatedBy(actorId);
        user.setUpdatedAt(OffsetDateTime.now());
        auditWrite(actorId, "USER_UPDATED", targetId, user.getEmailNormalized());
        return userMapper.toDto(user);
    }

    @Transactional
    public UserDto activate(UUID actorId, UUID targetId) {
        UserEntity user = mustLoad(targetId);
        user.setStatus(UserStatus.ACTIVE);
        user.setLockoutUntil(null);
        user.setUpdatedBy(actorId);
        user.setUpdatedAt(OffsetDateTime.now());
        auditWrite(actorId, "USER_ACTIVATED", targetId, user.getEmailNormalized());
        return userMapper.toDto(user);
    }

    @Transactional
    public UserDto deactivate(UUID actorId, UUID targetId) {
        UserEntity user = mustLoad(targetId);
        if (actorId.equals(targetId)) {
            throw new PortalException.Forbidden("Cannot deactivate yourself");
        }
        authorization.checkCanDisableOrDeleteUser(targetId);
        user.setStatus(UserStatus.DISABLED);
        user.setUpdatedBy(actorId);
        user.setUpdatedAt(OffsetDateTime.now());
        auditWrite(actorId, "USER_DEACTIVATED", targetId, user.getEmailNormalized());
        return userMapper.toDto(user);
    }

    @Transactional
    public UserDto unlock(UUID actorId, UUID targetId) {
        UserEntity user = mustLoad(targetId);
        user.setStatus(UserStatus.ACTIVE);
        user.setLockoutUntil(null);
        user.setFailedLoginCount(0);
        user.setUpdatedBy(actorId);
        user.setUpdatedAt(OffsetDateTime.now());
        auditWrite(actorId, "USER_UNLOCKED", targetId, user.getEmailNormalized());
        return userMapper.toDto(user);
    }

    @Transactional
    public List<RoleDto> replaceRoles(UUID actorId, UUID targetId, Set<UUID> roleIds) {
        UserEntity user = mustLoad(targetId);
        List<RoleEntity> newRoles = roles.findAllById(roleIds);
        if (newRoles.size() != roleIds.size()) {
            throw new PortalException.Validation("One or more role ids are unknown");
        }
        authorization.checkCanAssignRoles(actorId, targetId, newRoles);
        userRoles.deleteByUserId(targetId);
        for (RoleEntity role : newRoles) {
            userRoles.save(new UserRoleEntity(targetId, role.getId(), actorId));
        }
        rbacService.invalidate(targetId);
        auditService.append(AuditContext.builder()
                .eventType("USER_ROLES_REPLACED")
                .category("RBAC")
                .severity(AuditSeverityLevel.NOTICE)
                .actorType("USER").actorId(actorId)
                .targetType("USER").targetId(targetId.toString())
                .targetDisplay(user.getEmailNormalized())
                .addPayload("roleIds", roleIds)
                .build());
        return rbacService.rolesOf(targetId).stream()
                .map(r -> new RoleDto(r.getId(), r.getCode(), r.getName(), r.getDescription(),
                        r.isSystemRole(), List.of(), r.getCreatedAt(), r.getUpdatedAt()))
                .toList();
    }

    private RoleEntity mustDefaultRole() {
        return roles.findByCode(com.company.portal.shared.security.PortalRoles.USER)
                .orElseThrow(() -> new IllegalStateException("Default USER role missing"));
    }

    private UserEntity mustLoad(UUID id) {
        return users.findById(id).orElseThrow(() -> new PortalException.NotFound("User not found"));
    }

    private void auditWrite(UUID actorId, String eventType, UUID targetId, String display) {
        auditService.append(AuditContext.builder()
                .eventType(eventType)
                .category("USER_ADMIN")
                .severity(AuditSeverityLevel.NOTICE)
                .outcome(AuditOutcome.SUCCESS)
                .actorType("USER").actorId(actorId)
                .targetType("USER").targetId(targetId.toString()).targetDisplay(display)
                .build());
    }

    private static void applyApiStatus(UserEntity user, String apiStatus) {
        switch (apiStatus) {
            case "ACTIVE" -> user.setStatus(UserStatus.ACTIVE);
            case "INACTIVE" -> user.setStatus(UserStatus.DISABLED);
            case "LOCKED" -> user.setStatus(UserStatus.LOCKED);
            case "PENDING" -> user.setStatus(UserStatus.PENDING_VERIFICATION);
            default -> throw new PortalException.Validation("Unknown status: " + apiStatus);
        }
    }
}
