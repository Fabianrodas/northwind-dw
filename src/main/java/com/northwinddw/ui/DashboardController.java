package com.northwinddw.ui;

import com.northwinddw.data.AnalyticsRepository;
import com.northwinddw.model.DashboardData;
import com.northwinddw.model.ReportRows.CategoryRevenue;
import com.northwinddw.model.ReportRows.CountryRevenue;
import com.northwinddw.model.ReportRows.CustomerRevenue;
import com.northwinddw.model.ReportRows.EmployeeRevenue;
import com.northwinddw.model.ReportRows.QuarterlySales;
import com.northwinddw.model.ReportRows.SuppliersByCountry;
import com.northwinddw.service.DashboardExporter;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;

public final class DashboardController {
    private static final String ALL_YEARS = "Todos los años";
    private static final Preferences USER_PREFERENCES = Preferences.userNodeForPackage(DashboardTheme.class);

    @FXML private BorderPane dashboardRoot;
    @FXML private Label connectionBadge;
    @FXML private Label statusLabel;
    @FXML private Label revenueValue;
    @FXML private Label customersValue;
    @FXML private Label suppliersValue;
    @FXML private Label yearLabel;
    @FXML private ComboBox<String> yearFilter;
    @FXML private Button refreshButton;
    @FXML private Button exportPdfButton;
    @FXML private Button exportSqlButton;
    @FXML private Button themeToggleButton;
    @FXML private LineChart<String, Number> quarterlyChart;
    @FXML private PieChart categoryChart;
    @FXML private BarChart<Number, String> countryChart;
    @FXML private BarChart<Number, String> customerChart;
    @FXML private LineChart<String, Number> employeeChart;
    @FXML private BarChart<Number, String> supplierChart;

    private final AnalyticsRepository repository;
    private final DashboardExporter exporter;
    private final String configurationError;
    private final Preferences themePreferences;
    private DashboardTheme theme;
    private final ExecutorService background = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "northwind-background");
        thread.setDaemon(true);
        return thread;
    });

    private DashboardData currentData;
    private boolean settingYears;

    public DashboardController(AnalyticsRepository repository, DashboardExporter exporter, String configurationError) {
        this(repository, exporter, configurationError, DashboardTheme.load(USER_PREFERENCES), USER_PREFERENCES);
    }

    public DashboardController(AnalyticsRepository repository, DashboardExporter exporter, String configurationError,
                               DashboardTheme initialTheme, Preferences themePreferences) {
        this.repository = repository;
        this.exporter = exporter;
        this.configurationError = configurationError;
        this.theme = initialTheme;
        this.themePreferences = themePreferences;
    }

    @FXML
    private void initialize() {
        yearLabel.setLabelFor(yearFilter);
        yearFilter.setItems(FXCollections.observableArrayList(ALL_YEARS));
        yearFilter.setValue(ALL_YEARS);
        yearFilter.setOnAction(event -> {
            if (!settingYears && yearFilter.getValue() != null) {
                refresh();
            }
        });
        refreshButton.setTooltip(new Tooltip("Vuelve a consultar los datos del almacén."));
        exportPdfButton.setTooltip(new Tooltip("Guarda un informe con el resumen y los seis análisis."));
        exportSqlButton.setTooltip(new Tooltip("Guarda el script para preparar una base vacía."));
        applyTheme();
        exportPdfButton.setDisable(true);
        setConnectionState("Conectando…", "status-loading");
        statusLabel.setText("Preparando el tablero…");

        if (configurationError != null) {
            showConnectionError(configurationError);
        } else {
            refresh();
        }
    }

    @FXML
    private void toggleTheme() {
        theme = theme.next();
        theme.save(themePreferences);
        applyTheme();
    }

    private void applyTheme() {
        dashboardRoot.getStyleClass().removeAll(DashboardTheme.LIGHT.styleClass(), DashboardTheme.DARK.styleClass());
        dashboardRoot.getStyleClass().add(theme.styleClass());
        themeToggleButton.setText(theme.toggleLabel());
        themeToggleButton.setAccessibleText("Cambiar al " + theme.toggleLabel().toLowerCase());
        themeToggleButton.setTooltip(new Tooltip("Cambiar la apariencia del tablero."));
    }

    @FXML
    private void refresh() {
        if (repository == null) {
            showConnectionError("Revisa la configuración del archivo .env.");
            return;
        }

        Integer selectedYear = selectedYear();
        setLoading(true);
        setConnectionState("Actualizando…", "status-loading");
        statusLabel.setText("Consultando el almacén de datos…");

        Task<DashboardData> task = new Task<>() {
            @Override
            protected DashboardData call() throws Exception {
                return repository.loadDashboard(selectedYear);
            }
        };
        task.setOnSucceeded(event -> {
            currentData = task.getValue();
            if (selectedYear == null) {
                updateYearOptions(currentData.availableYears());
            }
            render(currentData);
            setLoading(false);
            setConnectionState("Conectado", "status-connected");
            statusLabel.setText(currentData.summary().totalRevenue().signum() == 0
                    ? "La conexión está lista, pero no hay ventas para mostrar en este periodo."
                    : "Datos actualizados · " + currentData.availableYears().size() + " años disponibles.");
            exportPdfButton.setDisable(false);
        });
        task.setOnFailed(event -> {
            setLoading(false);
            setConnectionState("Sin conexión", "status-error");
            if (currentData == null) {
                clearCharts();
                statusLabel.setText("No se pudo conectar a PostgreSQL. Revisa que esté activo y que el archivo .env tenga los datos correctos.");
                exportPdfButton.setDisable(true);
            } else {
                statusLabel.setText("No se pudo actualizar. Se mantienen visibles los últimos datos cargados.");
            }
        });
        background.submit(task);
    }

    @FXML
    private void exportPdf() {
        if (currentData == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar resumen Northwind DW");
        chooser.setInitialFileName("northwind-dashboard.pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF", "*.pdf"));
        File selected = chooser.showSaveDialog(stage());
        if (selected == null) {
            return;
        }

        DashboardData dataToExport = currentData;
        Path destination = selected.toPath();
        exportPdfButton.setDisable(true);
        statusLabel.setText("Preparando el PDF…");
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws IOException {
                exporter.exportPdf(dataToExport, destination);
                return null;
            }
        };
        task.setOnSucceeded(event -> {
            exportPdfButton.setDisable(false);
            statusLabel.setText("PDF guardado: " + destination.getFileName());
        });
        task.setOnFailed(event -> {
            exportPdfButton.setDisable(false);
            statusLabel.setText("No se pudo guardar el PDF. Comprueba la ubicación elegida.");
            showExportError("No se pudo crear el PDF. Comprueba que tengas permiso para guardar en esa ubicación.");
        });
        background.submit(task);
    }

    @FXML
    private void exportSql() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar script de la base de datos");
        chooser.setInitialFileName("northwind_dw.sql");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Script SQL", "*.sql"));
        File selected = chooser.showSaveDialog(stage());
        if (selected == null) {
            return;
        }

        try {
            exporter.copySql(selected.toPath());
            statusLabel.setText("Script SQL guardado: " + selected.getName());
        } catch (IOException exception) {
            statusLabel.setText("No se pudo guardar el script SQL.");
            showExportError("No se pudo copiar el script. Comprueba la ubicación elegida.");
        }
    }

    public void shutdown() {
        background.shutdownNow();
    }

    private void render(DashboardData data) {
        revenueValue.setText(DashboardFormat.usd(data.summary().totalRevenue()));
        customersValue.setText(Long.toString(data.summary().customers()));
        suppliersValue.setText(Long.toString(data.summary().suppliers()));
        renderQuarterlySales(data.quarterlySales());
        renderCategories(data.categoryRevenue());
        renderCountries(data.topCountries());
        renderCustomers(data.topCustomers());
        renderEmployees(data.employeeRevenue());
        renderSuppliers(data.suppliersByCountry());
    }

    private void renderQuarterlySales(List<QuarterlySales> rows) {
        Map<Integer, XYChart.Series<String, Number>> byYear = new TreeMap<>();
        for (QuarterlySales row : rows) {
            XYChart.Series<String, Number> series = byYear.computeIfAbsent(row.year(), year -> {
                XYChart.Series<String, Number> created = new XYChart.Series<>();
                created.setName(String.valueOf(year));
                return created;
            });
            XYChart.Data<String, Number> point = new XYChart.Data<>(row.quarter(), row.revenue());
            installTooltip(point, row.quarter() + " " + row.year() + " · " + DashboardFormat.usd(row.revenue()));
            series.getData().add(point);
        }
        quarterlyChart.getData().setAll(byYear.values());
        quarterlyChart.setAccessibleText("Ventas por trimestre y año. Cada línea representa un año.");
    }

    private void renderCategories(List<CategoryRevenue> rows) {
        List<PieChart.Data> slices = new ArrayList<>();
        for (CategoryRevenue row : rows) {
            PieChart.Data slice = new PieChart.Data(row.category(), row.revenue().doubleValue());
            slice.nodeProperty().addListener((observable, previous, node) -> {
                if (node != null) {
                    Tooltip.install(node, new Tooltip(row.category() + " · "
                            + DashboardFormat.percentage(row.percentage()) + " · " + DashboardFormat.usd(row.revenue())));
                }
            });
            slices.add(slice);
        }
        categoryChart.setData(FXCollections.observableArrayList(slices));
        categoryChart.setAccessibleText("Participación de ventas por categoría de producto.");
    }

    private void renderCountries(List<CountryRevenue> rows) {
        XYChart.Series<Number, String> series = new XYChart.Series<>();
        series.setName("Ventas");
        for (CountryRevenue row : rows) {
            XYChart.Data<Number, String> point = new XYChart.Data<>(row.revenue(), row.country());
            installTooltip(point, row.country() + " · " + DashboardFormat.usd(row.revenue()));
            series.getData().add(point);
        }
        countryChart.getData().setAll(series);
        countryChart.setAccessibleText("Los diez países con mayor venta, ordenados por ingresos.");
    }

    private void renderCustomers(List<CustomerRevenue> rows) {
        XYChart.Series<Number, String> series = new XYChart.Series<>();
        series.setName("Ventas");
        for (CustomerRevenue row : rows) {
            XYChart.Data<Number, String> point = new XYChart.Data<>(row.revenue(), row.customer());
            installTooltip(point, row.customer() + " · " + DashboardFormat.usd(row.revenue()));
            series.getData().add(point);
        }
        customerChart.getData().setAll(series);
        customerChart.setAccessibleText("Los diez clientes con mayor venta, ordenados por ingresos.");
    }

    private void renderEmployees(List<EmployeeRevenue> rows) {
        Map<String, XYChart.Series<String, Number>> byEmployee = new TreeMap<>();
        for (EmployeeRevenue row : rows) {
            XYChart.Series<String, Number> series = byEmployee.computeIfAbsent(row.employee(), employee -> {
                XYChart.Series<String, Number> created = new XYChart.Series<>();
                created.setName(employee);
                return created;
            });
            XYChart.Data<String, Number> point = new XYChart.Data<>(String.valueOf(row.year()), row.revenue());
            installTooltip(point, row.employee() + " · " + row.year() + " · " + DashboardFormat.usd(row.revenue()));
            series.getData().add(point);
        }
        employeeChart.getData().setAll(byEmployee.values());
        employeeChart.setAccessibleText("Ventas anuales de todos los empleados con datos.");
    }

    private void renderSuppliers(List<SuppliersByCountry> rows) {
        XYChart.Series<Number, String> series = new XYChart.Series<>();
        series.setName("Proveedores");
        for (SuppliersByCountry row : rows) {
            XYChart.Data<Number, String> point = new XYChart.Data<>(row.supplierCount(), row.country());
            installTooltip(point, row.country() + " · " + row.supplierCount() + " proveedores");
            series.getData().add(point);
        }
        supplierChart.getData().setAll(series);
        supplierChart.setAccessibleText("Cantidad de proveedores registrados por país.");
    }

    private void updateYearOptions(List<Integer> years) {
        List<String> options = new ArrayList<>();
        options.add(ALL_YEARS);
        years.stream().sorted().map(String::valueOf).forEach(options::add);
        settingYears = true;
        yearFilter.setItems(FXCollections.observableArrayList(options));
        yearFilter.setValue(ALL_YEARS);
        settingYears = false;
    }

    private Integer selectedYear() {
        String selected = yearFilter.getValue();
        return selected == null || ALL_YEARS.equals(selected) ? null : Integer.valueOf(selected);
    }

    private void setLoading(boolean loading) {
        refreshButton.setDisable(loading);
        yearFilter.setDisable(loading);
    }

    private void setConnectionState(String text, String styleClass) {
        connectionBadge.setText(text);
        connectionBadge.getStyleClass().removeAll("status-connected", "status-error", "status-loading");
        connectionBadge.getStyleClass().add(styleClass);
    }

    private void showConnectionError(String details) {
        setLoading(false);
        setConnectionState("Sin conexión", "status-error");
        clearCharts();
        statusLabel.setText("No se pudo conectar a PostgreSQL. " + details);
    }

    private void showExportError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText("No se pudo guardar el archivo");
        alert.showAndWait();
    }

    private void clearCharts() {
        quarterlyChart.getData().clear();
        categoryChart.getData().clear();
        countryChart.getData().clear();
        customerChart.getData().clear();
        employeeChart.getData().clear();
        supplierChart.getData().clear();
    }

    private static void installTooltip(XYChart.Data<?, ?> point, String text) {
        point.nodeProperty().addListener((observable, previous, node) -> {
            if (node != null) {
                Tooltip.install(node, new Tooltip(text));
            }
        });
    }

    private Stage stage() {
        return (Stage) refreshButton.getScene().getWindow();
    }
}
