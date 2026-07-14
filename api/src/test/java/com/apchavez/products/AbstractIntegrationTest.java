package com.apchavez.products;

import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

// Intentionally does NOT declare the @Container Postgres field here. A single static field
// shared via inheritance across the 3 subclasses (ProductPersistenceAdapterTest,
// ProductControllerIntegrationTest, ActuatorHealthTest) proved unreliable in practice — logs
// showed a fresh container getting created per subclass instead of one truly shared instance,
// and whichever class ended up on the "wrong" container intermittently failed to connect. Each
// concrete test class below declares its own dedicated @Container/@ServiceConnection field
// instead, which is the standard, unambiguous Testcontainers lifecycle-per-class pattern.
@Testcontainers
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {}
