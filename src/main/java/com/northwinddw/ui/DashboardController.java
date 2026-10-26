package com.northwinddw.ui;

import com.northwinddw.data.DashboardDataSource;
import com.northwinddw.model.DashboardData;
import com.northwinddw.model.DashboardReport;
import com.northwinddw.model.DashboardReportContent;
import com.northwinddw.model.ReportRows.CategoryRevenue;
import com.northwinddw.model.ReportRows.CountryRevenue;
import com.northwinddw.model.ReportRows.CustomerRevenue;
import com.northwinddw.model.ReportRows.EmployeeRevenue;
import com.northwinddw.model.ReportRows.QuarterlySales;
import com.northwinddw.model.ReportRows.SuppliersByCountry;
import com.northwinddw.service.DashboardExporter;
import com.northwinddw.service.DashboardQrService;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.AccessibleRole;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.Chart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
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
    @FXML private HBox summaryRow;
    @FXML private ScrollPane dashboardScroll;
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
    @FXML private Button backToGridButton;
    @FXML private Button detailPdfButton;
    @FXML private Button detailQrButton;
    @FXML private VBox detailPanel;
    @FXML private Label detailTitle;
    @FXML private Label detailContext;
    @FXML private Label detailPeriod;
    @FXML private Label detailStatusLabel;
    @FXML private StackPane detailChartHost;
    @FXML private FlowPane reportsGrid;
    @FXML private VBox quarterlyCard;
    @FXML private VBox categoryCard;
    @FXML private VBox countryCard;
    @FXML private VBox customerCard;
    @FXML private VBox employeeCard;
    @FXML private VBox supplierCard;
    @FXML private LineChart<String, Number> quarterlyChart;
    @FXML private PieChart categoryChart;
    @FXML private BarChart<Number, String> countryChart;
    @FXML private BarChart<Number, String> customerChart;
    @FXML private LineChart<String, Number> employeeChart;
    @FXML private BarChart<Number, String> supplierChart;

    private final DashboardDataSource repository;
    private final DashboardExporter exporter;
    private final DashboardQrService qrService = new DashboardQrService();
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
    private DashboardReport selectedReport;
    private VBox selectedCard;
    private int selectedChartIndex = -1;
    private double previousScrollPosition;

    public DashboardController(DashboardDataSource repository, DashboardExporter exporter, String configurationError) {
        this(repository, exporter, configurationError, DashboardTheme.load(USER_PREFERENCES), USER_PREFERENCES);
    }

    public DashboardController(DashboardDataSource repository, DashboardExporter exporter, String configurationError,
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
        detailPdfButton.setTooltip(new Tooltip("Guarda este gráfico con su explicación y los datos del periodo elegido."));
        detailQrButton.setTooltip(new Tooltip("Muestra un código QR con el resumen de este gráfico."));
        applyTheme();
        configureReportCard(quarterlyCard, DashboardReport.QUARTERLY_SALES);
        configureReportCard(categoryCard, DashboardReport.CATEGORY_REVENUE);
        configureReportCard(countryCard, DashboardReport.COUNTRY_REVENUE);
        configureReportCard(customerCard, DashboardReport.CUSTOMER_REVENUE);
        configureReportCard(employeeCard, DashboardReport.EMPLOYEE_REVENUE);
        configureReportCard(supplierCard, DashboardReport.SUPPLIERS_BY_COUNTRY);
        reportsGrid.widthProperty().addListener((observable, previousWidth, width) -> resizeReportCards(width.doubleValue()));
        detailChartHost.widthProperty().addListener((observable, previousWidth, width) -> resizeDetailChart(width.doubleValue()));
        Platform.runLater(() -> resizeReportCards(reportsGrid.getWidth()));
        exportPdfButton.setDisable(true);
        detailPdfButton.setDisable(true);
        detailQrButton.setDisable(true);
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

    private void resizeReportCards(double paneWidth) {
        if (paneWidth <= 0) {
            return;
        }
        ReportGridLayout layout = ReportGridLayout.calculate(paneWidth);
        reportsGrid.setPrefWrapLength(Math.max(320, paneWidth - 60));
        List<VBox> cards = List.of(quarterlyCard, categoryCard, countryCard,
                customerCard, employeeCard, supplierCard);
        for (VBox card : cards) {
            card.setMinWidth(layout.cardWidth());
            card.setPrefWidth(layout.cardWidth());
            card.setMaxWidth(layout.cardWidth());
            card.setPrefHeight(layout.cardHeight());
        }
        List<Chart> charts = List.of(quarterlyChart, categoryChart, countryChart,
                customerChart, employeeChart, supplierChart);
        charts.forEach(chart -> chart.setPrefHeight(layout.chartHeight()));
    }

    private void configureReportCard(VBox card, DashboardReport report) {
        card.setFocusTraversable(true);
        card.setAccessibleRole(AccessibleRole.BUTTON);
        card.setAccessibleText(report.title() + ". " + report.context() + " Abrir detalle.");
        card.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                openDetail(report);
                event.consume();
            }
        });
        card.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                openDetail(report);
                event.consume();
            }
        });
    }

    public void openDetail(DashboardReport report) {
        if (currentData == null || report == null || selectedReport != null) {
            return;
        }

        Chart chart = chartFor(report);
        VBox card = cardFor(report);
        selectedReport = report;
        selectedCard = card;
        selectedChartIndex = card.getChildren().indexOf(chart);
        previousScrollPosition = dashboardScroll.getVvalue();
        card.getChildren().remove(chart);
        detailChartHost.getChildren().setAll(chart);

        updateDetailContent(report);
        detailStatusLabel.setText("");
        reportsGrid.setVisible(false);
        reportsGrid.setManaged(false);
        detailPanel.setVisible(true);
        detailPanel.setManaged(true);
        summaryRow.setVisible(false);
        summaryRow.setManaged(false);
        dashboardScroll.setVvalue(0);
        resizeDetailChart(detailChartHost.getWidth());
        Platform.runLater(detailChartHost::requestLayout);
    }

    @FXML
    public void returnToGrid() {
        if (selectedReport == null) {
            return;
        }

        Chart chart = chartFor(selectedReport);
        detailChartHost.getChildren().remove(chart);
        int insertionIndex = Math.max(0, Math.min(selectedChartIndex, selectedCard.getChildren().size()));
        selectedCard.getChildren().add(insertionIndex, chart);
        VBox cardToFocus = selectedCard;
        reportsGrid.setVisible(true);
        reportsGrid.setManaged(true);
        detailPanel.setVisible(false);
        detailPanel.setManaged(false);
        summaryRow.setVisible(true);
        summaryRow.setManaged(true);
        selectedReport = null;
        selectedCard = null;
        selectedChartIndex = -1;
        resizeReportCards(reportsGrid.getWidth());
        Platform.runLater(() -> {
            dashboardScroll.setVvalue(previousScrollPosition);
            cardToFocus.requestFocus();
        });
    }

    private void updateDetailContent(DashboardReport report) {
        detailTitle.setText(report.title());
        detailContext.setText(report.context());
        detailPeriod.setText("Periodo: " + DashboardFormat.yearLabel(selectedYear()));
        detailTitle.setAccessibleText(report.title());
        detailContext.setAccessibleText(report.context());
        detailPeriod.setAccessibleText("Periodo seleccionado: " + DashboardFormat.yearLabel(selectedYear()));
        detailPdfButton.setAccessibleText("Exportar " + report.title() + " como PDF");
        detailQrButton.setAccessibleText("Generar código QR de " + report.title());
    }

    private void resizeDetailChart(double hostWidth) {
        if (selectedReport == null || hostWidth <= 0) {
            return;
        }
        Chart chart = chartFor(selectedReport);
        chart.setPrefHeight(Math.max(320, Math.min(620, hostWidth * 0.52)));
        chart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    }

    private Chart chartFor(DashboardReport report) {
        return switch (report) {
            case QUARTERLY_SALES -> quarterlyChart;
            case CATEGORY_REVENUE -> categoryChart;
            case COUNTRY_REVENUE -> countryChart;
            case CUSTOMER_REVENUE -> customerChart;
            case EMPLOYEE_REVENUE -> employeeChart;
            case SUPPLIERS_BY_COUNTRY -> supplierChart;
        };
    }

    private VBox cardFor(DashboardReport report) {
        return switch (report) {
            case QUARTERLY_SALES -> quarterlyCard;
            case CATEGORY_REVENUE -> categoryCard;
            case COUNTRY_REVENUE -> countryCard;
            case CUSTOMER_REVENUE -> customerCard;
            case EMPLOYEE_REVENUE -> employeeCard;
            case SUPPLIERS_BY_COUNTRY -> supplierCard;
        };
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
            detailPdfButton.setDisable(false);
            detailQrButton.setDisable(false);
            if (selectedReport != null) {
                updateDetailContent(selectedReport);
            }
        });
        task.setOnFailed(event -> {
            setLoading(false);
            setConnectionState("Sin conexión", "status-error");
            if (currentData == null) {
                clearCharts();
                statusLabel.setText("No se pudo conectar a PostgreSQL. Revisa que esté activo y que el archivo .env tenga los datos correctos.");
                exportPdfButton.setDisable(true);
                detailPdfButton.setDisable(true);
                detailQrButton.setDisable(true);
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
    public void exportSelectedReportPdf() {
        if (currentData == null || selectedReport == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar " + selectedReport.title());
        chooser.setInitialFileName("northwind-" + selectedReport.name().toLowerCase() + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF", "*.pdf"));
        File selected = chooser.showSaveDialog(stage());
        if (selected == null) {
            return;
        }

        DashboardReport reportToExport = selectedReport;
        DashboardReportContent content = DashboardReportContent.from(reportToExport, currentData, selectedYear());
        BufferedImage chartImage;
        try {
            Chart chart = chartFor(reportToExport);
            chart.applyCss();
            chart.layout();
            int width = Math.max(1, (int) Math.ceil(chart.getBoundsInLocal().getWidth()));
            int height = Math.max(1, (int) Math.ceil(chart.getBoundsInLocal().getHeight()));
            chartImage = snapshotAsBufferedImage(chart.snapshot(new SnapshotParameters(), new WritableImage(width, height)));
        } catch (RuntimeException exception) {
            detailStatusLabel.setText("No se pudo preparar la imagen de este gráfico.");
            return;
        }

        Path destination = selected.toPath();
        detailPdfButton.setDisable(true);
        detailStatusLabel.setText("Preparando el PDF de este gráfico…");
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws IOException {
                exporter.exportReportPdf(content, chartImage, destination);
                return null;
            }
        };
        task.setOnSucceeded(event -> {
            detailPdfButton.setDisable(false);
            detailStatusLabel.setText("PDF guardado: " + destination.getFileName());
            statusLabel.setText("PDF del gráfico guardado: " + destination.getFileName());
        });
        task.setOnFailed(event -> {
            detailPdfButton.setDisable(false);
            detailStatusLabel.setText("No se pudo guardar el PDF. Comprueba la ubicación elegida.");
            showExportError("No se pudo crear el PDF de este gráfico. Comprueba la ubicación elegida.");
        });
        background.submit(task);
    }

    @FXML
    public void showSelectedReportQr() {
        if (currentData == null || selectedReport == null) {
            return;
        }
        DashboardReport report = selectedReport;
        DashboardReportContent content = DashboardReportContent.from(report, currentData, selectedYear());
        try {
            BitMatrix qrMatrix = qrService.encode(content.qrText(), 480);
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.initOwner(stage());
            dialog.setTitle("Código QR del gráfico");
            ButtonType saveButton = new ButtonType("Guardar QR como PNG", ButtonBar.ButtonData.OK_DONE);
            DialogPane pane = dialog.getDialogPane();
            pane.getButtonTypes().setAll(saveButton, ButtonType.CLOSE);
            pane.getStyleClass().add(theme.styleClass());
            pane.getStylesheets().add(com.northwinddw.Main.class
                    .getResource("/css/dashboard.css").toExternalForm());

            ImageView preview = new ImageView(qrImage(qrMatrix));
            preview.setFitWidth(320);
            preview.setFitHeight(320);
            preview.setPreserveRatio(true);
            preview.setAccessibleText("Código QR con el resumen de " + report.title());
            Label message = new Label("Escanea el código para consultar el contexto, el periodo y los datos de este gráfico.");
            message.setWrapText(true);
            VBox contentBox = new VBox(12, message, preview);
            contentBox.setPadding(new Insets(12));
            contentBox.setAlignment(javafx.geometry.Pos.CENTER);
            pane.setContent(contentBox);
            dialog.setResultConverter(button -> button);

            if (dialog.showAndWait().orElse(ButtonType.CLOSE) == saveButton) {
                saveReportQr(qrMatrix, report);
            }
        } catch (WriterException | IllegalArgumentException exception) {
            detailStatusLabel.setText("No se pudo preparar el código QR de este gráfico.");
        }
    }

    private void saveReportQr(BitMatrix qrMatrix, DashboardReport report) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar código QR como PNG");
        chooser.setInitialFileName("northwind-" + report.name().toLowerCase() + "-qr.png");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagen PNG", "*.png"));
        File selected = chooser.showSaveDialog(stage());
        if (selected == null) {
            return;
        }
        try {
            qrService.savePng(qrMatrix, selected.toPath());
            detailStatusLabel.setText("Código QR guardado: " + selected.getName());
            statusLabel.setText("Código QR del gráfico guardado: " + selected.getName());
        } catch (IOException | IllegalArgumentException exception) {
            detailStatusLabel.setText("No se pudo guardar el QR. Comprueba la ubicación elegida.");
        }
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
        detailPdfButton.setDisable(loading || currentData == null);
        detailQrButton.setDisable(loading || currentData == null);
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
        alert.initOwner(stage());
        alert.getDialogPane().getStyleClass().add(theme.styleClass());
        alert.getDialogPane().getStylesheets().add(com.northwinddw.Main.class
                .getResource("/css/dashboard.css").toExternalForm());
        alert.showAndWait();
    }

    private static BufferedImage snapshotAsBufferedImage(WritableImage snapshot) {
        int width = (int) snapshot.getWidth();
        int height = (int) snapshot.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        snapshot.getPixelReader().getPixels(0, 0, width, height,
                PixelFormat.getIntArgbInstance(), pixels, 0, width);
        return image;
    }

    private static WritableImage qrImage(BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int rowStart = y * width;
            for (int x = 0; x < width; x++) {
                pixels[rowStart + x] = matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
            }
        }
        WritableImage image = new WritableImage(width, height);
        PixelWriter writer = image.getPixelWriter();
        writer.setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
        return image;
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
