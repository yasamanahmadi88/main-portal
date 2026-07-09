package com.company.portal.accesscontrol.web;

import com.company.portal.accesscontrol.application.PermissionService;
import com.company.portal.accesscontrol.application.PermissionService.PermissionMatrix;
import com.company.portal.accesscontrol.application.RoleService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/permissions")
public class PermissionController {

    private final PermissionService permissionService;
    private final RoleService roleService;

    public PermissionController(PermissionService permissionService, RoleService roleService) {
        this.permissionService = permissionService;
        this.roleService = roleService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('permission:read')")
    public List<PermissionDto> list() {
        return permissionService.listAll();
    }

    @GetMapping("/matrix")
    @PreAuthorize("hasAuthority('permission:read')")
    public MatrixResponse matrix() {
        PermissionMatrix m = permissionService.matrix();
        List<RoleDto> roleDtos = m.roles().stream().map(roleService::toDto).toList();
        List<PermissionDto> permissionDtos = m.permissions().stream()
                .map(RoleService::toPermissionDto).toList();
        List<Map<String, Object>> assignments = new java.util.ArrayList<>();
        m.assignments().forEach((roleId, codes) ->
                codes.forEach(code -> {
                    Map<String, Object> assignment = new LinkedHashMap<>();
                    assignment.put("roleId", roleId);
                    assignment.put("permissionCode", code);
                    assignments.add(assignment);
                }));
        return new MatrixResponse(roleDtos, permissionDtos, assignments);
    }

    public record MatrixResponse(List<RoleDto> roles,
                                 List<PermissionDto> permissions,
                                 List<Map<String, Object>> assignments) { }
}
