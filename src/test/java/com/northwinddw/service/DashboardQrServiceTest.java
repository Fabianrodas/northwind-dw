package com.northwinddw.service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.northwinddw.model.DashboardData;
import com.northwinddw.model.DashboardReport;
import com.northwinddw.model.DashboardReportContent;
import com.northwinddw.model.ReportRows.CategoryRevenue;
import com.northwinddw.model.ReportRows.CountryRevenue;
import com.northwinddw.model.ReportRows.CustomerRevenue;
import com.northwinddw.model.ReportRows.EmployeeRevenue;
import com.northwinddw.model.ReportRows.QuarterlySales;
import com.northwinddw.model.ReportRows.SuppliersByCountry;
import com.northwinddw.model.RevenueSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DashboardQrServiceTest {
    @TempDir
    Path temporaryDirectory;

    @ParameterizedTest
    @EnumSource(DashboardReport.class)
    void savesReadableQrPngForEveryReport(DashboardReport report) throws Exception {
        DashboardReportContent content = DashboardReportContent.from(report, sampleData(), null);

        assertReadablePng(content.qrText(), temporaryDirectory.resolve(report.name() + ".png"));
    }

    @ParameterizedTest
    @EnumSource(DashboardReport.class)
    void savesReadableQrPngWhenReportHasNoRows(DashboardReport report) throws Exception {
        DashboardData empty = new DashboardData(new RevenueSummary(BigDecimal.ZERO, 0, 0),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        DashboardReportContent content = DashboardReportContent.from(report, empty, null);

        assertReadablePng(content.qrText(), temporaryDirectory.resolve(report.name() + "-empty.png"));
    }

    @Test
    void encodesLongReportLabels() throws Exception {
        String longCustomerName = "Compañía Internacional de Alimentos del Norte";
        String longCountryName = "United States of America";
        String longEmployeeName = "Alexandra Marie-Prescott-Williams";
        DashboardData longLabels = new DashboardData(new RevenueSummary(new BigDecimal("100.00"), 1, 1),
                List.of(), List.of(), List.of(new CountryRevenue(longCountryName, new BigDecimal("100.00"))),
                List.of(new CustomerRevenue(longCustomerName, new BigDecimal("100.00"))),
                List.of(new EmployeeRevenue(1997, longEmployeeName, new BigDecimal("100.00"))),
                List.of(), List.of(1997));

        assertReadablePng(DashboardReportContent.from(DashboardReport.CUSTOMER_REVENUE, longLabels, 1997).qrText(),
                temporaryDirectory.resolve("long-customer-label.png"));
        assertReadablePng(DashboardReportContent.from(DashboardReport.COUNTRY_REVENUE, longLabels, 1997).qrText(),
                temporaryDirectory.resolve("long-country-label.png"));
        assertReadablePng(DashboardReportContent.from(DashboardReport.EMPLOYEE_REVENUE, longLabels, 1997).qrText(),
                temporaryDirectory.resolve("long-employee-label.png"));
    }

    @Test
    void rejectsBlankQrText() {
        assertThrows(IllegalArgumentException.class, () -> new DashboardQrService().encode(" \t", 300));
    }

    @Test
    void rejectsQrSizesOutsideTheSupportedRange() {
        DashboardQrService service = new DashboardQrService();

        assertThrows(IllegalArgumentException.class, () -> service.encode("Northwind", 127));
        assertThrows(IllegalArgumentException.class, () -> service.encode("Northwind", 2049));
    }

    private void assertReadablePng(String text, Path destination) throws Exception {
        DashboardQrService service = new DashboardQrService();
        service.savePng(service.encode(text, 300), destination);

        BufferedImage image = ImageIO.read(destination.toFile());
        assertEquals(300, image.getWidth());
        assertEquals(300, image.getHeight());
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(
                image.getWidth(), image.getHeight(), image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()))));
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
        Result decoded = new QRCodeReader().decode(bitmap, hints);
        assertEquals(text, decoded.getText());
    }

    private static DashboardData sampleData() {
        return new DashboardData(new RevenueSummary(new BigDecimal("32.50"), 2, 1),
                List.of(new QuarterlySales(1997, "T1", new BigDecimal("12.50"))),
                List.of(new CategoryRevenue("Bebidas", new BigDecimal("20.00"), new BigDecimal("100.00"))),
                List.of(new CountryRevenue("España", new BigDecimal("20.00"))),
                List.of(new CustomerRevenue("Alfreds", new BigDecimal("18.00"))),
                List.of(new EmployeeRevenue(1997, "Nancy", new BigDecimal("28.00"))),
                List.of(new SuppliersByCountry("Alemania", 2)), List.of(1997));
    }
}
