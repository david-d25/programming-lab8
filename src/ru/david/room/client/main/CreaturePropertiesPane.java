package ru.david.room.client.main;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import ru.david.room.CreatureModel;
import ru.david.room.client.Client;

import java.util.ResourceBundle;

public class CreaturePropertiesPane extends VBox {
    private CreatureModel selected;

    private CreatureDeletingListener deletingListener = creatureId -> {};
    private CreatureApplyingListener applyingListener = model -> {};

    private Label idLabel, ownerIdLabel, createdLabel;

    private TextField nameInput;
    private Slider xInput, yInput, radiusInput;
    private Button applyButton, resetButton, deleteButton;
    private CheckBox autoApplyCheckbox;

    private boolean autoApplyingEnabled = true;

    private Thread debounceThread = new Thread();

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

        autoApplyCheckbox = new CheckBox(bundle.getString("main.auto-apply"));

        xInput.setBlockIncrement(1);
        xInput.setShowTickLabels(true);
        xInput.setShowTickMarks(true);

        yInput.setBlockIncrement(1);
        yInput.setShowTickLabels(true);
        yInput.setShowTickMarks(true);

        radiusInput.setBlockIncrement(1);
        radiusInput.setShowTickLabels(true);
        radiusInput.setShowTickMarks(true);

        autoApplyCheckbox.setSelected(true);

        HBox nameInputPane = new HBox(new Label(bundle.getString("main.name")), nameInput);
        nameInputPane.setAlignment(Pos.CENTER_LEFT);
        HBox.setMargin(nameInput, new Insets(0, 0, 0, 20));
        HBox.setHgrow(nameInput, Priority.ALWAYS);

        HBox.setHgrow(xInput, Priority.ALWAYS);
        HBox.setHgrow(yInput, Priority.ALWAYS);
        HBox.setHgrow(radiusInput, Priority.ALWAYS);

        HBox buttonsPane = new HBox(applyButton, resetButton, deleteButton);
        buttonsPane.setAlignment(Pos.CENTER);

        HBox autoApplyPane = new HBox(autoApplyCheckbox);

        HBox.setMargin(nameInput, new Insets(5));
        HBox.setMargin(applyButton, new Insets(5));
        HBox.setMargin(resetButton, new Insets(5));
        HBox.setMargin(deleteButton, new Insets(5));
        HBox.setMargin(xInput, new Insets(5));
        HBox.setMargin(yInput, new Insets(5));
        HBox.setMargin(radiusInput, new Insets(5));
        HBox.setMargin(autoApplyCheckbox, new Insets(5));

        getChildren().addAll(
                nameInputPane,
                new HBox(new Label("X"), xInput),
                new HBox(new Label("Y"), yInput),
                new HBox(new Label(bundle.getString("main.radius")), radiusInput),
                buttonsPane,
                autoApplyPane
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
        autoApplyingEnabled = false;
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
        autoApplyingEnabled = true;
    }

    void setApplyingListener(CreatureApplyingListener applyingListener) {
        this.applyingListener = applyingListener;
    }

    void setDeletingListener(CreatureDeletingListener deletingListener) {
        this.deletingListener = deletingListener;
    }

    private void onEdited() {
        applyButton.setDisable(false);
        resetButton.setDisable(false);

        if (autoApplyCheckbox.isSelected() && autoApplyingEnabled)
            onApply();
    }

    private void onApply() {
        if (nameInput.getText().length() == 0 || nameInput.getText().length() > 32)
            return; // TODO: Show warning
        if (!debounceThread.isAlive()) {
            debounceThread = new Thread(() -> {
                try {
                    Thread.sleep(300);
                    applyingListener.applyRequested(new CreatureModel(
                            selected.getId(),
                            (int)xInput.getValue(),
                            (int)yInput.getValue(),
                            (float)radiusInput.getValue(),
                            selected.getOwnerid(),
                            nameInput.getText()
                    ));
                } catch (InterruptedException ignored) {
                }
            });
            debounceThread.start();
        }
    }

    private void onReset() {
        resetProperties();
    }

    private void onDelete() {
        deletingListener.deleteRequested(selected.getId());
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
