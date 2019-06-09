package ru.david.room.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Сервер. Просто Сервер. К нему подключаются Просто Клиенты.
 */
public class Server implements HubFriendly {
    private ServerSocket serverSocket;
    private Hub hub;

    @Override
    public void onHubConnected(Hub hub) {
        this.hub = hub;
    }

    @Override
    public void onHubReady() {
        ServerConfig config = hub.getConfig();
        Logger logger = hub.getLogger();

        try {
            serverSocket = new ServerSocket(config.getPort());
        } catch (IOException e) {
            logger.err("Ошибка создания серверного сокета (" + e.getMessage() + "), приложение будет остановлено.");
            System.exit(1);
        }

        logger.log("Сервер запущен и слушает порт " + config.getPort() + "...");

        new Thread(() -> {
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    hub.getClientPool().addClient(clientSocket);
                } catch (IOException e) {
                    logger.err("Ошибка подключения: " + e.getMessage());
                }
            }
        }).start();
    }
}