package ru.david.room.server;

import javafx.scene.paint.Color;
import ru.david.room.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;

/**
 * Как понятно из названия, бассейн с клиентами.
 */
public class ClientPool implements HubFriendly {
    private Hub hub;
    private Logger logger;
    private Set<ClientConnector> connectors = new HashSet<>();

    @Override
    public void onHubConnected(Hub hub) {
        this.hub = hub;
    }

    @Override
    public void onHubReady() {
        logger = hub.getLogger();
    }

    void addClient(Socket socket) {
        connectors.add(new ClientConnector(socket));
    }

    /**
     * Делает <strong>сильное</strong> заявление.
     * Фактически, отправляет сообщение всем подключённым клиентам
     *
     * @param message сообщение для отправки
     */
    public void makeStrongStatement(Message message) {
        int sendCount = 0;
        for (ClientConnector connector : connectors) {
            if (connector.isSubscribedToStrongStatements()) {
                connector.sendMessage(message);
                sendCount++;
            }
        }
        logger.log("Сильное заявление: " + message.getText() + ", отправлено " + sendCount + " пользователям");
        logger.log("Проверять мы его, конечно, не будем");
    }

    private void removeConnector(ClientConnector c) {
        connectors.remove(c);
    }

    class ClientConnector extends Thread {
        private Socket socket;
        private ObjectInputStream in;
        private ObjectOutputStream out;

        private boolean subscribedToStrongStatements = false;

        ClientConnector(Socket s) {
            try {
                socket = s;
                in = new ObjectInputStream(s.getInputStream());
                out = new ObjectOutputStream(s.getOutputStream());
                start();
            } catch (IOException e) {
                logger.warn("Клиент не смог присоединиться: " + e.toString());
                removeConnector(this);
            }
        }

        void sendMessage(Message message) {
            try {
                out.writeObject(message);
                out.flush();
            } catch (IOException e) {
                logger.log("Не получилось отправить сообщение клиенту " + socket.getInetAddress());
                removeConnector(this);
            }
        }

        boolean isSubscribedToStrongStatements() {
            return subscribedToStrongStatements;
        }

        @Override
        public void run() {
            try {
                while (!socket.isClosed()) {
                    Message message = (Message) in.readObject();
                    switch (message.getText()) {
                        case "disconnect":
                            socket.close();
                            removeConnector(this);
                            continue;

                        case "subscribe":
                            subscribedToStrongStatements = true;
                            continue;

                        case "unsubscribe":
                            subscribedToStrongStatements = false;
                            continue;
                    }
                    hub.getRequestResolver().resolveAsync(out, message);
                }
            } catch (ClassNotFoundException e) {
                logger.err("Клиент отправил экземпляр незнакомого серверу класса: " + e.toString());
            } catch (IOException e) {
                if (!(e instanceof SocketException && e.getMessage().equals("Connection reset")))
                    logger.warn("Ошибка подключения клиента: " + e.toString());
            } finally {
                removeConnector(this);
            }
        }
    }
}
