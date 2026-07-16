package com.apchavez.products.infrastructure.messaging;

import com.apchavez.products.domain.event.ProductEvent;
import com.apchavez.products.domain.port.ProductEventPublisherPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "spring.kafka.producer.bootstrap-servers")
public class KafkaProductEventPublisher implements ProductEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaProductEventPublisher.class);
    private static final String TOPIC = "product-events";

    // Dedicated instance rather than an autowired Spring bean: ProductEvent (de)serialization
    // needs no Spring codecs (see the identical rationale in ProductPersistenceAdapter).
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaProductEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(ProductEvent event) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, event.product().id().toString(), json).get();
            log.info("Event published: type={}, productId={}", event.eventType(), event.product().id());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while publishing event: type={}", event.eventType(), e);
        } catch (Exception e) {
            log.error("Failed to publish event: type={}", event.eventType(), e);
        }
    }
}
