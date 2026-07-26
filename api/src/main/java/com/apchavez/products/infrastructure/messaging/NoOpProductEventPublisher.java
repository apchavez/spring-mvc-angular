package com.apchavez.products.infrastructure.messaging;

import com.apchavez.products.domain.event.ProductEvent;
import com.apchavez.products.domain.port.ProductEventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "spring.kafka.producer.bootstrap-servers", havingValue = "__noop__", matchIfMissing = true)
public class NoOpProductEventPublisher implements ProductEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(NoOpProductEventPublisher.class);

    @Override
    public void publish(ProductEvent event) {
        log.debug("Kafka not configured — skipping event: type={}, productId={}",
                event.eventType(), event.product().id());
    }
}
