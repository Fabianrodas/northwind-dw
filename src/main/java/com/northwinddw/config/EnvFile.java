package com.northwinddw.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Reads the small KEY=value file used for local PostgreSQL settings. */
public final class EnvFile {
    private EnvFile() {
    }

    public static Map<String, String> read(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return Map.of();
        }

        Map<String, String> values = new LinkedHashMap<>();
        try {
            for (String sourceLine : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String line = sourceLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("export ")) {
                    line = line.substring("export ".length()).trim();
                }

                int separator = line.indexOf('=');
                if (separator <= 0) {
                    continue;
                }

                String key = line.substring(0, separator).trim();
                if (!key.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                    continue;
                }

                String value = line.substring(separator + 1).trim();
                if (value.length() >= 2) {
                    char first = value.charAt(0);
                    char last = value.charAt(value.length() - 1);
                    if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                        value = value.substring(1, value.length() - 1);
                    } else {
                        int comment = value.indexOf(" #");
                        if (comment >= 0) {
                            value = value.substring(0, comment).trim();
                        }
                    }
                }
                values.put(key, value);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer el archivo .env.", exception);
        }
        return Map.copyOf(values);
    }
}
