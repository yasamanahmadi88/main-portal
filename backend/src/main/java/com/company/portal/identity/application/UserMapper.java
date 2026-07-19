package com.company.portal.identity.application;

import com.company.portal.accesscontrol.api.EffectiveAuthorities;
import com.company.portal.accesscontrol.api.RbacQueryPort;
import com.company.portal.accesscontrol.application.RbacService;
import com.company.portal.accesscontrol.web.RoleDto;
import com.company.portal.identity.application.PreferenceService.PreferencesSnapshot;
import com.company.portal.identity.domain.MfaCredentialEntity;
import com.company.portal.identity.domain.UserEntity;
import com.company.portal.identity.domain.UserSessionMetadataEntity;
import com.company.portal.identity.web.MfaFactorDto;
import com.company.portal.identity.web.PreferencesDto;
import com.company.portal.identity.web.SessionDto;
import com.company.portal.identity.web.UserDto;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Central entity → DTO translation for the identity module. All controllers
 * go through here so the wire-format is contract-driven and never leaks JPA.
 */
@Component
public class UserMapper {

    private final RbacService rbacService;
    private final PreferenceService preferenceService;
    private final RbacQueryPort rbac;

    public UserMapper(RbacService rbacService,
                      PreferenceService preferenceService, RbacQueryPort rbac) {
        this.rbacService = rbacService;
        this.preferenceService = preferenceService;
        this.rbac = rbac;
    }

    public UserDto toDto(UserEntity user) {
        EffectiveAuthorities auth = rbac.loadEffectiveAuthorities(user.getId());
        // Role summaries only — permission codes are already aggregated on the user DTO.
        // Avoid RoleService.toDto here: it touches lazy RoleEntity.permissions outside a session.
        List<RoleDto> roles = rbacService.rolesOf(user.getId()).stream()
                .map(r -> new RoleDto(
                        r.getId(), r.getCode(), r.getName(), r.getDescription(),
                        r.isSystemRole(), List.of(), r.getCreatedAt(), r.getUpdatedAt()))
                .toList();
        PreferencesSnapshot prefs = preferenceService.getPreferences(user.getId());
        return new UserDto(
                user.getId(),
                user.getEmailNormalized(),
                user.getEmail(),
                user.getDisplayName(),
                user.getStatus().toApiValue(),
                new PreferencesDto(prefs.language(), prefs.theme(), prefs.timezone(), prefs.density()),
                roles,
                auth.permissionCodes().stream()
                        .map(c -> c.replace(':', '_').toUpperCase())
                        .sorted().toList(),
                user.isMfaEnabled(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public MfaFactorDto toDto(MfaCredentialEntity credential) {
        return new MfaFactorDto(
                credential.getId(),
                credential.getType(),
                credential.getLabel() == null ? "Authenticator" : credential.getLabel(),
                credential.isActivated(),
                credential.getCreatedAt(),
                credential.getLastUsedAt());
    }

    public SessionDto toDto(UserSessionMetadataEntity session, String currentSessionId, String username) {
        boolean current = currentSessionId != null && currentSessionId.equals(session.getSessionId());
        return new SessionDto(
                session.getSessionId(),
                session.getUserId(),
                username,
                session.getIpAddress(),
                session.getUserAgent(),
                session.getCreatedAt(),
                session.getLastSeenAt(),
                session.getExpiresAt() == null
                        ? session.getLastSeenAt().plusMinutes(30)
                        : session.getExpiresAt(),
                session.getRevokedAt(),
                current
        );
    }
}
