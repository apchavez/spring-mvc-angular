package com.apchavez.products.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Confirmación de que el Job de import fue lanzado")
public record ImportJobResponseDTO(

        @Schema(description = "ID de ejecución del Job — usarlo para consultar el estado", example = "42")
        Long jobExecutionId,

        @Schema(description = "Estado inicial del Job", example = "STARTING")
        String status) {
}
