package ru.david.room.server.resolvers;

import ru.david.room.Message;
import ru.david.room.server.Hub;

import javax.mail.MessagingException;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

@SuppressWarnings("unused")
public class RequestPasswordResetResolver implements Resolver {
    @Override
    public Message resolve(Message message, Hub hub) throws SQLException, MessagingException {
        Properties properties = (Properties)message.getAttachment();
        String email = properties.getProperty("email", "");
        String localeLanguage = properties.getProperty("locale-language", "en");
        String localeCountry = properties.getProperty("locale-country", "US");

        hub.getController().removeAllExpiredPasswordResetTokens();

        Connection connection = hub.getController().getConnection();

        PreparedStatement emailCheckingStatement = connection.prepareStatement(
                "select * from users where email = ?"
        );
        emailCheckingStatement.setString(1, email);
        ResultSet users = emailCheckingStatement.executeQuery();

        if (users.next()) {
            int userid = users.getInt("id");
            int token = hub.getController().generatePasswordResetToken(userid);

            ResourceBundle bundle = ResourceBundle.getBundle("i18n/text", new Locale(localeLanguage, localeCountry));

            hub.getController().sendMail(email, bundle.getString("forgot-password-dialog.mail-title"),
                    "<p style=\"text-align: center; padding: 25px;\">" +
                            bundle.getString("forgot-password-dialog.your-id") +
                            " <b style=\"padding: 12px; margin: 12px; background: antiquewhite; border-radius: 3px\">" + userid + "</b></p>" +
                            "<p style=\"text-align: center; padding: 25px;\">" +
                            bundle.getString("forgot-password-dialog.your-code") +
                            " <b style=\"padding: 12px; margin: 12px; background: antiquewhite; border-radius: 3px\">" + token + "</b></p>");
            return new Message("OK");
        } else
            return new Message("EMAIL_NOT_EXIST");
    }
}
