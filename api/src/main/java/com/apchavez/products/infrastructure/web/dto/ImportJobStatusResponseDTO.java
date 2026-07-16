package com.apchavez.products.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Estado y resultado de un Job de import")
public record ImportJobStatusResponseDTO(

        @Schema(description = "ID de ejecución del Job", example = "42")
        Long jobExecutionId,

        @Schema(description = "Estado del Job (STARTING, STARTED, COMPLETED, FAILED, etc.)", example = "COMPLETED")
        String status,

        @Schema(description = "Cantidad de productos creados exitosamente", example = "18")
        int imported,

        @Schema(description = "Cantidad de filas omitidas por error", example = "2")
        int failed,

        @Schema(description = "Detalle de las filas omitidas, una por error")
        List<ImportRowErrorDTO> errors) {
}
