package ru.david.room.client.ui;

import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class UsersList extends VBox {
    public void addUser(int userid, String name, Color color) {
        getChildren().add(new UsersListItem(userid, name, color));
    }

    public void removeUser(int userid) {
        getChildren().remove(new UsersListItem(userid));
    }

    public void clear() {
        getChildren().clear();
    }
}
