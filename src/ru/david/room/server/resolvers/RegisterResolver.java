package ru.david.room.server.resolvers;

import ru.david.room.Message;
import ru.david.room.Utils;
import ru.david.room.server.Hub;
import ru.david.room.server.ServerController;

import javax.mail.MessagingException;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

@SuppressWarnings("unused")
public class RegisterResolver implements Resolver {
    @Override
    public Message resolve(Message message, Hub hub) throws SQLException, GeneralSecurityException, MessagingException {
        Properties properties = (Properties)message.getAttachment();

        String name = properties.getProperty("name", "");
        String email = properties.getProperty("email", "");
        String password = properties.getProperty("password", "");
        String localeLanguage = properties.getProperty("locale-language", "en_US");
        String localeCountry  = properties.getProperty("locale-country", "en_US");

        if (!name.matches("[A-Za-z0-9_]{2,64}"))
            return new Message("INCORRECT_NAME");

        if (!Utils.isValidEmailAddress(email))
            return new Message("INCORRECT_EMAIL");

        if (password.length() < 8)
            return new Message("SHORT_PASSWORD");

        if (Utils.isPasswordTooWeak(password))
            return new Message("WEAK_PASSWORD");

        ServerController controller = hub.getController();
        controller.removeAllExpiredRegistrationTokens();

        Connection connection = controller.getConnection();

        PreparedStatement statement = connection.prepareStatement("select email from users WHERE email = ?");
        statement.setString(1, email);
        if (statement.executeQuery().next())
            return new Message("EMAIL_EXISTS");

        statement = connection.prepareStatement("select email from registration_tokens WHERE email = ?");
        statement.setString(1, email);
        if (statement.executeQuery().next())
            return new Message("EMAIL_IN_USE");

        int token = controller.generateRegistrationToken(name, email, password);

        ResourceBundle bundle = ResourceBundle.getBundle("i18n/text", new Locale(localeLanguage, localeCountry));

        controller.sendMail(email, bundle.getString("register-dialog.registration-confirmation-email-subject"),
                "<p style=\"text-align: center; padding: 25px;\">" +
                        bundle.getString("register-dialog.registration-confirmation-email-content") +
                        " <b style=\"padding: 12px; margin: 12px; background: antiquewhite; border-radius: 3px\">" + token + "</b></p>"
        );

        return new Message("OK");
    }
}
