package com.apchavez.products.infrastructure.web.exception;

/** Thrown when the uploaded import file is missing/empty or the batch job fails to launch. */
public class InvalidImportFileException extends RuntimeException {

    public InvalidImportFileException(String message) {
        super(message);
    }
}
