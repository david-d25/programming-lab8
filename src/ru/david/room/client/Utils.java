package ru.david.room.client;

import javafx.stage.Stage;

public class Utils {
    public static void twitch(Stage stage) {
        new Thread(() -> {
            double initialX = stage.getX();

            try {
                for (float i = 0; i < 4 * Math.PI; i += 0.1) {
                    Thread.sleep(1);
                    stage.setX(initialX + 8 * Math.sin(i));
                }
            } catch (InterruptedException ignored) {
            } finally {
                stage.setX(initialX);
            }
        }).start();
    }
}
