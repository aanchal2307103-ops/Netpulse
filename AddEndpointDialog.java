package com.netpulse.ui;

import com.netpulse.model.Endpoint;
import com.netpulse.model.EndpointType;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Optional;

public class AddEndpointDialog {

    /** Shows the dialog and blocks until the user closes it; returns the new Endpoint, or empty if cancelled. */
    public static Optional<Endpoint> show(Stage owner) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Add Endpoint");

        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Main Web Server");

        TextField hostField = new TextField();
        hostField.setPromptText("e.g. example.com or 192.168.1.1");

        Spinner<Integer> portSpinner = new Spinner<>(1, 65535, 443);
        portSpinner.setEditable(true);

        ComboBox<EndpointType> typeBox = new ComboBox<>();
        typeBox.getItems().addAll(EndpointType.values());
        typeBox.setValue(EndpointType.WEBSITE);

        Spinner<Integer> intervalSpinner = new Spinner<>(2, 3600, 10);
        intervalSpinner.setEditable(true);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));
        Label nameLabel = new Label("Name:");
        Label hostLabel = new Label("Host / IP:");
        Label portLabel = new Label("Port:");
        Label typeLabel = new Label("Type:");
        Label intervalLabel = new Label("Check every (sec):");

        for (Label label : new Label[]{nameLabel, hostLabel, portLabel, typeLabel, intervalLabel}) {
            label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            label.setTextFill(Color.web("#334155"));
        }

        grid.addRow(0, nameLabel, nameField);
        grid.addRow(1, hostLabel, hostField);
        grid.addRow(2, portLabel, portSpinner);
        grid.addRow(3, typeLabel, typeBox);
        grid.addRow(4, intervalLabel, intervalSpinner);

        Button saveButton = new Button("Add");
        Button cancelButton = new Button("Cancel");
        styleButton(saveButton, Color.web("#0f766e"));
        styleButton(cancelButton, Color.web("#64748b"));

        HBoxLike buttons = new HBoxLike(saveButton, cancelButton);
        buttons.pane.setPadding(new Insets(10, 0, 0, 0));
        grid.add(buttons.pane, 1, 5);

        final Endpoint[] created = { null };
        saveButton.setOnAction(e -> {
            if (nameField.getText().isBlank() || hostField.getText().isBlank()) {
                new Alert(Alert.AlertType.WARNING, "Name and Host are required.").showAndWait();
                return;
            }
            Endpoint ep = new Endpoint(
                    nameField.getText().trim(),
                    hostField.getText().trim(),
                    portSpinner.getValue(),
                    typeBox.getValue());
            ep.setIntervalSeconds(intervalSpinner.getValue());
            created[0] = ep;
            stage.close();
        });
        cancelButton.setOnAction(e -> stage.close());

        Scene scene = new Scene(grid);
        scene.setFill(Color.web("#f4f7fb"));
        stage.setScene(scene);
        stage.showAndWait();

        return Optional.ofNullable(created[0]);
    }

    private static void styleButton(Button button, Color color) {
        button.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        button.setTextFill(Color.WHITE);
        button.setBackground(new javafx.scene.layout.Background(
                new javafx.scene.layout.BackgroundFill(color, new javafx.scene.layout.CornerRadii(6), Insets.EMPTY)));
        button.setPadding(new Insets(8, 14, 8, 14));
    }

    /** Tiny helper so this file doesn't need an extra import block for HBox. */
    private static class HBoxLike {
        final javafx.scene.layout.HBox pane;
        HBoxLike(Button... buttons) {
            pane = new javafx.scene.layout.HBox(10, buttons);
        }
    }
}
