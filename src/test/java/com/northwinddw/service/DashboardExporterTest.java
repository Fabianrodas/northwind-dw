package com.northwinddw.service;

import com.northwinddw.model.DashboardData;
import com.northwinddw.model.ReportRows.QuarterlySales;
import com.northwinddw.model.RevenueSummary;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardExporterTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void exportsAReadablePdfFile() throws IOException {
        Path pdf = temporaryDirectory.resolve("northwind-dashboard.pdf");

        new DashboardExporter().exportPdf(sampleDashboard(), pdf);

        byte[] contents = Files.readAllBytes(pdf);
        assertTrue(new String(contents, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-"));
        assertTrue(contents.length > 500);
        String text;
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            text = new PDFTextStripper().getText(document);
        }
        assertTrue(text.contains("123.45"));
        assertTrue(text.contains("1996"));
        assertTrue(text.contains("Ventas por trimestre y año"));
    }

    @Test
    void continuesLongReportsAcrossPages() throws IOException {
        List<QuarterlySales> quarters = new ArrayList<>();
        for (int index = 0; index < 150; index++) {
            quarters.add(new QuarterlySales(1950 + index / 4, "Q" + (index % 4 + 1), new BigDecimal("123.45")));
        }
        DashboardData longDashboard = new DashboardData(new RevenueSummary(new BigDecimal("123.45"), 2, 1),
                quarters, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(1950, 1987));
        Path pdf = temporaryDirectory.resolve("long-dashboard.pdf");

        new DashboardExporter().exportPdf(longDashboard, pdf);

        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            assertTrue(document.getNumberOfPages() > 1);
        }
    }

    @Test
    void copiesThePortableSqlWithoutDatabaseDropOrOwnerCommands() throws IOException {
        Path sql = temporaryDirectory.resolve("northwind_dw.sql");

        new DashboardExporter().copySql(sql);

        String contents = Files.readString(sql, StandardCharsets.UTF_8);
        assertTrue(contents.contains("CREATE TABLE public.customers"));
        assertTrue(!contents.contains("DROP DATABASE"));
        assertTrue(!contents.contains("CREATE DATABASE"));
        assertTrue(!contents.contains("OWNER TO fabian"));
    }

    @Test
    void reportsAnUnwritablePdfDestination() {
        Path blockingFile = temporaryDirectory.resolve("not-a-directory");
        try {
            Files.writeString(blockingFile, "temporary");
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }

        assertThrows(IOException.class, () -> new DashboardExporter().exportPdf(
                sampleDashboard(), blockingFile.resolve("dashboard.pdf")));
    }

    private static DashboardData sampleDashboard() {
        return new DashboardData(new RevenueSummary(new BigDecimal("123.45"), 2, 1),
                List.of(new QuarterlySales(1996, "Q1", new BigDecimal("123.45"))),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(1996));
    }
}
