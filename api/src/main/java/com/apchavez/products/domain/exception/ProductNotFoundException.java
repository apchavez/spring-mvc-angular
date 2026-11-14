package com.apchavez.products.domain.exception;

public class ProductNotFoundException extends ProductDomainException {
    public ProductNotFoundException(Integer id) {
        super("No se encontró un producto con el ID: " + id);
    }
}
