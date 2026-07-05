package com.thebyline;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Smoke test: verifies the full application context loads cleanly.
 *
 * Requires Docker to be running locally or in CI. Skips gracefully if Docker
 * is not available (e.g. on developer machines without Docker Desktop).
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class TheBylineApplicationTests {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("pgvector/pgvector:pg16")
                    .withDatabaseName("thebyline_test")
                    .withUsername("test")
                    .withPassword("test");

    /**
     * Mock Redis so the context loads without a real Redis server.
     */
    @MockBean
    RedisConnectionFactory redisConnectionFactory;

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.cache.type",          () -> "simple");
        registry.add("app.security.remember-me-key", () -> "test-key-context-load");
    }

    @Test
    @DisplayName("Application context loads without errors")
    void contextLoads() {
        // Spring Boot fails here if any bean fails to initialise,
        // any @Value injection is missing, or Flyway errors out.
    }
}
