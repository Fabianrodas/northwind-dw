package com.northwinddw.ui;

import com.northwinddw.Main;
import com.northwinddw.service.DashboardExporter;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardThemeViewTest {
    @Test
    void themeButtonUpdatesTheWholeViewAndSavesTheChoice() throws Exception {
        Preferences preferences = Preferences.userRoot().node("northwind-dw/tests/" + UUID.randomUUID());
        CountDownLatch complete = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Platform.startup(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/dashboard.fxml"));
                loader.setControllerFactory(type -> new DashboardController(
                        null, new DashboardExporter(), "Prueba sin conexión",
                        DashboardTheme.LIGHT, preferences));
                Parent root = loader.load();
                Scene scene = new Scene(root);
                scene.getStylesheets().add(Main.class.getResource("/css/dashboard.css").toExternalForm());
                root.applyCss();
                Button themeButton = (Button) loader.getNamespace().get("themeToggleButton");

                assertTrue(root.getStyleClass().contains(DashboardTheme.LIGHT.styleClass()));
                assertEquals("Modo oscuro", themeButton.getText());
                themeButton.fire();
                root.applyCss();
                assertTrue(root.getStyleClass().contains(DashboardTheme.DARK.styleClass()));
                assertEquals("Modo claro", themeButton.getText());
                assertEquals(DashboardTheme.DARK, DashboardTheme.load(preferences));
            } catch (Throwable exception) {
                failure.set(exception);
            } finally {
                Platform.exit();
                complete.countDown();
            }
        });

        try {
            assertTrue(complete.await(15, TimeUnit.SECONDS), "JavaFX theme view did not finish.");
            if (failure.get() != null) {
                throw new AssertionError("The theme button did not apply its selection.", failure.get());
            }
        } finally {
            preferences.removeNode();
            Preferences.userRoot().flush();
        }
    }
}
