package com.northwinddw.config;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public record DatabaseConfig(String host, int port, String database, String username, String password) {
    private static final String PREFIX = "NORTHWIND_DB_";

    public DatabaseConfig {
        host = required(host, "NORTHWIND_DB_HOST");
        database = required(database, "NORTHWIND_DB_NAME");
        username = required(username, "NORTHWIND_DB_USER");
        password = Objects.requireNonNullElse(password, "");
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("NORTHWIND_DB_PORT debe estar entre 1 y 65535.");
        }
    }

    public static DatabaseConfig load() {
        return from(System.getenv(), Path.of(".env"));
    }

    public static DatabaseConfig from(Map<String, String> environment, Path envFile) {
        Map<String, String> file = EnvFile.read(envFile);
        int port;
        try {
            port = Integer.parseInt(value(environment, file, "PORT", "5432"));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("NORTHWIND_DB_PORT debe ser un número entre 1 y 65535.");
        }

        return new DatabaseConfig(
                value(environment, file, "HOST", "localhost"),
                port,
                value(environment, file, "NAME", "northwind_pentaho"),
                value(environment, file, "USER", "postgres"),
                value(environment, file, "PASSWORD", ""));
    }

    public String jdbcUrl() {
        String jdbcHost = host.contains(":") && !host.startsWith("[") ? "[" + host + "]" : host;
        return "jdbc:postgresql://" + jdbcHost + ":" + port + "/" + database;
    }

    public Properties connectionProperties() {
        Properties properties = new Properties();
        properties.setProperty("user", username);
        properties.setProperty("password", password);
        properties.setProperty("ApplicationName", "Northwind DW");
        properties.setProperty("connectTimeout", "5");
        return properties;
    }

    @Override
    public String toString() {
        return "DatabaseConfig[host=" + host + ", port=" + port + ", database=" + database
                + ", username=" + username + ", password=<redactado>]";
    }

    private static String value(Map<String, String> environment, Map<String, String> file,
                                String suffix, String defaultValue) {
        String key = PREFIX + suffix;
        String value = environment.get(key);
        if (value == null) {
            value = file.get(key);
        }
        return value == null ? defaultValue : value;
    }

    private static String required(String value, String key) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " no puede estar vacío.");
        }
        return value.trim();
    }
}
