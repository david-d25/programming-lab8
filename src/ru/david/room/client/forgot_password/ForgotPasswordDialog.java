package ru.david.room.client.forgot_password;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import ru.david.room.Message;
import ru.david.room.client.Client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.ResourceBundle;

import static ru.david.room.client.Utils.twitch;

@SuppressWarnings("unused")
public class ForgotPasswordDialog {
    private Stage stage;
    private ForgotPasswordDialogListener listener;

    @FXML  private TextField emailInput;
    @FXML private Button proceedButton;
    @FXML private Button haveCodeButton;
    @FXML private Label confirmationLabel;
    @FXML private TextField confirmationInput;
    @FXML private Button confirmButton;
    @FXML private Button cancelConfirmationButton;
    @FXML private Tab emailTab;
    @FXML private Tab confirmationTab;
    @FXML private TabPane tabPane;
    @FXML private TextField passwordInput;
    @FXML private TextField repeatPasswordInput;

    public ForgotPasswordDialog(ForgotPasswordDialogListener l) {
        listener = l;

        stage = new Stage();
        loadView();
        stage.setResizable(false);
        stage.show();

        confirmationTab.setDisable(true);

        stage.setOnCloseRequest(event -> listener.dialogClosed());
    }

    private void loadView() {
        ResourceBundle bundle = Client.currentResourceBundle();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/layout/forgot-password.fxml"), bundle);
            loader.setController(this);
            Pane root = loader.load();
            stage.setTitle(bundle.getString("forgot-password-dialog.window-title"));
            stage.setScene(new Scene(root));

            stage.getScene().getStylesheets().add("/style/register.css");
        } catch (Exception e) {
//            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(bundle.getString("login-dialog.error-alert-title"));
            alert.setHeaderText(bundle.getString("login-dialog.error-alert-header"));
            alert.setContentText(e.toString());
            alert.show();
        }
    }

    @FXML
    public void onProceedClick() {
        emailInput.setDisable(true);
        proceedButton.setDisable(true);

        cancelConfirmationButton.setDisable(true);
        cancelConfirmationButton.setManaged(false);
        cancelConfirmationButton.setVisible(false);

        ResourceBundle bundle = Client.currentResourceBundle();

        try {
            if (emailInput.getText().isEmpty()) {
                emailInput.getStyleClass().add("register-textfield__error");
                twitch(stage);
                return;
            }

            Socket socket = new Socket(Client.getConfig().getServerHost(), Client.getConfig().getServerPort());
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

            Properties request = new Properties();

            request.setProperty("email", emailInput.getText());
            request.setProperty("locale-language", Client.getCurrentLocale().getLanguage());
            request.setProperty("locale-country", Client.getCurrentLocale().getCountry());

            oos.writeObject(new Message("request_password_reset", request));

            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            Message response = (Message)ois.readObject();

            oos.writeObject(new Message("disconnect"));

            switch (response.getText()) {
                case "OK":
                    emailTab.setDisable(true);
                    confirmationTab.setDisable(false);
                    tabPane.getSelectionModel().select(confirmationTab);
                    emailInput.clear();
                    break;

                case "EMAIL_NOT_EXIST":
                    emailInput.getStyleClass().add("register-textfield__error");
                    showErrorMessage(bundle.getString("forgot-password-dialog.email-not-exist"));
                    twitch(stage);
                    break;

                case "INTERNAL_ERROR":
                    showErrorMessage(bundle.getString("register-dialog.internal-error"));
                    break;
            }

        } catch (UnknownHostException e) {
            showErrorMessage(bundle.getString("register-dialog.unknown-host"));
        } catch (IOException e) {
            if (e.getMessage().equals("Connection refused: connect"))
                showErrorMessage(bundle.getString("register-dialog.no-connection"));
            else
                showErrorMessage(bundle.getString("register-dialog.cant-connect-to-server") + '\n' + e.toString());
        } catch (ClassNotFoundException e) {
            showErrorMessage(bundle.getString("register-dialog.class-not-found") + '\n' + e.toString());
        } finally {
            emailInput.setDisable(false);
            proceedButton.setDisable(false);
        }
    }

    @FXML
    public void onConfirmClick() {
        confirmationInput.setDisable(true);
        confirmButton.setDisable(true);

        ResourceBundle bundle = Client.currentResourceBundle();
        try {
            if (confirmationInput.getText().isEmpty()) {
                confirmationInput.getStyleClass().add("register-textfield__error");
                twitch(stage);
                return;
            }

            if (!passwordInput.getText().equals(repeatPasswordInput.getText())) {
                passwordInput.getStyleClass().add("register-textfield__error");
                repeatPasswordInput.getStyleClass().add("register-textfield__error");
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText(bundle.getString("register-dialog.passwords-dont-match"));
                alert.show();
                twitch(stage);
                return;
            }

            Socket socket = new Socket(Client.getConfig().getServerHost(), Client.getConfig().getServerPort());
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

            Properties properties = new Properties();
            properties.setProperty("confirmation", confirmationInput.getText());
            properties.setProperty("password", passwordInput.getText());

            oos.writeObject(new Message("reset_password", properties));

            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            Message response = (Message)ois.readObject();

            oos.writeObject(new Message("disconnect"));

            switch (response.getText()) {
                case "OK":
                    listener.dialogClosed();
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setHeaderText(bundle.getString("register-dialog.registration-confirmed-title")); // todo
                    alert.setContentText(bundle.getString("register-dialog.registration-confirmed-text")); // todo
                    alert.show();
                    stage.close();
                    break;

                case "SHORT_PASSWORD":
                    passwordInput.getStyleClass().add("register-textfield__error");
                    showErrorMessage(bundle.getString("register-dialog.short-password"));
                    twitch(stage);
                    break;

                case "WEAK_PASSWORD":
                    passwordInput.getStyleClass().add("register-textfield__error");
                    showErrorMessage(bundle.getString("register-dialog.weak-password"));
                    twitch(stage);
                    break;

                case "WRONG":
                    showErrorMessage(bundle.getString("register-dialog.wrong-confirmation-code"));
                    confirmationInput.getStyleClass().add("register-textfield__error");
                    twitch(stage);
                    break;

                case "INTERNAL_SERVER":
                    showErrorMessage(bundle.getString("register-dialog.internal-error"));
                    break;
            }

        } catch (UnknownHostException e) {
            showErrorMessage(bundle.getString("register-dialog.unknown-host"));
        } catch (IOException e) {
            if (e.getMessage().equals("Connection refused: connect"))
                showErrorMessage(bundle.getString("register-dialog.no-connection"));
            else
                showErrorMessage(bundle.getString("register-dialog.cant-connect-to-server") + '\n' + e.toString());
        } catch (ClassNotFoundException e) {
            showErrorMessage(bundle.getString("register-dialog.class-not-found") + '\n' + e.toString());
        } finally {
            confirmationInput.setDisable(false);
            confirmButton.setDisable(false);
        }
    }

    @FXML
    public void onHaveCodeClick() {
        confirmationLabel.setText(Client.currentResourceBundle().getString("forgot-password-dialog.confirmation-text-alternative"));  // todo
        emailTab.setDisable(true);
        confirmationTab.setDisable(false);
        tabPane.getSelectionModel().select(confirmationTab);
        cancelConfirmationButton.setDisable(false);
        cancelConfirmationButton.setManaged(true);
        cancelConfirmationButton.setVisible(true);
    }

    @FXML
    public void onCancelConfirmationClick() {
        confirmationLabel.setText(Client.currentResourceBundle().getString("register-dialog.confirmation-text")); // todo
        emailTab.setDisable(false);
        confirmationTab.setDisable(true);
        tabPane.getSelectionModel().select(emailTab);
    }

    @FXML
    public void onTextFieldKeyTyped(KeyEvent keyEvent) {
        if (!((TextField)keyEvent.getTarget()).getText().isEmpty())
            ((TextField)keyEvent.getTarget()).getStyleClass().remove("register-textfield__error");
    }

    private void showErrorMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}
