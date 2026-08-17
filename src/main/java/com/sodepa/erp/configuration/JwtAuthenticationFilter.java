package com.sodepa.erp.configuration;

import com.sodepa.erp.share.CurrentUserAuthenticationToken;
import com.sodepa.erp.share.UserData;
import com.sodepa.erp.utils.UserManagementEnginePort;
import com.sodepa.erp.utils.UserOutput;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final FilterBaseMethod filterBaseMethod;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String BEARER_PREFIX = "Bearer ";
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String requestPath = request.getRequestURI();
            log.debug("Processing JWT authentication for request path: {}", requestPath);
            if(requestPath.startsWith("/public") ||
                    requestPath.startsWith("/swagger-ui") ||
                    requestPath.startsWith("/v3/api-docs") ||
                    requestPath.startsWith("/api-docs") ||
                    requestPath.startsWith("/actuator")) {
                log.debug("Skipping JWT authentication for public or actuator endpoint: {}", requestPath);
                filterChain.doFilter(request, response);
                return;
            }
            String correlationId = request.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.isBlank()) {
                throw new RuntimeException("Correlation Id header not provide");
            }
            String jwt = extractJwtFromRequest(request);
            if (jwt != null) {
                filterBaseMethod.authenticateWithJwt(jwt);
                log.debug("JWT authentication successful for request: {}", request.getRequestURI());
            } else {
                log.debug("No JWT token found in request: {}", request.getRequestURI());
            }
        } catch (JwtException e) {
            log.error("JWT validation failed: {}", e.getMessage());
            // Ne pas bloquer la requête, laisser Spring Security gérer l'accès
        } catch (Exception e) {
            log.error("Error processing JWT: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extrait le JWT du header Authorization
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }





}
