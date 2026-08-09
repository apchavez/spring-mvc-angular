package com.apchavez.products.infrastructure.web.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String timestamp,
        int status,
        String error,
        String mensaje,
        List<FieldError> errores) {

    public record FieldError(String campo, String mensaje) {}

    public static ErrorResponse of(int status, String error, String mensaje) {
        return new ErrorResponse(Instant.now().toString(), status, error, mensaje, null);
    }

    public static ErrorResponse ofValidation(int status, String error, String mensaje, List<FieldError> errores) {
        return new ErrorResponse(Instant.now().toString(), status, error, mensaje, errores);
    }
}
