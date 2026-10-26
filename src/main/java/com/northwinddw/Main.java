package com.northwinddw;

import com.northwinddw.config.DatabaseConfig;
import com.northwinddw.data.AnalyticsRepository;
import com.northwinddw.data.ConnectionFactory;
import com.northwinddw.service.DashboardExporter;
import com.northwinddw.ui.DashboardController;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

public final class Main extends Application {
    private DashboardController dashboardController;

    @Override
    public void start(Stage stage) throws IOException {
        AnalyticsRepository repository = null;
        String configurationError = null;
        try {
            repository = new AnalyticsRepository(ConnectionFactory.from(DatabaseConfig.load()));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            configurationError = exception.getMessage();
        }

        DashboardExporter exporter = new DashboardExporter();
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/dashboard.fxml"));
        AnalyticsRepository finalRepository = repository;
        String finalConfigurationError = configurationError;
        loader.setControllerFactory(type -> {
            if (type == DashboardController.class) {
                dashboardController = new DashboardController(finalRepository, exporter, finalConfigurationError);
                return dashboardController;
            }
            throw new IllegalStateException("Controlador JavaFX no esperado: " + type.getName());
        });

        Parent root = loader.load();
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double windowWidth = Math.min(1480, Math.max(320, screen.getWidth() - 48));
        double windowHeight = Math.min(960, Math.max(320, screen.getHeight() - 48));
        Scene scene = new Scene(root, windowWidth, windowHeight);
        scene.getStylesheets().add(Main.class.getResource("/css/dashboard.css").toExternalForm());
        stage.setTitle("Northwind DW | Ventas y operaciones");
        var appIcon = Main.class.getResource("/images/northwind.png");
        if (appIcon != null) {
            stage.getIcons().add(new Image(appIcon.toExternalForm()));
        }
        stage.setScene(scene);
        stage.setMinWidth(Math.min(720, windowWidth));
        stage.setMinHeight(Math.min(620, windowHeight));
        stage.setWidth(windowWidth);
        stage.setHeight(windowHeight);
        stage.setOnCloseRequest(event -> stopDashboard());
        stage.show();
        stage.centerOnScreen();
    }

    @Override
    public void stop() {
        stopDashboard();
    }

    private void stopDashboard() {
        if (dashboardController != null) {
            dashboardController.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
