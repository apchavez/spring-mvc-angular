package com.apchavez.products.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    static final int MAX_REQUESTS = 100;
    private static final int WINDOW_SECONDS = 60;
    private static final String KEY_PREFIX = "rl:";
    private static final String TARGET_PATH_PREFIX = "/api/v1/products";
    private static final Set<String> TARGET_METHODS = Set.of("POST", "PUT", "DELETE");

    // Atomic fixed-window: INCR then set TTL only on first call to avoid resetting the window.
    // Keys auto-expire in Redis so no manual cleanup thread is needed.
    private static final RedisScript<Long> RATE_LIMIT_SCRIPT = RedisScript.of("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public RateLimitingFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!TARGET_METHODS.contains(request.getMethod()) || !request.getRequestURI().startsWith(TARGET_PATH_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String ip = extractClientIp(request);
        long bucket = System.currentTimeMillis() / (WINDOW_SECONDS * 1000L);
        String key = KEY_PREFIX + ip + ":" + bucket;

        long count;
        try {
            Long result = redisTemplate.execute(RATE_LIMIT_SCRIPT, List.of(key), String.valueOf(WINDOW_SECONDS));
            count = result != null ? result : 0L;
        } catch (Exception ex) {
            // Redis unavailable: fail-open to avoid blocking legitimate traffic.
            log.warn("[RATE-LIMIT] Redis no disponible (fail-open) — IP '{}': {}", ip, ex.getMessage());
            chain.doFilter(request, response);
            return;
        }

        if (count > MAX_REQUESTS) {
            log.warn("[RATE-LIMIT] IP '{}' bloqueada — solicitud #{} ({} {})",
                    ip, count, request.getMethod(), request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(WINDOW_SECONDS));
            return;
        }
        chain.doFilter(request, response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // Rightmost IP is added by trusted infrastructure; leftmost can be spoofed.
            String[] parts = forwarded.split(",");
            for (int i = parts.length - 1; i >= 0; i--) {
                String ip = parts[i].trim();
                if (!ip.isBlank()) {
                    return ip;
                }
            }
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "unknown";
    }
}
