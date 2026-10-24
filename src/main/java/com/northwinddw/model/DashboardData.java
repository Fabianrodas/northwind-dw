package com.northwinddw.model;

import java.util.List;

import com.northwinddw.model.ReportRows.CategoryRevenue;
import com.northwinddw.model.ReportRows.CountryRevenue;
import com.northwinddw.model.ReportRows.CustomerRevenue;
import com.northwinddw.model.ReportRows.EmployeeRevenue;
import com.northwinddw.model.ReportRows.QuarterlySales;
import com.northwinddw.model.ReportRows.SuppliersByCountry;

public record DashboardData(
        RevenueSummary summary,
        List<QuarterlySales> quarterlySales,
        List<CategoryRevenue> categoryRevenue,
        List<CountryRevenue> topCountries,
        List<CustomerRevenue> topCustomers,
        List<EmployeeRevenue> employeeRevenue,
        List<SuppliersByCountry> suppliersByCountry,
        List<Integer> availableYears) {

    public DashboardData {
        quarterlySales = List.copyOf(quarterlySales);
        categoryRevenue = List.copyOf(categoryRevenue);
        topCountries = List.copyOf(topCountries);
        topCustomers = List.copyOf(topCustomers);
        employeeRevenue = List.copyOf(employeeRevenue);
        suppliersByCountry = List.copyOf(suppliersByCountry);
        availableYears = List.copyOf(availableYears);
    }
}
