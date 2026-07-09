package com.company.portal.accesscontrol.web;

public record PermissionDto(String code, String module, String action, String description, boolean system) { }
