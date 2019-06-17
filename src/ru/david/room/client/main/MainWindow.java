package ru.david.room.client.main;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
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
 * Основное окно приложения. Милая мордашка, не правда ли?
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

    @FXML private Label userNameLabel;
    @FXML private UsersList usersList;
    @FXML private Tab creatureCanvasTab;
    @FXML private Circle userColorCircle;
    @FXML private Label creaturesCountLabel;
    @FXML private CreaturesTable creaturesTable;
    @FXML private Label selectCreaturesLabel;
    @FXML private ScrollPane propertiesScrollPane;
    @FXML private CreaturesCanvas creaturesCanvas;
    @FXML private CreaturePropertiesPane creaturePropertiesPane;

    @FXML private CreatureCreatingHyperlink creatureCreatingHyperlink;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.hide();

        if (Client.getSocket() == null)
            promptLogin();
    }

    /**
     * Показывает {@link LoginDialog}, после успешного входа снова показывает это окно
     */
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

            initReceivingThread();
            initAliveDetectingThread();
        });
    }

    /**
     * Запускает слушатель сообщений от сервера
     */
    private void initReceivingThread() {
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
                    Platform.runLater(() -> onMessageReceived(incoming));
                }
            } catch (Exception e) {
                onMessageReceivingException(e);
            }
        });
        receivingThread.setDaemon(true);
        receivingThread.start();
    }

    /**
     * Запускает поток, проверяющий наличие активности в главном окне
     */
    private void initAliveDetectingThread() {
        Thread aliveDetectingThread = new Thread(() -> {
            try {
                Thread.sleep(10000);
                Platform.runLater(() -> stage.getScene().setOnMouseMoved(e -> {
                    stage.getScene().setOnMouseMoved(null);
                    sendMessage("i_am_alive");
                    initAliveDetectingThread();
                }));
            } catch (InterruptedException e) {
                System.err.println("[ WARN ] Alive detecting thread has been interrupted");
                initAliveDetectingThread();
            }
        });
        aliveDetectingThread.setDaemon(true);
        aliveDetectingThread.start();
    }

    /**
     * Загружает/обновляет внешний вид приложения
     */
    private void loadView() {
        ResourceBundle bundle = Client.currentResourceBundle();

        double width = stage.getWidth();
        double height = stage.getHeight();

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

            stage.setWidth(width);
            stage.setHeight(height);

            creaturesTable.getSelectionModel().selectedItemProperty().addListener(
                    (observable, oldValue, newValue) -> {
                        onCreatureSelected(newValue);
                        creaturesCanvas.selectCreature(newValue);
                    }
            );

            updateCreaturesCountText();
            creatureCreatingHyperlink.setParent(stage);

            creatureCreatingHyperlink.setCreatureCreatingListener((name, x, y, radius) -> {
                Properties properties = new Properties();
                properties.setProperty("name", name);
                properties.setProperty("x", Integer.toString(x));
                properties.setProperty("y", Integer.toString(y));
                properties.setProperty("radius", Float.toString(radius));
                sendMessage("create_creature", properties);
            });

            creaturePropertiesPane.setDeletingListener(creatureId -> sendMessage("delete_creature", creatureId));
            creaturePropertiesPane.setApplyingListener(model -> sendMessage("modify_creature", model));

            creaturesCanvas.setTarget(creaturesTable.getItems());

            creaturesCanvas.widthProperty().bind(creatureCanvasTab.getTabPane().widthProperty());
            creaturesCanvas.heightProperty().bind(creatureCanvasTab.getTabPane().heightProperty());
            creaturesCanvas.setSelectingListener((m) -> {
                if (m != null)
                    creaturesTable.getSelectionModel().select(m);
                else
                    creaturesTable.getSelectionModel().clearSelection();
            });
            sendMessage("request_users");
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(bundle.getString("login-dialog.error-alert-title"));
            alert.setHeaderText(bundle.getString("login-dialog.error-alert-header"));
            alert.setContentText(e.toString());
            alert.setOnCloseRequest((v) -> stage.close());
            alert.show();
        }
    }

    /**
     * Отправляет сообщение серверу
     *
     * @param message сообщение
     */
    private void sendMessage(String message) {
        sendMessage(message, null);
    }

    /**
     * Отправляет серверу сообщение с приложением
     *
     * @param message сообщение
     *
     * @param attachment приложение
     */
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

    /**
     * Вызывается, когда существо выбрано
     *
     * @param model модель существа
     */
    private void onCreatureSelected(CreatureModel model) {
        propertiesScrollPane.setVisible(true);
        propertiesScrollPane.setManaged(true);
        selectCreaturesLabel.setVisible(false);
        selectCreaturesLabel.setManaged(false);
        creaturePropertiesPane.selectCreature(model, model != null && model.getOwnerid() == userid);
    }

    /**
     * Должен вызываться каждый раз, когда приходит сообщение от сервера
     *
     * <del>
     * Если б мне платили каждый раз,
     * каждый раз, когда я думаю о тебе,
     * я бы "disconnected" каждый час,
     * я бы кинула Exception и stackTrace
     * </del>
     *
     * @param message сообщение
     */
    private void onMessageReceived(Message message) {
        System.out.println(message.getText());
        switch (message.getText()) {
            case "users_list_updated":
                @SuppressWarnings("unchecked")
                Set<Properties> users = (Set<Properties>)message.getAttachment();

                Platform.runLater(() -> {
                    boolean thisUserIsLoggedOut = true;
                    creaturesCanvas.getUserColors().clear();
                    usersList.clear();
                    for (Properties user : users) {
                        if (Integer.parseInt(user.getProperty("id")) == userid)
                            thisUserIsLoggedOut = false;

                        usersList.addUser(
                                Integer.parseInt(user.getProperty("id")),
                                user.getProperty("name"),
                                Color.valueOf(user.getProperty("color"))
                        );

                        creaturesCanvas.getUserColors().put(
                                Integer.parseInt(user.getProperty("id")),
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
                updateCreaturesCountText();
                break;
            }

            case "creature_deleted": {
                CreatureModel model = (CreatureModel) message.getAttachment();
                creaturesTable.getItems().remove(model);
                updateCreaturesCountText();
                break;
            }

            case "creature_modified":
                CreatureModel model = (CreatureModel) message.getAttachment();
                ObservableList<CreatureModel> items = creaturesTable.getItems();
                for (int i = 0, itemsSize = items.size(); i < itemsSize; i++) {
                    if (items.get(i).getId() == model.getId()) {
                        creaturesTable.getItems().get(i).setFromCreatureModel(model);
                        creaturesTable.getSelectionModel().select(i);
                        break;
                    }
                }
                break;

            case "creatures_list_updated": {
                @SuppressWarnings("unchecked")
                Set<CreatureModel> creatureModels = (Set<CreatureModel>) message.getAttachment();
                creaturesTable.getItems().setAll(creatureModels);
                updateCreaturesCountText();
                break;
            }
        }
    }

    /**
     * Обновляет текст с количеством существ
     */
    private void updateCreaturesCountText() {
        int creaturesCount = 0;
        for (CreatureModel current : creaturesTable.getItems())
            creaturesCount += current.getOwnerid() == userid ? 1 : 0;
        creaturesCountLabel.setText(creaturesCount + " " + Client.currentResourceBundle().getString("main.creatures-count-text"));
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

    /**
     * Вызывается, если во время приема сообщения от сервера что-то пошло не так
     * @param e возникшее исключение
     */
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
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText(Client.currentResourceBundle().getString("main.connection-error"));
                alert.setContentText(e.toString());
                alert.show();
            }
        });
    }
}
