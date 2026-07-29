package com.apchavez.products.domain.service;

import com.apchavez.products.domain.exception.DuplicateSkuException;
import com.apchavez.products.domain.exception.ProductNotFoundException;
import com.apchavez.products.domain.model.Product;
import com.apchavez.products.domain.port.ProductRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductDomainServiceTest {

    @Mock
    private ProductRepositoryPort repositoryPort;

    private ProductDomainService domainService;

    private static final Product PRODUCT_WITHOUT_ID =
            new Product(null, "SKU-001", "Wireless Mouse", "desc", "Electronics", 29.99, 150, true);
    private static final Product SAVED_PRODUCT =
            new Product(1, "SKU-001", "Wireless Mouse", "desc", "Electronics", 29.99, 150, true);

    @BeforeEach
    void setUp() {
        domainService = new ProductDomainService(repositoryPort);
    }

    // ── createProduct ───────────────────────────────────────────────────────

    @Test
    void createProduct_shouldDelegateToSave_whenSkuNotTaken() {
        when(repositoryPort.findBySku("SKU-001")).thenReturn(Optional.empty());
        when(repositoryPort.save(any())).thenReturn(SAVED_PRODUCT);

        Product result = domainService.createProduct(PRODUCT_WITHOUT_ID);

        assertThat(result).isEqualTo(SAVED_PRODUCT);
        verify(repositoryPort).save(PRODUCT_WITHOUT_ID);
        verify(repositoryPort, never()).findById(any());
    }

    @Test
    void createProduct_shouldThrowDuplicateSkuException_whenSkuTaken() {
        when(repositoryPort.findBySku("SKU-001")).thenReturn(Optional.of(SAVED_PRODUCT));

        assertThatThrownBy(() -> domainService.createProduct(PRODUCT_WITHOUT_ID))
                .isInstanceOf(DuplicateSkuException.class)
                .hasMessageContaining("SKU-001");

        verify(repositoryPort, never()).save(any());
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    void findById_shouldReturnProduct_whenExists() {
        when(repositoryPort.findById(1)).thenReturn(Optional.of(SAVED_PRODUCT));

        assertThat(domainService.findById(1)).isEqualTo(SAVED_PRODUCT);
    }

    @Test
    void findById_shouldThrowProductNotFoundException_whenNotExists() {
        when(repositoryPort.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> domainService.findById(99))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── listActiveProducts ──────────────────────────────────────────────────

    @Test
    void listActiveProducts_shouldDelegateToRepositoryPort() {
        Product active1 = new Product(1, "SKU-002", "Keyboard", "desc", "Electronics", 79.99, 10, true);
        Product active2 = new Product(3, "SKU-003", "Hub", "desc", "Accessories", 24.50, 80, true);
        when(repositoryPort.findAllActive(0, 10)).thenReturn(List.of(active1, active2));

        assertThat(domainService.listActiveProducts(0, 10)).containsExactly(active1, active2);

        verify(repositoryPort).findAllActive(0, 10);
    }

    // ── listInactiveProducts ────────────────────────────────────────────────

    @Test
    void listInactiveProducts_shouldDelegateToRepositoryPort() {
        Product inactive1 = new Product(2, "SKU-001", "Mouse", "desc", "Electronics", 29.99, 0, false);
        when(repositoryPort.findAllInactive(0, 10)).thenReturn(List.of(inactive1));

        assertThat(domainService.listInactiveProducts(0, 10)).containsExactly(inactive1);

        verify(repositoryPort).findAllInactive(0, 10);
    }

    // ── updateProduct ───────────────────────────────────────────────────────

    @Test
    void updateProduct_shouldReturnUpdatedProduct_whenExists() {
        Product updatedData = new Product(null, "SKU-001", "Wireless Mouse Pro", "desc2", "Electronics", 34.99, 120, false);
        Product expectedResult = new Product(1, "SKU-001", "Wireless Mouse Pro", "desc2", "Electronics", 34.99, 120, false);

        when(repositoryPort.findById(1)).thenReturn(Optional.of(SAVED_PRODUCT));
        when(repositoryPort.update(any())).thenReturn(expectedResult);

        Product result = domainService.updateProduct(1, updatedData);

        assertThat(result.name()).isEqualTo("Wireless Mouse Pro");
        assertThat(result.description()).isEqualTo("desc2");
        assertThat(result.active()).isFalse();
        assertThat(result.stock()).isEqualTo(120);
        assertThat(result.id()).isEqualTo(1);

        verify(repositoryPort).findById(1);
        verify(repositoryPort).update(expectedResult);
    }

    @Test
    void updateProduct_shouldThrowProductNotFoundException_whenNotExists() {
        Product updatedData = new Product(null, "SKU-001", "Wireless Mouse", "desc", "Electronics", 29.99, 150, true);
        when(repositoryPort.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> domainService.updateProduct(99, updatedData))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");

        verify(repositoryPort).findById(99);
        verify(repositoryPort, never()).update(any());
    }

    // ── deleteProduct ───────────────────────────────────────────────────────

    @Test
    void deleteProduct_shouldReturnDeletedProduct_whenExists() {
        when(repositoryPort.findById(1)).thenReturn(Optional.of(SAVED_PRODUCT));

        Product result = domainService.deleteProduct(1);

        assertThat(result).isEqualTo(SAVED_PRODUCT);
        verify(repositoryPort).findById(1);
        verify(repositoryPort).delete(1);
    }

    @Test
    void deleteProduct_shouldThrowProductNotFoundException_whenNotExists() {
        when(repositoryPort.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> domainService.deleteProduct(99))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");

        verify(repositoryPort).findById(99);
        verify(repositoryPort, never()).delete(any());
    }
}
