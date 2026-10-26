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
import com.northwinddw.service.ChartDownloadServer;
import com.northwinddw.service.DashboardExporter;
import com.northwinddw.service.DashboardQrService;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.Chart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
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
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
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
    @FXML private BarChart<Number, String> categoryChart;
    @FXML private BarChart<Number, String> countryChart;
    @FXML private BarChart<Number, String> customerChart;
    @FXML private StackPane employeeChartHost;
    @FXML private LineChart<String, Number> employeeTrendChart;
    @FXML private BarChart<Number, String> employeeYearChart;
    @FXML private BarChart<Number, String> supplierChart;

    private final DashboardDataSource repository;
    private final DashboardExporter exporter;
    private final DashboardQrService qrService = new DashboardQrService();
    private final Map<DashboardReport, ChartDownloadServer> qrDownloadServers =
            new EnumMap<>(DashboardReport.class);
    private final String configurationError;
    private final Preferences themePreferences;
    private DashboardTheme theme;
    private final ExecutorService background = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "northwind-background");
        thread.setDaemon(true);
        return thread;
    });

    private DashboardData currentData;
    private Integer loadedYear;
    private boolean settingYears;
    private DashboardReport selectedReport;
    private VBox selectedCard;
    private Pane selectedChartParent;
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
        detailQrButton.setTooltip(new Tooltip("Crea un QR para descargar este gráfico en otro dispositivo de la misma red local."));
        configureChartAxes();
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
                customerChart, employeeTrendChart, employeeYearChart, supplierChart);
        charts.forEach(chart -> chart.setPrefHeight(layout.chartHeight()));
        employeeChartHost.setPrefHeight(layout.chartHeight());
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
        selectedChartParent = (Pane) chart.getParent();
        selectedChartIndex = selectedChartParent.getChildren().indexOf(chart);
        previousScrollPosition = dashboardScroll.getVvalue();
        selectedChartParent.getChildren().remove(chart);
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
        Platform.runLater(() -> {
            detailChartHost.requestLayout();
            backToGridButton.requestFocus();
        });
    }

    @FXML
    public void returnToGrid() {
        if (selectedReport == null) {
            return;
        }

        Chart chart = chartFor(selectedReport);
        detailChartHost.getChildren().remove(chart);
        int insertionIndex = Math.max(0, Math.min(selectedChartIndex, selectedChartParent.getChildren().size()));
        selectedChartParent.getChildren().add(insertionIndex, chart);
        VBox cardToFocus = selectedCard;
        reportsGrid.setVisible(true);
        reportsGrid.setManaged(true);
        detailPanel.setVisible(false);
        detailPanel.setManaged(false);
        summaryRow.setVisible(true);
        summaryRow.setManaged(true);
        selectedReport = null;
        selectedCard = null;
        selectedChartParent = null;
        selectedChartIndex = -1;
        resizeReportCards(reportsGrid.getWidth());
        Platform.runLater(() -> {
            dashboardScroll.setVvalue(previousScrollPosition);
            cardToFocus.requestFocus();
        });
    }

    private void updateDetailContent(DashboardReport report) {
        String context = report == DashboardReport.EMPLOYEE_REVENUE && loadedYear != null
                ? "Compara las ventas de cada empleado durante " + loadedYear + "."
                : report.context();
        detailTitle.setText(report.title());
        detailContext.setText(context);
        detailPeriod.setText("Periodo: " + DashboardFormat.yearLabel(loadedYear));
        detailTitle.setAccessibleText(report.title());
        detailContext.setAccessibleText(context);
        detailPeriod.setAccessibleText("Periodo seleccionado: " + DashboardFormat.yearLabel(loadedYear));
        detailPdfButton.setAccessibleText("Exportar " + report.title() + " como PDF");
        detailQrButton.setAccessibleText("Generar QR para descargar " + report.title());
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
            case EMPLOYEE_REVENUE -> loadedYear == null ? employeeTrendChart : employeeYearChart;
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
            loadedYear = selectedYear;
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
                restoreLoadedYearSelection();
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
        DashboardReportContent content = DashboardReportContent.from(reportToExport, currentData, loadedYear);
        BufferedImage chartImage;
        try {
            chartImage = snapshotChart(chartFor(reportToExport));
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
        try {
            ChartDownloadServer previousServer = qrDownloadServers.remove(report);
            if (previousServer != null) {
                previousServer.close();
            }
            BufferedImage chartImage = snapshotChart(chartFor(report));
            ByteArrayOutputStream imageBytes = new ByteArrayOutputStream();
            if (!ImageIO.write(chartImage, "png", imageBytes)) {
                throw new IOException("No se pudo preparar el gráfico como imagen PNG.");
            }
            ChartDownloadServer downloadServer = ChartDownloadServer.startForLocalNetwork(
                    imageBytes.toByteArray(), chartDownloadFileName(report));
            BitMatrix qrMatrix;
            try {
                qrMatrix = qrService.encode(downloadServer.downloadUri().toASCIIString(), 480);
            } catch (WriterException | RuntimeException exception) {
                downloadServer.close();
                throw exception;
            }
            qrDownloadServers.put(report, downloadServer);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.initOwner(stage());
            dialog.setTitle("Descargar gráfico con QR");
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
            preview.setAccessibleText("Código QR para descargar " + report.title() + " como imagen PNG.");
            Label message = new Label("Escanea el código desde un teléfono conectado a esta misma red local. "
                    + "El gráfico se descargará como PNG. El enlace dura "
                    + ChartDownloadServer.DOWNLOAD_LIFETIME_MINUTES + " minutos y requiere que la aplicación siga abierta.");
            message.setWrapText(true);
            VBox contentBox = new VBox(12, message, preview);
            contentBox.setPadding(new Insets(12));
            contentBox.setAlignment(javafx.geometry.Pos.CENTER);
            pane.setContent(contentBox);
            dialog.setResultConverter(button -> button);
            detailStatusLabel.setText("QR listo para descargar el gráfico desde la misma red local.");

            if (dialog.showAndWait().orElse(ButtonType.CLOSE) == saveButton) {
                saveReportQr(qrMatrix, report);
            }
        } catch (IOException | WriterException | RuntimeException exception) {
            detailStatusLabel.setText("No se pudo preparar la descarga. Conecta el equipo a una red Wi-Fi o Ethernet y vuelve a intentarlo.");
        }
    }

    private static String chartDownloadFileName(DashboardReport report) {
        return "northwind-" + report.name().toLowerCase(Locale.ROOT).replace('_', '-') + ".png";
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
        qrDownloadServers.values().forEach(ChartDownloadServer::close);
        qrDownloadServers.clear();
    }

    private void render(DashboardData data) {
        revenueValue.setText(DashboardFormat.usd(data.summary().totalRevenue()));
        customersValue.setText(Long.toString(data.summary().customers()));
        suppliersValue.setText(Long.toString(data.summary().suppliers()));
        renderQuarterlySales(data.quarterlySales());
        renderCategories(data.categoryRevenue());
        renderCountries(data.topCountries());
        renderCustomers(data.topCustomers());
        renderEmployees(data.employeeRevenue(), loadedYear);
        renderSuppliers(data.suppliersByCountry());
    }

    private void renderQuarterlySales(List<QuarterlySales> rows) {
        Map<Integer, XYChart.Series<String, Number>> byYear = new TreeMap<>();
        List<QuarterlySales> orderedRows = rows.stream()
                .sorted(Comparator.comparingInt(QuarterlySales::year)
                        .thenComparingInt(row -> quarterOrder(row.quarter())))
                .toList();
        Map<Integer, Integer> yearSeriesIndexes = new TreeMap<>();
        Map<Integer, BigDecimal> largestQuarterByYear = new TreeMap<>();
        for (QuarterlySales row : orderedRows) {
            yearSeriesIndexes.computeIfAbsent(row.year(), ignored -> yearSeriesIndexes.size());
            largestQuarterByYear.merge(row.year(), row.revenue(), BigDecimal::max);
        }
        for (QuarterlySales row : orderedRows) {
            XYChart.Series<String, Number> series = byYear.computeIfAbsent(row.year(), year -> {
                XYChart.Series<String, Number> created = new XYChart.Series<>();
                created.setName(String.valueOf(year));
                return created;
            });
            String quarter = DashboardFormat.quarterLabel(row.quarter());
            XYChart.Data<String, Number> point = new XYChart.Data<>(quarter, row.revenue());
            String description = quarter + " " + row.year() + " · " + DashboardFormat.usd(row.revenue());
            installLineLabel(point, DashboardFormat.chartUsd(row.revenue()), description,
                    yearSeriesIndexes.get(row.year()), series.getData().size(), false,
                    row.revenue().compareTo(largestQuarterByYear.get(row.year())) >= 0);
            installTooltip(point, description);
            series.getData().add(point);
        }
        quarterlyChart.getData().setAll(byYear.values());
        ((CategoryAxis) quarterlyChart.getXAxis()).setCategories(
                FXCollections.observableArrayList("T1", "T2", "T3", "T4"));
        quarterlyChart.setAccessibleText("Ventas netas por trimestre en USD. Cada línea representa un año y los trimestres sin registros no se completan.");
    }

    private void renderCategories(List<CategoryRevenue> rows) {
        XYChart.Series<Number, String> series = new XYChart.Series<>();
        series.setName("Ventas por categoría");
        for (CategoryRevenue row : rows) {
            XYChart.Data<Number, String> point = new XYChart.Data<>(row.revenue(), row.category());
            String description = row.category() + " · " + DashboardFormat.percentage(row.percentage())
                    + " · " + DashboardFormat.usd(row.revenue());
            installBarLabel(point, compactName(row.category(), 16) + " · " + DashboardFormat.chartUsd(row.revenue()) + " · "
                    + DashboardFormat.percentage(row.percentage()), description, series.getData().size());
            installTooltip(point, description);
            series.getData().add(point);
        }
        categoryChart.getData().setAll(series);
        setCategories(categoryChart, rows.stream().map(CategoryRevenue::category).toList());
        categoryChart.setAccessibleText("Ventas netas por categoría de producto en USD. Barras ordenadas de mayor a menor; el tooltip incluye la participación porcentual.");
    }

    private void renderCountries(List<CountryRevenue> rows) {
        XYChart.Series<Number, String> series = new XYChart.Series<>();
        series.setName("Ventas");
        for (CountryRevenue row : rows) {
            XYChart.Data<Number, String> point = new XYChart.Data<>(row.revenue(), row.country());
            String description = row.country() + " · " + DashboardFormat.usd(row.revenue());
            installBarLabel(point, compactName(row.country(), 15) + " · " + DashboardFormat.chartUsd(row.revenue()),
                    description, series.getData().size());
            installTooltip(point, description);
            series.getData().add(point);
        }
        countryChart.getData().setAll(series);
        setCategories(countryChart, rows.stream().map(CountryRevenue::country).toList());
        countryChart.setAccessibleText("Los diez países con mayor venta, ordenados por ingresos.");
    }

    private void renderCustomers(List<CustomerRevenue> rows) {
        XYChart.Series<Number, String> series = new XYChart.Series<>();
        series.setName("Ventas");
        for (CustomerRevenue row : rows) {
            XYChart.Data<Number, String> point = new XYChart.Data<>(row.revenue(), row.customer());
            String description = row.customer() + " · " + DashboardFormat.usd(row.revenue());
            installBarLabel(point, compactName(row.customer(), 18) + " · " + DashboardFormat.chartUsd(row.revenue()),
                    description, series.getData().size());
            installTooltip(point, description);
            series.getData().add(point);
        }
        customerChart.getData().setAll(series);
        setCategories(customerChart, rows.stream().map(CustomerRevenue::customer).toList());
        customerChart.setAccessibleText("Los diez clientes con mayor venta, ordenados por ingresos.");
    }

    private void renderEmployees(List<EmployeeRevenue> rows, Integer selectedYear) {
        Map<String, XYChart.Series<String, Number>> byEmployee = new TreeMap<>();
        List<EmployeeRevenue> orderedRows = rows.stream()
                .sorted(Comparator.comparingInt(EmployeeRevenue::year).thenComparing(EmployeeRevenue::employee))
                .toList();
        Map<String, Integer> employeeSeriesIndexes = new TreeMap<>();
        rows.stream().map(EmployeeRevenue::employee).distinct().sorted()
                .forEach(employee -> employeeSeriesIndexes.put(employee, employeeSeriesIndexes.size()));
        for (EmployeeRevenue row : orderedRows) {
            XYChart.Series<String, Number> series = byEmployee.computeIfAbsent(row.employee(), employee -> {
                XYChart.Series<String, Number> created = new XYChart.Series<>();
                created.setName(employee);
                return created;
            });
            XYChart.Data<String, Number> point = new XYChart.Data<>(String.valueOf(row.year()), row.revenue());
            String description = row.employee() + " · " + row.year() + " · " + DashboardFormat.usd(row.revenue());
            installLineLabel(point, DashboardFormat.chartUsd(row.revenue()), description,
                    employeeSeriesIndexes.get(row.employee()), series.getData().size(), true, false);
            installTooltip(point, description);
            series.getData().add(point);
        }
        employeeTrendChart.getData().setAll(byEmployee.values());

        XYChart.Series<Number, String> comparison = new XYChart.Series<>();
        comparison.setName(selectedYear == null ? "Ventas" : "Ventas " + selectedYear);
        orderedRows.stream()
                .filter(row -> selectedYear != null && row.year() == selectedYear)
                .sorted(Comparator.comparing(EmployeeRevenue::revenue).reversed()
                        .thenComparing(EmployeeRevenue::employee))
                .forEach(row -> {
                    XYChart.Data<Number, String> point = new XYChart.Data<>(row.revenue(), row.employee());
                    String description = row.employee() + " · " + row.year() + " · "
                            + DashboardFormat.usd(row.revenue());
                    installBarLabel(point, compactName(row.employee(), 14) + " · " + DashboardFormat.chartUsd(row.revenue()), description,
                            comparison.getData().size());
                    installTooltip(point, description);
                    comparison.getData().add(point);
                });
        employeeYearChart.getData().setAll(comparison);
        setCategories(employeeYearChart,
                comparison.getData().stream().map(XYChart.Data::getYValue).toList());

        boolean showTrend = selectedYear == null;
        employeeTrendChart.setVisible(showTrend);
        employeeTrendChart.setManaged(showTrend);
        employeeYearChart.setVisible(!showTrend);
        employeeYearChart.setManaged(!showTrend);
        employeeTrendChart.setAccessibleText("Ventas anuales por empleado en USD. Cada línea representa a una persona.");
        employeeYearChart.setAccessibleText("Comparación de ventas por empleado para " + selectedYear
                + " en USD, ordenada de mayor a menor.");
    }

    private void renderSuppliers(List<SuppliersByCountry> rows) {
        XYChart.Series<Number, String> series = new XYChart.Series<>();
        series.setName("Proveedores");
        for (SuppliersByCountry row : rows) {
            XYChart.Data<Number, String> point = new XYChart.Data<>(row.supplierCount(), row.country());
            String description = row.country() + " · " + row.supplierCount() + " proveedores";
            installBarLabel(point, compactName(row.country(), 15) + " · " + row.supplierCount(),
                    description, series.getData().size());
            installTooltip(point, description);
            series.getData().add(point);
        }
        supplierChart.getData().setAll(series);
        setCategories(supplierChart, rows.stream().map(SuppliersByCountry::country).toList());
        long maxCount = rows.stream().mapToLong(SuppliersByCountry::supplierCount).max().orElse(0);
        double countTick = Math.max(1, Math.ceil(maxCount / 5.0));
        NumberAxis countAxis = (NumberAxis) supplierChart.getXAxis();
        countAxis.setAutoRanging(false);
        countAxis.setLowerBound(0);
        countAxis.setTickUnit(countTick);
        countAxis.setUpperBound(Math.max(countTick, (Math.floor(maxCount / countTick) + 1) * countTick));
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

    private void restoreLoadedYearSelection() {
        settingYears = true;
        try {
            yearFilter.setValue(loadedYear == null ? ALL_YEARS : String.valueOf(loadedYear));
        } finally {
            settingYears = false;
        }
        if (selectedReport != null) {
            updateDetailContent(selectedReport);
        }
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

    private static BufferedImage snapshotChart(Chart chart) {
        chart.applyCss();
        chart.layout();
        int width = Math.max(1, (int) Math.ceil(chart.getBoundsInLocal().getWidth()));
        int height = Math.max(1, (int) Math.ceil(chart.getBoundsInLocal().getHeight()));
        return snapshotAsBufferedImage(chart.snapshot(new SnapshotParameters(), new WritableImage(width, height)));
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
        employeeTrendChart.getData().clear();
        employeeYearChart.getData().clear();
        clearCategories(categoryChart);
        clearCategories(countryChart);
        clearCategories(customerChart);
        clearCategories(employeeYearChart);
        clearCategories(supplierChart);
        supplierChart.getData().clear();
    }

    private static void setCategories(Chart chart, List<String> categories) {
        List<String> displayOrder = new ArrayList<>(categories);
        Collections.reverse(displayOrder);
        CategoryAxis axis = (CategoryAxis) ((javafx.scene.chart.XYChart<?, ?>) chart).getYAxis();
        axis.setAutoRanging(false);
        axis.setCategories(FXCollections.observableArrayList(displayOrder));
    }

    private static void clearCategories(Chart chart) {
        ((CategoryAxis) ((javafx.scene.chart.XYChart<?, ?>) chart).getYAxis()).getCategories().clear();
    }

    private void configureChartAxes() {
        configureUsdAxis((NumberAxis) quarterlyChart.getYAxis());
        configureUsdAxis((NumberAxis) categoryChart.getXAxis());
        configureUsdAxis((NumberAxis) countryChart.getXAxis());
        configureUsdAxis((NumberAxis) customerChart.getXAxis());
        configureUsdAxis((NumberAxis) employeeTrendChart.getYAxis());
        configureUsdAxis((NumberAxis) employeeYearChart.getXAxis());
        configureCategoryAxis((CategoryAxis) categoryChart.getYAxis(), 100);
        configureCategoryAxis((CategoryAxis) countryChart.getYAxis(), 104);
        configureCategoryAxis((CategoryAxis) customerChart.getYAxis(), 138);
        configureCategoryAxis((CategoryAxis) employeeYearChart.getYAxis(), 128);
        configureCategoryAxis((CategoryAxis) supplierChart.getYAxis(), 108);
        NumberAxis supplierAxis = (NumberAxis) supplierChart.getXAxis();
        supplierAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number value) {
                return Long.toString(Math.round(value.doubleValue()));
            }

            @Override
            public Number fromString(String value) {
                return Long.parseLong(value);
            }
        });
    }

    private static void configureUsdAxis(NumberAxis axis) {
        axis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number value) {
                return DashboardFormat.chartUsd(BigDecimal.valueOf(value.doubleValue()));
            }

            @Override
            public Number fromString(String value) {
                throw new UnsupportedOperationException("Revenue axes are output only.");
            }
        });
    }

    private static void configureCategoryAxis(CategoryAxis axis, double minimumWidth) {
        axis.setTickLabelsVisible(true);
        axis.setMinWidth(minimumWidth);
    }

    private static int quarterOrder(String quarter) {
        return switch (DashboardFormat.quarterLabel(quarter)) {
            case "T1" -> 1;
            case "T2" -> 2;
            case "T3" -> 3;
            case "T4" -> 4;
            default -> 5;
        };
    }

    private static String compactName(String name, int maxLength) {
        if (name.length() <= maxLength) {
            return name;
        }
        return name.substring(0, maxLength - 1).stripTrailing() + "…";
    }

    private static void installBarLabel(XYChart.Data<?, ?> point, String labelText,
                                        String accessibleText, int dataIndex) {
        Label label = new Label(labelText);
        label.getStyleClass().add("chart-data-label");
        label.setAccessibleText(accessibleText);
        label.setMouseTransparent(true);
        StackPane bar = new StackPane(label);
        bar.getStyleClass().setAll("chart-bar", "default-color0", "series0", "data" + dataIndex);
        StackPane.setAlignment(label, Pos.CENTER_LEFT);
        label.setTranslateX(5);
        bar.setAccessibleText(accessibleText);
        point.setNode(bar);
    }

    private static void installLineLabel(XYChart.Data<?, ?> point, String labelText,
                                         String accessibleText, int seriesIndex, int pointIndex,
                                         boolean separateEmployeeSeries, boolean placeBelow) {
        Label label = new Label(labelText);
        label.getStyleClass().add("chart-data-label");
        label.setAccessibleText(accessibleText);
        label.setMouseTransparent(true);
        label.setManaged(false);
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        label.setPadding(new Insets(1, 3, 1, 3));
        double labelWidth = Math.max(30, labelText.length() * 6.2 + 8);
        label.setMinSize(labelWidth, 16);
        label.setPrefSize(labelWidth, 16);
        label.setMaxSize(labelWidth, 16);
        label.resize(labelWidth, 16);
        boolean placeRight = (seriesIndex + pointIndex) % 2 == 0;
        StackPane symbol = new StackPane(label);
        symbol.setMinSize(10, 10);
        symbol.setPrefSize(10, 10);
        symbol.setMaxSize(10, 10);
        symbol.getStyleClass().setAll("chart-line-symbol", "default-color" + Math.floorMod(seriesIndex, 9),
                "series" + seriesIndex, "data" + pointIndex);
        double x;
        double y;
        if (separateEmployeeSeries) {
            label.widthProperty().addListener((observable, oldWidth, width) -> label.relocate(
                    placeRight ? 8 : -width.doubleValue() - 8, label.getLayoutY()));
            x = placeRight ? 8 : -label.getWidth() - 8;
            y = switch (Math.floorMod(seriesIndex + pointIndex, 3)) {
                case 0 -> -label.getHeight() - 6;
                case 1 -> -label.getHeight() / 2;
                default -> 8;
            };
        } else {
            x = 5 - label.getWidth() / 2;
            y = placeBelow ? 9 : -label.getHeight() - 4;
        }
        label.relocate(x, y);
        symbol.setAccessibleText(accessibleText);
        point.setNode(symbol);
    }

    private static void installTooltip(XYChart.Data<?, ?> point, String text) {
        point.nodeProperty().addListener((observable, previous, node) -> {
            if (node != null) {
                Tooltip.install(node, new Tooltip(text));
            }
        });
        if (point.getNode() != null) {
            Tooltip.install(point.getNode(), new Tooltip(text));
        }
    }

    private Stage stage() {
        return (Stage) refreshButton.getScene().getWindow();
    }
}
