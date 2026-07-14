package com.apchavez.products.domain.port;

import com.apchavez.products.domain.event.ProductEvent;

public interface ProductEventPublisherPort {
    void publish(ProductEvent event);
}
