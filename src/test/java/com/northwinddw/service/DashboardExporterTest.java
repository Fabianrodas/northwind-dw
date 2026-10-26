package com.northwinddw.service;

import com.northwinddw.model.DashboardData;
import com.northwinddw.model.DashboardReport;
import com.northwinddw.model.DashboardReportContent;
import com.northwinddw.model.ReportRows.QuarterlySales;
import com.northwinddw.model.RevenueSummary;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void exportsOnlyTheSelectedReportWithItsChartImageAndContext() throws IOException {
        Path pdf = temporaryDirectory.resolve("quarterly-report.pdf");
        DashboardReportContent content = DashboardReportContent.from(
                DashboardReport.QUARTERLY_SALES, sampleDashboard(), 1996);

        new DashboardExporter().exportReportPdf(content, sampleChartImage(), pdf);

        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            assertEquals(1, document.getNumberOfPages());
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Ventas por trimestre"));
            assertTrue(text.contains("Compara la evolución de cada año."));
            assertTrue(text.contains("1996"));
            assertTrue(text.contains("Q1"));
            assertTrue(text.contains("123.45"));
            assertTrue(!text.contains("Ventas por categoría"));
            assertTrue(!text.contains("Proveedores por país"));
            assertEquals(1, embeddedImageCount(document));
        }
    }

    @Test
    void paginatesSelectedReportRowsAfterEmbeddingItsChartOnce() throws IOException {
        List<QuarterlySales> quarters = new ArrayList<>();
        for (int index = 0; index < 150; index++) {
            quarters.add(new QuarterlySales(1950 + index / 4, "Q" + (index % 4 + 1), new BigDecimal("123.45")));
        }
        DashboardData longDashboard = new DashboardData(new RevenueSummary(new BigDecimal("123.45"), 2, 1),
                quarters, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(1950, 1987));
        DashboardReportContent content = DashboardReportContent.from(
                DashboardReport.QUARTERLY_SALES, longDashboard, null);
        Path pdf = temporaryDirectory.resolve("long-quarterly-report.pdf");

        new DashboardExporter().exportReportPdf(content, sampleChartImage(), pdf);

        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            assertTrue(document.getNumberOfPages() > 1);
            assertTrue(new PDFTextStripper().getText(document).contains("1987"));
            assertEquals(1, embeddedImageCount(document));
        }
    }

    @Test
    void reportsAnUnwritableReportPdfDestination() {
        Path blockingFile = temporaryDirectory.resolve("not-a-directory-for-report");
        try {
            Files.writeString(blockingFile, "temporary");
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
        DashboardReportContent content = DashboardReportContent.from(
                DashboardReport.QUARTERLY_SALES, sampleDashboard(), null);

        assertThrows(IOException.class, () -> new DashboardExporter().exportReportPdf(
                content, sampleChartImage(), blockingFile.resolve("report.pdf")));
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

    private static BufferedImage sampleChartImage() {
        BufferedImage image = new BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0xFFCC6600);
        return image;
    }

    private static int embeddedImageCount(PDDocument document) throws IOException {
        int count = 0;
        for (PDPage page : document.getPages()) {
            for (COSName name : page.getResources().getXObjectNames()) {
                if (page.getResources().getXObject(name) instanceof PDImageXObject) {
                    count++;
                }
            }
        }
        return count;
    }
}
