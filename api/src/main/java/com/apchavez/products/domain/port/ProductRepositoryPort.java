package com.apchavez.products.domain.port;

import com.apchavez.products.domain.model.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepositoryPort {
    Product save(Product product);
    Product update(Product product);
    Optional<Product> findById(Integer id);
    Optional<Product> findBySku(String sku);
    List<Product> findAllActive(int page, int size);
    long countActive();
    List<Product> findAllInactive(int page, int size);
    long countInactive();
    List<Product> searchByNamePrefix(String prefix, int page, int size);
    long countByNamePrefix(String prefix);
    void delete(Integer id);
}
