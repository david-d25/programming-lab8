package ru.david.room.server.resolvers;

import ru.david.room.Message;
import ru.david.room.server.Hub;
import ru.david.room.server.ServerController;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@SuppressWarnings("unused")
public class LogoutResolver implements Resolver, RequiresAuthorization {
    @Override
    public Message resolve(Message message, Hub hub) throws SQLException {
        ServerController controller = hub.getController();

        PreparedStatement statement = controller.getConnection().prepareStatement(
                "delete from user_tokens where userid = ? and token = ?;"
        );
        statement.setInt(1, message.getUserid());
        statement.setInt(2, message.getToken());
        statement.execute();

        hub.getClientPool().makeStrongStatement(
                new Message("users_list_updated", controller.generateOnlineUsersList())
        );

        return null;
    }
}
