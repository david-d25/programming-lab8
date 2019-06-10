package ru.david.room.client.main;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import ru.david.room.CreatureModel;
import ru.david.room.Message;
import ru.david.room.client.Client;
import ru.david.room.client.login.LoginDialog;
import ru.david.room.client.settings.SettingsDialog;
import ru.david.room.client.ui.UsersList;

import java.io.*;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Основное окно приложения
 */
@SuppressWarnings("unused")
public class MainWindow extends Application {
    private Stage stage;

    private int userid = -1;
    private int token = -1;
    private String userName = "<no-name>";
    private Color userColor = new Color(0, 0, 0, 1);
    private Set<CreatureModel> creatureModels = new HashSet<>();

    private ObjectInputStream in;
    private ObjectOutputStream out;

    private Thread receivingThread;

    @FXML private Circle userColorCircle;
    @FXML private Label userNameLabel;
    @FXML private Label creaturesCountLabel;
    @FXML private UsersList usersList;
    @FXML private CreaturesTable creaturesTable;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.hide();

        if (Client.getSocket() == null)
            promptLogin();
    }

    private void promptLogin() {
        new LoginDialog((userid, token, name, color, socket, in, out) -> {
            Client.setSocket(socket);
            this.userid = userid;
            this.token = token;
            this.userColor = color;
            this.userName = name;
            this.in = in;
            this.out = out;
            loadView();
            stage.show();

            receivingThread = new Thread(() -> {
                try {
                    sendMessage("subscribe");
                    sendMessage("request_users");
                    sendMessage("request_creatures");
                    while (true) {
                        Message incoming = (Message) in.readObject();
                        if (incoming.getText().equals("disconnected")) {
                            Platform.runLater(() -> {
                                stage.close();
                                promptLogin();
                            });
                            break;
                        }
                        onMessageReceived(incoming);
                    }
                } catch (Exception e) {
                    onMessageReceivingException(e);
                }
            });
            receivingThread.setDaemon(true);
            receivingThread.start();
        });
    }

    private void loadView() {
        ResourceBundle bundle = Client.currentResourceBundle();

        ObservableList<CreatureModel> creatureModels = null;
        ObservableList<Node> usersListChildren = null;

        if (creaturesTable != null && usersList != null) {
            creatureModels = creaturesTable.getItems();
            usersListChildren = usersList.getChildren();
        }

        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setResources(bundle);
            loader.setController(this);
            Pane root = loader.load(getClass().getResourceAsStream("/layout/main.fxml"));
            stage.setTitle(bundle.getString("main.window-title"));
            stage.setScene(new Scene(root));

            userColorCircle.setFill(userColor);
            userNameLabel.setText(userName);

            stage.setMinWidth(400);
            stage.setMinHeight(400);

            stage.getScene().getStylesheets().add("/style/main.css");

            stage.setOnCloseRequest((e) -> {
                sendMessage("logout");
                sendMessage("disconnect");
            });

            if (creatureModels != null && usersListChildren != null) {
                creaturesTable.setItems(creatureModels);
                usersList.getChildren().setAll(usersListChildren);
            }
        } catch (Exception e) {
//            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(bundle.getString("login-dialog.error-alert-title"));
            alert.setHeaderText(bundle.getString("login-dialog.error-alert-header"));
            alert.setContentText(e.toString());
            alert.setOnCloseRequest((v) -> stage.close());
            alert.show();
        }
    }

    private void sendMessage(String message) {
        sendMessage(message, null);
    }

    private void sendMessage(String message, Serializable attachment) {
        try {
            Message request = new Message(message, attachment);
            request.setUserid(userid);
            request.setToken(token);
            out.writeObject(request);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onMessageReceived(Message message) {
        System.out.println(message.getText());
        switch (message.getText()) {
            case "users_list_updated":
                @SuppressWarnings("unchecked")
                Set<Properties> users = (Set<Properties>)message.getAttachment();

                Platform.runLater(() -> {
                    boolean thisUserIsLoggedOut = true;
                    usersList.clear();
                    for (Properties user : users) {
                        if (Integer.parseInt(user.getProperty("id")) == userid)
                            thisUserIsLoggedOut = false;

                        usersList.addUser(
                                Integer.parseInt(user.getProperty("id")),
                                user.getProperty("name"),
                                Color.valueOf(user.getProperty("color"))
                        );
                    }

                    if (thisUserIsLoggedOut) {
                        sendMessage("disconnect");
                        stage.close();
                        promptLogin();
                        new Alert(
                                Alert.AlertType.INFORMATION,
                                Client.currentResourceBundle().getString("main.user-timed-out"),
                                ButtonType.OK
                        ).show();
                    }
                });
                break;

            case "creature_added": {
                CreatureModel model = (CreatureModel) message.getAttachment();
                creaturesTable.getItems().add(model);
                break;
            }

            case "creature_removed": {
                CreatureModel model = (CreatureModel) message.getAttachment();
                creaturesTable.getItems().remove(model);
                break;
            }

            case "creature_updated":
                CreatureModel model = (CreatureModel) message.getAttachment();
                for (CreatureModel current : creaturesTable.getItems()) {
                    if (current.getId() == model.getId()) {
                        creaturesTable.getItems().remove(current);
                        break;
                    }
                }
                creaturesTable.getItems().add(model);
                break;

            case "creatures_list_updated": {
                @SuppressWarnings("unchecked")
                Set<CreatureModel> creatureModels = (Set<CreatureModel>) message.getAttachment();
                creaturesTable.getItems().setAll(creatureModels);
                break;
            }
        }
    }

    @FXML
    public void onLogoutClicked() {
        if (receivingThread.isAlive()) {
            Properties properties = new Properties();
            properties.setProperty("send_response", "true");
            sendMessage("disconnect", properties);
        } else {
            Platform.runLater(() -> {
                stage.close();
                promptLogin();
            });
        }
    }

    @FXML
    public void onSettingsClicked() throws InterruptedException {
        new SettingsDialog((changed) -> {
            if (changed)
                loadView();
            stage.setOpacity(1);
        });
        for (float i = 1; i >= 0.5; i -= 0.01) {
            Thread.sleep(10);
            stage.setOpacity(i);
        }
    }

    private void onMessageReceivingException(Exception e) {
        Platform.runLater(() -> {
            if (e instanceof EOFException)
                return;
            if (e instanceof SocketException && e.getMessage().equals("Socket closed"))
                return;
            if (e instanceof SocketException && e.getMessage().equals("Connection reset")) {
                stage.close();
                promptLogin();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText(Client.currentResourceBundle().getString("main.connection-lost"));
                alert.show();
            } else {
                stage.close();
                promptLogin();
//                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText(Client.currentResourceBundle().getString("main.connection-error"));
                alert.setContentText(e.toString());
                alert.show();
            }
        });
    }
}
