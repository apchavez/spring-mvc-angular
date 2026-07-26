package com.apchavez.products.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(-100)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    public static final String REQUEST_ID_CONTEXT_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        long startMs = System.currentTimeMillis();

        response.addHeader("X-Request-Id", requestId);
        MDC.put(REQUEST_ID_CONTEXT_KEY, requestId);
        try {
            chain.doFilter(request, response);
            log.atInfo()
                    .addKeyValue("requestId", requestId)
                    .addKeyValue("http.method", request.getMethod())
                    .addKeyValue("url.path", request.getRequestURI())
                    .addKeyValue("http.response.status_code", response.getStatus())
                    .addKeyValue("event.duration_ms", System.currentTimeMillis() - startMs)
                    .log("HTTP request completed");
        } catch (ServletException | IOException | RuntimeException e) {
            log.atError()
                    .addKeyValue("requestId", requestId)
                    .addKeyValue("http.method", request.getMethod())
                    .addKeyValue("url.path", request.getRequestURI())
                    .addKeyValue("event.duration_ms", System.currentTimeMillis() - startMs)
                    .addKeyValue("error.type", e.getClass().getSimpleName())
                    .log("HTTP request failed", e);
            throw e;
        } finally {
            MDC.remove(REQUEST_ID_CONTEXT_KEY);
        }
    }
}
