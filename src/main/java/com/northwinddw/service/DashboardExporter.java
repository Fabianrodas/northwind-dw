package com.northwinddw.service;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.northwinddw.model.DashboardData;
import com.northwinddw.model.RevenueSummary;
import com.northwinddw.ui.DashboardFormat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public final class DashboardExporter {
    private static final String SQL_RESOURCE = "/db/northwind_dw.sql";

    public void exportPdf(DashboardData data, Path destination) throws IOException {
        createParentDirectory(destination);
        PdfWriter writer = new PdfWriter(destination.toString());
        Document document = new Document(new PdfDocument(writer), PageSize.A4);
        try {
            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            document.setMargins(32, 34, 32, 34);
            document.add(new Paragraph("Northwind DW | Resumen comercial")
                    .setFontSize(20)
                    .setFont(boldFont));
            document.add(new Paragraph("Periodo: " + period(data.availableYears()))
                    .setFontSize(10)
                    .setMarginBottom(8));
            addSummary(document, data.summary());
            addQuarterlySales(document, data, boldFont);
            addCategories(document, data, boldFont);
            addCountries(document, data, boldFont);
            addCustomers(document, data, boldFont);
            addEmployees(document, data, boldFont);
            addSuppliers(document, data, boldFont);
        } finally {
            document.close();
        }
    }

    public void copySql(Path destination) throws IOException {
        createParentDirectory(destination);
        try (InputStream source = DashboardExporter.class.getResourceAsStream(SQL_RESOURCE)) {
            if (source == null) {
                throw new IOException("No se encontró el script SQL incluido con la aplicación.");
            }
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void addSummary(Document document, RevenueSummary summary) {
        NumberFormat countFormat = NumberFormat.getIntegerInstance(Locale.US);
        document.add(new Paragraph("Ventas registradas: " + DashboardFormat.usd(summary.totalRevenue())
                + "    Clientes: " + countFormat.format(summary.customers())
                + "    Proveedores: " + countFormat.format(summary.suppliers()))
                .setFontSize(11)
                .setMarginBottom(12));
    }

    private static void addQuarterlySales(Document document, DashboardData data, PdfFont boldFont) {
        addTable(document, boldFont, "Ventas por trimestre y año", List.of("Año", "Trimestre", "Ventas"),
                data.quarterlySales().stream()
                        .map(row -> List.of(String.valueOf(row.year()), row.quarter(), DashboardFormat.usd(row.revenue())))
                        .toList());
    }

    private static void addCategories(Document document, DashboardData data, PdfFont boldFont) {
        addTable(document, boldFont, "Participación por categoría", List.of("Categoría", "Ventas", "Participación"),
                data.categoryRevenue().stream()
                        .map(row -> List.of(row.category(), DashboardFormat.usd(row.revenue()), DashboardFormat.percentage(row.percentage())))
                        .toList());
    }

    private static void addCountries(Document document, DashboardData data, PdfFont boldFont) {
        addTable(document, boldFont, "Diez países con más ventas", List.of("País", "Ventas"),
                data.topCountries().stream()
                        .map(row -> List.of(row.country(), DashboardFormat.usd(row.revenue())))
                        .toList());
    }

    private static void addCustomers(Document document, DashboardData data, PdfFont boldFont) {
        addTable(document, boldFont, "Diez clientes con más ventas", List.of("Cliente", "Ventas"),
                data.topCustomers().stream()
                        .map(row -> List.of(row.customer(), DashboardFormat.usd(row.revenue())))
                        .toList());
    }

    private static void addEmployees(Document document, DashboardData data, PdfFont boldFont) {
        addTable(document, boldFont, "Ventas por empleado y año", List.of("Año", "Empleado", "Ventas"),
                data.employeeRevenue().stream()
                        .map(row -> List.of(String.valueOf(row.year()), row.employee(), DashboardFormat.usd(row.revenue())))
                        .toList());
    }

    private static void addSuppliers(Document document, DashboardData data, PdfFont boldFont) {
        addTable(document, boldFont, "Proveedores por país", List.of("País", "Proveedores"),
                data.suppliersByCountry().stream()
                        .map(row -> List.of(row.country(), String.valueOf(row.supplierCount())))
                        .toList());
    }

    private static void addTable(Document document, PdfFont boldFont, String title,
                                 List<String> headers, List<List<String>> rows) {
        document.add(new Paragraph(title).setFontSize(13).setFont(boldFont).setMarginTop(9).setMarginBottom(4));
        if (rows.isEmpty()) {
            document.add(new Paragraph("Sin datos para mostrar.").setFontSize(9));
            return;
        }

        Table table = new Table(UnitValue.createPercentArray(headers.size())).useAllAvailableWidth();
        for (String header : headers) {
            table.addHeaderCell(new Cell().add(new Paragraph(header).setFont(boldFont).setFontSize(9)));
        }
        for (List<String> row : rows) {
            for (String value : row) {
                table.addCell(new Cell().add(new Paragraph(value).setFontSize(8)));
            }
        }
        document.add(table);
    }

    private static String period(List<Integer> years) {
        if (years.isEmpty()) {
            return "sin resultados";
        }
        if (years.size() == 1) {
            return String.valueOf(years.getFirst());
        }
        return years.getFirst() + "–" + years.getLast();
    }

    private static void createParentDirectory(Path destination) throws IOException {
        Path parent = destination.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
