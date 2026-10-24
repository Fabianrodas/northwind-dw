package com.northwinddw.ui;

import com.northwinddw.Main;
import com.northwinddw.config.DatabaseConfig;
import com.northwinddw.data.AnalyticsRepository;
import com.northwinddw.data.ConnectionFactory;
import com.northwinddw.service.DashboardExporter;
import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DashboardScreenshotTest {
    @Test
    void capturesConnectedAndConnectionErrorStates() throws Exception {
        String screenshotDirectory = System.getenv("NORTHWIND_SCREENSHOT_DIR");
        assumeTrue(screenshotDirectory != null && !screenshotDirectory.isBlank(),
                "Set NORTHWIND_SCREENSHOT_DIR to generate the README screenshots.");
        Path outputDirectory = Path.of(screenshotDirectory);
        Files.createDirectories(outputDirectory);

        CountDownLatch complete = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.startup(() -> openWindow(false, outputDirectory.resolve("dashboard.png"), () ->
                openWindow(true, outputDirectory.resolve("connection-error.png"), () -> {
                    Platform.exit();
                    complete.countDown();
                }, failure, complete), failure, complete));

        assertTrue(complete.await(30, TimeUnit.SECONDS), "The JavaFX screenshot capture did not finish.");
        if (failure.get() != null) {
            throw new AssertionError("The JavaFX screenshot capture failed.", failure.get());
        }
        assertTrue(Files.size(outputDirectory.resolve("dashboard.png")) > 10_000);
        assertTrue(Files.size(outputDirectory.resolve("connection-error.png")) > 10_000);
    }

    private static void openWindow(boolean offline, Path destination, Runnable onCaptured,
                                   AtomicReference<Throwable> failure, CountDownLatch complete) {
        try {
            DatabaseConfig config = offline
                    ? new DatabaseConfig("127.0.0.1", 5432, "northwind_pentaho", "postgres", "")
                    : DatabaseConfig.load();
            AnalyticsRepository repository = new AnalyticsRepository(ConnectionFactory.from(config));
            DashboardExporter exporter = new DashboardExporter();
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/dashboard.fxml"));
            loader.setControllerFactory(type -> new DashboardController(repository, exporter, null));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1480, 1160);
            scene.getStylesheets().add(Main.class.getResource("/css/dashboard.css").toExternalForm());
            Stage stage = new Stage();
            stage.setTitle(offline ? "Northwind DW | Sin conexión" : "Northwind DW | Captura");
            stage.setScene(scene);
            stage.setWidth(1480);
            stage.setHeight(1160);
            stage.show();

            DashboardController controller = loader.getController();
            Label status = (Label) loader.getNamespace().get("statusLabel");
            String expectedPrefix = offline ? "No se pudo conectar" : "Datos actualizados";
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
            AnimationTimer timer = new AnimationTimer() {
                private boolean scheduled;

                @Override
                public void handle(long now) {
                    if (!scheduled && status.getText().startsWith(expectedPrefix)) {
                        scheduled = true;
                        stop();
                        PauseTransition afterLayout = new PauseTransition(Duration.millis(450));
                        afterLayout.setOnFinished(event -> {
                            try {
                                saveSnapshot(root, destination);
                                stage.close();
                                controller.shutdown();
                                onCaptured.run();
                            } catch (Throwable exception) {
                                failure.set(exception);
                                stage.close();
                                controller.shutdown();
                                Platform.exit();
                                complete.countDown();
                            }
                        });
                        afterLayout.play();
                    } else if (System.nanoTime() > deadline) {
                        stop();
                        failure.set(new IllegalStateException("The application never reached state: " + expectedPrefix));
                        stage.close();
                        controller.shutdown();
                        Platform.exit();
                        complete.countDown();
                    }
                }
            };
            timer.start();
        } catch (Throwable exception) {
            failure.set(exception);
            Platform.exit();
            complete.countDown();
        }
    }

    private static void saveSnapshot(Parent root, Path destination) throws IOException {
        root.applyCss();
        root.layout();
        int width = (int) Math.ceil(root.getBoundsInLocal().getWidth());
        int height = (int) Math.ceil(root.getBoundsInLocal().getHeight());
        WritableImage snapshot = new WritableImage(width, height);
        root.snapshot(null, snapshot);

        int[] pixels = new int[width * height];
        snapshot.getPixelReader().getPixels(0, 0, width, height,
                PixelFormat.getIntArgbInstance(), pixels, 0, width);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, pixels, 0, width);
        ImageIO.write(image, "png", destination.toFile());
    }
}
