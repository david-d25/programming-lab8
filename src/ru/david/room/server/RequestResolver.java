package ru.david.room.server;

import ru.david.room.Message;
import ru.david.room.Utils;
import ru.david.room.server.resolvers.RequiresAuthorization;
import ru.david.room.server.resolvers.Resolver;

import javax.mail.MessagingException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.GeneralSecurityException;
import java.sql.SQLException;

/**
 * Решатель запросов. Решает запросы.
 */
public class RequestResolver implements HubFriendly {
    private Hub hub;
    private Logger logger;

    @Override
    public void onHubConnected(Hub hub) {
        this.hub = hub;
    }

    @Override
    public void onHubReady() {
        logger = hub.getLogger();
    }

    void resolveAsync(ObjectOutputStream out, Message message) {
        new AsyncResolver(out, message);
    }

    private class AsyncResolver extends Thread {
        private ObjectOutputStream out;
        private Message message;

        AsyncResolver(ObjectOutputStream o, Message m) {
            out = o;
            message = m;
            start();
        }

        @Override
        public void run() {
            try {
                String command = message.getText();

                if (message.getUserid() == null)
                    logger.log("Запрос от анонима: " + command);
                else
                    logger.log("Запрос от id " + message.getUserid() + ": " + command);

                Message response = new Message("COMMAND_NOT_SUPPORTED");

                try {
                    Class<?> clazz = Class.forName("ru.david.room.server.resolvers." + Utils.toCamelCase(command) + "Resolver");
                    Object object = clazz.newInstance();
                    Resolver resolver = (Resolver)object;
                    if (resolver instanceof RequiresAuthorization) {
                        if (message.getUserid() != null && message.getToken() != null) {
                            int userid = message.getUserid();
                            int token = message.getToken();

                            if (hub.getController().isUserAuthorized(userid, token, true))
                                response = resolver.resolve(message, hub);
                            else
                                response = new Message("AUTH_FAILED");
                        } else
                            response = new Message("AUTH_FAILED");
                    } else
                        response = resolver.resolve(message, hub);

                } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
                } catch (ClassCastException | IllegalAccessException | InstantiationException e) {
                    logger.warn("В пакете ru.david.room.server.resolvers нашелся класс неверного формата " + e.toString());
                } catch (SQLException e) {
                    logger.err("Произошла ошибка при работе с базами данных во время выполнения команды " + command + ":\n" + e.toString());
                    response = new Message("INTERNAL_ERROR");
                } catch (GeneralSecurityException e) {
                    logger.err("Произошла ошибка безопасности во время выполнения команды " + command + ":\n" + e.toString());
                    response = new Message("INTERNAL_ERROR");
                } catch (MessagingException e) {
                    logger.err("Произошла ошибка отправки электронного письма при выполнении команды " + command + ":\n" + e.toString());
                    response = new Message("INTERNAL_ERROR");
                } finally {
                    synchronized (connector) {
                        if (response != null)
                            out.writeObject(response);
                    }
                }
            } catch (IOException | NullPointerException e) {
                logger.warn("Не получилось отправить ответ: " + e.toString());
            }
        }
    }
}
