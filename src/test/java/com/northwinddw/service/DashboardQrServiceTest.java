package com.northwinddw.service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.northwinddw.model.DashboardReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.time.Duration;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DashboardQrServiceTest {
    @TempDir
    Path temporaryDirectory;

    @ParameterizedTest
    @EnumSource(DashboardReport.class)
    void createsAScannableDownloadQrForEveryReport(DashboardReport report) throws Exception {
        String reportName = report.name().toLowerCase(Locale.ROOT).replace('_', '-');
        String downloadUrl = "http://192.168.1.50:49152/download/" + "a".repeat(64)
                + "/northwind-" + reportName + ".png";

        assertReadablePng(downloadUrl, temporaryDirectory.resolve(report.name() + ".png"));
    }

    @Test
    void createsAScannableQrForARealChartDownloadLink() throws Exception {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", png);
        try (ChartDownloadServer server = ChartDownloadServer.start(InetAddress.getByName("127.0.0.1"),
                png.toByteArray(), "northwind-chart.png", Duration.ofMinutes(1))) {
            assertReadablePng(server.downloadUri().toASCIIString(), temporaryDirectory.resolve("download-link.png"));
        }
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
                image.getWidth(), image.getHeight(),
                image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()))));
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
        Result decoded = new QRCodeReader().decode(bitmap, hints);
        assertEquals(text, decoded.getText());
    }
}
