package com.apchavez.products.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error de una fila específica durante el import CSV")
public record ImportRowErrorDTO(

        @Schema(description = "Número de línea en el CSV (1 = encabezado, 2 = primera fila de datos)", example = "3")
        int lineNumber,

        @Schema(description = "SKU de la fila, si pudo leerse", example = "SKU-100")
        String sku,

        @Schema(description = "Motivo por el que la fila fue omitida", example = "El SKU no puede estar vacío")
        String message) {
}
