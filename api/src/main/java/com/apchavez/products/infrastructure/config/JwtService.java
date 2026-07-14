package com.apchavez.products.infrastructure.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;

@Service
public class JwtService {

    private final NimbusJwtEncoder encoder;
    private final String issuer;
    private final long expirationMs;

    public JwtService(
            @Value("classpath:certs/public.pem") Resource publicKeyResource,
            @Value("${jwt.private-key-location}") Resource privateKeyResource,
            @Value("${jwt.issuer:product-service}") String issuer,
            @Value("${jwt.expiration-ms:3600000}") long expirationMs) throws Exception {

        RSAPublicKey publicKey = RsaKeyConverters.x509().convert(publicKeyResource.getInputStream());
        RSAPrivateKey privateKey = RsaKeyConverters.pkcs8().convert(privateKeyResource.getInputStream());

        RSAKey rsaKey = new RSAKey.Builder(publicKey).privateKey(privateKey).build();
        ImmutableJWKSet<SecurityContext> jwkSet = new ImmutableJWKSet<>(new JWKSet(rsaKey));
        this.encoder = new NimbusJwtEncoder(jwkSet);
        this.issuer = issuer;
        this.expirationMs = expirationMs;
    }

    public String generateToken(String subject, String role) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plusMillis(expirationMs))
                .subject(subject)
                .claim("roles", List.of("ROLE_" + role))
                .build();
        return encoder.encode(
                JwtEncoderParameters.from(
                        JwsHeader.with(SignatureAlgorithm.RS256).build(),
                        claims))
                .getTokenValue();
    }
}
