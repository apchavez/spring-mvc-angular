package com.apchavez.products.infrastructure.messaging;

import com.apchavez.products.domain.event.ProductEvent;
import com.apchavez.products.domain.event.ProductEventType;
import com.apchavez.products.domain.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaProductEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private KafkaProductEventPublisher publisher;

    private static final Product PRODUCT =
            new Product(1, "SKU-001", "Wireless Mouse", "desc", "Electronics", 29.99, 150, true);

    @BeforeEach
    void setUp() {
        publisher = new KafkaProductEventPublisher(kafkaTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void publish_shouldSendJsonToKafkaTopic() {
        ProductEvent event = ProductEvent.of(ProductEventType.PRODUCT_CREATED, PRODUCT);
        SendResult<String, String> sendResult = mock(SendResult.class);

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        assertThatCode(() -> publisher.publish(event)).doesNotThrowAnyException();

        verify(kafkaTemplate).send(eq("product-events"), eq("1"), any(String.class));
    }

    @Test
    void publish_shouldCompleteGracefully_whenKafkaFails() {
        ProductEvent event = ProductEvent.of(ProductEventType.PRODUCT_CREATED, PRODUCT);

        CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka unavailable"));
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(failedFuture);

        assertThatCode(() -> publisher.publish(event)).doesNotThrowAnyException();
    }
}
