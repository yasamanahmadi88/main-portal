package com.company.portal.accesscontrol.api;

import java.util.UUID;

/**
 * Read port exposed to other modules (identity in particular). Caching lives
 * behind this port so callers don't need to know Redis is involved.
 */
public interface RbacQueryPort {

    EffectiveAuthorities loadEffectiveAuthorities(UUID userId);

    void invalidate(UUID userId);
}
