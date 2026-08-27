package com.sodepa.erp.configuration.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
public class EndpointThrottleService {

    private static final String UNKNOWN_RULE = "anonymous-rule";
    private static final String KEY_PREFIX = "sodepa:endpoint-throttle:";

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
        ThrottleFailurePolicy failurePolicy = resolveFailurePolicy(rule);
        try {
            Long requestCount = redisTemplate.opsForValue().increment(redisKey);
            if (requestCount == null) {
                return handleBackendFailure(redisKey, failurePolicy, "Redis increment returned null");
            }

            if (Long.valueOf(1L).equals(requestCount)) {
                Boolean expirySet = redisTemplate.expire(redisKey, Duration.ofSeconds(Math.max(rule.getDurationSeconds(), 1L)));
                if (!Boolean.TRUE.equals(expirySet)) {
                    return handleBackendFailure(redisKey, failurePolicy, "Redis expire was not acknowledged");
                }
            }

            return requestCount <= Math.max(rule.getLimit(), 1);
        } catch (RuntimeException exception) {
            log.error("SODEPA endpoint throttle Redis backend failure for key {}. Applying {}", redisKey, failurePolicy, exception);
            return ThrottleFailurePolicy.FAIL_OPEN.equals(failurePolicy);
        }
    }

    private boolean handleBackendFailure(String redisKey,
                                         ThrottleFailurePolicy failurePolicy,
                                         String message) {
        log.error("{} for key {}. Applying {}", message, redisKey, failurePolicy);
        return ThrottleFailurePolicy.FAIL_OPEN.equals(failurePolicy);
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
