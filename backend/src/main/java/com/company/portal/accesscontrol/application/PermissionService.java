package com.company.portal.accesscontrol.application;

import com.company.portal.accesscontrol.domain.PermissionEntity;
import com.company.portal.accesscontrol.domain.RoleEntity;
import com.company.portal.accesscontrol.repository.PermissionRepository;
import com.company.portal.accesscontrol.repository.RoleRepository;
import com.company.portal.accesscontrol.web.PermissionDto;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PermissionService {

    private final PermissionRepository permissions;
    private final RoleRepository roles;

    public PermissionService(PermissionRepository permissions, RoleRepository roles) {
        this.permissions = permissions;
        this.roles = roles;
    }

    @Transactional(readOnly = true)
    public List<PermissionDto> listAll() {
        return permissions.findAllOrdered().stream()
                .map(RoleService::toPermissionDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PermissionMatrix matrix() {
        List<RoleEntity> allRoles = roles.findAllOrdered();
        List<PermissionEntity> allPerms = permissions.findAllOrdered();
        Map<UUID, List<String>> assignments = new LinkedHashMap<>();
        for (RoleEntity r : allRoles) {
            assignments.put(r.getId(), r.getPermissions().stream()
                    .map(p -> p.getCode().replace(':', '_').toUpperCase())
                    .sorted().toList());
        }
        return new PermissionMatrix(allRoles, allPerms, assignments);
    }

    public record PermissionMatrix(List<RoleEntity> roles,
                                   List<PermissionEntity> permissions,
                                   Map<UUID, List<String>> assignments) { }
}
