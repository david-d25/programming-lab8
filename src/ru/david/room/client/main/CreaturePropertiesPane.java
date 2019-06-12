package ru.david.room.client.main;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import ru.david.room.CreatureModel;
import ru.david.room.client.Client;

import java.util.ResourceBundle;

public class CreaturePropertiesPane extends VBox {
    private CreatureModel selected;

    private Label idLabel, ownerIdLabel, createdLabel;

    private TextField nameInput;
    private Slider xInput, yInput, radiusInput;
    private Button applyButton, resetButton, deleteButton;

    public CreaturePropertiesPane() {
        ResourceBundle bundle = Client.currentResourceBundle();
        setPadding(new Insets(20));
        setFillWidth(true);

        idLabel = new Label();
        ownerIdLabel = new Label();
        createdLabel = new Label();

        nameInput = new TextField();
        xInput = new Slider(0, 1000, 0);
        yInput = new Slider(0, 1000, 0);
        radiusInput = new Slider(15, 300, 15);

        applyButton = new Button(bundle.getString("main.apply"));
        resetButton = new Button(bundle.getString("main.reset"));
        deleteButton = new Button(bundle.getString("main.delete"));

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

        HBox buttonsPane = new HBox(applyButton, resetButton, deleteButton);
        buttonsPane.setAlignment(Pos.CENTER);

        HBox.setMargin(nameInput, new Insets(5));
        HBox.setMargin(applyButton, new Insets(5));
        HBox.setMargin(resetButton, new Insets(5));
        HBox.setMargin(deleteButton, new Insets(5));
        HBox.setMargin(xInput, new Insets(5));
        HBox.setMargin(yInput, new Insets(5));
        HBox.setMargin(radiusInput, new Insets(5));

        getChildren().addAll(
                nameInputPane,
                new HBox(new Label("X"), xInput),
                new HBox(new Label("Y"), yInput),
                new HBox(new Label(bundle.getString("main.radius")), radiusInput),
                buttonsPane
        );

        nameInput.textProperty().addListener((observable, oldValue, newValue) -> onEdited());
        xInput.valueProperty().addListener((observable, oldValue, newValue) -> onEdited());
        yInput.valueProperty().addListener((observable, oldValue, newValue) -> onEdited());
        radiusInput.valueProperty().addListener((observable, oldValue, newValue) -> onEdited());

        applyButton.setOnAction((e) -> onApply());
        resetButton.setOnAction((e) -> onReset());
        deleteButton.setOnAction((e) -> onDelete());
    }

    void selectCreature(CreatureModel model, boolean editable) {
        selected = model;

        applyButton.setDisable(true);
        resetButton.setDisable(true);
        deleteButton.setDisable(!editable);
        nameInput.setDisable(!editable);
        xInput.setDisable(!editable);
        yInput.setDisable(!editable);
        radiusInput.setDisable(!editable);

        if (model == null)
            setDisable(true);
        else {
            resetProperties();
            setDisable(false);
        }
    }

    private void onEdited() {
        applyButton.setDisable(false);
        resetButton.setDisable(false);
    }

    private void onApply() {
        // TODO
    }

    private void onReset() {
        resetProperties();
    }

    private void onDelete() {
        // TODO
    }

    private void resetProperties() {
        if (selected != null) {
            idLabel.setText("ID " + selected.getId());
            ownerIdLabel.setText(Client.currentResourceBundle().getString("main.owner-id") + " " + selected.getOwnerid());
            createdLabel.setText(Client.currentResourceBundle().getString("main.created") + " " + selected.getCreated().toString());

            nameInput.setText(selected.getName());
            xInput.setValue(selected.getX());
            yInput.setValue(selected.getY());
            radiusInput.setValue(selected.getRadius());

            applyButton.setDisable(true);
            resetButton.setDisable(true);
        }
    }
}
