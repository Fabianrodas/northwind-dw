package com.northwinddw.model;

import com.northwinddw.model.ReportRows.CategoryRevenue;
import com.northwinddw.model.ReportRows.CountryRevenue;
import com.northwinddw.model.ReportRows.CustomerRevenue;
import com.northwinddw.model.ReportRows.EmployeeRevenue;
import com.northwinddw.model.ReportRows.QuarterlySales;
import com.northwinddw.model.ReportRows.SuppliersByCountry;
import com.northwinddw.ui.DashboardFormat;

import java.util.List;
import java.util.Objects;

public record DashboardReportContent(
        String title,
        String context,
        String period,
        List<String> headers,
        List<List<String>> rows,
        String qrText) {

    public DashboardReportContent {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(period, "period");
        headers = List.copyOf(headers);
        rows = rows.stream().map(List::copyOf).toList();
        Objects.requireNonNull(qrText, "qrText");
    }

    public static DashboardReportContent from(DashboardReport report, DashboardData data, Integer selectedYear) {
        Objects.requireNonNull(report, "report");
        Objects.requireNonNull(data, "data");

        List<String> headers;
        List<List<String>> rows;
        switch (report) {
            case QUARTERLY_SALES -> {
                headers = List.of("Año", "Trimestre", "Ventas");
                rows = data.quarterlySales().stream()
                        .map(row -> List.of(String.valueOf(row.year()), row.quarter(), DashboardFormat.usd(row.revenue())))
                        .toList();
            }
            case CATEGORY_REVENUE -> {
                headers = List.of("Categoría", "Ventas", "Participación");
                rows = data.categoryRevenue().stream()
                        .map(row -> List.of(row.category(), DashboardFormat.usd(row.revenue()),
                                DashboardFormat.percentage(row.percentage())))
                        .toList();
            }
            case COUNTRY_REVENUE -> {
                headers = List.of("País", "Ventas");
                rows = data.topCountries().stream()
                        .map(row -> List.of(row.country(), DashboardFormat.usd(row.revenue())))
                        .toList();
            }
            case CUSTOMER_REVENUE -> {
                headers = List.of("Cliente", "Ventas");
                rows = data.topCustomers().stream()
                        .map(row -> List.of(row.customer(), DashboardFormat.usd(row.revenue())))
                        .toList();
            }
            case EMPLOYEE_REVENUE -> {
                headers = List.of("Año", "Empleado", "Ventas");
                rows = data.employeeRevenue().stream()
                        .map(row -> List.of(String.valueOf(row.year()), row.employee(), DashboardFormat.usd(row.revenue())))
                        .toList();
            }
            case SUPPLIERS_BY_COUNTRY -> {
                headers = List.of("País", "Proveedores");
                rows = data.suppliersByCountry().stream()
                        .map(row -> List.of(row.country(), String.valueOf(row.supplierCount())))
                        .toList();
            }
            default -> throw new IllegalStateException("Informe no soportado: " + report);
        }

        String period = DashboardFormat.yearLabel(selectedYear);
        StringBuilder qrText = new StringBuilder()
                .append(report.title()).append('\n')
                .append(report.context()).append('\n')
                .append("Periodo: ").append(period).append('\n');
        if (rows.isEmpty()) {
            qrText.append("Sin datos para mostrar.");
        } else {
            qrText.append(String.join(" | ", headers)).append('\n');
            for (List<String> row : rows) {
                qrText.append(String.join(" | ", row)).append('\n');
            }
        }

        return new DashboardReportContent(report.title(), report.context(), period, headers, rows,
                qrText.toString().stripTrailing());
    }
}
