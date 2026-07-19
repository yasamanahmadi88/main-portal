package com.company.portal.identity.security;

import com.company.portal.identity.domain.UserEntity;
import com.company.portal.identity.domain.UserStatus;
import com.company.portal.shared.security.PortalRoles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * {@link UserDetails} carrying the portal-specific fields (userId, mfa flag)
 * so downstream components can access them without another round-trip.
 * Authorities are the union of {@code ROLE_<code>} strings and permission
 * codes, matching {@link com.company.portal.shared.security.PortalPermission}.
 */
public final class PortalUserDetails implements UserDetails {

    private final UUID userId;
    private final String username;
    private final String passwordHash;
    private final UserStatus status;
    private final boolean mfaEnabled;
    private final boolean mfaEnforced;
    private final Set<String> roleCodes;
    private final Set<String> permissionCodes;

    public PortalUserDetails(UserEntity user, Set<String> roleCodes, Set<String> permissionCodes) {
        this.userId = user.getId();
        this.username = user.getEmailNormalized();
        this.passwordHash = user.getPasswordHash();
        this.status = user.getStatus();
        this.mfaEnabled = user.isMfaEnabled();
        this.mfaEnforced = user.isMfaEnforced();
        this.roleCodes = roleCodes;
        this.permissionCodes = permissionCodes;
    }

    public UUID getUserId() { return userId; }

    public boolean isMfaEnabled() { return mfaEnabled; }

    public boolean isMfaEnforced() { return mfaEnforced; }

    public Set<String> getRoleCodes() { return roleCodes; }

    public Set<String> getPermissionCodes() { return permissionCodes; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>(roleCodes.size() + permissionCodes.size());
        for (String code : roleCodes) {
            authorities.add(new SimpleGrantedAuthority(PortalRoles.authority(code)));
        }
        for (String code : permissionCodes) {
            authorities.add(new SimpleGrantedAuthority(code));
        }
        return authorities;
    }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public String getUsername() { return username; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return status != UserStatus.LOCKED; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE || status == UserStatus.PENDING_VERIFICATION;
    }
}
