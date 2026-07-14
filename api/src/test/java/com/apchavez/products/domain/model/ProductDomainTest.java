package com.apchavez.products.domain.model;

import com.apchavez.products.domain.exception.InvalidProductException;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.DoubleRange;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ProductDomainTest {

    // ── Construcción válida ──────────────────────────────────────────────────

    @Test
    void should_create_product_with_valid_data() {
        Product product = new Product(1, "SKU-001", "Wireless Mouse", "desc", "Electronics", 29.99, 150, true);
        assertThat(product.sku()).isEqualTo("SKU-001");
        assertThat(product.name()).isEqualTo("Wireless Mouse");
        assertThat(product.price()).isEqualTo(29.99);
        assertThat(product.stock()).isEqualTo(150);
        assertThat(product.active()).isTrue();
    }

    @Test
    void should_create_product_with_null_id() {
        assertDoesNotThrow(() -> new Product(null, "SKU-002", "Keyboard", "desc", "Electronics", 79.99, 10, true));
    }

    // ── Validaciones de sku ──────────────────────────────────────────────────

    @Test
    void should_throw_when_sku_is_null() {
        assertThatThrownBy(() -> new Product(null, null, "Mouse", "desc", "cat", 1.0, 1, true))
                .isInstanceOf(InvalidProductException.class)
                .hasMessage("El SKU no puede estar vacío");
    }

    @Test
    void should_throw_when_sku_is_blank() {
        assertThatThrownBy(() -> new Product(null, "   ", "Mouse", "desc", "cat", 1.0, 1, true))
                .isInstanceOf(InvalidProductException.class)
                .hasMessage("El SKU no puede estar vacío");
    }

    @Test
    void should_throw_when_sku_has_65_chars() {
        String overSku = "A".repeat(65);
        assertThatThrownBy(() -> new Product(null, overSku, "Mouse", "desc", "cat", 1.0, 1, true))
                .isInstanceOf(InvalidProductException.class);
    }

    // ── Validaciones de name ─────────────────────────────────────────────────

    @Test
    void should_throw_when_name_is_null() {
        assertThatThrownBy(() -> new Product(null, "SKU-001", null, "desc", "cat", 1.0, 1, true))
                .isInstanceOf(InvalidProductException.class)
                .hasMessage("El nombre no puede estar vacío");
    }

    @Test
    void should_throw_when_name_is_blank() {
        assertThatThrownBy(() -> new Product(null, "SKU-001", "\t", "desc", "cat", 1.0, 1, true))
                .isInstanceOf(InvalidProductException.class)
                .hasMessage("El nombre no puede estar vacío");
    }

    @Test
    void should_throw_when_name_has_201_chars() {
        String overName = "A".repeat(201);
        assertThatThrownBy(() -> new Product(null, "SKU-001", overName, "desc", "cat", 1.0, 1, true))
                .isInstanceOf(InvalidProductException.class);
    }

    // ── Validaciones de price ────────────────────────────────────────────────

    @Test
    void should_throw_when_price_is_null() {
        assertThatThrownBy(() -> new Product(null, "SKU-001", "Mouse", "desc", "cat", null, 1, true))
                .isInstanceOf(InvalidProductException.class)
                .hasMessage("El precio debe ser mayor o igual a cero");
    }

    @Test
    void should_throw_when_price_is_negative() {
        assertThatThrownBy(() -> new Product(null, "SKU-001", "Mouse", "desc", "cat", -1.0, 1, true))
                .isInstanceOf(InvalidProductException.class);
    }

    @Test
    void should_pass_when_price_is_zero() {
        assertDoesNotThrow(() -> new Product(null, "SKU-001", "Mouse", "desc", "cat", 0.0, 1, true));
    }

    // ── Validaciones de stock ────────────────────────────────────────────────

    @Test
    void should_throw_when_stock_is_null() {
        assertThatThrownBy(() -> new Product(null, "SKU-001", "Mouse", "desc", "cat", 1.0, null, true))
                .isInstanceOf(InvalidProductException.class)
                .hasMessage("El stock debe ser mayor o igual a cero");
    }

    @Test
    void should_throw_when_stock_is_negative() {
        assertThatThrownBy(() -> new Product(null, "SKU-001", "Mouse", "desc", "cat", 1.0, -5, true))
                .isInstanceOf(InvalidProductException.class);
    }

    @Test
    void should_pass_when_stock_is_zero() {
        assertDoesNotThrow(() -> new Product(null, "SKU-001", "Mouse", "desc", "cat", 1.0, 0, true));
    }

    // ── Validaciones de active ───────────────────────────────────────────────

    @Test
    void should_throw_when_active_is_null() {
        assertThatThrownBy(() -> new Product(null, "SKU-001", "Mouse", "desc", "cat", 1.0, 1, null))
                .isInstanceOf(InvalidProductException.class)
                .hasMessage("El estado activo debe ser 'true' o 'false'");
    }

    // ── Property-Based Testing: construcción válida ──────────────────────────

    @Property
    void valid_inputs_should_always_succeed(
            @ForAll @AlphaChars @StringLength(min = 1, max = 64) String sku,
            @ForAll @AlphaChars @StringLength(min = 1, max = 200) String name,
            @ForAll @DoubleRange(min = 0, max = 100000) double price,
            @ForAll @IntRange(min = 0, max = 100000) int stock,
            @ForAll boolean active) {
        assertDoesNotThrow(() -> new Product(null, sku, name, null, null, price, stock, active));
    }

    // ── Property-Based Testing: sku en blanco siempre falla ──────────────

    @Provide
    Arbitrary<String> stringsBlancos() {
        return Arbitraries.oneOf(
                Arbitraries.just(""),
                Arbitraries.strings().withChars(' ', '\t', '\n').ofMinLength(1).ofMaxLength(50));
    }

    @Property
    void blank_sku_should_always_throw(@ForAll("stringsBlancos") String sku) {
        assertThatThrownBy(() -> new Product(null, sku, "Mouse", "desc", "cat", 1.0, 1, true))
                .isInstanceOf(InvalidProductException.class);
    }
}
