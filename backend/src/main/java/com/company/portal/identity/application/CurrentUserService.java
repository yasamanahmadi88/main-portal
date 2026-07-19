package com.company.portal.identity.application;

import com.company.portal.identity.security.PortalUserDetails;
import com.company.portal.shared.security.CurrentUserAccessor;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Wraps the Spring Security context so callers don't touch it directly and
 * exposes the current HTTP session id when a servlet request is active.
 */
@Component
public class CurrentUserService implements CurrentUserAccessor {

    @Override
    public Optional<UUID> currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return Optional.empty();
        if (auth.getPrincipal() instanceof PortalUserDetails pud) {
            return Optional.ofNullable(pud.getUserId());
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> currentSessionId() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes sra) {
            var session = sra.getRequest().getSession(false);
            if (session != null) return Optional.of(session.getId());
        }
        return Optional.empty();
    }

    public Optional<PortalUserDetails> currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof PortalUserDetails pud)) {
            return Optional.empty();
        }
        return Optional.of(pud);
    }
}
