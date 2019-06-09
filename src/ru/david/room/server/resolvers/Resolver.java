package ru.david.room.server.resolvers;

import ru.david.room.Message;
import ru.david.room.server.Hub;

import javax.mail.MessagingException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;

/**
 * Интерфейс для всего, чот может выполнять клиентские команды
 */
public interface Resolver {
    /**
     * Выполняет команду клиента, возвращает результат, который следует отправить клиенту.
     *
     * @param hub хаб для управления сервером
     *
     * @return сообщение с ответом
     *
     * @throws SQLException если произойдет ошибка при работе с базами данных
     */
    Message resolve(Message message, Hub hub) throws SQLException, GeneralSecurityException, MessagingException;
}
