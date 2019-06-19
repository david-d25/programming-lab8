package ru.david.room.server.resolvers;

import ru.david.room.Message;
import ru.david.room.Utils;
import ru.david.room.server.Hub;

import javax.mail.MessagingException;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

@SuppressWarnings("unused")
public class ResetPasswordResolver implements Resolver {
    @Override
    public Message resolve(Message message, Hub hub) throws SQLException, GeneralSecurityException, MessagingException {
        Properties properties = (Properties)message.getAttachment();

        String token = properties.getProperty("confirmation");
        String newPassword = properties.getProperty("password");

        hub.getController().removeAllExpiredPasswordResetTokens();

        Connection connection = hub.getController().getConnection();

        PreparedStatement statement = connection.prepareStatement(
                "select * from password_reset_tokens where token = ?"
        );

        if (newPassword.length() < 8)
            return new Message("SHORT_PASSWORD");

        if (Utils.isPasswordTooWeak(newPassword))
            return new Message("WEAK_PASSWORD");

        statement.setString(1, token);

        ResultSet resultSet = statement.executeQuery();

        if (resultSet.next()) {
            statement = connection.prepareStatement(
                    "update users set password_hash = ? where id = ?"
            );
            statement.setBytes(1, hub.getController().hashPassword(newPassword));
            statement.setInt(2, resultSet.getInt("userid"));
            statement.execute();

            statement = connection.prepareStatement(
                    "delete from password_reset_tokens where token = ? and userid = ?"
            );
            statement.setString(1, token);
            statement.setInt(2, resultSet.getInt("userid"));
            statement.execute();
            return new Message("OK");
        } else
            return new Message("WRONG");
    }
}
