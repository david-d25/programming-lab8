package ru.david.room.server.resolvers;

import ru.david.room.Message;
import ru.david.room.server.Hub;

@SuppressWarnings("unused")
public class IAmAliveResolver implements Resolver, RequiresAuthorization, UpdatesTokenLifetime {
    @Override
    public Message resolve(Message message, Hub hub) {
        return null;
    }
}
