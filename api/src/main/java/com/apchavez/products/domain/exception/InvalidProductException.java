package com.apchavez.products.domain.exception;

public class InvalidProductException extends ProductDomainException {
    public InvalidProductException(String message) {
        super(message);
    }
}
