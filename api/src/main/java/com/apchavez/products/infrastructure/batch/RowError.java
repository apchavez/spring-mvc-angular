package com.apchavez.products.infrastructure.batch;

public record RowError(int lineNumber, String sku, String message) {
}
