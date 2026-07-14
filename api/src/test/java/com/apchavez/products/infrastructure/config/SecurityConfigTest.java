package com.apchavez.products.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    // ── CORS: no allowed-origins configured → no CORS config applied ────────

    @Test
    void corsConfigurationSource_returnsNullConfig_whenNoOriginsConfigured() {
        ReflectionTestUtils.setField(securityConfig, "corsAllowedOrigins", "");

        CorsConfigurationSource source =
                (CorsConfigurationSource) ReflectionTestUtils.invokeMethod(securityConfig, "corsConfigurationSource");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");

        CorsConfiguration config = source.getCorsConfiguration(request);

        assertThat(config).isNotNull();
        assertThat(config.getAllowedOrigins()).isNull();
    }

    // ── CORS: allowed-origins configured → origins/methods/headers applied ──

    @Test
    void corsConfigurationSource_appliesTrimmedOrigins_whenOriginsConfigured() {
        ReflectionTestUtils.setField(
                securityConfig, "corsAllowedOrigins", "https://example.com, https://foo.bar ,, ");

        CorsConfigurationSource source =
                (CorsConfigurationSource) ReflectionTestUtils.invokeMethod(securityConfig, "corsConfigurationSource");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");

        CorsConfiguration config = source.getCorsConfiguration(request);

        assertThat(config).isNotNull();
        assertThat(config.getAllowedOrigins()).containsExactly("https://example.com", "https://foo.bar");
        assertThat(config.getAllowedMethods()).containsExactlyInAnyOrder("GET", "POST", "PUT", "DELETE", "OPTIONS");
        assertThat(config.getAllowedHeaders()).containsExactly("*");
    }

    // ── JWT authorities: no "roles" claim → no role authorities granted ─────
    //
    // JwtAuthenticationConverter (Spring Security 7) always adds its own
    // FACTOR_BEARER FactorGrantedAuthority on top of whatever the custom
    // jwtGrantedAuthoritiesConverter returns — unrelated to role-based
    // authorization (hasRole/hasAuthority checks only match ROLE_* authorities),
    // so it's asserted here rather than filtered out.

    @Test
    void jwtAuthenticationConverter_grantsNoRoleAuthorities_whenRolesClaimMissing() {
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("test-user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claims(claims -> claims.putAll(Map.of("sub", "test-user")))
                .build();

        assertThat(converter.convert(jwt).getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("FACTOR_BEARER");
    }

    // ── JWT authorities: "roles" claim present → authorities mapped ─────────

    @Test
    void jwtAuthenticationConverter_grantsAuthorities_whenRolesClaimPresent() {
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("test-admin")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claim("roles", List.of("ROLE_ADMIN"))
                .build();

        assertThat(converter.convert(jwt).getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "FACTOR_BEARER");
    }
}
