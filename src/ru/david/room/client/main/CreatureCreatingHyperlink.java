package ru.david.room.client.main;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import ru.david.room.client.Client;

import java.util.ResourceBundle;

/**
 * Ультра-прокачанная утяжелённая экспериментальная сверхпрочная сверхкрутая
 * сверхдизайнерская сверх-гипер-ссылка. При клике создаёт поп-ап для создания существа.
 */
public class CreatureCreatingHyperlink extends Hyperlink {
    private CreatureCreatingListener listener = (name, x, y, radius) -> {};
    private Stage popup;
    private Stage parent;

    private VBox wrapper;
    private VBox container;

    private TextField nameInput;
    private Slider xInput, yInput, radiusInput;

    private Button createButton, closeButton;

    public CreatureCreatingHyperlink() {
        setText(Client.currentResourceBundle().getString("main.create") + "...");
        setOnMouseClicked((e) -> openPopup(e.getScreenX() - 10, e.getScreenY() - 10));

        container = new VBox();
        container.setPadding(new Insets(20));
        container.setFillWidth(true);

        wrapper = new VBox();
        wrapper.setFillWidth(true);
        wrapper.getChildren().add(container);

        popup = new Stage(StageStyle.UNDECORATED);
        popup.initModality(Modality.WINDOW_MODAL);
        popup.setScene(new Scene(wrapper));
        popup.setResizable(false);
        popup.setWidth(500);

        popup.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused)
                closePopup();
        });

        popup.getScene().setOnKeyPressed((e) -> {
            if (e.getCode() == KeyCode.ESCAPE)
                closePopup();
        });

        ResourceBundle bundle = Client.currentResourceBundle();
        setPadding(new Insets(20));

        nameInput = new TextField();
        xInput = new Slider(0, 1000, 0);
        yInput = new Slider(0, 1000, 0);
        radiusInput = new Slider(15, 300, 15);

        createButton = new Button(bundle.getString("main.create"));
        closeButton = new Button(bundle.getString("main.close"));

        xInput.setBlockIncrement(1);
        xInput.setShowTickLabels(true);
        xInput.setShowTickMarks(true);

        yInput.setBlockIncrement(1);
        yInput.setShowTickLabels(true);
        yInput.setShowTickMarks(true);

        radiusInput.setBlockIncrement(1);
        radiusInput.setShowTickLabels(true);
        radiusInput.setShowTickMarks(true);

        HBox nameInputPane = new HBox(new Label(bundle.getString("main.name")), nameInput);
        nameInputPane.setAlignment(Pos.CENTER_LEFT);
        HBox.setMargin(nameInput, new Insets(0, 0, 0, 20));
        HBox.setHgrow(nameInput, Priority.ALWAYS);

        HBox.setHgrow(xInput, Priority.ALWAYS);
        HBox.setHgrow(yInput, Priority.ALWAYS);
        HBox.setHgrow(radiusInput, Priority.ALWAYS);

        HBox buttonsPane = new HBox(createButton, closeButton);
        buttonsPane.setAlignment(Pos.CENTER);

        HBox.setMargin(nameInput, new Insets(5));
        HBox.setMargin(createButton, new Insets(5));
        HBox.setMargin(closeButton, new Insets(5));
        HBox.setMargin(xInput, new Insets(5));
        HBox.setMargin(yInput, new Insets(5));
        HBox.setMargin(radiusInput, new Insets(5));

        container.getChildren().addAll(
                nameInputPane,
                new HBox(new Label("X"), xInput),
                new HBox(new Label("Y"), yInput),
                new HBox(new Label(bundle.getString("main.radius")), radiusInput),
                buttonsPane
        );

        wrapper.setPadding(new Insets(20));
        wrapper.setEffect(new DropShadow(20, new Color(0, 0, 0, .5)));
        wrapper.setBackground(Background.EMPTY);

        popup.initStyle(StageStyle.TRANSPARENT);
        popup.getScene().setFill(Color.TRANSPARENT);

        container.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));

        closeButton.setOnAction((e) -> closePopup());
        createButton.setOnAction((e) -> onCreate());
    }

    void setParent(Stage p) {
        parent = p;
    }

    void setCreatureCreatingListener(CreatureCreatingListener l) {
        listener = l;
    }

    private void onCreate() {
        if (nameInput.getText().length() == 0 || nameInput.getText().length() > 32)
            return; // TODO check name

        listener.onCreateRequested(
                nameInput.getText(),
                (int)xInput.getValue(),
                (int)yInput.getValue(),
                (float)radiusInput.getValue()
        );

        closePopup();
    }

    private void openPopup(double x, double y) {
        popup.setX(x);
        popup.setY(y);
        popup.show();
    }

    private void closePopup() {
        popup.hide();
        Platform.runLater(() -> {
            if (parent != null)
                parent.requestFocus();
        });
    }
}
