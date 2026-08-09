package com.apchavez.products.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link StartupConfig#validateEnvVars()}, the only real conditional logic
 * in this {@code @Configuration} class. Real OS environment variables are set/cleared for
 * the duration of each test via JUnit Pioneer (System.getenv() reads the actual process
 * environment and cannot be mocked — Mockito explicitly refuses to mock java.lang.System
 * statics), so both branches — all required production vars present, and one or more
 * missing/blank — are exercised deterministically regardless of the host running the test.
 */
class StartupConfigTest {

    private final StartupConfig config = new StartupConfig();

    @Test
    @ClearEnvironmentVariable(key = "DB_HOST")
    @ClearEnvironmentVariable(key = "DB_PORT")
    @ClearEnvironmentVariable(key = "DB_NAME")
    @ClearEnvironmentVariable(key = "DB_USER")
    @ClearEnvironmentVariable(key = "DB_PASSWORD")
    void validateEnvVars_throwsAndListsAllMissingVars_whenNoneAreSet() {
        assertThatThrownBy(config::validateEnvVars)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DB_HOST")
                .hasMessageContaining("DB_PORT")
                .hasMessageContaining("DB_NAME")
                .hasMessageContaining("DB_USER")
                .hasMessageContaining("DB_PASSWORD");
    }

    @Test
    @SetEnvironmentVariable(key = "DB_HOST", value = "db.internal")
    @SetEnvironmentVariable(key = "DB_PORT", value = "5432")
    @SetEnvironmentVariable(key = "DB_NAME", value = "products")
    @SetEnvironmentVariable(key = "DB_USER", value = "app")
    @SetEnvironmentVariable(key = "DB_PASSWORD", value = "   ")
    void validateEnvVars_throws_whenOneRequiredVarIsBlank() {
        assertThatThrownBy(config::validateEnvVars)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DB_PASSWORD")
                .hasMessageNotContaining("DB_HOST")
                .hasMessageNotContaining("DB_PORT")
                .hasMessageNotContaining("DB_NAME")
                .hasMessageNotContaining("DB_USER");
    }

    @Test
    @SetEnvironmentVariable(key = "DB_HOST", value = "db.internal")
    @SetEnvironmentVariable(key = "DB_PORT", value = "5432")
    @SetEnvironmentVariable(key = "DB_NAME", value = "products")
    @SetEnvironmentVariable(key = "DB_USER", value = "app")
    @SetEnvironmentVariable(key = "DB_PASSWORD", value = "secret")
    void validateEnvVars_doesNotThrow_whenAllRequiredVarsPresent() {
        assertThatCode(config::validateEnvVars).doesNotThrowAnyException();
    }

    @Test
    void logStartupInfo_doesNotThrow() {
        assertThatCode(config::logStartupInfo).doesNotThrowAnyException();
    }
}
