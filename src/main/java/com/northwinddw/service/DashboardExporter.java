package com.northwinddw.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import com.northwinddw.model.DashboardData;
import com.northwinddw.model.RevenueSummary;
import com.northwinddw.ui.DashboardFormat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DashboardExporter {
    private static final String SQL_RESOURCE = "/db/northwind_dw.sql";
    private static final PDFont REGULAR_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont BOLD_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    public void exportPdf(DashboardData data, Path destination) throws IOException {
        createParentDirectory(destination);
        try (PDDocument document = new PDDocument()) {
            PDDocumentInformation information = document.getDocumentInformation();
            information.setTitle("Northwind DW | Resumen comercial");
            information.setAuthor("Northwind DW");
            try (PdfReport report = new PdfReport(document)) {
                report.add("Northwind DW | Resumen comercial", BOLD_FONT, 20);
                report.add("Periodo: " + period(data.availableYears()), REGULAR_FONT, 10);
                report.addBlank();
                addSummary(report, data.summary());
                addQuarterlySales(report, data);
                addCategories(report, data);
                addCountries(report, data);
                addCustomers(report, data);
                addEmployees(report, data);
                addSuppliers(report, data);
            }
            document.save(destination.toFile());
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

    private static void addSummary(PdfReport report, RevenueSummary summary) throws IOException {
        NumberFormat countFormat = NumberFormat.getIntegerInstance(Locale.US);
        report.add("Ventas registradas: " + DashboardFormat.usd(summary.totalRevenue())
                + "    Clientes: " + countFormat.format(summary.customers())
                + "    Proveedores: " + countFormat.format(summary.suppliers()), REGULAR_FONT, 11);
        report.addBlank();
    }

    private static void addQuarterlySales(PdfReport report, DashboardData data) throws IOException {
        addTable(report, "Ventas por trimestre y año", List.of("Año", "Trimestre", "Ventas"),
                data.quarterlySales().stream()
                        .map(row -> List.of(String.valueOf(row.year()), row.quarter(), DashboardFormat.usd(row.revenue())))
                        .toList());
    }

    private static void addCategories(PdfReport report, DashboardData data) throws IOException {
        addTable(report, "Participación por categoría", List.of("Categoría", "Ventas", "Participación"),
                data.categoryRevenue().stream()
                        .map(row -> List.of(row.category(), DashboardFormat.usd(row.revenue()), DashboardFormat.percentage(row.percentage())))
                        .toList());
    }

    private static void addCountries(PdfReport report, DashboardData data) throws IOException {
        addTable(report, "Diez países con más ventas", List.of("País", "Ventas"),
                data.topCountries().stream()
                        .map(row -> List.of(row.country(), DashboardFormat.usd(row.revenue())))
                        .toList());
    }

    private static void addCustomers(PdfReport report, DashboardData data) throws IOException {
        addTable(report, "Diez clientes con más ventas", List.of("Cliente", "Ventas"),
                data.topCustomers().stream()
                        .map(row -> List.of(row.customer(), DashboardFormat.usd(row.revenue())))
                        .toList());
    }

    private static void addEmployees(PdfReport report, DashboardData data) throws IOException {
        addTable(report, "Ventas por empleado y año", List.of("Año", "Empleado", "Ventas"),
                data.employeeRevenue().stream()
                        .map(row -> List.of(String.valueOf(row.year()), row.employee(), DashboardFormat.usd(row.revenue())))
                        .toList());
    }

    private static void addSuppliers(PdfReport report, DashboardData data) throws IOException {
        addTable(report, "Proveedores por país", List.of("País", "Proveedores"),
                data.suppliersByCountry().stream()
                        .map(row -> List.of(row.country(), String.valueOf(row.supplierCount())))
                        .toList());
    }

    private static void addTable(PdfReport report, String title,
                                 List<String> headers, List<List<String>> rows) throws IOException {
        report.addBlank();
        report.add(title, BOLD_FONT, 13);
        report.add(String.join("   |   ", headers), BOLD_FONT, 9);
        if (rows.isEmpty()) {
            report.add("Sin datos para mostrar.", REGULAR_FONT, 9);
            return;
        }
        for (List<String> row : rows) {
            report.add(String.join("   |   ", row), REGULAR_FONT, 8);
        }
        report.addBlank();
    }

    private static final class PdfReport implements AutoCloseable {
        private static final float MARGIN = 36;
        private static final float LINE_GAP = 5;

        private final PDDocument document;
        private PDPageContentStream content;
        private float cursorY;

        private PdfReport(PDDocument document) throws IOException {
            this.document = document;
            nextPage();
        }

        private void add(String text, PDFont font, float fontSize) throws IOException {
            for (String line : wrap(text, font, fontSize)) {
                if (cursorY < MARGIN + fontSize) {
                    nextPage();
                }
                content.beginText();
                content.setFont(font, fontSize);
                content.newLineAtOffset(MARGIN, cursorY);
                content.showText(line);
                content.endText();
                cursorY -= fontSize + LINE_GAP;
            }
        }

        private void addBlank() {
            cursorY -= LINE_GAP + 2;
        }

        private List<String> wrap(String text, PDFont font, float fontSize) throws IOException {
            float maxWidth = PDRectangle.A4.getWidth() - (2 * MARGIN);
            List<String> lines = new ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (String word : text.trim().split("\\s+")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (font.getStringWidth(candidate) * fontSize / 1000 <= maxWidth) {
                    if (!line.isEmpty()) {
                        line.append(' ');
                    }
                    line.append(word);
                    continue;
                }

                if (!line.isEmpty()) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                if (font.getStringWidth(word) * fontSize / 1000 <= maxWidth) {
                    line.append(word);
                    continue;
                }

                StringBuilder segment = new StringBuilder();
                for (int offset = 0; offset < word.length();) {
                    int codePoint = word.codePointAt(offset);
                    String character = new String(Character.toChars(codePoint));
                    String next = segment + character;
                    if (!segment.isEmpty() && font.getStringWidth(next) * fontSize / 1000 > maxWidth) {
                        lines.add(segment.toString());
                        segment.setLength(0);
                    }
                    segment.append(character);
                    offset += Character.charCount(codePoint);
                }
                line.append(segment);
            }
            if (!line.isEmpty()) {
                lines.add(line.toString());
            }
            return lines;
        }

        private void nextPage() throws IOException {
            if (content != null) {
                content.close();
            }
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            cursorY = page.getMediaBox().getHeight() - MARGIN;
        }

        @Override
        public void close() throws IOException {
            if (content != null) {
                content.close();
            }
        }
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
