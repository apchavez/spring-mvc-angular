package com.apchavez.products.domain.service;

import com.apchavez.products.domain.exception.DuplicateSkuException;
import com.apchavez.products.domain.exception.ProductNotFoundException;
import com.apchavez.products.domain.model.Product;
import com.apchavez.products.domain.port.ProductRepositoryPort;

import java.util.List;
import java.util.Optional;

public class ProductDomainService {

    private final ProductRepositoryPort repositoryPort;

    public ProductDomainService(ProductRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    public Product createProduct(Product product) {
        if (repositoryPort.findBySku(product.sku()).isPresent()) {
            throw new DuplicateSkuException(product.sku());
        }
        return repositoryPort.save(product);
    }

    public Product findById(Integer id) {
        return repositoryPort.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    public Optional<Product> findBySku(String sku) {
        return repositoryPort.findBySku(sku);
    }

    public List<Product> listActiveProducts(int page, int size) {
        return repositoryPort.findAllActive(page, size);
    }

    public long countActiveProducts() {
        return repositoryPort.countActive();
    }

    public List<Product> listInactiveProducts(int page, int size) {
        return repositoryPort.findAllInactive(page, size);
    }

    public long countInactiveProducts() {
        return repositoryPort.countInactive();
    }

    public List<Product> searchByNamePrefix(String prefix, int page, int size) {
        return repositoryPort.searchByNamePrefix(prefix, page, size);
    }

    public long countByNamePrefix(String prefix) {
        return repositoryPort.countByNamePrefix(prefix);
    }

    public List<Product> listAllProducts() {
        return repositoryPort.findAll();
    }

    public Product updateProduct(Integer id, Product updatedData) {
        repositoryPort.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return repositoryPort.update(
                new Product(id, updatedData.sku(), updatedData.name(), updatedData.description(),
                        updatedData.category(), updatedData.price(), updatedData.stock(),
                        updatedData.active()));
    }

    public Product deleteProduct(Integer id) {
        Product existing = repositoryPort.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        repositoryPort.delete(id);
        return existing;
    }
}
