package com.apchavez.products.infrastructure.batch;

import com.apchavez.products.application.ProductApplicationService;
import com.apchavez.products.domain.model.Product;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

/**
 * Delegates each product to {@link ProductApplicationService#createProduct}, the same
 * validation/use-case path as {@code POST /api/v1/products}. Unlike the WebFlux sibling
 * (which needs {@code .block()} because that service returns {@code Mono<Product>}), this
 * repo's application service is already synchronous.
 */
public class ProductImportItemWriter implements ItemWriter<Product> {

    private final ProductApplicationService applicationService;

    public ProductImportItemWriter(ProductApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Override
    public void write(Chunk<? extends Product> chunk) {
        for (Product product : chunk) {
            applicationService.createProduct(product);
        }
    }
}
