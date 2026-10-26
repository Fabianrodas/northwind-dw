package com.northwinddw.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class DashboardQrService {
    private static final int MINIMUM_SIZE = 128;
    private static final int MAXIMUM_SIZE = 2048;

    public BitMatrix encode(String text, int size) throws WriterException {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("El contenido del QR no puede estar vacío.");
        }
        if (size < MINIMUM_SIZE || size > MAXIMUM_SIZE) {
            throw new IllegalArgumentException("El tamaño del QR debe estar entre 128 y 2048 píxeles.");
        }

        return new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, Map.of(
                EncodeHintType.CHARACTER_SET, "UTF-8",
                EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN, 4));
    }

    public void savePng(BitMatrix matrix, Path destination) throws IOException {
        if (matrix == null) {
            throw new IllegalArgumentException("El código QR no puede estar vacío.");
        }
        if (destination == null) {
            throw new IllegalArgumentException("Debes elegir dónde guardar el código QR.");
        }

        Path absoluteDestination = destination.toAbsolutePath();
        Path parent = absoluteDestination.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < matrix.getWidth(); x++) {
            for (int y = 0; y < matrix.getHeight(); y++) {
                image.setRGB(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        if (!ImageIO.write(image, "png", absoluteDestination.toFile())) {
            throw new IOException("No se pudo generar la imagen PNG del código QR.");
        }
    }
}
