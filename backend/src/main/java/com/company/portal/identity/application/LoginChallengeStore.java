package com.company.portal.identity.application;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Short-lived MFA challenges keyed by opaque id, stored in Redis. The
 * challenge captures the user id and the moment it was issued so we can
 * reject stale attempts even if the ID is guessed.
 */
@Component
public class LoginChallengeStore {

    private static final String KEY_PREFIX = "portal:mfa:challenge:";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final RedisTemplate<String, Object> redisTemplate;

    public LoginChallengeStore(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String issue(UUID userId) {
        String id = UUID.randomUUID().toString();
        Challenge value = new Challenge(userId, Instant.now().toEpochMilli());
        redisTemplate.opsForValue().set(KEY_PREFIX + id, value, TTL);
        return id;
    }

    public UUID consume(String challengeId) {
        if (challengeId == null || challengeId.isBlank()) return null;
        Object value = redisTemplate.opsForValue().get(KEY_PREFIX + challengeId);
        redisTemplate.delete(KEY_PREFIX + challengeId);
        if (value instanceof Challenge c) return c.userId();
        return null;
    }

    public UUID peek(String challengeId) {
        if (challengeId == null || challengeId.isBlank()) return null;
        Object value = redisTemplate.opsForValue().get(KEY_PREFIX + challengeId);
        return value instanceof Challenge c ? c.userId() : null;
    }

    public record Challenge(UUID userId, long issuedAtMillis) implements Serializable { }
}
