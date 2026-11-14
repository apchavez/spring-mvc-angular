package com.apchavez.products.infrastructure.batch;

import com.apchavez.products.application.ProductApplicationService;
import com.apchavez.products.domain.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.Chunk;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductImportItemWriterTest {

    @Mock
    private ProductApplicationService applicationService;

    private ProductImportItemWriter writer;

    @BeforeEach
    void setUp() {
        writer = new ProductImportItemWriter(applicationService);
    }

    @Test
    void write_shouldCreateEachProductInTheChunk_viaTheApplicationService() {
        Product first = new Product(null, "SKU-100", "Mouse", "desc", "Electronics", 29.99, 150, true);
        Product second = new Product(null, "SKU-101", "Keyboard", "desc", "Electronics", 79.99, 10, true);

        writer.write(new Chunk<>(first, second));

        verify(applicationService).createProduct(first);
        verify(applicationService).createProduct(second);
    }
}
