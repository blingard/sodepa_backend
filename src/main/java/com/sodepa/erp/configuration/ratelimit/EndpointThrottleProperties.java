package com.sodepa.erp.configuration.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.interceptor-throttle")
public class EndpointThrottleProperties {

    private boolean enabled;
    private List<Rule> rules = new ArrayList<>();
    private Rule defaultRule;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }

    public Rule getDefaultRule() {
        return defaultRule;
    }

    public void setDefaultRule(Rule defaultRule) {
        this.defaultRule = defaultRule;
    }

    public static class Rule {
        private String name;
        private List<String> pathPatterns = new ArrayList<>();
        private List<String> methods = new ArrayList<>();
        private int limit;
        private long durationSeconds;
        private ThrottleKeyType keyType = ThrottleKeyType.PUBLIC_ENDPOINT;
        private boolean includePathInKey = true;
        private ThrottleFailurePolicy failurePolicy = ThrottleFailurePolicy.FAIL_CLOSED;
        private String errorCode = "RATE_LIMIT_EXCEEDED";
        private String message = "Too many requests. Please try again later.";

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<String> getPathPatterns() { return pathPatterns; }
        public void setPathPatterns(List<String> pathPatterns) { this.pathPatterns = pathPatterns; }
        public List<String> getMethods() { return methods; }
        public void setMethods(List<String> methods) { this.methods = methods; }
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
        public long getDurationSeconds() { return durationSeconds; }
        public void setDurationSeconds(long durationSeconds) { this.durationSeconds = durationSeconds; }
        public ThrottleKeyType getKeyType() { return keyType; }
        public void setKeyType(ThrottleKeyType keyType) { this.keyType = keyType; }
        public boolean isIncludePathInKey() { return includePathInKey; }
        public void setIncludePathInKey(boolean includePathInKey) { this.includePathInKey = includePathInKey; }
        public ThrottleFailurePolicy getFailurePolicy() { return failurePolicy; }
        public void setFailurePolicy(ThrottleFailurePolicy failurePolicy) { this.failurePolicy = failurePolicy; }
        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
