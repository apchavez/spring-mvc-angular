package com.apchavez.products.infrastructure.batch;

public record ProductCsvRow(
        String sku,
        String name,
        String description,
        String category,
        String price,
        String stock,
        String active) {
}
