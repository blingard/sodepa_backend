package com.sodepa.erp.configuration.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ThrottleResponseWriter {

    private static final String[] TRACE_HEADERS = {
            "X-Correlation-Id",
            "X-B3-TraceId",
            "X-B3-SpanId",
            "traceparent"
    };

    private final ObjectMapper objectMapper;

    public ThrottleResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void writeRateLimited(HttpServletRequest request,
                                 HttpServletResponse response,
                                 String errorCode,
                                 String message) throws IOException {
        copyTraceHeaders(request, response);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        body.put("error", hasText(errorCode) ? errorCode : "RATE_LIMIT_EXCEEDED");
        body.put("message", hasText(message) ? message : "Too many requests. Please try again later.");
        body.put("path", request.getRequestURI());

        objectMapper.writeValue(response.getWriter(), body);
    }

    private void copyTraceHeaders(HttpServletRequest request, HttpServletResponse response) {
        for (String header : TRACE_HEADERS) {
            String value = request.getHeader(header);
            if (hasText(value) && !response.containsHeader(header)) {
                response.setHeader(header, value);
            }
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
