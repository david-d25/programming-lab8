package ru.david.room.server;

import ru.david.room.Message;
import ru.david.room.json.JSONObject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

class BroadcastingController {
    private ServerController controller;
    private Logger logger;
    private ConcurrentHashMap<Long, BroadcastingListener> listeners = new ConcurrentHashMap<>();

    BroadcastingController(ServerController c, Logger l) {
        controller = c;
        logger = l;

        Runnable timeoutCheckingRunnable = () -> {
            logger.log("Проверка таймаутов запущена");
            try {
                while (!Thread.interrupted()) {
                    checkForTimedOutUsers();
                    Thread.sleep(2500);
                }
            } catch (InterruptedException ignored) {}
            logger.log("Проверка таймаутов остановлена");
        };
        new Thread(timeoutCheckingRunnable).start();
    }

    public void addListener(long id) {
        listeners.put(id, new BroadcastingListener(id));
    }

    public void removeListener(long id) {
        listeners.remove(id);
    }

    public BroadcastingListener getListener(long id) {
        return listeners.get(id);
    }

    /**
     * Проверяет, нет ли пользователей, вышедших из-за таймаута.
     * Если такие есть, удаляет из базы данных их токены и вызывает {@link #userTimedOut(int, String)}
     */
    private void checkForTimedOutUsers() {
        Connection connection = controller.getConnection();
        if (connection == null)
            return;
        try {
            String prefix = Server.getConfig().getTableNamesPrefix();
            PreparedStatement statement = connection.prepareStatement(
                    "select userid, name from " + prefix + "user_tokens join " +
                            prefix + "users on " + prefix + "user_tokens.userid = " + prefix + "users.id where expires < ?"
            );
            statement.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            ResultSet timedOutUsers = statement.executeQuery();
            while (timedOutUsers.next())
                userTimedOut(timedOutUsers.getInt("userid"), timedOutUsers.getString("name"));

            statement = connection.prepareStatement(
                    "delete from " + Server.getConfig().getTableNamesPrefix() + "user_tokens where expires < ?"
            );
            statement.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            statement.execute();
        } catch (SQLException e) {
            logger.err("Произошла ошибка SQL во время проверки токенов пользователей: " + e.toString());
        }
    }

    void userLoggedIn(int userid, String name) {
        logger.log(String.format("Пользователь %s [%s] вошел в систему", name, userid));
        for (BroadcastingListener listener : listeners.values())
            listener.send("login " + userid + " " + name);
    }

    void userLoggedOut(int userid, String name) {
        logger.log(String.format("Пользователь %s [%s] вышел из системы", name, userid));
        for (BroadcastingListener listener : listeners.values())
            listener.send("logout " + userid + " " + name);
    }

    private void userTimedOut(int userid, String name) {
        logger.log(String.format("Пользователь %s [%s] вышел по таймауту", name, userid));
        for (BroadcastingListener listener : listeners.values())
            listener.send("timeout " + userid + " " + name);
    }

    public class BroadcastingListener {
        private long id;
        private List<String> pool = new LinkedList<>();

        BroadcastingListener(long id) {
            this.id = id;
        }

        void pullUpdates(ObjectOutputStream oos) {
            try {
                for (String message : pool)
                    oos.writeObject(new Message(message, null, true));
                if (pool.size() == 0)
                    oos.writeObject(new Message("nop", null, true));
                oos.flush();
                pool.clear();
            } catch (IOException e) {
                logger.warn("Получатель вещания неожиданно отключился: " + e.toString());
                listeners.remove(id);
            }
        }

        void send(String object) {
            pool.add(object);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || obj.getClass() != getClass())
                return false;
            BroadcastingListener l = (BroadcastingListener)obj;
            return id == l.id;
        }
    }
}