package com.apchavez.products.infrastructure.mapper;

import com.apchavez.products.domain.model.Product;
import com.apchavez.products.infrastructure.persistence.ProductEntity;
import com.apchavez.products.infrastructure.web.dto.ProductRequestDTO;
import com.apchavez.products.infrastructure.web.dto.ProductResponseDTO;
import com.apchavez.products.infrastructure.web.dto.ProductUpdateRequestDTO;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toDomain(ProductRequestDTO dto) {
        return new Product(
                null,
                dto.sku(),
                dto.name(),
                dto.description(),
                dto.category(),
                dto.price(),
                dto.stock(),
                dto.active());
    }

    public Product toDomain(ProductUpdateRequestDTO dto) {
        return new Product(
                null,
                dto.sku(),
                dto.name(),
                dto.description(),
                dto.category(),
                dto.price(),
                dto.stock(),
                dto.active());
    }

    public Product toDomain(ProductEntity entity) {
        return new Product(
                entity.getId(),
                entity.getSku(),
                entity.getName(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getPrice(),
                entity.getStock(),
                entity.getActive());
    }

    public ProductEntity toEntity(Product product) {
        ProductEntity entity = new ProductEntity();
        entity.setId(product.id());
        entity.setSku(product.sku());
        entity.setName(product.name());
        entity.setDescription(product.description());
        entity.setCategory(product.category());
        entity.setPrice(product.price());
        entity.setStock(product.stock());
        entity.setActive(product.active());
        return entity;
    }

    public ProductResponseDTO toResponseDTO(Product product) {
        return new ProductResponseDTO(
                product.id(),
                product.sku(),
                product.name(),
                product.description(),
                product.category(),
                product.price(),
                product.stock(),
                product.active());
    }
}
