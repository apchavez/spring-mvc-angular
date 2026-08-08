package com.apchavez.products.infrastructure.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    private MockHttpServletRequest buildRequest(String path) {
        return new MockHttpServletRequest("GET", path);
    }


    @Test
    void should_addRequestIdHeader_andCompleteSuccessfully_whenChainSucceeds() throws Exception {
        MockHttpServletRequest request = buildRequest("/api/v1/products/active");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Request-Id")).isNotBlank();
        verify(chain).doFilter(request, response);
    }


    @Test
    void should_propagateError_andLog_whenChainFails() throws Exception {
        MockHttpServletRequest request = buildRequest("/api/v1/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RuntimeException boom = new RuntimeException("downstream failure");
        FilterChain chain = mock(FilterChain.class);
        Mockito.doThrow(boom).when(chain).doFilter(request, response);

        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isSameAs(boom);

        assertThat(response.getHeader("X-Request-Id")).isNotBlank();
    }
}
