package ru.david.room.server.resolvers;

import ru.david.room.Message;
import ru.david.room.server.Hub;
import ru.david.room.server.ServerController;

import java.security.GeneralSecurityException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

@SuppressWarnings("unused")
public class LoginResolver implements Resolver {
    @Override
    public Message resolve(Message message, Hub hub) throws SQLException, GeneralSecurityException {
        Properties properties = (Properties)message.getAttachment();

        String email = properties.getProperty("email", "");
        String password = properties.getProperty("password", "");

        ServerController controller = hub.getController();
        PreparedStatement statement = controller.getConnection().prepareStatement(
                "select * from users where email = ? and password_hash = ?"
        );
        statement.setString(1, email);
        statement.setBytes(2, controller.hashPassword(password));

        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) {
            Properties result = new Properties();

            String userid = resultSet.getString("id");
            String userName = resultSet.getString("name");
            String userToken = Integer.toString(controller.generateUserToken(
                    resultSet.getInt("id"),
                    userName
            ));
            int userColor = resultSet.getInt("color888");

            result.setProperty("userid", userid);
            result.setProperty("user_name", userName);
            result.setProperty("user_color", String.format("#%06x", userColor));
            result.setProperty("user_token", userToken);

            hub.getClientPool().makeStrongStatement(new Message("users_list_updated", controller.generateOnlineUsersList()));

            return new Message("OK", result);
        } else {
            return new Message("WRONG");
        }
    }
}
