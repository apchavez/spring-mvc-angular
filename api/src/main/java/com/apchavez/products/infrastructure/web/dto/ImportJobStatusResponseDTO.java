package com.apchavez.products.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Estado y resultado de un Job de import")
public record ImportJobStatusResponseDTO(

        @Schema(description = "ID de ejecución del Job", example = "42")
        Long jobExecutionId,

        @Schema(description = "Estado del Job (STARTING, STARTED, COMPLETED, FAILED, etc.)", example = "COMPLETED")
        String status,

        @Schema(description = "Filas leídas del CSV", example = "20")
        long readCount,

        @Schema(description = "Productos creados exitosamente", example = "18")
        long writeCount,

        @Schema(description = "Filas omitidas por errores", example = "2")
        long skipCount,

        @Schema(description = "Detalle de las filas omitidas")
        List<ImportRowErrorDTO> errors) {
}
