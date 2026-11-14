package com.apchavez.products.domain.model;

import com.apchavez.products.domain.exception.InvalidProductException;

public record Product(
        Integer id,
        String sku,
        String name,
        String description,
        String category,
        Double price,
        Integer stock,
        Boolean active) {

    public Product {
        if (sku == null || sku.isBlank()) {
            throw new InvalidProductException("El SKU no puede estar vacío");
        }
        if (sku.length() > 64) {
            throw new InvalidProductException("El SKU no puede superar los 64 caracteres");
        }
        if (name == null || name.isBlank()) {
            throw new InvalidProductException("El nombre no puede estar vacío");
        }
        if (name.length() > 200) {
            throw new InvalidProductException("El nombre no puede superar los 200 caracteres");
        }
        if (description != null && description.length() > 1000) {
            throw new InvalidProductException("La descripción no puede superar los 1000 caracteres");
        }
        if (category != null && category.length() > 100) {
            throw new InvalidProductException("La categoría no puede superar los 100 caracteres");
        }
        if (price == null || price < 0) {
            throw new InvalidProductException("El precio debe ser mayor o igual a cero");
        }
        if (stock == null || stock < 0) {
            throw new InvalidProductException("El stock debe ser mayor o igual a cero");
        }
        if (active == null) {
            throw new InvalidProductException("El estado activo debe ser 'true' o 'false'");
        }
    }
}
