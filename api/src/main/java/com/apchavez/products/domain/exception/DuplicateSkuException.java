package com.apchavez.products.domain.exception;

public class DuplicateSkuException extends ProductDomainException {
    public DuplicateSkuException(String sku) {
        super("Ya existe un producto con el SKU: " + sku);
    }
}
