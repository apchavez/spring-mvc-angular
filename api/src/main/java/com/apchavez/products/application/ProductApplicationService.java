package com.apchavez.products.application;

import com.apchavez.products.domain.event.ProductEvent;
import com.apchavez.products.domain.model.Product;
import com.apchavez.products.domain.port.ProductEventPublisherPort;
import com.apchavez.products.domain.service.ProductDomainService;
import com.apchavez.products.infrastructure.config.RequestLoggingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.apchavez.products.domain.event.ProductEventType.*;

@Service
public class ProductApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ProductApplicationService.class);

    private final ProductDomainService domainService;
    private final ProductEventPublisherPort eventPublisher;

    public ProductApplicationService(ProductDomainService domainService,
                                      ProductEventPublisherPort eventPublisher) {
        this.domainService = domainService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Product createProduct(Product product) {
        String rid = requestId();
        log.info("[{}] Crear producto — sku='{}', name='{}'", rid, product.sku(), product.name());
        Product saved = domainService.createProduct(product);
        eventPublisher.publish(ProductEvent.of(PRODUCT_CREATED, saved));
        log.info("[{}] Producto creado — id={}", rid, saved.id());
        return saved;
    }

    public Product findById(Integer id) {
        log.debug("[{}] Buscar producto — id={}", requestId(), id);
        return domainService.findById(id);
    }

    public Optional<Product> findBySku(String sku) {
        log.debug("Buscar producto — sku={}", sku);
        return domainService.findBySku(sku);
    }

    public List<Product> listActiveProducts(int page, int size) {
        log.debug("Listar productos activos — página={}, tamaño={}", page, size);
        return domainService.listActiveProducts(page, size);
    }

    public long countActiveProducts() {
        return domainService.countActiveProducts();
    }

    public List<Product> listInactiveProducts(int page, int size) {
        log.debug("Listar productos inactivos — página={}, tamaño={}", page, size);
        return domainService.listInactiveProducts(page, size);
    }

    public long countInactiveProducts() {
        return domainService.countInactiveProducts();
    }

    public List<Product> searchByNamePrefix(String prefix, int page, int size) {
        log.debug("Buscar productos por prefijo de nombre — prefix={}, página={}, tamaño={}", prefix, page, size);
        return domainService.searchByNamePrefix(prefix, page, size);
    }

    public long countByNamePrefix(String prefix) {
        return domainService.countByNamePrefix(prefix);
    }

    @Transactional
    public Product updateProduct(Integer id, Product updatedData) {
        String rid = requestId();
        log.info("[{}] Actualizar producto — id={}", rid, id);
        Product updated = domainService.updateProduct(id, updatedData);
        eventPublisher.publish(ProductEvent.of(PRODUCT_UPDATED, updated));
        log.info("[{}] Producto actualizado — id={}", rid, updated.id());
        return updated;
    }

    @Transactional
    public void deleteProduct(Integer id) {
        String rid = requestId();
        log.info("[{}] Eliminar producto — id={}", rid, id);
        Product deleted = domainService.deleteProduct(id);
        eventPublisher.publish(ProductEvent.of(PRODUCT_DELETED, deleted));
        log.info("[{}] Producto eliminado — id={}", rid, id);
    }

    private static String requestId() {
        String rid = MDC.get(RequestLoggingFilter.REQUEST_ID_CONTEXT_KEY);
        return rid != null ? rid : "-";
    }
}
