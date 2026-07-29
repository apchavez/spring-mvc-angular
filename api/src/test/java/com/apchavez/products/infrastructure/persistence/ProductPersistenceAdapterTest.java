package com.apchavez.products.infrastructure.persistence;

import com.apchavez.products.AbstractIntegrationTest;
import com.apchavez.products.domain.model.Product;
import com.apchavez.products.infrastructure.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ProductPersistenceAdapterTest extends AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection(name = "redis")
    static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @Autowired
    private ProductJdbcRepository jdbcRepository;

    @Autowired
    private ProductMapper mapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private ProductPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ProductPersistenceAdapter(jdbcRepository, mapper, redisTemplate);
        jdbcRepository.deleteAll();
        Set<String> keys = redisTemplate.keys("*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    // ── save ─────────────────────────────────────────────────────────────────

    @Test
    void save_shouldPersistProductAndReturnWithGeneratedId() {
        Product product = new Product(null, "SKU-001", "Wireless Mouse", "desc", "Electronics", 29.99, 150, true);

        Product saved = adapter.save(product);

        assertThat(saved.id()).isNotNull();
        assertThat(saved.sku()).isEqualTo("SKU-001");
        assertThat(saved.name()).isEqualTo("Wireless Mouse");
        assertThat(saved.price()).isEqualTo(29.99);
        assertThat(saved.active()).isTrue();
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    void findById_shouldReturnProduct_whenExists() {
        ProductEntity entity = jdbcRepository
                .save(new ProductEntity(null, "SKU-002", "Keyboard", "desc", "Electronics", 79.99, 10, true));

        Optional<Product> found = adapter.findById(entity.getId());

        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(entity.getId());
        assertThat(found.get().name()).isEqualTo("Keyboard");
        assertThat(found.get().active()).isTrue();
    }

    @Test
    void findById_shouldReturnEmpty_whenNotExists() {
        assertThat(adapter.findById(9999)).isEmpty();
    }

    // ── findBySku ────────────────────────────────────────────────────────────

    @Test
    void findBySku_shouldReturnProduct_whenExists() {
        jdbcRepository.save(new ProductEntity(null, "SKU-003", "Hub", "desc", "Accessories", 24.50, 80, true));

        Optional<Product> found = adapter.findBySku("SKU-003");

        assertThat(found).isPresent();
        assertThat(found.get().name()).isEqualTo("Hub");
    }

    @Test
    void findBySku_shouldReturnEmpty_whenNotExists() {
        assertThat(adapter.findBySku("SKU-NOPE")).isEmpty();
    }

    // ── findAllActive ─────────────────────────────────────────────────────────

    @Test
    void findAllActive_shouldReturnOnlyActiveProducts() {
        jdbcRepository.save(new ProductEntity(null, "SKU-001", "Mouse", "desc", "Electronics", 29.99, 150, true));
        jdbcRepository.save(new ProductEntity(null, "SKU-002", "Keyboard", "desc", "Electronics", 79.99, 0, false));
        jdbcRepository.save(new ProductEntity(null, "SKU-003", "Hub", "desc", "Accessories", 24.50, 80, true));

        assertThat(adapter.findAllActive(0, 10))
                .hasSize(2)
                .allMatch(Product::active);
    }

    @Test
    void findAllActive_shouldReturnEmpty_whenNoActiveProducts() {
        jdbcRepository.save(new ProductEntity(null, "SKU-002", "Keyboard", "desc", "Electronics", 79.99, 0, false));

        assertThat(adapter.findAllActive(0, 10)).isEmpty();
    }

    // ── findAllInactive ───────────────────────────────────────────────────────

    @Test
    void findAllInactive_shouldReturnOnlyInactiveProducts() {
        jdbcRepository.save(new ProductEntity(null, "SKU-001", "Mouse", "desc", "Electronics", 29.99, 150, true));
        jdbcRepository.save(new ProductEntity(null, "SKU-002", "Keyboard", "desc", "Electronics", 79.99, 0, false));
        jdbcRepository.save(new ProductEntity(null, "SKU-003", "Hub", "desc", "Accessories", 24.50, 80, false));

        assertThat(adapter.findAllInactive(0, 10))
                .hasSize(2)
                .allMatch(p -> !p.active());
    }

    @Test
    void findAllInactive_shouldReturnEmpty_whenNoInactiveProducts() {
        jdbcRepository.save(new ProductEntity(null, "SKU-001", "Mouse", "desc", "Electronics", 29.99, 150, true));

        assertThat(adapter.findAllInactive(0, 10)).isEmpty();
    }

    // ── searchByNamePrefix ───────────────────────────────────────────────────

    @Test
    void searchByNamePrefix_shouldReturnCaseInsensitiveMatches() {
        jdbcRepository.save(new ProductEntity(null, "SKU-001", "Wireless Mouse", "desc", "Electronics", 29.99, 150, true));
        jdbcRepository.save(new ProductEntity(null, "SKU-002", "wireless Keyboard", "desc", "Electronics", 79.99, 10, true));
        jdbcRepository.save(new ProductEntity(null, "SKU-003", "USB Hub", "desc", "Accessories", 24.50, 80, true));

        assertThat(adapter.searchByNamePrefix("wireless", 0, 10)).hasSize(2);
    }

    // ── update ───────────────────────────────────────────────────────────────

    @Test
    void update_shouldPersistNewValues_whenProductExists() {
        ProductEntity saved = jdbcRepository
                .save(new ProductEntity(null, "SKU-001", "Wireless Mouse", "desc", "Electronics", 29.99, 150, true));

        Product toUpdate = new Product(saved.getId(), "SKU-001", "Wireless Mouse Pro", "desc2", "Electronics", 34.99, 120, false);

        Product updated = adapter.update(toUpdate);

        assertThat(updated.id()).isEqualTo(saved.getId());
        assertThat(updated.name()).isEqualTo("Wireless Mouse Pro");
        assertThat(updated.price()).isEqualTo(34.99);
        assertThat(updated.active()).isFalse();
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    void delete_shouldRemoveProduct_whenExists() {
        ProductEntity saved = jdbcRepository
                .save(new ProductEntity(null, "SKU-001", "Wireless Mouse", "desc", "Electronics", 29.99, 150, true));

        adapter.delete(saved.getId());

        assertThat(adapter.findById(saved.getId())).isEmpty();
    }

    // ── Redis cache: proves it's a real cache, not decoration ──────────────────

    @Test
    void findById_servesStaleDataFromCache_untilInvalidated() {
        ProductEntity saved = jdbcRepository
                .save(new ProductEntity(null, "SKU-002", "Keyboard", "desc", "Electronics", 79.99, 10, true));

        // Populates the Redis cache entry for this id.
        assertThat(adapter.findById(saved.getId())).map(Product::name).contains("Keyboard");

        // Mutate Postgres directly, bypassing the adapter (and its cache invalidation).
        jdbcRepository.save(new ProductEntity(saved.getId(), "SKU-002", "Mutated Directly", "desc", "Electronics", 79.99, 10, true));

        // The cached (now stale) value is still served — proves Redis is actually being read.
        assertThat(adapter.findById(saved.getId())).map(Product::name).contains("Keyboard");

        // A write through the adapter invalidates the cache...
        Product toUpdate = new Product(saved.getId(), "SKU-002", "Updated Via Adapter", "desc", "Electronics", 79.99, 10, true);
        adapter.update(toUpdate);

        // ...so the next read reflects the fresh value.
        assertThat(adapter.findById(saved.getId())).map(Product::name).contains("Updated Via Adapter");
    }

    @Test
    void findAllActive_servesStaleDataFromCache_untilInvalidated() {
        jdbcRepository.save(new ProductEntity(null, "SKU-002", "Keyboard", "desc", "Electronics", 79.99, 10, true));

        // Populates the Redis cache entry for this page/size.
        assertThat(adapter.findAllActive(0, 10)).hasSize(1);

        // A save through the adapter invalidates the cache...
        adapter.save(new Product(null, "SKU-003", "Hub", "desc", "Accessories", 24.50, 80, true));

        // ...so the next read reflects both products.
        assertThat(adapter.findAllActive(0, 10)).hasSize(2);
    }
}
