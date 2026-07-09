package com.company.portal.accesscontrol.web;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record RoleDto(
        UUID id,
        String code,
        String name,
        String description,
        boolean system,
        List<PermissionDto> permissions,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) { }
