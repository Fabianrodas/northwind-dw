package com.northwinddw.model;

import java.math.BigDecimal;

public final class ReportRows {
    private ReportRows() {
    }

    public record QuarterlySales(int year, String quarter, BigDecimal revenue) {
    }

    public record CategoryRevenue(String category, BigDecimal revenue, BigDecimal percentage) {
    }

    public record CountryRevenue(String country, BigDecimal revenue) {
    }

    public record CustomerRevenue(String customer, BigDecimal revenue) {
    }

    public record EmployeeRevenue(int year, String employee, BigDecimal revenue) {
    }

    public record SuppliersByCountry(String country, long supplierCount) {
    }
}
