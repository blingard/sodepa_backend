package com.sodepa.erp.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.audit.application.usecase.AuditEventPublisher;
import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.utils.UserManagementEnginePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class WebConfig implements WebMvcConfigurer {
    private final FilterBaseMethod filterBaseMethod;
    private final UtilsService utilsService;
    private final AuditEventPublisher auditEventPublisher;
    private final JwtDecoder jwtDecoder;
    private final UserManagementEnginePort userManagementEnginePort;
    private final ObjectMapper objectMapper;

    @Bean
    public TrackingUserActionsFilter getTrackingUserActionsFilter(){
        return new TrackingUserActionsFilter(filterBaseMethod, utilsService, auditEventPublisher, jwtDecoder,
                userManagementEnginePort, objectMapper);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(getTrackingUserActionsFilter())
                .excludePathPatterns(
                        "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**", "/actuator/**"
                );
    }

}
