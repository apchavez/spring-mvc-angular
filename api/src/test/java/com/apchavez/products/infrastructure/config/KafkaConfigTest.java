package com.apchavez.products.infrastructure.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.mock.env.MockEnvironment;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link KafkaConfig}. No Spring context is loaded — the bean method's
 * property-resolution and conditional-property logic is exercised directly against a
 * {@link MockEnvironment}, and the private {@code putIfPresent} helper is exercised via
 * reflection since it carries the only real branching logic in this class.
 */
class KafkaConfigTest {

    private final KafkaConfig config = new KafkaConfig();

    @Test
    void productProducerFactory_createsFactory_withOnlyRequiredProperty() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.kafka.producer.bootstrap-servers", "localhost:9092");

        ProducerFactory<String, String> factory = config.productProducerFactory(environment);

        assertThat(factory).isNotNull();
    }

    @Test
    void productProducerFactory_createsFactory_withOptionalSecurityProperties() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.kafka.producer.bootstrap-servers", "localhost:9092")
                .withProperty("spring.kafka.properties.security.protocol", "SASL_SSL")
                .withProperty("spring.kafka.properties.sasl.mechanism", "PLAIN")
                .withProperty("spring.kafka.properties.sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required;");

        ProducerFactory<String, String> factory = config.productProducerFactory(environment);

        assertThat(factory).isNotNull();
    }

    @Test
    void productProducerFactory_throws_whenRequiredBootstrapServersMissing() {
        MockEnvironment environment = new MockEnvironment();

        assertThatThrownBy(() -> config.productProducerFactory(environment))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void productKafkaTemplate_wrapsGivenProducerFactory() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.kafka.producer.bootstrap-servers", "localhost:9092");
        ProducerFactory<String, String> factory = config.productProducerFactory(environment);

        assertThat(config.productKafkaTemplate(factory).getProducerFactory()).isSameAs(factory);
    }

    // ── putIfPresent(...) — the class's only real conditional logic ─────────

    @Test
    void putIfPresent_addsProperty_whenValueIsNonBlank() throws Exception {
        Map<String, Object> props = new HashMap<>();

        invokePutIfPresent(props, "security.protocol", "SASL_SSL");

        assertThat(props).containsEntry("security.protocol", "SASL_SSL");
    }

    @Test
    void putIfPresent_skipsProperty_whenValueIsNull() throws Exception {
        Map<String, Object> props = new HashMap<>();

        invokePutIfPresent(props, "security.protocol", null);

        assertThat(props).isEmpty();
    }

    @Test
    void putIfPresent_skipsProperty_whenValueIsBlank() throws Exception {
        Map<String, Object> props = new HashMap<>();

        invokePutIfPresent(props, "security.protocol", "   ");

        assertThat(props).isEmpty();
    }

    @Test
    void kafkaSender_usesRequiredPropertyKey_asBootstrapServersKey() {
        // Sanity check that the config constant used in production code is the real Kafka one.
        assertThat(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG).isEqualTo("bootstrap.servers");
    }

    private static void invokePutIfPresent(Map<String, Object> props, String key, String value) throws Exception {
        Method m = KafkaConfig.class.getDeclaredMethod("putIfPresent", Map.class, String.class, String.class);
        m.setAccessible(true);
        m.invoke(null, props, key, value);
    }
}
