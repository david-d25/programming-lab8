package ru.david.room.server.resolvers;

import ru.david.room.CreatureModel;
import ru.david.room.Message;
import ru.david.room.server.Hub;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;

@SuppressWarnings("unused")
public class RequestCreaturesResolver implements Resolver, RequiresAuthorization, UpdatesTokenLifetime {
    @Override
    public Message resolve(Message message, Hub hub) throws SQLException {
        ResultSet resultSet = hub.getController().getConnection().createStatement().executeQuery("select * from creatures");
        HashSet<CreatureModel> creatureModels = new HashSet<>();

        while (resultSet.next())
            creatureModels.add(CreatureModel.fromResultSet(resultSet));

        return new Message("creatures_list_updated", creatureModels);
    }
}
