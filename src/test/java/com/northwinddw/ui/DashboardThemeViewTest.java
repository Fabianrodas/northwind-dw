package com.northwinddw.ui;

import com.northwinddw.Main;
import com.northwinddw.service.DashboardExporter;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.List;
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
                FlowPane reportsGrid = (FlowPane) loader.getNamespace().get("reportsGrid");
                List<VBox> cards = List.of("quarterlyCard", "categoryCard", "countryCard",
                                "customerCard", "employeeCard", "supplierCard").stream()
                        .map(id -> (VBox) loader.getNamespace().get(id))
                        .toList();
                Stage stage = new Stage();
                stage.setScene(scene);
                stage.setWidth(1000);
                stage.setHeight(900);
                stage.show();

                assertTrue(root.getStyleClass().contains(DashboardTheme.LIGHT.styleClass()));
                assertEquals("Modo oscuro", themeButton.getText());
                themeButton.fire();
                root.applyCss();
                assertTrue(root.getStyleClass().contains(DashboardTheme.DARK.styleClass()));
                assertEquals("Modo claro", themeButton.getText());
                assertEquals(DashboardTheme.DARK, DashboardTheme.load(preferences));
                verifyResponsiveWidths(stage, root, reportsGrid, cards,
                        new double[]{1480, 1000, 800}, 0, loader.getController(), failure, complete);
            } catch (Throwable exception) {
                failure.set(exception);
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

    private static void verifyResponsiveWidths(Stage stage, Parent root, FlowPane grid, List<VBox> cards,
                                              double[] widths, int index, DashboardController controller,
                                              AtomicReference<Throwable> failure,
                                              CountDownLatch complete) {
        if (index == widths.length) {
            stage.close();
            controller.shutdown();
            Platform.exit();
            complete.countDown();
            return;
        }

        stage.setWidth(widths[index]);
        PauseTransition layoutWait = new PauseTransition(Duration.millis(180));
        layoutWait.setOnFinished(event -> {
            try {
                root.applyCss();
                root.layout();
                ReportGridLayout expected = ReportGridLayout.calculate(grid.getWidth());
                assertEquals(expected.cardWidth(), cards.getFirst().getWidth(), 2.0);
                long cardsOnFirstRow = cards.stream()
                        .filter(card -> Math.abs(card.getBoundsInParent().getMinY()
                                - cards.getFirst().getBoundsInParent().getMinY()) < 1.0)
                        .count();
                assertEquals(expected.columns(), cardsOnFirstRow,
                        "stageWidth=" + stage.getWidth() + ", gridWidth=" + grid.getWidth()
                                + ", expectedCardWidth=" + expected.cardWidth()
                                + ", actualCardWidths=" + cards.stream().map(VBox::getWidth).toList()
                                + ", cardX=" + cards.stream().map(card -> card.getBoundsInParent().getMinX()).toList()
                                + ", cardY=" + cards.stream().map(card -> card.getBoundsInParent().getMinY()).toList());
                verifyResponsiveWidths(stage, root, grid, cards, widths, index + 1, controller,
                        failure, complete);
            } catch (Throwable exception) {
                failure.set(exception);
                stage.close();
                controller.shutdown();
                Platform.exit();
                complete.countDown();
            }
        });
        layoutWait.play();
    }
}
