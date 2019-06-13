package ru.david.room.server.resolvers;

import ru.david.room.CreatureModel;
import ru.david.room.Message;
import ru.david.room.server.Hub;

import javax.mail.MessagingException;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Решатель запроса на модификацию существа. В качестве объекта-аргумента принимает {@link CreatureModel},
 * его поля используются для установки новых значений.
 *
 * Важно: поле id владельца из объекта-аргумента игнорируется. Вместо него используется
 * id отправителя запроса, чтобы предотвратить возможную подмену владельца существа.
 */
@SuppressWarnings("unused")
public class ModifyCreatureResolver implements Resolver, RequiresAuthorization, UpdatesTokenLifetime {
    @Override
    public Message resolve(Message message, Hub hub) throws SQLException {
        Connection connection = hub.getController().getConnection();

        CreatureModel model = (CreatureModel)message.getAttachment();

        if (model.getName().length() == 0 || model.getName().length() > 32)
            return new Message("BAD_REQUEST");
        if (model.getX() < 0 || model.getY() < 0 || model.getX() > 1000 || model.getY() > 1000)
            return new Message("BAD_REQUEST");

        PreparedStatement statement = connection.prepareStatement(
                "update creatures set name = ?, x = ?, y = ?, radius = ? where id = ? and ownerid = ?"
        );
        statement.setString(1, model.getName());
        statement.setInt(2, model.getX());
        statement.setInt(3, model.getY());
        statement.setFloat(4, model.getRadius());
        statement.setLong(5, model.getId());
        statement.setInt(6, message.getUserid());
        statement.execute();

        // Создаётся ещё одна модель, чтобы не использовать id владельца из объекта-аргумента (см. javadoc к классу)
        CreatureModel newModel = new CreatureModel(
                model.getId(), model.getX(), model.getY(), model.getRadius(), message.getUserid(), model.getName()
        );

        hub.getClientPool().makeStrongStatement(new Message("creature_modified", newModel));

        return null;
    }
}
