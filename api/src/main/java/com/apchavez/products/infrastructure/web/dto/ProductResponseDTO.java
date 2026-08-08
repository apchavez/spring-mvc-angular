package com.apchavez.products.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos de un producto en la respuesta")
public record ProductResponseDTO(

        @Schema(description = "ID del producto", example = "1")
        Integer id,

        @Schema(description = "SKU único del producto", example = "SKU-001")
        String sku,

        @Schema(description = "Nombre del producto", example = "Wireless Mouse")
        String name,

        @Schema(description = "Descripción del producto", example = "Mouse inalámbrico ergonómico")
        String description,

        @Schema(description = "Categoría del producto", example = "Electronics")
        String category,

        @Schema(description = "Precio del producto", example = "29.99")
        Double price,

        @Schema(description = "Cantidad en stock", example = "150")
        Integer stock,

        @Schema(description = "Indica si el producto está activo", example = "true")
        Boolean active) {
}
