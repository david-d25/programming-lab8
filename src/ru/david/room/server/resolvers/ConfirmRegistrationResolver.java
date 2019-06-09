package ru.david.room.server.resolvers;

import ru.david.room.Message;
import ru.david.room.server.Hub;
import ru.david.room.server.ServerController;

import java.sql.*;

@SuppressWarnings("unused")
public class ConfirmRegistrationResolver implements Resolver {
    @Override
    public Message resolve(Message message, Hub hub) throws SQLException {
        String code = (String)message.getAttachment();

        ServerController controller = hub.getController();
        Connection connection = controller.getConnection();

        PreparedStatement statement = connection.prepareStatement(
                "select * from registration_tokens where token = ?"
        );
        statement.setString(1, code);

        controller.removeAllExpiredRegistrationTokens();

        ResultSet result = statement.executeQuery();
        if (result.next()) {
            String name = result.getString("name");
            String email = result.getString("email");
            byte[] passwordHash = result.getBytes("password_hash");
            Timestamp registered = new Timestamp(System.currentTimeMillis());

            controller.removeRegistrationToken(code);

            statement = connection.prepareStatement("insert into users " +
                    "(name, email, password_hash, registered) values (?, ?, ?, ?)");

            statement.setString(1, name);
            statement.setString(2, email);
            statement.setBytes(3, passwordHash);
            statement.setTimestamp(4, registered);

            statement.execute();

            return new Message("OK");
        }
        return new Message("WRONG");
    }
}
