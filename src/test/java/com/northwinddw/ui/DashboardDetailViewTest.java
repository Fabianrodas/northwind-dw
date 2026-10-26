package com.northwinddw.ui;

import com.northwinddw.Main;
import com.northwinddw.model.DashboardData;
import com.northwinddw.model.DashboardReport;
import com.northwinddw.model.RevenueSummary;
import com.northwinddw.model.ReportRows.CategoryRevenue;
import com.northwinddw.model.ReportRows.CountryRevenue;
import com.northwinddw.model.ReportRows.CustomerRevenue;
import com.northwinddw.model.ReportRows.EmployeeRevenue;
import com.northwinddw.model.ReportRows.QuarterlySales;
import com.northwinddw.model.ReportRows.SuppliersByCountry;
import com.northwinddw.service.DashboardExporter;
import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.Chart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardDetailViewTest {
    @Test
    void providesAChartDetailPanelAlongsideTheExistingDashboardControls() throws Exception {
        Preferences preferences = Preferences.userRoot().node("northwind-dw/detail-tests/" + UUID.randomUUID());
        CountDownLatch complete = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Platform.startup(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/dashboard.fxml"));
                loader.setControllerFactory(type -> new DashboardController(
                        year -> {
                            if (Integer.valueOf(1998).equals(year)) {
                                throw new SQLException("Simulated refresh failure.");
                            }
                            return sampleData(year);
                        }, new DashboardExporter(), null,
                        DashboardTheme.LIGHT, preferences));
                Parent root = loader.load();
                ImageView brandLogo = (ImageView) loader.getNamespace().get("brandLogo");
                assertNotNull(brandLogo, "The Northwind brand logo should be present in the dashboard header.");
                assertNotNull(brandLogo.getImage(), "The Northwind logo image should load from app resources.");
                assertEquals("Logotipo de Northwind Traders", brandLogo.getAccessibleText());
                Scene scene = new Scene(root, 1200, 900);
                scene.getStylesheets().add(Main.class.getResource("/css/dashboard.css").toExternalForm());
                Stage stage = new Stage();
                stage.setScene(scene);
                stage.show();
                DashboardController controller = loader.getController();
                Label status = (Label) loader.getNamespace().get("statusLabel");
                new AnimationTimer() {
                    @Override
                    public void handle(long now) {
                        if (status.getText().startsWith("Datos actualizados")) {
                            stop();
                            try {
                                assertChartMappings(loader);
                                assertEveryPointHasVisibleLabel(loader);
                                List<VBox> cards = List.of("quarterlyCard", "categoryCard", "countryCard",
                                                "customerCard", "employeeCard", "supplierCard").stream()
                                        .map(id -> (VBox) loader.getNamespace().get(id))
                                        .toList();
                                verifyResponsiveWidths(stage, root,
                                        (FlowPane) loader.getNamespace().get("reportsGrid"), cards,
                                        new double[]{1480, 1000, 800}, 0, () -> startDetailFlow(
                                                loader, root, stage, cards, controller, preferences, failure, complete),
                                        exception -> finish(stage, controller, failure, complete, exception));
                            } catch (Throwable exception) {
                                finish(stage, controller, failure, complete, exception);
                            }
                        }
                    }
                }.start();
            } catch (Throwable exception) {
                failure.set(exception);
                Platform.exit();
                complete.countDown();
            }
        });

        try {
            assertTrue(complete.await(15, TimeUnit.SECONDS), "JavaFX detail view did not finish.");
            if (failure.get() != null) {
                throw new AssertionError("The chart detail view is missing.", failure.get());
            }
        } finally {
            preferences.removeNode();
            Preferences.userRoot().flush();
        }
    }

    private static DashboardData sampleData(Integer selectedYear) {
        List<QuarterlySales> quarterlySales = List.of(
                new QuarterlySales(1996, "Third", new BigDecimal("100.00")),
                new QuarterlySales(1997, "First", new BigDecimal("120.00")),
                new QuarterlySales(1997, "Second", new BigDecimal("140.00")),
                new QuarterlySales(1998, "Fourth", new BigDecimal("160.00"))).stream()
                .filter(row -> selectedYear == null || row.year() == selectedYear)
                .toList();
        List<EmployeeRevenue> employeeRevenue = List.of(
                new EmployeeRevenue(1996, "Nancy Davolio", new BigDecimal("110.00")),
                new EmployeeRevenue(1997, "Nancy Davolio", new BigDecimal("255.00")),
                new EmployeeRevenue(1998, "Nancy Davolio", new BigDecimal("180.00")),
                new EmployeeRevenue(1996, "Janet Leverling", new BigDecimal("120.00")),
                new EmployeeRevenue(1997, "Janet Leverling", new BigDecimal("220.00")),
                new EmployeeRevenue(1998, "Janet Leverling", new BigDecimal("210.00")),
                new EmployeeRevenue(1997, "Andrew Fuller", new BigDecimal("150.00")),
                new EmployeeRevenue(1998, "Andrew Fuller", new BigDecimal("190.00"))).stream()
                .filter(row -> selectedYear == null || row.year() == selectedYear)
                .toList();
        return new DashboardData(
                new RevenueSummary(selectedYear == null ? new BigDecimal("1265792.94") : new BigDecimal("260.00"), 89, 29),
                quarterlySales,
                List.of(new CategoryRevenue("Bebidas", new BigDecimal("320000"), new BigDecimal("25.3")),
                        new CategoryRevenue("Lácteos", new BigDecimal("280000"), new BigDecimal("22.1")),
                        new CategoryRevenue("Carnes", new BigDecimal("240000"), new BigDecimal("19.0"))),
                List.of(new CountryRevenue("Estados Unidos", new BigDecimal("245000")),
                        new CountryRevenue("Alemania", new BigDecimal("185000")),
                        new CountryRevenue("Austria", new BigDecimal("136000"))),
                List.of(new CustomerRevenue("Alfreds Futterkiste", new BigDecimal("68000"))),
                employeeRevenue,
                List.of(new SuppliersByCountry("Estados Unidos", 4)),
                List.of(1996, 1997, 1998));
    }

    private static void startDetailFlow(FXMLLoader loader, Parent root, Stage stage, List<VBox> cards,
                                        DashboardController controller, Preferences preferences,
                                        AtomicReference<Throwable> failure, CountDownLatch complete) {
        try {
            VBox detailPanel = (VBox) loader.getNamespace().get("detailPanel");
            Label title = (Label) loader.getNamespace().get("detailTitle");
            Label period = (Label) loader.getNamespace().get("detailPeriod");
            StackPane chartHost = (StackPane) loader.getNamespace().get("detailChartHost");
            FlowPane grid = (FlowPane) loader.getNamespace().get("reportsGrid");
            HBox summaryRow = (HBox) loader.getNamespace().get("summaryRow");
            ComboBox<String> yearFilter = (ComboBox<String>) loader.getNamespace().get("yearFilter");
            Button themeButton = (Button) loader.getNamespace().get("themeToggleButton");
            Button backButton = (Button) loader.getNamespace().get("backToGridButton");
            assertEquals("Regresar", backButton.getText(), "The back button should not contain a decorative arrow.");
            assertNotNull(detailPanel, "The dashboard should have a chart detail panel.");
            assertNotNull(loader.getNamespace().get("detailPdfButton"));
            assertNotNull(loader.getNamespace().get("detailQrButton"));
            assertTrue(root.getStyleClass().contains(DashboardTheme.LIGHT.styleClass()));
            assertTrue(((LineChart<?, ?>) loader.getNamespace().get("employeeTrendChart")).isVisible(),
                    "All years should show the employee time trend.");
            assertFalse(((BarChart<?, ?>) loader.getNamespace().get("employeeYearChart")).isVisible(),
                    "The one-year employee comparison should be hidden for all years.");

            ScrollPaneAccess scroll = new ScrollPaneAccess(loader);
            scroll.pane().setVvalue(0.42);
            double previousPosition = scroll.pane().getVvalue();
            VBox quarterlyCard = cards.getFirst();
            quarterlyCard.fireEvent(keyPress(KeyCode.SPACE));
            assertTrue(detailPanel.isVisible());
            assertTrue(!grid.isVisible());
            assertTrue(!summaryRow.isVisible());
            assertEquals(DashboardReport.QUARTERLY_SALES.title(), title.getText());
            assertEquals("Periodo: Todos los años", period.getText());
            assertEquals(chartHost, chartFor(loader, DashboardReport.QUARTERLY_SALES).getParent());
            assertEquals(AccessibleRole.BUTTON, quarterlyCard.getAccessibleRole());
            PauseTransition focusCheck = new PauseTransition(Duration.millis(120));
            focusCheck.setOnFinished(event -> {
                try {
                    assertEquals(backButton, stage.getScene().getFocusOwner(),
                            "Opening a report should move keyboard focus to the Back button.");
                    themeButton.fire();
                    assertTrue(root.getStyleClass().contains(DashboardTheme.DARK.styleClass()));
                    root.applyCss();
                    assertLabelContrast(loader, "categoryChart");
                    assertTrue(themeButton.isVisible());
                    assertTrue(yearFilter.isVisible());
                    yearFilter.setValue("1997");
                    awaitYearRefresh(loader, root, stage, cards, controller, preferences, previousPosition,
                            failure, complete);
                } catch (Throwable exception) {
                    finish(stage, controller, failure, complete, exception);
                }
            });
            focusCheck.play();
        } catch (Throwable exception) {
            finish(stage, controller, failure, complete, exception);
        }
    }

    private static void awaitYearRefresh(FXMLLoader loader, Parent root, Stage stage, List<VBox> cards,
                                         DashboardController controller, Preferences preferences,
                                         double previousPosition, AtomicReference<Throwable> failure,
                                         CountDownLatch complete) {
        Label status = (Label) loader.getNamespace().get("statusLabel");
        new AnimationTimer() {
            private boolean loadingStarted;

            @Override
            public void handle(long now) {
                if (status.getText().startsWith("Consultando")) {
                    loadingStarted = true;
                }
                if (loadingStarted && status.getText().startsWith("Datos actualizados")) {
                    stop();
                    try {
                        Label period = (Label) loader.getNamespace().get("detailPeriod");
                        assertEquals("Periodo: 1997", period.getText());
                        LineChart<String, Number> chart = (LineChart<String, Number>)
                                loader.getNamespace().get("quarterlyChart");
                        assertEquals(1, chart.getData().size());
                        assertEquals("1997", chart.getData().getFirst().getName());
                        assertEquals(2, chart.getData().getFirst().getData().size());
                        assertEquals(List.of("T1", "T2"), chart.getData().getFirst().getData().stream()
                                .map(XYChart.Data::getXValue).toList());
                        assertEquals(3, ((LineChart<?, ?>) loader.getNamespace().get("employeeTrendChart"))
                                .getData().size());
                        assertFalse(((LineChart<?, ?>) loader.getNamespace().get("employeeTrendChart")).isVisible());
                        assertTrue(((BarChart<?, ?>) loader.getNamespace().get("employeeYearChart")).isVisible(),
                                "A selected year should show employee rankings as bars.");
                        assertEveryPointHasVisibleLabel(loader, "employeeYearChart");
                        ComboBox<String> yearFilter = (ComboBox<String>) loader.getNamespace().get("yearFilter");
                        yearFilter.setValue("1998");
                        awaitFailedYearRefresh(loader, root, stage, cards, controller, preferences,
                                previousPosition, failure, complete);
                    } catch (Throwable exception) {
                        finish(stage, controller, failure, complete, exception);
                    }
                }
            }
        }.start();
    }

    private static void awaitFailedYearRefresh(FXMLLoader loader, Parent root, Stage stage, List<VBox> cards,
                                               DashboardController controller, Preferences preferences,
                                               double previousPosition, AtomicReference<Throwable> failure,
                                               CountDownLatch complete) {
        Label status = (Label) loader.getNamespace().get("statusLabel");
        new AnimationTimer() {
            private long startedAt;

            @Override
            public void handle(long now) {
                if (startedAt == 0) {
                    startedAt = now;
                }
                if (status.getText().startsWith("No se pudo actualizar")) {
                    stop();
                    try {
                        ComboBox<String> yearFilter = (ComboBox<String>) loader.getNamespace().get("yearFilter");
                        assertEquals("1997", yearFilter.getValue(),
                                "After a failed refresh, the filter should match the visible data.");
                        assertEquals("Periodo: 1997", ((Label) loader.getNamespace().get("detailPeriod")).getText());
                        LineChart<String, Number> chart = (LineChart<String, Number>)
                                loader.getNamespace().get("quarterlyChart");
                        assertEquals("1997", chart.getData().getFirst().getName());
                        assertTrue(((BarChart<?, ?>) loader.getNamespace().get("employeeYearChart")).isVisible(),
                                "A failed refresh must preserve the chart for the visible year.");
                        Button backButton = (Button) loader.getNamespace().get("backToGridButton");
                        backButton.fire();
                        assertTrue(((FlowPane) loader.getNamespace().get("reportsGrid")).isVisible());
                        assertTrue(((HBox) loader.getNamespace().get("summaryRow")).isVisible());
                        PauseTransition focusRestore = new PauseTransition(Duration.millis(120));
                        focusRestore.setOnFinished(focusEvent -> {
                            try {
                                ScrollPaneAccess scroll = new ScrollPaneAccess(loader);
                                assertEquals(previousPosition, scroll.pane().getVvalue(), 0.05);
                                assertEquals(cards.getFirst(), stage.getScene().getFocusOwner());
                                assertTrue(root.getStyleClass().contains(DashboardTheme.DARK.styleClass()));
                                verifyEveryReport(loader, root, stage, cards, controller, preferences,
                                        0, failure, complete);
                            } catch (Throwable exception) {
                                finish(stage, controller, failure, complete, exception);
                            }
                        });
                        focusRestore.play();
                    } catch (Throwable exception) {
                        finish(stage, controller, failure, complete, exception);
                    }
                } else if (now - startedAt > Duration.seconds(5).toMillis() * 1_000_000) {
                    stop();
                    finish(stage, controller, failure, complete,
                            new AssertionError("Timed out waiting for the simulated refresh failure."));
                }
            }
        }.start();
    }

    private static void verifyEveryReport(FXMLLoader loader, Parent root, Stage stage, List<VBox> cards,
                                          DashboardController controller, Preferences preferences, int index,
                                          AtomicReference<Throwable> failure, CountDownLatch complete) {
        if (index == DashboardReport.values().length) {
            finish(stage, controller, failure, complete, null);
            return;
        }
        try {
            DashboardReport report = DashboardReport.values()[index];
            VBox card = cards.get(index);
            if (index == 0) {
                card.fireEvent(mouseClick());
            } else {
                card.fireEvent(keyPress(index == 1 ? KeyCode.ENTER : KeyCode.SPACE));
            }

            VBox detailPanel = (VBox) loader.getNamespace().get("detailPanel");
            FlowPane grid = (FlowPane) loader.getNamespace().get("reportsGrid");
            StackPane chartHost = (StackPane) loader.getNamespace().get("detailChartHost");
            Label title = (Label) loader.getNamespace().get("detailTitle");
            Button themeButton = (Button) loader.getNamespace().get("themeToggleButton");
            ComboBox<String> yearFilter = (ComboBox<String>) loader.getNamespace().get("yearFilter");
            Button pdfButton = (Button) loader.getNamespace().get("detailPdfButton");
            Button qrButton = (Button) loader.getNamespace().get("detailQrButton");
            assertTrue(detailPanel.isVisible());
            assertTrue(!grid.isVisible());
            assertEquals(report.title(), title.getText());
            assertEquals(chartHost, chartFor(loader, report).getParent());
            assertTrue(themeButton.isVisible());
            assertTrue(yearFilter.isVisible());
            assertTrue(pdfButton.isVisible());
            assertTrue(qrButton.isVisible());

            if (index == 2) {
                stage.setWidth(760);
                PauseTransition resizeCheck = new PauseTransition(Duration.millis(180));
                resizeCheck.setOnFinished(event -> {
                    try {
                        root.applyCss();
                        root.layout();
                        assertTrue(chartFor(loader, report).getWidth() > 250);
                        returnAndContinue(loader, root, stage, cards, controller, preferences,
                                index, failure, complete);
                    } catch (Throwable exception) {
                        finish(stage, controller, failure, complete, exception);
                    }
                });
                resizeCheck.play();
            } else {
                returnAndContinue(loader, root, stage, cards, controller, preferences,
                        index, failure, complete);
            }
        } catch (Throwable exception) {
            finish(stage, controller, failure, complete, exception);
        }
    }

    private static void returnAndContinue(FXMLLoader loader, Parent root, Stage stage, List<VBox> cards,
                                          DashboardController controller, Preferences preferences, int index,
                                          AtomicReference<Throwable> failure, CountDownLatch complete) {
        try {
            ((Button) loader.getNamespace().get("backToGridButton")).fire();
            assertTrue(((FlowPane) loader.getNamespace().get("reportsGrid")).isVisible());
            assertTrue(((HBox) loader.getNamespace().get("summaryRow")).isVisible());
            Object expectedParent = index == 4
                    ? loader.getNamespace().get("employeeChartHost") : cardFor(index, cards);
            assertEquals(expectedParent, chartFor(loader, DashboardReport.values()[index]).getParent());
            PauseTransition focusRestore = new PauseTransition(Duration.millis(100));
            focusRestore.setOnFinished(event -> {
                try {
                    assertEquals(cardFor(index, cards), stage.getScene().getFocusOwner());
                    assertTrue(root.getStyleClass().contains(DashboardTheme.DARK.styleClass()));
                    verifyEveryReport(loader, root, stage, cards, controller, preferences,
                            index + 1, failure, complete);
                } catch (Throwable exception) {
                    finish(stage, controller, failure, complete, exception);
                }
            });
            focusRestore.play();
        } catch (Throwable exception) {
            finish(stage, controller, failure, complete, exception);
        }
    }

    private static void verifyResponsiveWidths(Stage stage, Parent root, FlowPane grid, List<VBox> cards,
                                               double[] widths, int index, Runnable onComplete,
                                               java.util.function.Consumer<Throwable> onFailure) {
        if (index == widths.length) {
            onComplete.run();
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
                assertEquals(expected.columns(), cardsOnFirstRow);
                verifyResponsiveWidths(stage, root, grid, cards, widths, index + 1, onComplete, onFailure);
            } catch (Throwable exception) {
                onFailure.accept(exception);
            }
        });
        layoutWait.play();
    }

    private static Chart chartFor(FXMLLoader loader, DashboardReport report) {
        return switch (report) {
            case QUARTERLY_SALES -> (Chart) loader.getNamespace().get("quarterlyChart");
            case CATEGORY_REVENUE -> (Chart) loader.getNamespace().get("categoryChart");
            case COUNTRY_REVENUE -> (Chart) loader.getNamespace().get("countryChart");
            case CUSTOMER_REVENUE -> (Chart) loader.getNamespace().get("customerChart");
            case EMPLOYEE_REVENUE -> ((ComboBox<?>) loader.getNamespace().get("yearFilter")).getValue()
                    .equals("Todos los años")
                    ? (Chart) loader.getNamespace().get("employeeTrendChart")
                    : (Chart) loader.getNamespace().get("employeeYearChart");
            case SUPPLIERS_BY_COUNTRY -> (Chart) loader.getNamespace().get("supplierChart");
        };
    }

    private static void assertChartMappings(FXMLLoader loader) {
        ((Parent) loader.getRoot()).applyCss();
        ((Parent) loader.getRoot()).layout();
        assertLabelContrast(loader, "categoryChart");
        assertTrue(loader.getNamespace().get("categoryChart") instanceof BarChart<?, ?>,
                "Nominal product categories should use bars for precise comparisons.");

        LineChart<String, Number> quarters = (LineChart<String, Number>) loader.getNamespace().get("quarterlyChart");
        assertEquals("Trimestre", ((CategoryAxis) quarters.getXAxis()).getLabel());
        assertEquals("Ventas netas (USD)", ((NumberAxis) quarters.getYAxis()).getLabel());
        assertEquals(List.of("1996", "1997", "1998"), quarters.getData().stream()
                .map(XYChart.Series::getName).toList());
        assertEquals(1, quarters.getData().getFirst().getData().size(),
                "Missing quarters must remain gaps and must not be filled with zero values.");
        assertEquals("T3", quarters.getData().getFirst().getData().getFirst().getXValue());
        assertEquals(List.of("T1", "T2"), quarters.getData().get(1).getData().stream()
                .map(XYChart.Data::getXValue).toList());

        assertEquals("Ventas netas (USD)", ((NumberAxis) ((BarChart<?, ?>) loader.getNamespace()
                .get("categoryChart")).getXAxis()).getLabel());
        assertTrue(((NumberAxis) ((BarChart<?, ?>) loader.getNamespace()
                .get("categoryChart")).getXAxis()).isForceZeroInRange(),
                "Comparative bar charts should include a meaningful zero baseline.");
        assertEquals("Categoría", ((CategoryAxis) ((BarChart<?, ?>) loader.getNamespace()
                .get("categoryChart")).getYAxis()).getLabel());
        assertEquals(List.of("Carnes", "Lácteos", "Bebidas"),
                ((CategoryAxis) ((BarChart<?, ?>) loader.getNamespace().get("categoryChart"))
                        .getYAxis()).getCategories(),
                "Reverse axis categories so the largest value appears at the top of the ranking.");
        assertEquals("País", ((CategoryAxis) ((BarChart<?, ?>) loader.getNamespace()
                .get("countryChart")).getYAxis()).getLabel());
        assertEquals("Cliente", ((CategoryAxis) ((BarChart<?, ?>) loader.getNamespace()
                .get("customerChart")).getYAxis()).getLabel());
        assertEquals("Empleado", ((CategoryAxis) ((BarChart<?, ?>) loader.getNamespace()
                .get("employeeYearChart")).getYAxis()).getLabel());
        assertEquals("País", ((CategoryAxis) ((BarChart<?, ?>) loader.getNamespace()
                .get("supplierChart")).getYAxis()).getLabel());
        NumberAxis supplierAxis = (NumberAxis) ((BarChart<?, ?>) loader.getNamespace()
                .get("supplierChart")).getXAxis();
        assertFalse(supplierAxis.isAutoRanging(), "Provider counts should use fixed integer ticks.");
        assertEquals(0.0, supplierAxis.getLowerBound());
        assertEquals(0.0, supplierAxis.getTickUnit() % 1.0,
                "A count axis should advance in whole providers.");

        LineChart<String, Number> employeeTrend =
                (LineChart<String, Number>) loader.getNamespace().get("employeeTrendChart");
        assertEquals(List.of("1996", "1997", "1998"), employeeTrend.getData().stream()
                .filter(series -> series.getName().equals("Nancy Davolio")).findFirst().orElseThrow().getData().stream()
                .map(XYChart.Data::getXValue).toList());
        List<String> employeeColors = employeeTrend.getData().stream()
                .map(series -> series.getData().getFirst().getNode().getStyleClass().stream()
                        .filter(styleClass -> styleClass.matches("default-color\\d+"))
                        .findFirst().orElseThrow())
                .distinct().toList();
        assertEquals(employeeTrend.getData().size(), employeeColors.size(),
                "Each employee trend should keep a distinct series color.");
        assertEquals("$1.2K", ((NumberAxis) ((BarChart<?, ?>) loader.getNamespace()
                .get("categoryChart")).getXAxis()).getTickLabelFormatter().toString(1200),
                "Revenue axes should use compact dollar labels.");
        assertEquals("3", supplierAxis.getTickLabelFormatter().toString(3),
                "Supplier counts should stay whole numbers on the axis.");
    }

    private static void assertEveryPointHasVisibleLabel(FXMLLoader loader, String... chartIds) {
        if (chartIds.length == 0) {
            chartIds = new String[]{"quarterlyChart", "categoryChart", "countryChart", "customerChart",
                    "employeeTrendChart", "supplierChart"};
        }
        ((Parent) loader.getRoot()).applyCss();
        ((Parent) loader.getRoot()).layout();
        for (String chartId : chartIds) {
            XYChart<?, ?> chart = (XYChart<?, ?>) loader.getNamespace().get(chartId);
            for (XYChart.Series<?, ?> series : chart.getData()) {
                for (XYChart.Data<?, ?> point : series.getData()) {
                    if (point.getYValue() == null) {
                        continue;
                    }
                    Node node = point.getNode();
                    assertNotNull(node, chartId + " should create a visible node for each observation.");
                    Label label = (Label) node.lookup(".chart-data-label");
                    assertNotNull(label, chartId + " should show a value beside every plotted observation.");
                    assertFalse(label.getText().isBlank());
                    assertTrue(label.getWidth() > 0 && label.getHeight() > 0,
                            chartId + " data labels should occupy visible space.");
                    assertFalse(node.getAccessibleText() == null || node.getAccessibleText().isBlank(),
                            "Data points should expose their exact context to assistive technology.");
                    if (chartId.equals("categoryChart")) {
                        assertTrue(label.getText().contains(point.getYValue().toString()),
                                "Category bars should name the compared category alongside its measure.");
                    }
                }
            }
        }
    }

    private static void assertLabelContrast(FXMLLoader loader, String chartId) {
        XYChart<?, ?> chart = (XYChart<?, ?>) loader.getNamespace().get(chartId);
        Label label = (Label) chart.getData().getFirst().getData().getFirst().getNode()
                .lookup(".chart-data-label");
        Color background = (Color) label.getBackground().getFills().getFirst().getFill();
        assertTrue(Math.abs(background.getBrightness() - ((Color) label.getTextFill()).getBrightness()) > 0.65,
                "Data labels should retain readable contrast in each theme.");
    }

    private static VBox cardFor(int index, List<VBox> cards) {
        return cards.get(index);
    }

    private static KeyEvent keyPress(KeyCode code) {
        String character = code == KeyCode.SPACE ? " " : "";
        return new KeyEvent(KeyEvent.KEY_PRESSED, character, character, code, false, false, false, false);
    }

    private static MouseEvent mouseClick() {
        return new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, true, false, true, null);
    }

    private static void finish(Stage stage, DashboardController controller,
                               AtomicReference<Throwable> failure, CountDownLatch complete, Throwable exception) {
        if (exception != null) {
            failure.set(exception);
        }
        stage.close();
        controller.shutdown();
        Platform.exit();
        complete.countDown();
    }

    private static final class ScrollPaneAccess {
        private final javafx.scene.control.ScrollPane pane;

        private ScrollPaneAccess(FXMLLoader loader) {
            pane = (javafx.scene.control.ScrollPane) loader.getNamespace().get("dashboardScroll");
        }

        private javafx.scene.control.ScrollPane pane() {
            return pane;
        }
    }
}
