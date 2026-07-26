package com.apchavez.products.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductJdbcRepository extends ListCrudRepository<ProductEntity, Integer> {
    List<ProductEntity> findAllByActive(Boolean active, Pageable pageable);
    long countByActive(Boolean active);
    Optional<ProductEntity> findBySku(String sku);
    List<ProductEntity> findByNameStartingWithIgnoreCase(String prefix, Pageable pageable);
    long countByNameStartingWithIgnoreCase(String prefix);
}
