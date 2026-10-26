package com.northwinddw.service;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartDownloadServerTest {
    @Test
    void servesTheChartAsAnImageDownloadAndRejectsUnknownLinks() throws Exception {
        byte[] png = pngImage();
        try (ChartDownloadServer server = ChartDownloadServer.start(
                InetAddress.getByName("127.0.0.1"), png, "northwind-chart.png", Duration.ofMinutes(1))) {
            URI url = server.downloadUri();
            Response download = request(url, "GET");

            assertEquals(200, download.statusCode());
            assertEquals("image/png", download.contentType());
            assertEquals("attachment; filename=\"northwind-chart.png\"", download.contentDisposition());
            assertEquals("no-store", download.cacheControl());
            assertArrayEquals(png, download.body());
            assertTrue(url.getPath().matches("/download/[a-f0-9]{64}/northwind-chart\\.png"));

            URI unknownLink = url.resolve("/download/invalid/northwind-chart.png");
            assertEquals(404, request(unknownLink, "GET").statusCode());
        }
    }

    @Test
    void rejectsNonGetRequests() throws Exception {
        try (ChartDownloadServer server = ChartDownloadServer.start(
                InetAddress.getByName("127.0.0.1"), pngImage(), "northwind-chart.png", Duration.ofMinutes(1))) {
            Response response = request(server.downloadUri(), "POST");

            assertEquals(405, response.statusCode());
            assertEquals("GET", response.allow());
            assertEquals(0, response.body().length);
        }
    }

    @Test
    void closingTheTemporaryServerStopsTheDownloadLink() throws Exception {
        ChartDownloadServer server = ChartDownloadServer.start(
                InetAddress.getByName("127.0.0.1"), pngImage(), "northwind-chart.png", Duration.ofMinutes(1));
        URI url = server.downloadUri();

        server.close();

        assertThrows(IOException.class, () -> request(url, "GET"));
    }

    @Test
    void expiresTheDownloadLinkAfterItsLifetime() throws Exception {
        ChartDownloadServer server = ChartDownloadServer.start(
                InetAddress.getByName("127.0.0.1"), pngImage(), "northwind-chart.png", Duration.ofMillis(200));
        URI url = server.downloadUri();
        assertEquals(200, request(url, "GET").statusCode());

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        boolean expired = false;
        while (System.nanoTime() < deadline && !expired) {
            try {
                request(url, "GET");
                Thread.sleep(20);
            } catch (IOException exception) {
                expired = true;
            }
        }
        server.close();

        assertTrue(expired, "The chart download link should stop accepting requests after its lifetime.");
    }

    @Test
    void rejectsWildcardAddressesAndUnsafeFileNames() throws Exception {
        byte[] png = pngImage();
        assertThrows(IllegalArgumentException.class, () -> ChartDownloadServer.start(
                InetAddress.getByName("0.0.0.0"), png, "northwind-chart.png", Duration.ofMinutes(1)));
        assertThrows(IllegalArgumentException.class, () -> ChartDownloadServer.start(
                InetAddress.getByName("127.0.0.1"), png, "../private.png", Duration.ofMinutes(1)));
    }

    @Test
    void servesTheDownloadFromAUsablePrivateNetworkAddressWhenAvailable() throws Exception {
        ChartDownloadServer server;
        try {
            server = ChartDownloadServer.startForLocalNetwork(pngImage(), "northwind-network-test.png");
        } catch (IOException exception) {
            if (exception.getMessage() != null && (exception.getMessage().startsWith("Conecta este equipo")
                    || exception.getMessage().startsWith("No se encontró"))) {
                assumeTrue(false, "No hay una interfaz local para probar: " + exception.getMessage());
            }
            throw exception;
        }

        try (server) {
            assertTrue(InetAddress.getByName(server.downloadUri().getHost()).isSiteLocalAddress());
            assertEquals(200, request(server.downloadUri(), "GET").statusCode());
        }
    }

    private static Response request(URI uri, String method) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setConnectTimeout(2_000);
        connection.setReadTimeout(2_000);
        connection.setRequestMethod(method);
        try {
            int statusCode = connection.getResponseCode();
            InputStream bodyStream = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
            byte[] body = bodyStream == null ? new byte[0] : bodyStream.readAllBytes();
            if (bodyStream != null) {
                bodyStream.close();
            }
            return new Response(statusCode, connection.getContentType(), connection.getHeaderField("Content-Disposition"),
                    connection.getHeaderField("Cache-Control"), connection.getHeaderField("Allow"), body);
        } finally {
            connection.disconnect();
        }
    }

    private static byte[] pngImage() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0x123456);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private record Response(int statusCode, String contentType, String contentDisposition, String cacheControl,
                            String allow, byte[] body) {
    }
}
