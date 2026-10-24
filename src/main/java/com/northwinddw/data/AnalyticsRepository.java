package com.northwinddw.data;

import com.northwinddw.model.DashboardData;
import com.northwinddw.model.ReportRows.CategoryRevenue;
import com.northwinddw.model.ReportRows.CountryRevenue;
import com.northwinddw.model.ReportRows.CustomerRevenue;
import com.northwinddw.model.ReportRows.EmployeeRevenue;
import com.northwinddw.model.ReportRows.QuarterlySales;
import com.northwinddw.model.ReportRows.SuppliersByCountry;
import com.northwinddw.model.RevenueSummary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public final class AnalyticsRepository implements DashboardDataSource {
    private static final String YEAR_FILTER = "(CAST(? AS integer) IS NULL OR dt.year = ?)";

    private static final String SUMMARY_SQL = """
            SELECT COALESCE(SUM(od.total_orders), 0) AS total_revenue,
                   COUNT(DISTINCT od.customerid) AS customer_count,
                   COUNT(DISTINCT od.supplierid) AS supplier_count
            FROM orderdetails od
            INNER JOIN dim_time dt ON dt.timeid = od.timeid
            WHERE %s
            """.formatted(YEAR_FILTER);

    private static final String QUARTERLY_SQL = """
            SELECT dt.year, dt.quarter, SUM(od.total_orders) AS revenue,
                   MIN(dt.timeid) AS first_time_id
            FROM orderdetails od
            INNER JOIN dim_time dt ON dt.timeid = od.timeid
            WHERE %s
            GROUP BY dt.year, dt.quarter
            ORDER BY dt.year, first_time_id
            """.formatted(YEAR_FILTER);

    private static final String CATEGORY_SQL = """
            SELECT p.categoryname,
                   SUM(od.total_orders) AS revenue,
                   COALESCE(ROUND(100 * SUM(od.total_orders)
                       / NULLIF(SUM(SUM(od.total_orders)) OVER (), 0), 2), 0) AS percentage
            FROM products p
            INNER JOIN orderdetails od ON od.productid = p.productid
            INNER JOIN dim_time dt ON dt.timeid = od.timeid
            WHERE %s
            GROUP BY p.categoryname
            ORDER BY revenue DESC, p.categoryname
            """.formatted(YEAR_FILTER);

    private static final String COUNTRY_SQL = """
            SELECT c.country, SUM(od.total_orders) AS revenue
            FROM customers c
            INNER JOIN orderdetails od ON od.customerid = c.customerid
            INNER JOIN dim_time dt ON dt.timeid = od.timeid
            WHERE %s
            GROUP BY c.country
            ORDER BY revenue DESC, c.country
            LIMIT 10
            """.formatted(YEAR_FILTER);

    private static final String CUSTOMER_SQL = """
            SELECT c.companyname, SUM(od.total_orders) AS revenue
            FROM customers c
            INNER JOIN orderdetails od ON od.customerid = c.customerid
            INNER JOIN dim_time dt ON dt.timeid = od.timeid
            WHERE %s
            GROUP BY c.customerid, c.companyname
            ORDER BY revenue DESC, c.companyname
            LIMIT 10
            """.formatted(YEAR_FILTER);

    private static final String EMPLOYEE_SQL = """
            SELECT dt.year,
                   concat_ws(' ', e.firstname, e.lastname) AS employee,
                   SUM(od.total_orders) AS revenue
            FROM employees e
            INNER JOIN orderdetails od ON od.employeeid = e.employeeid
            INNER JOIN dim_time dt ON dt.timeid = od.timeid
            WHERE %s
            GROUP BY dt.year, e.employeeid, e.firstname, e.lastname
            ORDER BY dt.year, revenue DESC, employee
            """.formatted(YEAR_FILTER);

    private static final String SUPPLIERS_SQL = """
            SELECT s.country, COUNT(DISTINCT s.supplierid) AS supplier_count
            FROM suppliers s
            GROUP BY s.country
            ORDER BY supplier_count DESC, s.country
            """;

    private final ConnectionFactory connectionFactory;

    public AnalyticsRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public DashboardData loadDashboard(Integer year) throws SQLException {
        if (year != null && (year < 1000 || year > 9999)) {
            throw new IllegalArgumentException("El año debe tener cuatro dígitos.");
        }

        try (Connection connection = connectionFactory.open()) {
            RevenueSummary summary = readSummary(connection, year);
            List<QuarterlySales> quarterlySales = readRows(connection, QUARTERLY_SQL, year,
                    rows -> new QuarterlySales(rows.getInt("year"), rows.getString("quarter"), rows.getBigDecimal("revenue")));
            List<CategoryRevenue> categoryRevenue = readRows(connection, CATEGORY_SQL, year,
                    rows -> new CategoryRevenue(rows.getString("categoryname"), rows.getBigDecimal("revenue"), rows.getBigDecimal("percentage")));
            List<CountryRevenue> topCountries = readRows(connection, COUNTRY_SQL, year,
                    rows -> new CountryRevenue(rows.getString("country"), rows.getBigDecimal("revenue")));
            List<CustomerRevenue> topCustomers = readRows(connection, CUSTOMER_SQL, year,
                    rows -> new CustomerRevenue(rows.getString("companyname"), rows.getBigDecimal("revenue")));
            List<EmployeeRevenue> employeeRevenue = readRows(connection, EMPLOYEE_SQL, year,
                    rows -> new EmployeeRevenue(rows.getInt("year"), rows.getString("employee"), rows.getBigDecimal("revenue")));
            List<SuppliersByCountry> suppliersByCountry = readSuppliers(connection);
            List<Integer> years = new ArrayList<>(new TreeSet<>(quarterlySales.stream().map(QuarterlySales::year).toList()));

            return new DashboardData(summary, quarterlySales, categoryRevenue, topCountries, topCustomers,
                    employeeRevenue, suppliersByCountry, years);
        }
    }

    private RevenueSummary readSummary(Connection connection, Integer year) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SUMMARY_SQL)) {
            bindYear(statement, year);
            try (ResultSet rows = statement.executeQuery()) {
                rows.next();
                return new RevenueSummary(rows.getBigDecimal("total_revenue"), rows.getLong("customer_count"),
                        rows.getLong("supplier_count"));
            }
        }
    }

    private List<SuppliersByCountry> readSuppliers(Connection connection) throws SQLException {
        List<SuppliersByCountry> results = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(SUPPLIERS_SQL);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                results.add(new SuppliersByCountry(rows.getString("country"), rows.getLong("supplier_count")));
            }
        }
        return results;
    }

    private <T> List<T> readRows(Connection connection, String sql, Integer year, RowMapper<T> mapper) throws SQLException {
        List<T> results = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindYear(statement, year);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    results.add(mapper.map(rows));
                }
            }
        }
        return results;
    }

    private static void bindYear(PreparedStatement statement, Integer year) throws SQLException {
        if (year == null) {
            statement.setNull(1, Types.INTEGER);
            statement.setNull(2, Types.INTEGER);
        } else {
            statement.setInt(1, year);
            statement.setInt(2, year);
        }
    }

    @FunctionalInterface
    private interface RowMapper<T> {
        T map(ResultSet rows) throws SQLException;
    }
}
