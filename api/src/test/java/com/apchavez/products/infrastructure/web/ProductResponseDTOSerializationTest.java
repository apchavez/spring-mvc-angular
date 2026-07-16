package com.apchavez.products.infrastructure.web;

import com.apchavez.products.infrastructure.web.dto.ProductResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.assertThat;

class ProductResponseDTOSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Provide
    Arbitrary<ProductResponseDTO> validResponseDTOs() {
        return Combinators.combine(
                Arbitraries.integers().greaterOrEqual(1),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(64),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(200),
                Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(100),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(100),
                Arbitraries.doubles().between(0, 100000),
                Arbitraries.integers().between(0, 100000),
                Arbitraries.of(true, false))
                .as(ProductResponseDTO::new);
    }

    @Property
    void json_roundtrip_should_preserve_all_fields(@ForAll("validResponseDTOs") ProductResponseDTO dto)
            throws Exception {
        String json = objectMapper.writeValueAsString(dto);
        ProductResponseDTO deserialized = objectMapper.readValue(json, ProductResponseDTO.class);
        assertThat(deserialized).isEqualTo(dto);
    }
}
