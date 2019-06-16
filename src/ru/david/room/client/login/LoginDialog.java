package ru.david.room.client.login;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import ru.david.room.Message;
import ru.david.room.client.Client;
import ru.david.room.client.WelcomePhrases;
import ru.david.room.client.registration.RegisterDialog;
import ru.david.room.client.registration.RegisterDialogListener;
import ru.david.room.client.settings.SettingsDialog;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import static ru.david.room.client.Utils.twitch;

@SuppressWarnings("unused")
public class LoginDialog {
    private LoginDialogListener listener;
    private Stage stage;

    private TextField emailField;
    private PasswordField passwordField;
    private Button loginButton;

    public LoginDialog(LoginDialogListener l) {
        Client.setSocket(null);
        listener = l;

        stage = new Stage();
        loadView();
        stage.setResizable(false);
        stage.show();
    }

    private void loadView() {
        ResourceBundle bundle = Client.currentResourceBundle();

        String emailTextBackup = "";
        String passwordTextBackup = "";

        if (emailField != null)
            emailTextBackup = emailField.getText();
        if (passwordField != null)
            passwordTextBackup = passwordField.getText();

        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setResources(bundle);
            loader.setController(this);
            Pane root = loader.load(getClass().getResourceAsStream("/layout/login.fxml"));
            stage.setTitle(bundle.getString("login-dialog.window-title"));
            stage.setScene(new Scene(root));

            stage.getScene().getStylesheets().add("/style/login.css");

            emailField = (TextField)stage.getScene().getRoot().lookup("#email-input");
            passwordField = (PasswordField)stage.getScene().getRoot().lookup("#password-input");
            loginButton = (Button)stage.getScene().getRoot().lookup("#login-button");

            ImageView imageView = (ImageView)root.lookup("#image");
            if (imageView != null) {
                FileInputStream imageStream = new FileInputStream("resources/img/watermelon.png");
                imageView.setImage(new Image(imageStream));
            }

            emailField.setText(emailTextBackup);
            passwordField.setText(passwordTextBackup);

            emailField.requestFocus();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(bundle.getString("login-dialog.error-alert-title"));
            alert.setHeaderText(bundle.getString("login-dialog.error-alert-header"));
            alert.setContentText(e.toString());
            alert.show();
        }
    }

    @FXML
    public void onSettingsClicked() {
        new SettingsDialog((changed) -> {
            if (changed)
                loadView();
            stage.show();
        });
        stage.hide();
    }

    @FXML
    public void onLoginClick() {
        loginButton.setDisable(true);
        emailField.setDisable(true);
        passwordField.setDisable(true);

        try {
            String email = emailField.getText();
            String password = passwordField.getText();

            if (email.isEmpty()) {
                emailField.getStyleClass().add("login-textfield__error");
                twitch(stage);
                return;
            }

            if (password.isEmpty()) {
                passwordField.getStyleClass().add("login-textfield__error");
                twitch(stage);
                return;
            }

            Properties loginInfo = new Properties();
            loginInfo.setProperty("email", email);
            loginInfo.setProperty("password", password);

            Socket socket = new Socket(Client.getConfig().getServerHost(), Client.getConfig().getServerPort());
            Message request = new Message("login", loginInfo);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            oos.writeObject(request);
            Message response = (Message)(ois.readObject());

            Properties properties = (Properties)response.getAttachment();
            String status = response.getText();

            switch (status) {
                case "OK":
                    listener.onLogin(
                            Integer.parseInt(properties.getProperty("userid")),
                            Integer.parseInt(properties.getProperty("user_token")),
                            properties.getProperty("user_name"),
                            Color.valueOf(properties.getProperty("user_color")),
                            socket,
                            ois, oos
                    );
                    stage.close();
                    break;

                case "WRONG":
                    twitch(stage);
                    passwordField.clear();
                    showErrorMessage(Client.currentResourceBundle().getString("login-dialog.wrong-email-or-password"));
                    break;

                case "INTERNAL_ERROR":
                    passwordField.clear();
                    showErrorMessage(Client.currentResourceBundle().getString("login-dialog.internal-server-error"));
                    break;
            }

        } catch (UnknownHostException e) {
            showErrorMessage(Client.currentResourceBundle().getString("login-dialog.unknown-host"));
        } catch (IOException e) {
            if (e.getMessage().equals("Connection refused: connect"))
                showErrorMessage(Client.currentResourceBundle().getString("login-dialog.no-connection"));
            else
                showErrorMessage(Client.currentResourceBundle().getString("login-dialog.cant-connect-to-server") + '\n' + e.toString());
        } catch (ClassNotFoundException e) {
            showErrorMessage(Client.currentResourceBundle().getString("login-dialog.class-not-found") + '\n' + e.toString());
        } finally {
            loginButton.setDisable(false);
            emailField.setDisable(false);
            passwordField.setDisable(false);
        }
    }

    @FXML
    public void onRegisterClick() {
        stage.hide();
        new RegisterDialog(() -> stage.show());
    }

    @FXML
    public void onTextFieldKeyTyped(KeyEvent keyEvent) {
        if (!((TextField)keyEvent.getTarget()).getText().isEmpty())
            ((TextField)keyEvent.getTarget()).getStyleClass().remove("login-textfield__error");
    }

    @FXML
    public void onWatermelonClick() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(WelcomePhrases.getRandom());
        alert.show();
    }

    private void showErrorMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}
