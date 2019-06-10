package ru.david.room.client.settings;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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

    public SettingsDialog(SettingsDialogListener l) {
        stage = new Stage();
        stage.setResizable(false);

        VBox vBox = new VBox();
        vBox.setAlignment(Pos.CENTER);
        stage.setScene(new Scene(vBox));

        Label titleLabel = new Label();
        titleLabel.setText(Client.currentResourceBundle().getString("main.settings"));
        VBox.setMargin(titleLabel, new Insets(20));
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

        vBox.getChildren().addAll(titleLabel, languageLabel, localeChoiceBox, buttonsPane);

        localeChoiceBox.setOnAction(event -> saveButton.setDisable(false));

        stage.setOnCloseRequest((e) -> listener.closed(false));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setWidth(250);
        listener = l;
        stage.show();
    }

    private void onSave() {
        Client.setCurrentLocale(localeChoiceBox.getValue());
        stage.close();
        listener.closed(true);
    }

    private void onCancel() {
        stage.close();
        listener.closed(false);
    }
}
