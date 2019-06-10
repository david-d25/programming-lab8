package ru.david.room.server;

import javafx.scene.paint.Color;
import ru.david.room.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Properties;
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
        logger.log("Новое соединение: " + socket.getInetAddress() + ", соединено " + connectors.size() + " клиентов");
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

    void removeConnector(ClientConnector c) {
        removeConnector(c, true);
    }

    private void removeConnector(ClientConnector c, boolean log) {
        if (connectors.contains(c)) {
            connectors.remove(c);
            if (log)
                logger.log("Коннектор удален, соединено " + connectors.size() + " клиентов");
        }
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

        public ObjectOutputStream getOut() {
            return out;
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
                            if (message.hasAttachment()) {
                                Properties arguments = (Properties)message.getAttachment();
                                if ("true".equals(arguments.getProperty("send_response")))
                                    out.writeObject(new Message("disconnected"));
                            }

                            socket.close();
                            removeConnector(this, false);

                            if (message.getUserid() != null)
                                logger.log("Клиент с id " + message.getUserid() + " отсоединился. Соединено " + connectors.size() + " клиентов");
                            else
                                logger.log("Аноним отсоединился из ip " + socket.getInetAddress());
                            return;

                        case "subscribe":
                            subscribedToStrongStatements = true;

                            if (message.getUserid() != null)
                                logger.log("Клиент с id " + message.getUserid() + " подписался на события");
                            else
                                logger.log("Аноним подписался на события из ip " + socket.getInetAddress());
                            continue;

                        case "unsubscribe":
                            subscribedToStrongStatements = false;

                            if (message.getUserid() != null)
                                logger.log("Клиент с id " + message.getUserid() + " отписался от событий");
                            else
                                logger.log("Аноним отписался от событий из ip " + socket.getInetAddress());
                            continue;
                    }

                    hub.getRequestResolver().resolveAsync(this, message);
                }
            } catch (ClassNotFoundException e) {
                logger.err("Клиент отправил экземпляр незнакомого серверу класса: " + e.toString());
            } catch (IOException e) {
                if (!(e instanceof SocketException && e.getMessage().equals("Connection reset")))
                    logger.warn("Клиент неожиданно отсоединился: " + e.toString());
            } finally {
                removeConnector(this);
            }
        }
    }
}
