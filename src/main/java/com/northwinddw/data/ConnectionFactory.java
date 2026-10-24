package com.northwinddw.data;

import com.northwinddw.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@FunctionalInterface
public interface ConnectionFactory {
    Connection open() throws SQLException;

    static ConnectionFactory from(DatabaseConfig config) {
        return () -> DriverManager.getConnection(config.jdbcUrl(), config.connectionProperties());
    }
}
