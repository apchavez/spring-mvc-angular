package com.apchavez.products.infrastructure.batch;

import com.apchavez.products.domain.model.Product;
import org.springframework.batch.infrastructure.item.ItemProcessor;

/**
 * Converts a raw CSV row into a domain {@link Product}. Type-coercion failures
 * (non-numeric price/stock) and domain validation failures (thrown by the {@code Product}
 * constructor / domain service on write) both surface here as process-skips.
 */
public class ProductImportItemProcessor implements ItemProcessor<ProductCsvRow, Product> {

    @Override
    public Product process(ProductCsvRow row) {
        return new Product(
                null,
                row.sku(),
                row.name(),
                row.description(),
                row.category(),
                parseDouble(row.price()),
                parseInt(row.stock()),
                parseBoolean(row.active()));
    }

    private static Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Double.parseDouble(value.trim());
    }

    private static Integer parseInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Integer.parseInt(value.trim());
    }

    private static Boolean parseBoolean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Boolean.parseBoolean(value.trim());
    }
}
