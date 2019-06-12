package ru.david.room.server.resolvers;

import ru.david.room.CreatureModel;
import ru.david.room.Message;
import ru.david.room.server.Hub;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@SuppressWarnings("unused")
public class DeleteCreatureResolver implements Resolver, RequiresAuthorization, UpdatesTokenLifetime {
    @Override
    public Message resolve(Message message, Hub hub) throws SQLException {
        Long creatureId = (Long)message.getAttachment();
        if (creatureId == null)
            return new Message("BAD_REQUEST");

        Connection connection = hub.getController().getConnection();

        PreparedStatement statement = connection.prepareStatement(
                "select * from creatures where id = ? and ownerid = ?"
        );
        statement.setLong(1, creatureId);
        statement.setInt(2, message.getUserid());
        ResultSet resultSet = statement.executeQuery();

        if (resultSet.next()) {
            CreatureModel model = CreatureModel.fromResultSet(resultSet);

            statement = connection.prepareStatement("delete from creatures where id = ?;");
            statement.setLong(1, creatureId);
            statement.execute();

            hub.getClientPool().makeStrongStatement(new Message("creature_deleted", model));
        }

        return null;
    }
}
