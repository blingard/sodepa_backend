package com.sodepa.erp.configuration;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ContentCachingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String path = httpRequest.getRequestURI();

            // Skip caching for swagger/actuator to save memory
            if (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs") || path.startsWith("/api-docs") || path.startsWith("/actuator")) {
                chain.doFilter(request, response);
                return;
            }

            ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(httpRequest, 10000);
            ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper((HttpServletResponse) response);

            try {
                chain.doFilter(wrappedRequest, wrappedResponse);
            } finally {
                wrappedResponse.copyBodyToResponse();
            }
        } else {
            chain.doFilter(request, response);
        }
    }
}
