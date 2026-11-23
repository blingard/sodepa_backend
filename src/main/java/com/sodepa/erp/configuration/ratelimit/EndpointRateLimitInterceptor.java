package com.sodepa.erp.configuration.ratelimit;

import com.sodepa.erp.share.CurrentUserAuthenticationToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.Locale;

/**
 * Composant applicatif `EndpointRateLimitInterceptor` du système ERP SODEPA.
 */
public class EndpointRateLimitInterceptor implements HandlerInterceptor {

    private static final String PREFERRED_USERNAME = "preferred_username";

    private final EndpointThrottleProperties properties;
    private final EndpointThrottleService throttleService;
    private final ThrottleResponseWriter responseWriter;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public EndpointRateLimitInterceptor(EndpointThrottleProperties properties,
                                        EndpointThrottleService throttleService,
                                        ThrottleResponseWriter responseWriter) {
        this.properties = properties;
        this.throttleService = throttleService;
        this.responseWriter = responseWriter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!properties.isEnabled()) {
            return true;
        }

        String requestPath = resolveRequestPath(request);

        EndpointThrottleProperties.Rule rule = findRule(requestPath, request.getMethod());
        if (rule == null) {
            rule = properties.getDefaultRule();
            if (rule == null) {
                return true;
            }
        }

        String key = resolveKey(rule, request, requestPath);
        if (throttleService.isAllowed(rule, key)) {
            return true;
        }

        responseWriter.writeRateLimited(request, response, rule.getErrorCode(), rule.getMessage());
        return false;
    }

    private EndpointThrottleProperties.Rule findRule(String requestPath, String method) {
        List<EndpointThrottleProperties.Rule> rules = properties.getRules();
        if (rules == null || rules.isEmpty()) {
            return null;
        }

        for (EndpointThrottleProperties.Rule rule : rules) {
            List<String> pathPatterns = rule.getPathPatterns();
            if (!matchesMethod(rule.getMethods(), method) || pathPatterns == null || pathPatterns.isEmpty()) {
                continue;
            }
            for (String pattern : pathPatterns) {
                if (pathMatcher.match(pattern, requestPath)) {
                    return rule;
                }
            }
        }

        return null;
    }

    private boolean matchesMethod(List<String> configuredMethods, String requestMethod) {
        if (configuredMethods == null || configuredMethods.isEmpty()) {
            return true;
        }

        String normalizedMethod = requestMethod == null ? "" : requestMethod.toUpperCase(Locale.ROOT);
        for (String configuredMethod : configuredMethods) {
            if (normalizedMethod.equals(configuredMethod.toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String resolveKey(EndpointThrottleProperties.Rule rule, HttpServletRequest request, String requestPath) {
        ThrottleKeyType keyType = rule.getKeyType();
        String endpoint = sanitize(requestPath);

        if (ThrottleKeyType.AUTHENTICATED_USER.equals(keyType) || ThrottleKeyType.AUTHENTICATED_ENDPOINT.equals(keyType)) {
            String userIdentifier = getAuthenticatedUserIdentifier();
            if (hasText(userIdentifier)) {
                String userKey = sanitize(userIdentifier);
                return rule.isIncludePathInKey() ? userKey + "_" + endpoint : userKey;
            }
        }

        if (requestPath != null && requestPath.startsWith("/api/auth/")) {
            String ipKey = getClientIp(request);
            return rule.isIncludePathInKey() ? ipKey + "_" + endpoint : ipKey;
        }

        String clientKey = getClientIp(request) + "_" + sanitize(request.getHeader("User-Agent"));
        return rule.isIncludePathInKey() ? clientKey + "_" + endpoint : clientKey;
    }

    private String getAuthenticatedUserIdentifier() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof CurrentUserAuthenticationToken currentAuth) {
            return currentAuth.getUserData()
                    .map(ud -> hasText(ud.userId()) ? ud.userId() : ud.username())
                    .orElse(currentAuth.getName());
        }
        if (authentication instanceof JwtAuthenticationToken) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof Jwt jwt) {
                String username = jwt.getClaimAsString(PREFERRED_USERNAME);
                if (hasText(username)) {
                    return username;
                }
                return jwt.getSubject();
            }
        }
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equalsIgnoreCase(authentication.getName())) {
            return authentication.getName();
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        // S'appuie sur remoteAddr (résolu par Tomcat via server.forward-headers-strategy=native pour les proxies de confiance)
        // afin d'empêcher tout contournement de rate-limiting par spoofing de l'en-tête X-Forwarded-For.
        return sanitize(request.getRemoteAddr());
    }

    private String resolveRequestPath(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        if (hasText(servletPath)) {
            return servletPath;
        }

        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (hasText(contextPath) && requestUri != null && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }

    private String sanitize(String value) {
        if (!hasText(value)) {
            return "unknown";
        }
        return value.replaceAll("[^a-zA-Z0-9._/-]", "_").replace('/', '_');
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
