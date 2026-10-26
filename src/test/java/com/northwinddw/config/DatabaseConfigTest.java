package com.northwinddw.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseConfigTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void readsDotEnvAndLetsSystemEnvironmentOverrideIt() throws IOException {
        Path envFile = temporaryDirectory.resolve(".env");
        Files.writeString(envFile, """
                # Local PostgreSQL settings
                NORTHWIND_DB_HOST=warehouse.local
                NORTHWIND_DB_PORT=5432
                NORTHWIND_DB_NAME=northwind_pentaho
                NORTHWIND_DB_USER=sample-user
                NORTHWIND_DB_PASSWORD="sample-only-value"
                """);

        DatabaseConfig config = DatabaseConfig.from(Map.of(
                "NORTHWIND_DB_PORT", "55432",
                "NORTHWIND_DB_USER", "environment-user"), envFile);

        assertEquals("warehouse.local", config.host());
        assertEquals(55432, config.port());
        assertEquals("northwind_pentaho", config.database());
        assertEquals("environment-user", config.username());
        assertEquals("sample-only-value", config.password());
    }

    @Test
    void rejectsMissingPasswordWhenTheDotEnvFileIsMissing() {
        assertThrows(IllegalArgumentException.class, () -> DatabaseConfig.from(
                Map.of(), temporaryDirectory.resolve("missing.env")));
    }

    @Test
    void rejectsBlankDatabasePassword() {
        assertThrows(IllegalArgumentException.class, () -> DatabaseConfig.from(Map.of(
                "NORTHWIND_DB_PASSWORD", "   "), temporaryDirectory.resolve("missing.env")));
    }

    @Test
    void usesSafeLocalDefaultsWhenCredentialsAreExplicit() {
        DatabaseConfig config = DatabaseConfig.from(Map.of(
                "NORTHWIND_DB_PASSWORD", "sample-only-value"), temporaryDirectory.resolve("missing.env"));

        assertEquals("localhost", config.host());
        assertEquals(5432, config.port());
        assertEquals("northwind_pentaho", config.database());
        assertEquals("northwind_dw_app", config.username());
        assertEquals("sample-only-value", config.password());
    }

    @Test
    void rejectsInvalidPortValues() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> DatabaseConfig.from(
                Map.of("NORTHWIND_DB_PORT", "70000", "NORTHWIND_DB_PASSWORD", "sample-only-value"),
                temporaryDirectory.resolve("missing.env")));
        assertTrue(exception.getMessage().contains("NORTHWIND_DB_PORT"));
    }

    @Test
    void configuresThePostgresSessionAsReadOnly() {
        DatabaseConfig config = DatabaseConfig.from(Map.of(
                "NORTHWIND_DB_PASSWORD", "sample-only-value"), temporaryDirectory.resolve("missing.env"));

        assertEquals("true", config.connectionProperties().getProperty("readOnly"));
        assertEquals("always", config.connectionProperties().getProperty("readOnlyMode"));
    }

    @Test
    void neverIncludesPasswordInItsTextRepresentation() {
        DatabaseConfig config = DatabaseConfig.from(Map.of(
                "NORTHWIND_DB_PASSWORD", "sample-only-value"), temporaryDirectory.resolve("missing.env"));

        assertFalse(config.toString().contains("sample-only-value"));
    }
}
