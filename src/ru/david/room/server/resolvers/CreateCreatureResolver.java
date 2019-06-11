package ru.david.room.server.resolvers;

import ru.david.room.CreatureModel;
import ru.david.room.Message;
import ru.david.room.server.Hub;

import java.sql.*;
import java.util.Properties;

@SuppressWarnings("unused")
public class CreateCreatureResolver implements Resolver, RequiresAuthorization, UpdatesTokenLifetime {
    public Message resolve(Message message, Hub hub) throws SQLException {
        Connection connection = hub.getController().getConnection();
        Properties properties = (Properties)message.getAttachment();

        try {
            String name = properties.getProperty("name");
            int x = Integer.parseInt(properties.getProperty("x"));
            int y = Integer.parseInt(properties.getProperty("y"));
            float radius = Float.parseFloat(properties.getProperty("radius"));
            int ownerid = message.getUserid();
            Timestamp created = new Timestamp(System.currentTimeMillis());

            if (name.length() == 0 || name.length() > 32 || x < 0 || x > 1000 || y < 0 || y > 1000 || radius < 15 || radius > 300)
                return new Message("BAD_REQUEST");

            PreparedStatement statement = connection.prepareStatement(
                    "insert into creatures (name, x, y, radius, ownerid, created) VALUES (?, ?, ?, ?, ?, ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS
            );

            statement.setString(1, name);
            statement.setInt(2, x);
            statement.setInt(3, y);
            statement.setFloat(4, radius);
            statement.setInt(5, ownerid);
            statement.setTimestamp(6, created);

            ResultSet resultSet = statement.executeQuery();
            resultSet.next();

            CreatureModel model = CreatureModel.fromResultSet(resultSet);

            hub.getClientPool().makeStrongStatement(new Message("creature_added", model));
            return null;

        } catch (NumberFormatException e) {
            return new Message("BAD_REQUEST");
        }
    }
}
