package com.northwinddw.model;

import com.northwinddw.model.ReportRows.CategoryRevenue;
import com.northwinddw.model.ReportRows.CountryRevenue;
import com.northwinddw.model.ReportRows.CustomerRevenue;
import com.northwinddw.model.ReportRows.EmployeeRevenue;
import com.northwinddw.model.ReportRows.QuarterlySales;
import com.northwinddw.model.ReportRows.SuppliersByCountry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardReportContentTest {
    @ParameterizedTest
    @MethodSource("reportExpectations")
    void buildsOnlyTheSelectedReportRows(DashboardReport report, String title, String context,
                                         List<String> firstRow, String otherReportMarker) {
        DashboardReportContent content = DashboardReportContent.from(report, sampleData(), null);

        assertEquals(title, content.title());
        assertEquals(context, content.context());
        assertEquals("Todos los años", content.period());
        assertEquals(firstRow, content.rows().getFirst());
        assertFalse(content.rows().stream().flatMap(List::stream).toList().contains(otherReportMarker));
    }

    @Test
    void usesTheSelectedYearAndDataWhenBuildingAReport() {
        DashboardReportContent content = DashboardReportContent.from(
                DashboardReport.QUARTERLY_SALES, sampleYear(1997), 1997);

        assertEquals("1997", content.period());
        assertEquals(List.of("1997", "T1", "$12.50"), content.rows().getFirst());
    }

    @ParameterizedTest
    @EnumSource(DashboardReport.class)
    void keepsEmptyReportRowsEmpty(DashboardReport report) {
        DashboardData emptyData = new DashboardData(new RevenueSummary(BigDecimal.ZERO, 0, 0),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

        DashboardReportContent content = DashboardReportContent.from(report, emptyData, null);

        assertTrue(content.rows().isEmpty());
    }

    private static Stream<Arguments> reportExpectations() {
        return Stream.of(
                Arguments.of(DashboardReport.QUARTERLY_SALES, "01  Ventas por trimestre",
                        "Compara la evolución de cada año.", List.of("1997", "T1", "$12.50"), "Bebidas"),
                Arguments.of(DashboardReport.CATEGORY_REVENUE, "02  Ventas por categoría",
                        "Participación de cada grupo de productos.", List.of("Bebidas", "$20.00", "33.3%"), "Alfreds"),
                Arguments.of(DashboardReport.COUNTRY_REVENUE, "03  Ventas por país",
                        "Los diez países con más ingresos.", List.of("España", "$20.00"), "Nancy"),
                Arguments.of(DashboardReport.CUSTOMER_REVENUE, "04  Ventas por cliente",
                        "Los diez clientes con mayor importe.", List.of("Alfreds", "$18.00"), "Alemania"),
                Arguments.of(DashboardReport.EMPLOYEE_REVENUE, "05  Ventas por empleado",
                        "Todos los empleados con actividad, por año.", List.of("1997", "Nancy", "$28.00"), "Japón"),
                Arguments.of(DashboardReport.SUPPLIERS_BY_COUNTRY, "06  Proveedores por país",
                        "Cantidad registrada por país de origen.", List.of("Alemania", "2"), "T1"));
    }

    private static DashboardData sampleData() {
        return new DashboardData(new RevenueSummary(new BigDecimal("32.50"), 2, 1),
                List.of(new QuarterlySales(1997, "T1", new BigDecimal("12.50")),
                        new QuarterlySales(1998, "T1", new BigDecimal("20.00"))),
                List.of(new CategoryRevenue("Bebidas", new BigDecimal("20.00"), new BigDecimal("33.33"))),
                List.of(new CountryRevenue("España", new BigDecimal("20.00")),
                        new CountryRevenue("Japón", new BigDecimal("12.50"))),
                List.of(new CustomerRevenue("Alfreds", new BigDecimal("18.00")),
                        new CustomerRevenue("Great Lakes", new BigDecimal("14.50"))),
                List.of(new EmployeeRevenue(1997, "Nancy", new BigDecimal("28.00")),
                        new EmployeeRevenue(1998, "Andrew", new BigDecimal("4.50"))),
                List.of(new SuppliersByCountry("Alemania", 2), new SuppliersByCountry("Canadá", 1)),
                List.of(1997, 1998));
    }

    private static DashboardData sampleYear(int year) {
        return new DashboardData(new RevenueSummary(new BigDecimal("12.50"), 1, 1),
                List.of(new QuarterlySales(year, "T1", new BigDecimal("12.50"))),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(year));
    }
}
