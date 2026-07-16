package com.apchavez.products.infrastructure.batch;

import com.apchavez.products.domain.exception.InvalidProductException;
import com.apchavez.products.domain.model.Product;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductImportItemProcessorTest {

    private final ProductImportItemProcessor processor = new ProductImportItemProcessor();

    @Test
    void process_shouldBuildProduct_whenRowIsValid() {
        ProductCsvRow row = new ProductCsvRow("SKU-100", "Mouse", "desc", "Electronics", "29.99", "150", "true");

        Product product = processor.process(row);

        assertThat(product.id()).isNull();
        assertThat(product.sku()).isEqualTo("SKU-100");
        assertThat(product.price()).isEqualTo(29.99);
        assertThat(product.stock()).isEqualTo(150);
        assertThat(product.active()).isTrue();
    }

    @Test
    void process_shouldThrowInvalidProductException_whenSkuIsBlank() {
        ProductCsvRow row = new ProductCsvRow("", "NoSku", "desc", "Electronics", "10.0", "5", "true");

        assertThatThrownBy(() -> processor.process(row)).isInstanceOf(InvalidProductException.class);
    }

    @Test
    void process_shouldThrowNumberFormatException_whenPriceIsNotNumeric() {
        ProductCsvRow row = new ProductCsvRow("SKU-100", "Mouse", "desc", "Electronics", "not-a-number", "150", "true");

        assertThatThrownBy(() -> processor.process(row)).isInstanceOf(NumberFormatException.class);
    }

    @Test
    void process_shouldThrowInvalidProductException_whenPriceIsBlank() {
        // price/stock/active are parsed to null on a blank cell, but Product's own compact
        // constructor rejects a null price/stock/active — so a blank required cell is still a
        // (domain-level) process-skip, just via a different exception than a malformed one.
        ProductCsvRow row = new ProductCsvRow("SKU-100", "Mouse", "desc", "Electronics", "", "150", "true");

        assertThatThrownBy(() -> processor.process(row)).isInstanceOf(InvalidProductException.class);
    }

    @Test
    void process_shouldPassThroughBlankDescriptionAndCategory_unchanged() {
        // description/category have no non-null domain constraint, so an empty CSV cell for
        // either one is a valid (if unusual) product, not a skip.
        ProductCsvRow row = new ProductCsvRow("SKU-100", "Mouse", "", "", "29.99", "150", "true");

        Product product = processor.process(row);

        assertThat(product.description()).isEmpty();
        assertThat(product.category()).isEmpty();
    }
}
