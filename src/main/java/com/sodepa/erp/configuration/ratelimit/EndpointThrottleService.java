package com.sodepa.erp.configuration.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service métier d'application implémentant la logique et les règles de gestion pour `EndpointThrottleService` au sein de la SODEPA.
 */
@Service
@Slf4j
public class EndpointThrottleService {

    private static final String UNKNOWN_RULE = "anonymous-rule";
    private static final String KEY_PREFIX = "sodepa:endpoint-throttle:";
    private static final int MAX_LOCAL_FALLBACK_KEYS = 10_000;

    private static final DefaultRedisScript<Long> ATOMIC_INCR_EXPIRE_SCRIPT;
    static {
        ATOMIC_INCR_EXPIRE_SCRIPT = new DefaultRedisScript<>();
        ATOMIC_INCR_EXPIRE_SCRIPT.setScriptText(
                "local c = redis.call('INCR', KEYS[1]); " +
                "if c == 1 then redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1])) end; " +
                "return c;"
        );
        ATOMIC_INCR_EXPIRE_SCRIPT.setResultType(Long.class);
    }

    private static final class LocalWindow {
        final long expiresAtMillis;
        final AtomicInteger count = new AtomicInteger(0);

        LocalWindow(long expiresAtMillis) {
            this.expiresAtMillis = expiresAtMillis;
        }
    }

    private final ConcurrentHashMap<String, LocalWindow> localFallbackWindows = new ConcurrentHashMap<>();
    private volatile long lastRedisWarningMillis = 0L;

    private final StringRedisTemplate redisTemplate;

    public EndpointThrottleService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(EndpointThrottleProperties.Rule rule, String key) {
        if (rule == null || rule.getLimit() <= 0 || rule.getDurationSeconds() <= 0 || !hasText(key)) {
            return true;
        }

        String ruleName = resolveRuleName(rule);
        String redisKey = KEY_PREFIX + ruleName + "::" + key;
        long ttlSeconds = Math.max(rule.getDurationSeconds(), 1L);
        int limit = Math.max(rule.getLimit(), 1);
        ThrottleFailurePolicy failurePolicy = resolveFailurePolicy(rule);

        try {
            Long requestCount = redisTemplate.execute(
                    ATOMIC_INCR_EXPIRE_SCRIPT,
                    Collections.singletonList(redisKey),
                    String.valueOf(ttlSeconds)
            );
            if (requestCount == null) {
                return checkLocalFallbackOrPolicy(redisKey, limit, ttlSeconds, failurePolicy, "Redis script returned null");
            }
            return requestCount <= limit;
        } catch (RuntimeException exception) {
            return checkLocalFallbackOrPolicy(redisKey, limit, ttlSeconds, failurePolicy,
                    "Redis backend failure: " + exception.getMessage());
        }
    }

    private boolean checkLocalFallbackOrPolicy(String redisKey,
                                               int limit,
                                               long ttlSeconds,
                                               ThrottleFailurePolicy failurePolicy,
                                               String reason) {
        long now = System.currentTimeMillis();
        if (now - lastRedisWarningMillis > 30_000L) {
            lastRedisWarningMillis = now;
            log.warn("SODEPA endpoint throttle Redis fallback actif ({}) pour clé {}. Repli mémoire borné activé.", reason, redisKey);
        }

        if (localFallbackWindows.size() >= MAX_LOCAL_FALLBACK_KEYS) {
            localFallbackWindows.entrySet().removeIf(e -> e.getValue().expiresAtMillis <= now);
            if (localFallbackWindows.size() >= MAX_LOCAL_FALLBACK_KEYS) {
                return ThrottleFailurePolicy.FAIL_OPEN.equals(failurePolicy);
            }
        }

        LocalWindow window = localFallbackWindows.compute(redisKey, (k, existing) -> {
            if (existing == null || existing.expiresAtMillis <= now) {
                return new LocalWindow(now + ttlSeconds * 1000L);
            }
            return existing;
        });

        return window.count.incrementAndGet() <= limit;
    }

    private ThrottleFailurePolicy resolveFailurePolicy(EndpointThrottleProperties.Rule rule) {
        return rule.getFailurePolicy() == null ? ThrottleFailurePolicy.FAIL_CLOSED : rule.getFailurePolicy();
    }

    private String resolveRuleName(EndpointThrottleProperties.Rule rule) {
        if (!hasText(rule.getName())) {
            return UNKNOWN_RULE;
        }
        return rule.getName().trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
