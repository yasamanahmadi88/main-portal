package com.company.portal.accesscontrol.application;

import com.company.portal.accesscontrol.api.EffectiveAuthorities;
import com.company.portal.accesscontrol.domain.RoleEntity;
import com.company.portal.accesscontrol.repository.UserRoleRepository;
import com.company.portal.shared.error.ErrorCodes;
import com.company.portal.shared.error.PortalException;
import com.company.portal.shared.security.PortalRoles;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Cross-cutting RBAC policy checks that go beyond a single-permission
 * {@code hasAuthority(...)} rule.
 *
 * <p>Enforces:
 * <ul>
 *   <li>The final {@code SUPER_ADMIN} rule — only another {@code SUPER_ADMIN}
 *       may assign the {@code SUPER_ADMIN} role, and the caller must not be
 *       modifying themselves.</li>
 *   <li>The last active {@code SUPER_ADMIN} cannot be demoted, disabled, or
 *       deleted.</li>
 *   <li>Cannot grant a role/permission you do not yourself hold unless you
 *       are {@code SUPER_ADMIN}.</li>
 *   <li>System roles cannot be renamed, deleted, or have their code changed.</li>
 *   <li>Cannot escalate your own effective privileges.</li>
 * </ul>
 * </p>
 */
@Service
public class AuthorizationDecisionService {

    private final RbacService rbacService;
    private final UserRoleRepository userRoles;

    public AuthorizationDecisionService(RbacService rbacService, UserRoleRepository userRoles) {
        this.rbacService = rbacService;
        this.userRoles = userRoles;
    }

    public void checkCanAssignRoles(UUID actorId, UUID targetUserId, Collection<RoleEntity> newRoles) {
        if (actorId == null) {
            throw new PortalException.Forbidden(ErrorCodes.FORBIDDEN, "Anonymous cannot manage roles");
        }
        EffectiveAuthorities actor = rbacService.loadEffectiveAuthorities(actorId);
        boolean actorIsSuperAdmin = actor.roleCodes().contains(PortalRoles.SUPER_ADMIN);

        boolean grantsSuperAdmin = newRoles.stream().anyMatch(r -> PortalRoles.SUPER_ADMIN.equals(r.getCode()));
        boolean targetIsSuperAdmin = rbacService.loadEffectiveAuthorities(targetUserId)
                .roleCodes().contains(PortalRoles.SUPER_ADMIN);

        if (grantsSuperAdmin && !actorIsSuperAdmin) {
            throw new PortalException.Forbidden(ErrorCodes.FORBIDDEN,
                    "Only SUPER_ADMIN may grant the SUPER_ADMIN role");
        }
        if (actorId.equals(targetUserId) && grantsSuperAdmin && !targetIsSuperAdmin) {
            throw new PortalException.Forbidden(ErrorCodes.FORBIDDEN,
                    "Users cannot escalate themselves to SUPER_ADMIN");
        }
        if (targetIsSuperAdmin && !grantsSuperAdmin) {
            ensureNotFinalSuperAdmin(targetUserId, "demote");
        }

        if (!actorIsSuperAdmin) {
            for (RoleEntity role : newRoles) {
                if (!actor.roleCodes().contains(role.getCode())
                        && !actor.permissionCodes().containsAll(codesOf(role))) {
                    throw new PortalException.Forbidden(ErrorCodes.FORBIDDEN,
                            "Cannot grant role " + role.getCode() + " (missing permissions)");
                }
            }
        }
    }

    /**
     * Blocks disable/delete of the last active SUPER_ADMIN account.
     */
    public void checkCanDisableOrDeleteUser(UUID targetUserId) {
        boolean targetIsSuperAdmin = rbacService.loadEffectiveAuthorities(targetUserId)
                .roleCodes().contains(PortalRoles.SUPER_ADMIN);
        if (targetIsSuperAdmin) {
            ensureNotFinalSuperAdmin(targetUserId, "disable");
        }
    }

    private void ensureNotFinalSuperAdmin(UUID targetUserId, String action) {
        long activeSuperAdmins = userRoles.countActiveUsersWithRole(PortalRoles.SUPER_ADMIN);
        if (activeSuperAdmins <= 1L) {
            throw new PortalException.Conflict(
                    "Cannot " + action + " the final active SUPER_ADMIN");
        }
    }

    public void checkCanModifyRole(UUID actorId, RoleEntity role, boolean structuralChange) {
        if (role.isSystemRole() && structuralChange) {
            throw new PortalException.Forbidden(ErrorCodes.FORBIDDEN,
                    "System role " + role.getCode() + " cannot be modified structurally");
        }
        EffectiveAuthorities actor = rbacService.loadEffectiveAuthorities(actorId);
        if (PortalRoles.SUPER_ADMIN.equals(role.getCode())
                && !actor.roleCodes().contains(PortalRoles.SUPER_ADMIN)) {
            throw new PortalException.Forbidden(ErrorCodes.FORBIDDEN,
                    "Only SUPER_ADMIN may modify the SUPER_ADMIN role");
        }
    }

    public void checkCanAssignPermissions(UUID actorId, RoleEntity role, Collection<String> newPermissionCodes) {
        EffectiveAuthorities actor = rbacService.loadEffectiveAuthorities(actorId);
        boolean actorIsSuperAdmin = actor.roleCodes().contains(PortalRoles.SUPER_ADMIN);
        if (PortalRoles.SUPER_ADMIN.equals(role.getCode()) && !actorIsSuperAdmin) {
            throw new PortalException.Forbidden(ErrorCodes.FORBIDDEN,
                    "Only SUPER_ADMIN may modify SUPER_ADMIN role permissions");
        }
        if (!actorIsSuperAdmin) {
            Set<String> missing = new HashSet<>(newPermissionCodes);
            missing.removeAll(actor.permissionCodes());
            if (!missing.isEmpty()) {
                throw new PortalException.Forbidden(ErrorCodes.FORBIDDEN,
                        "Cannot grant permissions you do not hold: " + missing);
            }
        }
    }

    private static Set<String> codesOf(RoleEntity role) {
        Set<String> codes = new HashSet<>();
        role.getPermissions().forEach(p -> codes.add(p.getCode()));
        return codes;
    }
}
