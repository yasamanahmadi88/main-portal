package com.company.portal.accesscontrol.application;

import com.company.portal.accesscontrol.api.EffectiveAuthorities;
import com.company.portal.accesscontrol.api.RbacQueryPort;
import com.company.portal.accesscontrol.domain.PermissionEntity;
import com.company.portal.accesscontrol.domain.RoleEntity;
import com.company.portal.accesscontrol.repository.PermissionRepository;
import com.company.portal.accesscontrol.repository.RoleRepository;
import com.company.portal.accesscontrol.repository.UserRoleRepository;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves and caches the set of role codes + permission codes granted to a
 * user. Cache is keyed by user id and stored in Redis with a short TTL so a
 * compromised/expired session cannot outlive an admin-driven role change by
 * more than a few minutes. Explicit invalidation is called after any RBAC
 * mutation.
 */
@Service
public class RbacService implements RbacQueryPort {

    private static final Logger log = LoggerFactory.getLogger(RbacService.class);
    private static final String CACHE_PREFIX = "portal:rbac:effective:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final UserRoleRepository userRoles;
    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final RedisTemplate<String, Object> redisTemplate;

    public RbacService(UserRoleRepository userRoles,
                       RoleRepository roles,
                       PermissionRepository permissions,
                       RedisTemplate<String, Object> redisTemplate) {
        this.userRoles = userRoles;
        this.roles = roles;
        this.permissions = permissions;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public EffectiveAuthorities loadEffectiveAuthorities(UUID userId) {
        String key = CACHE_PREFIX + userId;
        Object cached = safeGet(key);
        if (cached instanceof EffectiveAuthorities ea) {
            return ea;
        }
        EffectiveAuthorities computed = compute(userId);
        safePut(key, computed);
        return computed;
    }

    @Override
    public void invalidate(UUID userId) {
        try {
            redisTemplate.delete(CACHE_PREFIX + userId);
        } catch (RuntimeException e) {
            log.warn("Failed to invalidate RBAC cache for user {}: {}", userId, e.getMessage());
        }
    }

    public void invalidateAll() {
        try {
            Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (RuntimeException e) {
            log.warn("Failed to invalidate RBAC cache globally: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<RoleEntity> rolesOf(UUID userId) {
        List<UUID> roleIds = userRoles.findByUserId(userId).stream()
                .map(ur -> ur.getId().getRoleId())
                .toList();
        return roles.findAllById(roleIds);
    }

    private EffectiveAuthorities compute(UUID userId) {
        List<RoleEntity> assigned = rolesOf(userId);
        Set<String> roleCodes = new LinkedHashSet<>();
        Set<String> permissionCodes = new LinkedHashSet<>();
        for (RoleEntity role : assigned) {
            roleCodes.add(role.getCode());
            for (PermissionEntity p : role.getPermissions()) {
                permissionCodes.add(p.getCode());
            }
        }
        return new EffectiveAuthorities(Set.copyOf(roleCodes), Set.copyOf(permissionCodes));
    }

    private Object safeGet(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (RuntimeException e) {
            log.debug("RBAC cache miss (backend error): {}", e.getMessage());
            return null;
        }
    }

    private void safePut(String key, EffectiveAuthorities value) {
        try {
            redisTemplate.opsForValue().set(key, value, CACHE_TTL);
        } catch (RuntimeException e) {
            log.debug("RBAC cache write skipped (backend error): {}", e.getMessage());
        }
    }

    /** Delegation helper for {@link PermissionRepository} used by admin services. */
    public List<PermissionEntity> allPermissions() {
        return permissions.findAllOrdered();
    }
}
