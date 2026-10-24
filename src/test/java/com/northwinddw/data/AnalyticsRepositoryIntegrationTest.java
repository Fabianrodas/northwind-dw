package com.northwinddw.data;

import com.northwinddw.config.DatabaseConfig;
import com.northwinddw.model.DashboardData;
import com.northwinddw.model.ReportRows.CategoryRevenue;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class AnalyticsRepositoryIntegrationTest {
    @Test
    void loadsSixReportsThroughOneConnectionAndFiltersByYear() throws Exception {
        Map<String, String> environment = new HashMap<>();
        environment.put("NORTHWIND_DB_HOST", System.getenv().getOrDefault("NORTHWIND_TEST_DB_HOST", "127.0.0.1"));
        environment.put("NORTHWIND_DB_PORT", System.getenv("NORTHWIND_TEST_DB_PORT"));
        environment.put("NORTHWIND_DB_NAME", System.getenv().getOrDefault("NORTHWIND_TEST_DB_NAME", "northwind_pentaho"));
        environment.put("NORTHWIND_DB_USER", System.getenv().getOrDefault("NORTHWIND_TEST_DB_USER", "postgres"));
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
    }
}
