package com.company.portal.identity.security;

import com.company.portal.shared.config.PortalProperties;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Fixed-window counter rate limiter backed by Redis {@code INCR + EXPIRE}.
 * Not a strict token bucket, but sufficient for auth-endpoint abuse
 * protection paired with progressive per-account lockouts.
 *
 * <p>Each call increments the counter for {@code key} in the window; if the
 * counter exceeds {@code capacity}, {@code allow} returns {@code false} and
 * the caller must reject the request. TTL is refreshed on every write so a
 * silent client cannot survive by pausing.</p>
 */
@Component
public class RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    private final RedisTemplate<String, Object> redisTemplate;

    public RateLimiter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Decision allow(String bucket, String subject, PortalProperties.Bucket policy) {
        if (policy == null || policy.getCapacity() <= 0) {
            return new Decision(true, 0, policy == null ? 0 : policy.getCapacity(), 0);
        }
        String key = "portal:ratelimit:" + bucket + ":" + subject;
        long window = Math.max(1, policy.getRefillPeriod() == null
                ? 60 : policy.getRefillPeriod().getSeconds());
        try {
            Long current = redisTemplate.opsForValue().increment(key, 1);
            if (current == null) current = 1L;
            if (current == 1L) {
                redisTemplate.expire(key, Duration.ofSeconds(window));
            }
            Long ttl = redisTemplate.getExpire(key);
            long remaining = Math.max(0L, policy.getCapacity() - current);
            long retry = ttl == null || ttl < 0 ? window : ttl;
            boolean allowed = current <= policy.getCapacity();
            return new Decision(allowed, current, policy.getCapacity(), retry);
        } catch (RuntimeException e) {
            log.debug("Rate limiter unavailable ({}) — failing open", e.getMessage());
            return new Decision(true, 0, policy.getCapacity(), 0);
        }
    }

    public record Decision(boolean allowed, long currentCount, long limit, long retryAfterSeconds) { }
}
