package com.apchavez.products.infrastructure.persistence;

import com.apchavez.products.domain.exception.ProductNotFoundException;
import com.apchavez.products.domain.model.Product;
import com.apchavez.products.domain.port.ProductRepositoryPort;
import com.apchavez.products.infrastructure.mapper.ProductMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class ProductPersistenceAdapter implements ProductRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(ProductPersistenceAdapter.class);

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final String PRODUCT_CACHE_PREFIX = "product-cache:";
    private static final String ACTIVE_CACHE_PREFIX = "products-active-cache:";
    private static final String SKU_CACHE_PREFIX = "product-sku-cache:";

    // Dedicated instance rather than the autowired Spring MVC/Jackson bean: record
    // (de)serialization for cache entries needs no Spring codecs, and keeping this
    // adapter's caching concern independent of the app's main ObjectMapper configuration
    // avoids coupling cache payload shape to unrelated web-layer Jackson customizations.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ProductJdbcRepository jdbcRepository;
    private final ProductMapper mapper;
    private final StringRedisTemplate redisTemplate;

    public ProductPersistenceAdapter(ProductJdbcRepository jdbcRepository,
                                      ProductMapper mapper,
                                      StringRedisTemplate redisTemplate) {
        this.jdbcRepository = jdbcRepository;
        this.mapper = mapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Product save(Product product) {
        Product saved = mapper.toDomain(jdbcRepository.save(mapper.toEntity(product)));
        invalidateCaches();
        return saved;
    }

    @Override
    public Product update(Product product) {
        if (!jdbcRepository.existsById(product.id())) {
            throw new ProductNotFoundException(product.id());
        }
        Product updated = mapper.toDomain(jdbcRepository.save(mapper.toEntity(product)));
        invalidateCaches();
        return updated;
    }

    @Override
    public Optional<Product> findById(Integer id) {
        String key = PRODUCT_CACHE_PREFIX + id;
        Optional<Product> cached = readCached(key, Product.class);
        if (cached.isPresent()) {
            return cached;
        }
        Optional<Product> found = jdbcRepository.findById(id).map(mapper::toDomain);
        found.ifPresent(product -> writeCached(key, product));
        return found;
    }

    @Override
    public Optional<Product> findBySku(String sku) {
        String key = SKU_CACHE_PREFIX + sku;
        Optional<Product> cached = readCached(key, Product.class);
        if (cached.isPresent()) {
            return cached;
        }
        Optional<Product> found = jdbcRepository.findBySku(sku).map(mapper::toDomain);
        found.ifPresent(product -> writeCached(key, product));
        return found;
    }

    @Override
    public List<Product> findAllActive(int page, int size) {
        String key = ACTIVE_CACHE_PREFIX + page + ":" + size;
        Optional<List<Product>> cached = readCachedList(key);
        if (cached.isPresent()) {
            return cached.get();
        }
        List<Product> list = jdbcRepository.findAllByActive(Boolean.TRUE, PageRequest.of(page, size))
                .stream().map(mapper::toDomain).toList();
        writeCached(key, list);
        return list;
    }

    @Override
    public long countActive() {
        return jdbcRepository.countByActive(Boolean.TRUE);
    }

    // No cache-aside here (unlike findAllActive): this is a low-traffic admin-only view,
    // not worth the added invalidation surface for a rarely-hit read.
    @Override
    public List<Product> findAllInactive(int page, int size) {
        return jdbcRepository.findAllByActive(Boolean.FALSE, PageRequest.of(page, size))
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countInactive() {
        return jdbcRepository.countByActive(Boolean.FALSE);
    }

    @Override
    public List<Product> searchByNamePrefix(String prefix, int page, int size) {
        return jdbcRepository.findByNameStartingWithIgnoreCase(prefix, PageRequest.of(page, size))
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countByNamePrefix(String prefix) {
        return jdbcRepository.countByNameStartingWithIgnoreCase(prefix);
    }

    @Override
    public void delete(Integer id) {
        jdbcRepository.deleteById(id);
        invalidateCaches();
    }

    // No cache-aside, same rationale as findAllInactive: an admin-only report view,
    // not worth adding this unbounded result set to the cache-invalidation surface.
    @Override
    public List<Product> findAll() {
        return jdbcRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    // ---- cache helpers — fail-open: any Redis error is logged and treated as a
    // cache miss/no-op so the product API keeps serving from Postgres uninterrupted. ----

    private void invalidateCaches() {
        try {
            Set<String> keys = redisTemplate.keys(PRODUCT_CACHE_PREFIX + "*");
            keys.addAll(redisTemplate.keys(ACTIVE_CACHE_PREFIX + "*"));
            keys.addAll(redisTemplate.keys(SKU_CACHE_PREFIX + "*"));
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception ex) {
            log.warn("[CACHE] No se pudo invalidar Redis (fail-open): {}", ex.getMessage());
        }
    }

    private <T> Optional<T> readCached(String key, Class<T> type) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return Optional.empty();
            }
            return deserialize(json, () -> OBJECT_MAPPER.readValue(json, type));
        } catch (Exception ex) {
            log.warn("[CACHE] Redis no disponible en lectura (fail-open) — key '{}': {}", key, ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<List<Product>> readCachedList(String key) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return Optional.empty();
            }
            return deserialize(json, () -> OBJECT_MAPPER.readValue(json, new TypeReference<List<Product>>() {}));
        } catch (Exception ex) {
            log.warn("[CACHE] Redis no disponible en lectura (fail-open) — key '{}': {}", key, ex.getMessage());
            return Optional.empty();
        }
    }

    private <T> Optional<T> deserialize(String json, JsonSupplier<T> supplier) {
        try {
            return Optional.of(supplier.get());
        } catch (Exception e) {
            log.warn("[CACHE] No se pudo deserializar el valor cacheado, se ignora: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private void writeCached(String key, Object value) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, CACHE_TTL);
        } catch (Exception ex) {
            log.warn("[CACHE] No se pudo escribir en Redis (fail-open) — key '{}': {}", key, ex.getMessage());
        }
    }

    @FunctionalInterface
    private interface JsonSupplier<T> {
        T get() throws Exception;
    }
}
