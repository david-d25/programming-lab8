package ru.david.room.client.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;

public class UsersListItem extends HBox {
    private Circle circle;
    private Label label;

    private String name = "";
    private int userid = -1;

    UsersListItem(int id, String name, Color color) {
        this(id);
        setName(name);
        setPaint(color);
    }

    UsersListItem(int id) {
        this();
        setUserid(id);
    }

    private UsersListItem() {
        setAlignment(Pos.CENTER_LEFT);
        circle = new Circle(4);
        label = new Label();
        label.setFont(new Font(14));
        getChildren().add(circle);
        getChildren().add(label);
        setMargin(circle, new Insets(5));
    }

    public Paint getColor() {
        return circle.getFill();
    }

    public void setPaint(Paint p) {
        circle.setFill(p);
    }

    public int getUserid() {
        return userid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
        label.setText(getName() + " [" + getUserid() + "]");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        label.setText(getName() + " [" + getUserid() + "]");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != getClass())
            return false;
        UsersListItem item = (UsersListItem)obj;
        return item.getUserid() == getUserid();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
