package com.northwinddw.data;

import com.northwinddw.config.DatabaseConfig;
import com.northwinddw.model.DashboardData;
import com.northwinddw.model.ReportRows.CategoryRevenue;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class AnalyticsRepositoryIntegrationTest {
    @Test
    void loadsSixReportsThroughOneConnectionAndFiltersByYear() throws Exception {
        Map<String, String> environment = new HashMap<>();
        environment.put("NORTHWIND_DB_HOST", System.getenv().getOrDefault("NORTHWIND_TEST_DB_HOST", "127.0.0.1"));
        environment.put("NORTHWIND_DB_PORT", System.getenv("NORTHWIND_TEST_DB_PORT"));
        environment.put("NORTHWIND_DB_NAME", System.getenv().getOrDefault("NORTHWIND_TEST_DB_NAME", "northwind_pentaho"));
        environment.put("NORTHWIND_DB_USER", System.getenv().getOrDefault("NORTHWIND_TEST_DB_USER", "northwind_dw_app"));
        environment.put("NORTHWIND_DB_PASSWORD", System.getenv().getOrDefault("NORTHWIND_TEST_DB_PASSWORD", ""));
        assumeTrue(environment.get("NORTHWIND_DB_PORT") != null, "Set NORTHWIND_TEST_DB_PORT to enable PostgreSQL integration tests.");

        DatabaseConfig config = DatabaseConfig.from(environment, Path.of(".integration-test-env-not-present"));
        ConnectionFactory delegate = ConnectionFactory.from(config);
        AtomicInteger openedConnections = new AtomicInteger();
        ConnectionFactory countingFactory = () -> {
            openedConnections.incrementAndGet();
            return delegate.open();
        };
        AnalyticsRepository repository = new AnalyticsRepository(countingFactory);

        DashboardData allYears = repository.loadDashboard(null);
        assertEquals(new BigDecimal("1265792.94"), allYears.summary().totalRevenue());
        assertEquals(89, allYears.summary().customers());
        assertEquals(29, allYears.summary().suppliers());
        assertEquals(3, allYears.availableYears().size());
        assertEquals(10, allYears.topCountries().size());
        assertEquals(10, allYears.topCustomers().size());
        assertTrue(allYears.employeeRevenue().size() > 5);
        assertTrue(allYears.suppliersByCountry().size() > 0);

        BigDecimal categoryShare = allYears.categoryRevenue().stream()
                .map(CategoryRevenue::percentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertTrue(categoryShare.subtract(new BigDecimal("100")).abs().compareTo(new BigDecimal("0.1")) < 0);
        assertEquals(1, openedConnections.get());

        DashboardData oneYear = repository.loadDashboard(1997);
        assertTrue(oneYear.quarterlySales().stream().allMatch(row -> row.year() == 1997));
        assertTrue(oneYear.summary().totalRevenue().compareTo(allYears.summary().totalRevenue()) < 0);
        assertEquals(2, openedConnections.get());

        try (Connection connection = delegate.open()) {
            assertTrue(connection.getAutoCommit());
            assertTrue(connection.isReadOnly());
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT current_setting('transaction_read_only'), "
                            + "has_table_privilege(current_user, 'public.employees', 'UPDATE')");
                 ResultSet rows = statement.executeQuery()) {
                assertTrue(rows.next());
                assertEquals("on", rows.getString(1));
                assertFalse(rows.getBoolean(2));
            }
            assertThrows(SQLException.class, () -> {
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE public.employees SET firstname = firstname WHERE employeeid = 1")) {
                    try {
                        statement.executeUpdate();
                    } catch (SQLException exception) {
                        assertEquals("25006", exception.getSQLState());
                        throw exception;
                    }
                }
            });
        }
    }
}
