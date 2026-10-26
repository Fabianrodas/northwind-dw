package com.northwinddw.data;

import com.northwinddw.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@FunctionalInterface
public interface ConnectionFactory {
    Connection open() throws SQLException;

    static ConnectionFactory from(DatabaseConfig config) {
        return () -> markReadOnly(DriverManager.getConnection(config.jdbcUrl(), config.connectionProperties()));
    }

    static Connection markReadOnly(Connection connection) throws SQLException {
        try {
            connection.setReadOnly(true);
            return connection;
        } catch (SQLException exception) {
            try {
                connection.close();
            } catch (SQLException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }
    }
}
