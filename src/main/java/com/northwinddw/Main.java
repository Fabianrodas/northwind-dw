package com.northwinddw;

import com.northwinddw.config.DatabaseConfig;
import com.northwinddw.data.AnalyticsRepository;
import com.northwinddw.data.ConnectionFactory;
import com.northwinddw.service.DashboardExporter;
import com.northwinddw.ui.DashboardController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
        Scene scene = new Scene(root, 1480, 960);
        scene.getStylesheets().add(Main.class.getResource("/css/dashboard.css").toExternalForm());
        stage.setTitle("Northwind DW | Ventas y operaciones");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(760);
        stage.setWidth(1480);
        stage.setHeight(960);
        stage.setOnCloseRequest(event -> stopDashboard());
        stage.show();
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
