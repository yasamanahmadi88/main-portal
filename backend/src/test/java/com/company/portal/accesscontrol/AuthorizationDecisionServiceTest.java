package com.company.portal.accesscontrol;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.company.portal.accesscontrol.api.EffectiveAuthorities;
import com.company.portal.accesscontrol.application.AuthorizationDecisionService;
import com.company.portal.accesscontrol.application.RbacService;
import com.company.portal.accesscontrol.domain.PermissionEntity;
import com.company.portal.accesscontrol.domain.RoleEntity;
import com.company.portal.shared.error.PortalException;
import com.company.portal.shared.security.PortalRoles;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Parametric-style coverage of the RBAC guard rails: the SUPER_ADMIN
 * finality rule, no-self-escalation, and "cannot grant what you don't hold".
 */
@ExtendWith(MockitoExtension.class)
class AuthorizationDecisionServiceTest {

    @Mock RbacService rbacService;

    @InjectMocks AuthorizationDecisionService service;

    private final UUID adminId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();

    @Test
    void nonSuperAdminCannotGrantSuperAdmin() {
        RoleEntity superAdmin = roleWith(PortalRoles.SUPER_ADMIN);
        when(rbacService.loadEffectiveAuthorities(adminId))
                .thenReturn(new EffectiveAuthorities(Set.of("ADMIN"), Set.of("user:read")));

        assertThatThrownBy(() ->
                service.checkCanAssignRoles(adminId, targetId, List.of(superAdmin)))
                .isInstanceOf(PortalException.Forbidden.class)
                .hasMessageContaining("SUPER_ADMIN");
    }

    @Test
    void nonSuperAdminCannotGrantRolesWithPermissionsTheyLack() {
        RoleEntity powerful = roleWith("SECURITY_ADMIN",
                new PermissionEntity(UUID.randomUUID(), "audit:read", "audit", "read"),
                new PermissionEntity(UUID.randomUUID(), "audit:export", "audit", "export"));

        when(rbacService.loadEffectiveAuthorities(adminId))
                .thenReturn(new EffectiveAuthorities(Set.of("ADMIN"),
                        Set.of("user:read", "user:write")));

        assertThatThrownBy(() ->
                service.checkCanAssignRoles(adminId, targetId, List.of(powerful)))
                .isInstanceOf(PortalException.Forbidden.class);
    }

    @Test
    void adminCanGrantRoleIfTheyHoldItsPermissions() {
        RoleEntity readOnly = roleWith("READER",
                new PermissionEntity(UUID.randomUUID(), "user:read", "user", "read"));
        when(rbacService.loadEffectiveAuthorities(adminId))
                .thenReturn(new EffectiveAuthorities(Set.of("ADMIN"),
                        Set.of("user:read", "user:write")));

        assertThatCode(() -> service.checkCanAssignRoles(adminId, targetId, List.of(readOnly)))
                .doesNotThrowAnyException();
    }

    @Test
    void systemRoleCannotBeStructurallyModified() {
        RoleEntity systemRole = roleWith(PortalRoles.ADMIN);
        systemRole.setSystemRole(true);

        assertThatThrownBy(() -> service.checkCanModifyRole(adminId, systemRole, true))
                .isInstanceOf(PortalException.Forbidden.class);
    }

    @Test
    void superAdminMayModifyAnyRole() {
        RoleEntity systemRole = roleWith(PortalRoles.SUPER_ADMIN);
        systemRole.setSystemRole(true);
        when(rbacService.loadEffectiveAuthorities(adminId))
                .thenReturn(new EffectiveAuthorities(Set.of(PortalRoles.SUPER_ADMIN), Set.of()));

        assertThatCode(() -> service.checkCanModifyRole(adminId, systemRole, false))
                .doesNotThrowAnyException();
    }

    @Test
    void nonSuperAdminCannotAssignPermissionsTheyLack() {
        RoleEntity role = roleWith("OPS");
        when(rbacService.loadEffectiveAuthorities(adminId))
                .thenReturn(new EffectiveAuthorities(Set.of("ADMIN"), Set.of("user:read")));

        assertThatThrownBy(() ->
                service.checkCanAssignPermissions(adminId, role, List.of("audit:export")))
                .isInstanceOf(PortalException.Forbidden.class);
    }

    private static RoleEntity roleWith(String code, PermissionEntity... permissions) {
        RoleEntity role = new RoleEntity(UUID.randomUUID(), code, code);
        for (PermissionEntity p : permissions) role.getPermissions().add(p);
        return role;
    }
}
