package com.northwinddw.service;

import com.northwinddw.model.DashboardData;
import com.northwinddw.model.ReportRows.QuarterlySales;
import com.northwinddw.model.RevenueSummary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
        PdfDocument document = new PdfDocument(new PdfReader(pdf.toString()));
        String text = PdfTextExtractor.getTextFromPage(document.getPage(1));
        document.close();
        assertTrue(text.contains("123.45"));
        assertTrue(text.contains("1996"));
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
