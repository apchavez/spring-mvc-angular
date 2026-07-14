package com.apchavez.products.infrastructure.config;

import com.apchavez.products.domain.port.ProductRepositoryPort;
import com.apchavez.products.domain.service.ProductDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    public ProductDomainService productDomainService(ProductRepositoryPort repositoryPort) {
        return new ProductDomainService(repositoryPort);
    }
}
