package com.apchavez.products.infrastructure.web.dto;

import java.util.Set;

public record LoginResponseDTO(
        String token,
        String tokenType,
        long expiresIn,
        String username,
        Set<String> roles
) {
}
