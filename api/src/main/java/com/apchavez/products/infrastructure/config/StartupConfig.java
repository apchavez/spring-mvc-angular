package com.apchavez.products.infrastructure.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;

import java.util.List;

@Configuration
@Profile("prod")
public class StartupConfig {

    private static final Logger log = LoggerFactory.getLogger(StartupConfig.class);
    private static final List<String> REQUIRED_VARS =
            List.of("DB_HOST", "DB_PORT", "DB_NAME", "DB_USER", "DB_PASSWORD");

    @PostConstruct
    public void validateEnvVars() {
        List<String> missing = REQUIRED_VARS.stream()
                .filter(v -> System.getenv(v) == null || System.getenv(v).isBlank())
                .toList();
        if (!missing.isEmpty()) {
            missing.forEach(v -> log.error("Variable de entorno requerida no definida: {}", v));
            throw new IllegalStateException(
                    "Variables de entorno de producción no configuradas: " + missing);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logStartupInfo() {
        log.info("Servicio iniciado — Java: {} | Spring Boot: {}",
                System.getProperty("java.version"),
                SpringBootVersion.getVersion());
    }
}
