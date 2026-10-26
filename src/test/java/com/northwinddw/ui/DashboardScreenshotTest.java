package com.northwinddw.ui;

import com.northwinddw.Main;
import com.northwinddw.data.DashboardDataSource;
import com.northwinddw.service.DashboardExporter;
import com.northwinddw.model.DashboardData;
import com.northwinddw.model.DashboardReport;
import com.northwinddw.model.RevenueSummary;
import com.northwinddw.model.ReportRows.CategoryRevenue;
import com.northwinddw.model.ReportRows.CountryRevenue;
import com.northwinddw.model.ReportRows.CustomerRevenue;
import com.northwinddw.model.ReportRows.EmployeeRevenue;
import com.northwinddw.model.ReportRows.QuarterlySales;
import com.northwinddw.model.ReportRows.SuppliersByCountry;
import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
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
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DashboardScreenshotTest {
    @Test
    void capturesLightAndDarkThemesAndConnectionError() throws Exception {
        String screenshotDirectory = System.getenv("NORTHWIND_SCREENSHOT_DIR");
        assumeTrue(screenshotDirectory != null && !screenshotDirectory.isBlank(),
                "Set NORTHWIND_SCREENSHOT_DIR to generate the README screenshots.");
        Path outputDirectory = Path.of(screenshotDirectory);
        Files.createDirectories(outputDirectory);
        Preferences preferences = Preferences.userRoot().node("northwind-dw/screenshots/" + UUID.randomUUID());

        CountDownLatch complete = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        DashboardData sample = sampleData();
        Platform.startup(() -> openWindow(year -> sample, null, DashboardTheme.LIGHT,
                outputDirectory.resolve("dashboard.png"), outputDirectory.resolve("dashboard-dark.png"),
                outputDirectory.resolve("dashboard-detail.png"), outputDirectory.resolve("dashboard-detail-dark.png"), () ->
                openWindow(null, "Prueba sin conexión", DashboardTheme.DARK,
                        outputDirectory.resolve("connection-error.png"), null, null, null, () -> {
                    Platform.exit();
                    complete.countDown();
                }, preferences, failure, complete), preferences, failure, complete));

        try {
            assertTrue(complete.await(45, TimeUnit.SECONDS), "The JavaFX screenshot capture did not finish.");
            if (failure.get() != null) {
                throw new AssertionError("The JavaFX screenshot capture failed.", failure.get());
            }
            assertTrue(Files.size(outputDirectory.resolve("dashboard.png")) > 10_000);
            assertTrue(Files.size(outputDirectory.resolve("dashboard-dark.png")) > 10_000);
            assertTrue(Files.size(outputDirectory.resolve("dashboard-detail.png")) > 10_000);
            assertTrue(Files.size(outputDirectory.resolve("dashboard-detail-dark.png")) > 10_000);
            assertTrue(Files.size(outputDirectory.resolve("connection-error.png")) > 10_000);
        } finally {
            preferences.removeNode();
            Preferences.userRoot().flush();
        }
    }

    private static void openWindow(DashboardDataSource source, String configurationError,
                                   DashboardTheme initialTheme, Path destination,
                                   Path secondDestination, Path detailDestination, Path detailDarkDestination,
                                   Runnable onCaptured, Preferences preferences,
                                   AtomicReference<Throwable> failure, CountDownLatch complete) {
        try {
            DashboardExporter exporter = new DashboardExporter();
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/dashboard.fxml"));
            loader.setControllerFactory(type -> new DashboardController(
                    source, exporter, configurationError, initialTheme, preferences));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1480, 1160);
            scene.getStylesheets().add(Main.class.getResource("/css/dashboard.css").toExternalForm());
            Stage stage = new Stage();
            stage.setTitle(configurationError == null ? "Northwind DW | Captura" : "Northwind DW | Sin conexión");
            stage.setScene(scene);
            stage.setWidth(1480);
            stage.setHeight(1160);
            stage.show();

            DashboardController controller = loader.getController();
            Label status = (Label) loader.getNamespace().get("statusLabel");
            String expectedPrefix = configurationError == null ? "Datos actualizados" : "No se pudo conectar";
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
                                if (secondDestination == null) {
                                    stage.close();
                                    controller.shutdown();
                                    onCaptured.run();
                                    return;
                                }

                                Button themeButton = (Button) loader.getNamespace().get("themeToggleButton");
                                themeButton.fire();
                                PauseTransition themeChange = new PauseTransition(Duration.millis(450));
                                themeChange.setOnFinished(themeEvent -> {
                                    try {
                                        saveSnapshot(root, secondDestination);
                                        if (detailDestination != null) {
                                            captureDetailThemes(loader, root, stage, controller, detailDestination,
                                                    detailDarkDestination, onCaptured, failure, complete);
                                            return;
                                        }
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
                                themeChange.play();
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

    private static void captureDetailThemes(FXMLLoader loader, Parent root, Stage stage,
                                            DashboardController controller, Path lightDestination,
                                            Path darkDestination, Runnable onCaptured,
                                            AtomicReference<Throwable> failure, CountDownLatch complete) {
        Button themeButton = (Button) loader.getNamespace().get("themeToggleButton");
        themeButton.fire(); // dark to light
        PauseTransition switchToLight = new PauseTransition(Duration.millis(350));
        switchToLight.setOnFinished(event -> {
            try {
                controller.openDetail(DashboardReport.CATEGORY_REVENUE);
                PauseTransition detailLayout = new PauseTransition(Duration.millis(450));
                detailLayout.setOnFinished(detailEvent -> {
                    try {
                        saveSnapshot(root, lightDestination);
                        themeButton.fire(); // light to dark
                        PauseTransition switchToDark = new PauseTransition(Duration.millis(350));
                        switchToDark.setOnFinished(darkEvent -> {
                            try {
                                saveSnapshot(root, darkDestination);
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
                        switchToDark.play();
                    } catch (Throwable exception) {
                        failure.set(exception);
                        stage.close();
                        controller.shutdown();
                        Platform.exit();
                        complete.countDown();
                    }
                });
                detailLayout.play();
            } catch (Throwable exception) {
                failure.set(exception);
                stage.close();
                controller.shutdown();
                Platform.exit();
                complete.countDown();
            }
        });
        switchToLight.play();
    }

    private static DashboardData sampleData() {
        return new DashboardData(
                new RevenueSummary(new BigDecimal("1265792.94"), 89, 29),
                List.of(new QuarterlySales(1997, "T1", new BigDecimal("289012.40")),
                        new QuarterlySales(1997, "T2", new BigDecimal("314802.17")),
                        new QuarterlySales(1997, "T3", new BigDecimal("362104.73")),
                        new QuarterlySales(1997, "T4", new BigDecimal("299873.64"))),
                List.of(new CategoryRevenue("Bebidas", new BigDecimal("320000"), new BigDecimal("25.3")),
                        new CategoryRevenue("Lácteos", new BigDecimal("280000"), new BigDecimal("22.1")),
                        new CategoryRevenue("Carnes", new BigDecimal("240000"), new BigDecimal("19.0"))),
                List.of(new CountryRevenue("Estados Unidos", new BigDecimal("245000")),
                        new CountryRevenue("Alemania", new BigDecimal("185000")),
                        new CountryRevenue("Austria", new BigDecimal("136000"))),
                List.of(new CustomerRevenue("Alfreds Futterkiste", new BigDecimal("68000")),
                        new CustomerRevenue("Ernst Handel", new BigDecimal("54200")),
                        new CustomerRevenue("Around the Horn", new BigDecimal("49100"))),
                List.of(new EmployeeRevenue(1997, "Nancy Davolio", new BigDecimal("255000")),
                        new EmployeeRevenue(1997, "Andrew Fuller", new BigDecimal("216000")),
                        new EmployeeRevenue(1997, "Janet Leverling", new BigDecimal("193000"))),
                List.of(new SuppliersByCountry("Estados Unidos", 4),
                        new SuppliersByCountry("Alemania", 3),
                        new SuppliersByCountry("Japón", 2)),
                List.of(1996, 1997, 1998));
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
