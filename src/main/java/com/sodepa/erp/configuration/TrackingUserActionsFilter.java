package com.sodepa.erp.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.audit.application.usecase.AuditEventPublisher;
import com.sodepa.erp.share.UserData;
import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.utils.UserManagementEnginePort;
import com.sodepa.erp.utils.UserOutput;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponseWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Enumeration;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class TrackingUserActionsFilter implements HandlerInterceptor {


    private final FilterBaseMethod filterBaseMethod;
    private final UtilsService utilsService;
    private final AuditEventPublisher auditEventPublisher;
    private final JwtDecoder jwtDecoder;
    private final UserManagementEnginePort userManagementEnginePort;
    private final ObjectMapper objectMapper;
    private long start;
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final int MAX_BODY_SIZE = 10_000;

    private RequestTrackingEvent buildEvent(String correlationId,
                                            ContentCachingRequestWrapper request,
                                            ContentCachingResponseWrapper response,
                                            long durationMs, String requestPath) {
        UserData userData = null;
        if (requestPath.startsWith("/api/auth/login")) {
            String token = extractAccessTokenFromResponse(response);
            if (token != null) {
                userData = filterBaseMethod.getUser(token);
            }
        } else {
            userData = utilsService.getCurrentUserData();
        }

        if (userData == null) {
            userData = new UserData(
                    "ANONYMOUS",
                    "ANONYMOUS",
                    "ANONYMOUS",
                    java.util.Collections.emptySet(),
                    "ANONYMOUS",
                    java.util.Collections.emptySet(),
                    "NO_SESSION",
                    ""
            );
        }

        return new RequestTrackingEvent(
                correlationId,
                userData.sessionId(),
                Instant.now(),
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                request.getRemoteAddr(),
                userData.name(),
                userData.userId(),
                extractHeaders(request),
                extractRequestBody(request),
                response.getStatus(),
                extractResponseHeaders(response),
                extractResponseBody(response),
                durationMs
        );
    }

    private String extractAccessTokenFromResponse(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        if (content.length == 0) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(content);
            JsonNode tokenNode = root.get("access_token");
            return tokenNode != null && !tokenNode.isNull() ? tokenNode.asText() : null;
        } catch (Exception e) {
            log.error("Échec du parsing du body de réponse pour extraire l'access_token: {}", e.getMessage());
            return null;
        }
    }

    private UserData getUserDataFromJwt(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            Map<String, Object> claims = jwt.getClaims();
            String userId = claims.get("USER_ID") != null
                    ? claims.get("USER_ID").toString()
                    : null;
            if (userId == null) {
                return null;
            }
            UserOutput userOutput = userManagementEnginePort.getUserById(UUID.fromString(userId));
            return new UserData(
                    userOutput.username(),
                    userOutput.nom() + " " + userOutput.prenom(),
                    userOutput.id().toString(),
                    userOutput.telephones(),
                    userOutput.email(),
                    userOutput.permissions(),
                    jwt.getClaimAsString("sid"),
                    jwt.getTokenValue()
            );
        } catch (Exception e) {
            log.error("Failed to extract user data from JWT", e);
            return null;
        }
    }


    private String extractRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length == 0) {
            return null;
        }
        String body = new String(content, 0, Math.min(content.length, MAX_BODY_SIZE), StandardCharsets.UTF_8);
        return maskSensitiveData(body);
    }

    private String extractResponseBody(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        if (content.length == 0) {
            return null;
        }
        return new String(content, 0, Math.min(content.length, MAX_BODY_SIZE), StandardCharsets.UTF_8);
    }

    private String extractHeaders(HttpServletRequest request) {
        Enumeration<String> names = request.getHeaderNames();
        StringBuilder sb = new StringBuilder();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            // On ne trace jamais le token brut
            String value = "authorization".equalsIgnoreCase(name) ? "***" : request.getHeader(name);
            sb.append(name).append(": ").append(value).append("; ");
        }
        return sb.toString();
    }

    private String extractResponseHeaders(HttpServletResponse response) {
        return response.getHeaderNames().stream()
                .map(name -> name + ": " + response.getHeader(name))
                .collect(Collectors.joining("; "));
    }

    /**
     * Masque les champs sensibles (mot de passe, etc.) dans le body avant envoi à ClickHouse.
     * A adapter selon vos payloads.
     */
    private String maskSensitiveData(String body) {
        return body.replaceAll("(?i)(\"password\"\\s*:\\s*\")[^\"]*(\")", "$1***$2");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        this.start = System.currentTimeMillis();
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable ModelAndView modelAndView) throws Exception {
        String requestPath = request.getRequestURI();
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            throw new RuntimeException("Correlation Id header not provide");
        }
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        if (requestPath.startsWith("/api/auth/login") && response.getStatus() != 200) {
            return;
        }
        long duration = System.currentTimeMillis() - start;

        ContentCachingRequestWrapper wrappedRequest = getRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = getResponseWrapper(response);

        if (wrappedRequest != null && wrappedResponse != null) {
            RequestTrackingEvent event = buildEvent(correlationId, wrappedRequest, wrappedResponse, duration, requestPath);
            auditEventPublisher.publishActivity(event);
        } else {
            log.warn("Request or response was not wrapped in ContentCaching wrappers. Event tracking skipped for: {}. requestWrapper: {}, responseWrapper: {}", 
                    requestPath, wrappedRequest != null, wrappedResponse != null);
        }

        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    private ContentCachingRequestWrapper getRequestWrapper(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper) {
            return (ContentCachingRequestWrapper) request;
        }
        while (request instanceof HttpServletRequestWrapper) {
            HttpServletRequest wrapped = (HttpServletRequest) ((HttpServletRequestWrapper) request).getRequest();
            if (wrapped instanceof ContentCachingRequestWrapper) {
                return (ContentCachingRequestWrapper) wrapped;
            }
            if (wrapped == request) {
                break;
            }
            request = wrapped;
        }
        return null;
    }

    private ContentCachingResponseWrapper getResponseWrapper(HttpServletResponse response) {
        if (response instanceof ContentCachingResponseWrapper) {
            return (ContentCachingResponseWrapper) response;
        }
        while (response instanceof HttpServletResponseWrapper) {
            HttpServletResponse wrapped = (HttpServletResponse) ((HttpServletResponseWrapper) response).getResponse();
            if (wrapped instanceof ContentCachingResponseWrapper) {
                return (ContentCachingResponseWrapper) wrapped;
            }
            if (wrapped == response) {
                break;
            }
            response = wrapped;
        }
        return null;
    }
}
