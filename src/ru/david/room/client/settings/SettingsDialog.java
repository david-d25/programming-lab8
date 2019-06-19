package ru.david.room.client.settings;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import ru.david.room.client.Client;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Сэттингс дайэлог. Извините за мой английский.
 */
public class SettingsDialog {
    private SettingsDialogListener listener;

    private Stage stage;

    private ChoiceBox<Locale> localeChoiceBox = new ChoiceBox<>();
    private TextField serverAddress;
    private TextField serverPort;

    public SettingsDialog(SettingsDialogListener l) {
        stage = new Stage();
        stage.setResizable(false);

        VBox vBox = new VBox();
        vBox.setAlignment(Pos.CENTER);
        stage.setScene(new Scene(vBox));

        Label titleLabel = new Label();
        titleLabel.setText(Client.currentResourceBundle().getString("main.settings"));
        titleLabel.setFont(new Font(35));

        localeChoiceBox.setConverter(new StringConverter<Locale>() {
            @Override
            public String toString(Locale object) {
                return object.getDisplayLanguage() + " (" + object.getDisplayCountry() + ")";
            }

            @Override
            public Locale fromString(String string) {
                return null;
            }
        });
        Map<Locale, ResourceBundle> bundles = Client.getResourceBundles();
        localeChoiceBox.setValue(Client.getCurrentLocale());
        for (Locale currentLocale : bundles.keySet())
            localeChoiceBox.getItems().add(currentLocale);


        Label languageLabel = new Label();
        languageLabel.setText(Client.currentResourceBundle().getString("main.language"));
        languageLabel.setLabelFor(localeChoiceBox);

        serverAddress = new TextField();
        serverAddress.setText(Client.getConfig().getServerHost());
        Label serverAddressLabel = new Label();
        serverAddressLabel.setText(Client.currentResourceBundle().getString("main.server-address"));
        serverAddressLabel.setLabelFor(serverAddress);

        serverPort = new TextField();
        serverPort.setText(Integer.toString(Client.getConfig().getServerPort()));
        Label serverPortLabel = new Label();
        serverPortLabel.setText(Client.currentResourceBundle().getString("main.server-port"));
        serverPortLabel.setLabelFor(serverPort);

        Button saveButton = new Button();
        saveButton.setDefaultButton(true);
        saveButton.setDisable(true);
        HBox.setMargin(saveButton, new Insets(0, 15, 0, 0));
        saveButton.setOnAction((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((event -> onSave()))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))));
        saveButton.setText(Client.currentResourceBundle().getString("main.save"));

        Button cancelButton = new Button();
        cancelButton.setCancelButton(true);
        cancelButton.setOnAction(event -> onCancel());
        cancelButton.setText(Client.currentResourceBundle().getString("main.cancel"));

        HBox buttonsPane = new HBox();
        buttonsPane.setAlignment(Pos.CENTER);
        VBox.setMargin(buttonsPane, new Insets(10));
        buttonsPane.getChildren().addAll(saveButton, cancelButton);

        VBox.setMargin(localeChoiceBox, new Insets(0, 0, 5, 0));
        VBox.setMargin(serverAddress, new Insets(0, 0, 5, 0));
        VBox.setMargin(serverPort, new Insets(0, 0, 5, 0));
        VBox.setMargin(serverPort, new Insets(0, 0, 5, 0));

        if (Client.getSocket() != null) {
            serverAddress.setDisable(true);
            serverPort.setDisable(true);

            serverAddressLabel.setDisable(true);
            serverPortLabel.setDisable(true);

            Label warningLabel = new Label(Client.currentResourceBundle().getString("main.logout-to-change-address"));
            warningLabel.setTextFill(Color.GRAY);
            warningLabel.setWrapText(true);
            warningLabel.setMaxHeight(Double.POSITIVE_INFINITY);

            vBox.getChildren().addAll(
                    titleLabel,
                    languageLabel,
                    localeChoiceBox,
                    serverAddressLabel,
                    serverAddress,
                    serverPortLabel,
                    serverPort,
                    warningLabel,
                    buttonsPane
            );
        } else {
            vBox.getChildren().addAll(
                    titleLabel,
                    languageLabel,
                    localeChoiceBox,
                    serverAddressLabel,
                    serverAddress,
                    serverPortLabel,
                    serverPort,
                    buttonsPane
            );
        }

        vBox.setPadding(new Insets(20, 20, 0, 20));

        localeChoiceBox.setOnAction(event -> saveButton.setDisable(false));
        serverAddress.textProperty().addListener(event -> saveButton.setDisable(false));
        serverPort.textProperty().addListener(event -> saveButton.setDisable(false));

        stage.setOnCloseRequest((e) -> listener.closed(false));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setWidth(250);
        listener = l;
        stage.show();
    }

    private void onSave() {
        Client.setCurrentLocale(localeChoiceBox.getValue());
        Client.getConfig().setServerHost(serverAddress.getText());
        try {
            Client.getConfig().setServerPort(Integer.parseInt(serverPort.getText()));
        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(Client.currentResourceBundle().getString("main.wrong-port"));
            alert.show();
        }
        stage.close();
        listener.closed(true);
    }

    private void onCancel() {
        stage.close();
        listener.closed(false);
    }
}
