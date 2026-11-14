package com.apchavez.products.domain.event;

import com.apchavez.products.domain.model.Product;

import java.time.Instant;
import java.util.UUID;

public record ProductEvent(
        String eventId,
        ProductEventType eventType,
        String occurredAt,
        Product product) {

    public static ProductEvent of(ProductEventType type, Product product) {
        return new ProductEvent(UUID.randomUUID().toString(), type, Instant.now().toString(), product);
    }
}
