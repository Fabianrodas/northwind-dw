package com.northwinddw.data;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectionFactoryTest {
    @Test
    void marksOpenedConnectionsReadOnly() throws SQLException {
        AtomicBoolean readOnly = new AtomicBoolean();
        AtomicBoolean closed = new AtomicBoolean();
        Connection connection = connection(readOnly, closed, false);

        Connection configured = ConnectionFactory.markReadOnly(connection);

        assertSame(connection, configured);
        assertTrue(readOnly.get());
        assertFalse(closed.get());
    }

    @Test
    void closesConnectionWhenReadOnlyModeCannotBeEnabled() {
        AtomicBoolean readOnly = new AtomicBoolean();
        AtomicBoolean closed = new AtomicBoolean();
        Connection connection = connection(readOnly, closed, true);

        assertThrows(SQLException.class, () -> ConnectionFactory.markReadOnly(connection));

        assertTrue(closed.get());
    }

    private static Connection connection(AtomicBoolean readOnly, AtomicBoolean closed, boolean failReadOnly) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("setReadOnly")) {
                        if (failReadOnly) {
                            throw new SQLException("Read-only mode unavailable.");
                        }
                        readOnly.set((boolean) arguments[0]);
                    } else if (method.getName().equals("close")) {
                        closed.set(true);
                    }
                    return null;
                });
    }
}
