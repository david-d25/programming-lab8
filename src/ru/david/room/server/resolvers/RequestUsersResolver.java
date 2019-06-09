package ru.david.room.server.resolvers;

import ru.david.room.Message;
import ru.david.room.server.Hub;

import java.sql.SQLException;

@SuppressWarnings("unused")
public class RequestUsersResolver implements Resolver, RequiresAuthorization {
    @Override
    public Message resolve(Message message, Hub hub) throws SQLException {
        return new Message("users_list_updated", hub.getController().generateOnlineUsersList());
    }
}