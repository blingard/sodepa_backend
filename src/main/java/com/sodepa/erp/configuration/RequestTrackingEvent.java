package com.sodepa.erp.configuration;

import java.time.Instant;

public record RequestTrackingEvent(
        String correlationId,
        String sessionId,
        Instant timestamp,
        String httpMethod,
        String uri,
        String queryString,
        String remoteIp,
        String username,
        String userId,
        String requestHeaders,
        String requestBody,
        int responseStatus,
        String responseHeaders,
        String responseBody,
        long durationMs
) {}
