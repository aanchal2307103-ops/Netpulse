package com.netpulse.ui;

import com.netpulse.model.*;
import com.netpulse.service.DatabaseManager;
import com.netpulse.service.JsonConfigManager;
import com.netpulse.service.NetworkProbeService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;


public class MainView {

    private final Stage stage;
    private final DatabaseManager db;
    private final JsonConfigManager jsonManager = new JsonConfigManager();
    private final NetworkProbeService probeService;

    private final ObservableList<Endpoint> endpoints = FXCollections.observableArrayList();
    private final TableView<Endpoint> table = new TableView<>();
    private final LineChart<String, Number> latencyChart;
    private final Label summaryLabel = new Label("No endpoints yet.");
    private final TextArea logArea = new TextArea();

    public MainView(Stage stage) {
        this.stage = stage;
        this.db = new DatabaseManager();
        this.probeService = new NetworkProbeService(4, this::onProbeResult);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Latency (ms)");
        this.latencyChart = new LineChart<>(new javafx.scene.chart.CategoryAxis(), yAxis);
        this.latencyChart.setTitle("Latency Trend (selected endpoint)");
        this.latencyChart.setAnimated(false);
        this.latencyChart.setCreateSymbols(true);

        buildTable();
        loadExistingEndpoints();
    }

    public void build() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));

        root.setTop(buildToolbar());
        root.setCenter(buildCenter());
        root.setBottom(buildLog());

        Scene scene = new Scene(root, 1080, 680);
        stage.setTitle("NetPulse — Network Diagnostic & Monitoring Dashboard");
        stage.setScene(scene);
        stage.show();
    }

    // ---------------------------------------------------------------- toolbar

    private HBox buildToolbar() {
        Button addBtn = new Button("Add Endpoint");
        addBtn.setOnAction(e -> onAddEndpoint());

        Button removeBtn = new Button("Remove Selected");
        styleButton(removeBtn, Color.web("#dc2626"));
        removeBtn.setOnAction(e -> onRemoveSelected());

        Button checkNowBtn = new Button("Check Now");
        styleButton(checkNowBtn, Color.web("#0f766e"));
        checkNowBtn.setOnAction(e -> onCheckNowSelected());

        Button stopMonitoringBtn = new Button("Stop Monitoring");
        styleButton(stopMonitoringBtn, Color.web("#b45309"));
        stopMonitoringBtn.setOnAction(e -> onStopMonitoringSelected());

        Button importBtn = new Button("Import JSON");
        styleButton(importBtn, Color.web("#64748b"));
        importBtn.setOnAction(e -> onImportJson());

        Button exportBtn = new Button("Export JSON");
        styleButton(exportBtn, Color.web("#64748b"));
        exportBtn.setOnAction(e -> onExportJson());

        Button apiProbeBtn = new Button("Probe JSON API…");
        apiProbeBtn.setOnAction(e -> onProbeJsonApi());

        styleButton(addBtn, Color.web("#2563eb"));
        styleButton(apiProbeBtn, Color.web("#2563eb"));

        HBox bar = new HBox(10, addBtn, removeBtn, checkNowBtn, stopMonitoringBtn,
                new Separator(), importBtn, exportBtn,
                new Separator(), apiProbeBtn);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 0, 12, 0));
        return bar;
    }

    // ---------------------------------------------------------------- center: table + chart

    private VBox buildCenter() {
        table.setPrefHeight(320);
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldEp, newEp) -> refreshChart(newEp));

        styleSummaryLabel();
        styleChart();
        VBox box = new VBox(10, table, summaryLabel, latencyChart);
        VBox.setVgrow(latencyChart, Priority.ALWAYS);
        VBox.setVgrow(table, Priority.NEVER);
        return box;
    }

    private void buildTable() {
        TableColumn<Endpoint, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Endpoint, String> hostCol = new TableColumn<>("Host");
        hostCol.setCellValueFactory(new PropertyValueFactory<>("host"));

        TableColumn<Endpoint, Number> portCol = new TableColumn<>("Port");
        portCol.setCellValueFactory(new PropertyValueFactory<>("port"));

        TableColumn<Endpoint, EndpointType> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));

        TableColumn<Endpoint, EndpointStatus> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        TableColumn<Endpoint, Number> latencyCol = new TableColumn<>("Last Latency (ms)");
        latencyCol.setCellValueFactory(new PropertyValueFactory<>("lastLatencyMs"));

        TableColumn<Endpoint, String> checkedCol = new TableColumn<>("Last Checked");
        checkedCol.setCellValueFactory(new PropertyValueFactory<>("lastChecked"));

        table.getColumns().setAll(List.of(nameCol, hostCol, portCol, typeCol, statusCol, latencyCol, checkedCol));
        table.setItems(endpoints);
        table.setPlaceholder(new Label("No endpoints yet — click \"Add Endpoint\" to start monitoring."));
    }

    private VBox buildLog() {
        logArea.setEditable(false);
        logArea.setPrefRowCount(6);
        Label title = new Label("Activity Log");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        title.setTextFill(Color.web("#334155"));
        VBox box = new VBox(4, title, logArea);
        box.setPadding(new Insets(10, 0, 0, 0));
        return box;
    }

    private void styleButton(Button button, Color color) {
        button.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        button.setTextFill(Color.WHITE);
        button.setBackground(new Background(new BackgroundFill(color, new CornerRadii(6), Insets.EMPTY)));
        button.setPadding(new Insets(8, 14, 8, 14));
    }

    private void styleSummaryLabel() {
        summaryLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        summaryLabel.setTextFill(Color.web("#334155"));
        summaryLabel.setPadding(new Insets(9, 12, 9, 12));
        summaryLabel.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(6), Insets.EMPTY)));
        summaryLabel.setBorder(new Border(new BorderStroke(Color.web("#dbe3ef"), BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1))));
    }

    private void styleChart() {
        latencyChart.setPadding(new Insets(8));
        latencyChart.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(7), Insets.EMPTY)));
        latencyChart.setBorder(new Border(new BorderStroke(Color.web("#dbe3ef"), BorderStrokeStyle.SOLID, new CornerRadii(7), new BorderWidths(1))));
    }

    // ---------------------------------------------------------------- data + events

    private void loadExistingEndpoints() {
        List<Endpoint> saved = db.loadEndpoints();
        endpoints.addAll(saved);
        endpoints.forEach(probeService::startMonitoring);
        updateSummary();
    }

    private void onAddEndpoint() {
        Optional<Endpoint> result = AddEndpointDialog.show(stage);
        result.ifPresent(ep -> {
            db.insertEndpoint(ep);
            endpoints.add(ep);
            probeService.startMonitoring(ep);
            log("Added endpoint: " + ep);
            updateSummary();
        });
    }

    private void onRemoveSelected() {
        Endpoint selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        probeService.stopMonitoring(selected.getId());
        db.deleteEndpoint(selected.getId());
        endpoints.remove(selected);
        log("Removed endpoint: " + selected);
        updateSummary();
    }

    private void onStopMonitoringSelected() {
        Endpoint selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Select an endpoint first, then click Stop Monitoring.").showAndWait();
            return;
        }
        probeService.stopMonitoring(selected.getId());
        log("Monitoring stopped for " + selected.getName() + ".");
    }

    private void onCheckNowSelected() {
        Endpoint selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        log("Manual check requested for " + selected.getName() + "…");
        probeService.probeOnceAsync(selected); // result still arrives via the normal callback
    }

    private void onImportJson() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import endpoints.json");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
        var file = chooser.showOpenDialog(stage);
        if (file == null) return;
        try {
            List<Endpoint> imported = jsonManager.importConfig(Path.of(file.getAbsolutePath()));
            for (Endpoint ep : imported) {
                db.insertEndpoint(ep);
                endpoints.add(ep);
                probeService.startMonitoring(ep);
            }
            log("Imported " + imported.size() + " endpoint(s) from " + file.getName());
            updateSummary();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Import failed: " + ex.getMessage()).showAndWait();
        }
    }

    private void onExportJson() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export endpoints.json");
        chooser.setInitialFileName("endpoints.json");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
        var file = chooser.showSaveDialog(stage);
        if (file == null) return;
        try {
            jsonManager.exportConfig(endpoints, Path.of(file.getAbsolutePath()));
            log("Exported " + endpoints.size() + " endpoint(s) to " + file.getName());
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Export failed: " + ex.getMessage()).showAndWait();
        }
    }

    private void onProbeJsonApi() {
        TextInputDialog dialog = new TextInputDialog("https://worldtimeapi.org/api/ip");
        dialog.setTitle("Probe JSON API");
        dialog.setHeaderText("Enter a JSON API URL to test its response and latency.");
        dialog.setContentText("URL:");
        Optional<String> url = dialog.showAndWait();
        url.ifPresent(u -> {
            log("Probing JSON API: " + u + " …");
            new Thread(() -> {
                var summary = jsonManager.probeJsonApi(u);
                javafx.application.Platform.runLater(() -> log(
                        "API result — success=" + summary.success() +
                        ", status=" + summary.statusCode() +
                        ", latency=" + summary.latencyMs() + "ms" +
                        ", body: " + summary.bodySnippet()));
            }, "netpulse-api-probe").start();
        });
    }

    // ---------------------------------------------------------------- probe callback (Week 4 -> Week 6)

    /** Invoked on the JavaFX Application Thread whenever a background probe finishes. */
    private void onProbeResult(ProbeResult result) {
        db.logResult(result);

        for (Endpoint ep : endpoints) {
            if (ep.getId() == result.getEndpointId()) {
                ep.setStatus(result.getStatus());
                ep.setLastLatencyMs(result.getLatencyMs());
                ep.setLastChecked(result.getTimestamp());
                break;
            }
        }
        log(result.getTimestamp() + " — " + result.getEndpointName() + ": " +
                result.getStatus() + " (" + result.getLatencyMs() + " ms)");
        updateSummary();

        Endpoint selected = table.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getId() == result.getEndpointId()) {
            refreshChart(selected);
        }
    }

    private void refreshChart(Endpoint ep) {
        latencyChart.getData().clear();
        if (ep == null) return;
        List<Long> history = db.recentLatencies(ep.getId(), 30);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(ep.getName());
        for (int i = 0; i < history.size(); i++) {
            series.getData().add(new XYChart.Data<>(String.valueOf(i + 1), history.get(i)));
        }
        latencyChart.getData().add(series);
    }

    private void updateSummary() {
        long online = endpoints.stream().filter(e -> e.getStatus() == EndpointStatus.ONLINE).count();
        long degraded = endpoints.stream().filter(e -> e.getStatus() == EndpointStatus.DEGRADED).count();
        long offline = endpoints.stream().filter(e -> e.getStatus() == EndpointStatus.OFFLINE).count();
        summaryLabel.setText(String.format("Monitoring %d endpoint(s) — Online: %d | Degraded: %d | Offline: %d",
                endpoints.size(), online, degraded, offline));
    }

    private void log(String line) {
        logArea.appendText(line + System.lineSeparator());
    }

    public void shutdown() {
        probeService.shutdown();
        db.close();
    }
}
