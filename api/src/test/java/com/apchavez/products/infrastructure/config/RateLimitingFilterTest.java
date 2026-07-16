package com.apchavez.products.infrastructure.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private FilterChain chain;

    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter(redisTemplate);
    }

    private MockHttpServletRequest buildRequest(String method, String path, String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr(ip);
        return request;
    }

    // ── GET is never rate-limited ────────────────────────────────────────────

    @Test
    void should_allow_get_requests_without_calling_redis() throws Exception {
        MockHttpServletRequest request = buildRequest("GET", "/api/v1/products/active", "1.1.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(chain).doFilter(request, response);
        verifyNoInteractions(redisTemplate);
    }

    // ── POST within limit ────────────────────────────────────────────────────

    @Test
    void should_allow_post_when_count_is_within_limit() throws Exception {
        doReturn(1L).when(redisTemplate).execute(any(), anyList(), any(Object[].class));

        MockHttpServletRequest request = buildRequest("POST", "/api/v1/products", "2.2.2.2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(chain).doFilter(request, response);
    }

    // ── POST blocked when limit exceeded ────────────────────────────────────

    @Test
    void should_block_post_when_redis_count_exceeds_limit() throws Exception {
        doReturn((long) RateLimitingFilter.MAX_REQUESTS + 1)
                .when(redisTemplate).execute(any(), anyList(), any(Object[].class));

        MockHttpServletRequest request = buildRequest("POST", "/api/v1/products", "3.3.3.3");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isNotNull();
        verify(chain, never()).doFilter(any(), any());
    }

    // ── PUT blocked when limit exceeded ─────────────────────────────────────

    @Test
    void should_block_put_when_redis_count_exceeds_limit() throws Exception {
        doReturn((long) RateLimitingFilter.MAX_REQUESTS + 1)
                .when(redisTemplate).execute(any(), anyList(), any(Object[].class));

        MockHttpServletRequest request = buildRequest("PUT", "/api/v1/products/1", "4.4.4.4");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(429);
    }

    // ── DELETE blocked when limit exceeded ───────────────────────────────────

    @Test
    void should_block_delete_when_redis_count_exceeds_limit() throws Exception {
        doReturn((long) RateLimitingFilter.MAX_REQUESTS + 1)
                .when(redisTemplate).execute(any(), anyList(), any(Object[].class));

        MockHttpServletRequest request = buildRequest("DELETE", "/api/v1/products/1", "5.5.5.5");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(429);
    }

    // ── Per-IP isolation ─────────────────────────────────────────────────────

    @Test
    void should_track_limits_independently_per_ip() throws Exception {
        ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();
        doAnswer(inv -> {
            String key = ((List<?>) inv.getArgument(1)).get(0).toString();
            return counters.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        }).when(redisTemplate).execute(any(), anyList(), any(Object[].class));

        String ip1 = "6.6.6.6";
        String ip2 = "7.7.7.7";

        for (int i = 0; i <= RateLimitingFilter.MAX_REQUESTS; i++) {
            filter.doFilter(buildRequest("POST", "/api/v1/products", ip1), new MockHttpServletResponse(), chain);
        }

        MockHttpServletResponse blockedIp1 = new MockHttpServletResponse();
        filter.doFilter(buildRequest("POST", "/api/v1/products", ip1), blockedIp1, chain);
        assertThat(blockedIp1.getStatus()).isEqualTo(429);

        MockHttpServletResponse allowedIp2 = new MockHttpServletResponse();
        filter.doFilter(buildRequest("POST", "/api/v1/products", ip2), allowedIp2, chain);
        assertThat(allowedIp2.getStatus()).isEqualTo(200);
    }

    // ── Fail-open when Redis is unavailable ──────────────────────────────────

    @Test
    void should_allow_request_when_redis_is_unavailable() throws Exception {
        doThrow(new RuntimeException("Connection refused: localhost/6379"))
                .when(redisTemplate).execute(any(), anyList(), any(Object[].class));

        MockHttpServletRequest request = buildRequest("POST", "/api/v1/products", "8.8.8.8");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(chain).doFilter(request, response);
    }

    // ── Matching method but non-matching path is never rate-limited ─────────

    @Test
    void should_allow_post_to_non_target_path_without_calling_redis() throws Exception {
        MockHttpServletRequest request = buildRequest("POST", "/api/v1/other-resource", "9.9.9.9");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        verifyNoInteractions(redisTemplate);
    }

    // ── X-Forwarded-For present but blank falls back to remote address ──────

    @Test
    void should_fall_back_to_remote_address_when_forwarded_header_is_blank() throws Exception {
        ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();
        doAnswer(inv -> {
            String key = ((List<?>) inv.getArgument(1)).get(0).toString();
            return counters.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        }).when(redisTemplate).execute(any(), anyList(), any(Object[].class));

        MockHttpServletRequest request = buildRequest("POST", "/api/v1/products", "11.11.11.11");
        request.addHeader("X-Forwarded-For", "   ");

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(counters.keySet()).anyMatch(key -> key.contains("11.11.11.11"));
    }

    // ── X-Forwarded-For with only blank segments falls back to remote address ─

    @Test
    void should_fall_back_to_remote_address_when_forwarded_header_has_only_blank_segments() throws Exception {
        ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();
        doAnswer(inv -> {
            String key = ((List<?>) inv.getArgument(1)).get(0).toString();
            return counters.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        }).when(redisTemplate).execute(any(), anyList(), any(Object[].class));

        MockHttpServletRequest request = buildRequest("POST", "/api/v1/products", "12.12.12.12");
        request.addHeader("X-Forwarded-For", " , ,");

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(counters.keySet()).anyMatch(key -> key.contains("12.12.12.12"));
    }

    // ── No forwarded header and no remote address resolves to "unknown" ─────

    @Test
    void should_use_unknown_ip_when_no_forwarded_header_and_no_remote_address() throws Exception {
        ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();
        doAnswer(inv -> {
            String key = ((List<?>) inv.getArgument(1)).get(0).toString();
            return counters.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        }).when(redisTemplate).execute(any(), anyList(), any(Object[].class));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/products");
        request.setRemoteAddr(null);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(counters.keySet()).anyMatch(key -> key.contains("unknown"));
    }

    // ── X-Forwarded-For: rightmost IP is used ────────────────────────────────

    @Test
    void should_use_rightmost_ip_from_x_forwarded_for_header() throws Exception {
        ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();
        doAnswer(inv -> {
            String key = ((List<?>) inv.getArgument(1)).get(0).toString();
            return counters.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        }).when(redisTemplate).execute(any(), anyList(), any(Object[].class));

        String spoofedIp = "1.2.3.4";
        String trustedIp = "10.10.10.10";

        for (int i = 0; i <= RateLimitingFilter.MAX_REQUESTS; i++) {
            MockHttpServletRequest req = buildRequest("POST", "/api/v1/products", "127.0.0.1");
            req.addHeader("X-Forwarded-For", spoofedIp + ", " + trustedIp);
            filter.doFilter(req, new MockHttpServletResponse(), chain);
        }

        MockHttpServletRequest blockedReq = buildRequest("POST", "/api/v1/products", "127.0.0.1");
        blockedReq.addHeader("X-Forwarded-For", spoofedIp + ", " + trustedIp);
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(blockedReq, blocked, chain);

        assertThat(blocked.getStatus()).isEqualTo(429);
    }
}
