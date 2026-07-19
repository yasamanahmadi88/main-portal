package com.company.portal.identity.security;

import com.company.portal.accesscontrol.api.EffectiveAuthorities;
import com.company.portal.accesscontrol.api.RbacQueryPort;
import com.company.portal.identity.domain.UserEntity;
import com.company.portal.identity.repository.UserRepository;
import java.util.Locale;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads {@link PortalUserDetails} by normalized email. Deliberately produces
 * a generic exception on lookup failures — actual authentication error
 * shaping happens upstream so we never leak whether an account exists.
 */
@Service
public class PortalUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RbacQueryPort rbac;

    public PortalUserDetailsService(UserRepository userRepository, RbacQueryPort rbac) {
        this.userRepository = userRepository;
        this.rbac = rbac;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        String normalized = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        UserEntity user = userRepository.findByEmailNormalized(normalized)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        EffectiveAuthorities authorities = rbac.loadEffectiveAuthorities(user.getId());
        return new PortalUserDetails(user, authorities.roleCodes(), authorities.permissionCodes());
    }
}
