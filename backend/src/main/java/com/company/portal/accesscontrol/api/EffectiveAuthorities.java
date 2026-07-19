package com.company.portal.accesscontrol.api;

import java.io.Serializable;
import java.util.Set;

/**
 * Cached view of a user's effective roles and permissions used by the
 * identity module to build Spring Security authorities. Serializable so it
 * can travel via the Redis cache.
 */
public record EffectiveAuthorities(Set<String> roleCodes, Set<String> permissionCodes)
        implements Serializable {

    public static final EffectiveAuthorities EMPTY = new EffectiveAuthorities(Set.of(), Set.of());
}
