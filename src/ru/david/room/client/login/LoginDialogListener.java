package ru.david.room.client.login;

import javafx.scene.paint.Color;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public interface LoginDialogListener {
    void onLogin(int userid, int token, String name, Color color, Socket socket, ObjectInputStream ois, ObjectOutputStream oos);
}
