package ru.david.room.server;

import com.lambdaworks.crypto.SCrypt;
import ru.david.room.CreatureModel;
import ru.david.room.Utils;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.security.GeneralSecurityException;
import java.sql.*;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * Контроллер операций между сервером и внешним миром.
 * Позволяет выполнять обращения к базе данных и отправлять email-писем.
 */
public class ServerController implements HubFriendly {
    private static byte[] PASSWORD_SALT = "Hg6trUBbHfgH7ggGV7yuv".getBytes();

    private Hub hub;
    private ServerConfig config;
    private Logger logger;
    private Connection connection;

    private Session mailSession;

    @Override
    public void onHubConnected(Hub hub) {
        this.hub = hub;
    }

    @Override
    public void onHubReady() {
        config = hub.getConfig();
        logger = hub.getLogger();

        try {
            Class.forName(config.getJdbcDriver());
        } catch (ClassNotFoundException | NullPointerException e) {
            logger.err(
                    "Не удалось найти драйвер базы данных " + config.getJdbcDriver() + ", сервер будет остановлен"
            );
            System.exit(1);
        }

        initConnection();
        if (connection != null)
            initTables();
        initEmail();
        initAutoLogout();
    }

    /**
     * Инициализирует соединение с JavaMail API
     */
    private void initEmail() {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", config.getSmtpHost());
        properties.put("mail.smtp.port", config.getSmtpPort());
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.ssl.enable", config.isSmtpSSLEnabled());
        properties.put("mail.mime.charset", "UTF-16");
        properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

        Authenticator mailAuth = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.getMailUsername(), config.getMailPassword());
            }
        };
        mailSession = Session.getDefaultInstance(properties, mailAuth);
    }

    private void initAutoLogout() {
        new Thread(() -> {
            try {
                while (true) {
                    PreparedStatement statement = connection.prepareStatement(
                            "select * from user_tokens where expires < ?"
                    );
                    statement.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                    ResultSet resultSet = statement.executeQuery();

                    statement = connection.prepareStatement(
                            "delete from user_tokens where expires < ?"
                    );
                    statement.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                    statement.execute();

                    while (resultSet.next()) {
                        logger.log("Пользователь с id " + resultSet.getInt("userid") + " вышел по таймауту");
                        hub.getClientPool().makeStrongStatement(
                                new ru.david.room.Message(
                                        "users_list_updated", generateOnlineUsersList()
                                )
                        );
                    }
                    Thread.sleep(3000);
                }
            } catch (InterruptedException ignored) {
            } catch (SQLException e) {
                logger.err("Во время автоматического удаления устаревших токенов произошла ошибка: " + e.toString());
            }
        }).start();
    }

    /**
     * Инициализирует соединение с базой данных
     */
    private void initConnection() {
        try {
            String databaseUrl = String.format(
                    "jdbc:%s://%s:%s/%s",
                    config.getJdbcLangProtocol(),
                    config.getDatabaseHost(),
                    config.getDatabasePort(),
                    config.getDatabaseName()
            );
            logger.log("Соединение с базой данных...");
            connection = DriverManager.getConnection(databaseUrl, config.getDatabaseUser(), config.getDatabasePassword());
            logger.log("Соединение с базой данных успешно");
        } catch (SQLException e) {
            logger.err("Ошибка общения с базой данных: " + e.getMessage());
        }
    }

    /**
     * Создаёт необходимые таблицы, если их нет
     */
    private void initTables() {
        logger.log("Проверка таблиц...");

        autoCreateTable(
                "creatures",
                "id serial primary key not null, name text, x integer, y integer, radius float, ownerid integer, created timestamp"
        );

        autoCreateTable(
                "users",
                "id serial primary key not null, name text, email text, color888 integer, password_hash blob, registered timestamp"
        );

        autoCreateTable(
                "registration_tokens",
                "token integer primary key not null, name text, email text, password_hash blob, expires timestamp"
        );

        autoCreateTable(
                "password_reset_tokens",
                "token integer primary key not null, userid integer unique, expires timestamp"
        );

        autoCreateTable(
                "user_tokens",
                "token integer not null, userid integer not null, expires timestamp not null"
        );
    }

    /**
     * Создаёт таблицу с указанными именем и структурой, если её ещё нет.
     * К имени таблицы автоматически прибавляется префикс из конфигурации сервера.
     *
     * @param name имя таблицы без префикса
     *
     * @param structure описание структуры таблицы в sql-формате (имена колонн и их типы)
     */
    private void autoCreateTable(String name, String structure) {
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            if (
                    !metaData.getTables(
                            null,
                            null,
                            name,
                            new String[]{"TABLE"}
                    ).next()
            ) {
                connection.createStatement().execute("create table if not exists " + name +" (" + structure + ")");
                logger.log("Создана таблица " + name);
            }
        } catch (SQLException e) {
            logger.err("Не получилось создать таблицу " + name + ": " + e.getMessage());
        }
    }

    public Connection getConnection() {
        return connection;
    }

    /**
     * Предназначен для входа пользователя в систему
     *
     * @param email email пользователя
     *
     * @param password пароль пользователя
     *
     * @return результат входа
     */
    ServerResponse.Login login(String email, String password) {
        ServerResponse.Login response = new ServerResponse.Login();

        if (connection == null) {
            response.responseType = ServerResponse.Login.ResponseType.DB_NOT_SUPPORTED;
            return response;
        }

        try {
            PreparedStatement statement = connection.prepareStatement("select * from users " +
                    "where email = ? and password_hash = ?");

            statement.setString(1, email);
            statement.setBytes(2, hashPassword(password));
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                int userid = resultSet.getInt("id");
                int token = generateUserToken(userid, resultSet.getString("name"));
                String name = resultSet.getString("name");

                response.userid = userid;
                response.token = token;
                response.name = name;

                if (token == -1) {
                    response.responseType = ServerResponse.Login.ResponseType.DB_NOT_SUPPORTED;
                    return response;
                }

                response.responseType = ServerResponse.Login.ResponseType.OK;
                return response;
            } else {
                response.responseType = ServerResponse.Login.ResponseType.WRONG_PASSWORD;
                return response;
            }
        } catch (GeneralSecurityException e) {
            logger.err("Во время входа пользователя в систему произошла ошибка безопасности: " + e.toString());
            response.responseType = ServerResponse.Login.ResponseType.INTERNAL_ERROR;
            return response;
        } catch (SQLException e) {
            logger.err("Во время взода пользователя в систему произошла ошибка sql:\n" + e.toString());
            response.responseType = ServerResponse.Login.ResponseType.INTERNAL_ERROR;
            return response;
        }
    }

    void logout(int userid, int token) {
        if (connection == null)
            return;
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "delete from user_tokens where userid = ? and token = ?"
            );
            statement.setInt(1, userid);
            statement.setInt(2, token);
            if (statement.executeUpdate() != 0) {
                statement = connection.prepareStatement(
                        "select name from users where id = ?"
                );
                statement.setInt(1, userid);
                ResultSet resultSet = statement.executeQuery();
//                if (resultSet.next())
//                    broadcastingController.userLoggedOut(userid, resultSet.getString("name"));
                // TODO: user logged out
            }
        } catch (SQLException e) {
            logger.err("Во время выхода пользователя произошла ошибка SQL: " + e.toString());
        }
    }

    /**
     * Генерирует токен и добавляет его в базу данных. Если нет соединения с базой данных, возвращает -1
     *
     * @param userid id пользователя, для которого следует создать токен
     *
     * @param name имя пользователя для бродкастинга
     *
     * @return созданный токен
     */
    public int generateUserToken(int userid, String name) throws SQLException {
        if (connection == null)
            return -1;
        PreparedStatement statement = connection.prepareStatement(
                "select * from user_tokens where userid = ? and expires > ?"
        );
        statement.setInt(1, userid);
        statement.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
        ResultSet resultSet = statement.executeQuery();

        if (resultSet.next()) {
            int token = resultSet.getInt("token");
            updateUserToken(userid, token);
            return token;
        } else {
            statement = connection.prepareStatement(
                    "insert into user_tokens values (?, ?, ?)"
            );
            int token = generateRandomToken(6);
            statement.setInt(1, token);
            statement.setInt(2, userid);
            statement.setTimestamp(3, new Timestamp(System.currentTimeMillis() + config.getUserTokenTimeout()));
            statement.execute();
            return token;
        }
    }

    /**
     * Обновляет время жизни токена пользователя. Если токена нет для указанного пользователя,
     * ничего не делает. Если нет соединения с базой данных, тоже ничего не делает.
     *
     * @param userid id пользователя, для которого следует обновить токен
     *
     * @param token токен пользователя
     *
     */
    public void updateUserToken(int userid, int token) throws SQLException {
        if (connection == null)
            return;
        PreparedStatement statement = connection.prepareStatement(
                "update user_tokens set expires = ? where userid = ? and token = ?"
        );
        statement.setTimestamp(1, new Timestamp(System.currentTimeMillis() + config.getUserTokenTimeout()));
        statement.setInt(2, userid);
        statement.setInt(3, token);
        statement.execute();
    }

    /**
     * Удаляет токен пользователя для указанного <code>userid</code>. Если нет соединения с базой данных или
     * нет указанного пользователя или нет токена указанного пользователя, ничего не делает.
     *
     * @param userid пользователь, токен которого следует удалить
     *
     * @param token токен пользователя
     */
    private void removeUserToken(int userid, int token) throws SQLException {
        if (connection == null)
            return;
        PreparedStatement statement = connection.prepareStatement(
                "delete from user_tokens where userid = ? and token = ?"
        );
        statement.setInt(1, userid);
        statement.setInt(2, token);
        statement.execute();
    }

    CreatureModel[] showCreatures(Integer userid) {
        if (connection == null)
            return new CreatureModel[0];
        List<CreatureModel> creatureModels = new LinkedList<>();
        try {
            PreparedStatement statement;
            if (userid != null) {
                statement = connection.prepareStatement(
                        "select * from creatures where " +
                                "ownerid = ?");
                statement.setInt(1, userid);
            } else {
                statement = connection.prepareStatement(
                        "select * from creatures");
            }

            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next())
                creatureModels.add(CreatureModel.fromResultSet(resultSet));
            CreatureModel[] c = new CreatureModel[0];
            return creatureModels.toArray(c);
        } catch (SQLException e) {
            logger.err("Во время показа существ произошла ошибка SQL: " + e.toString());
        }
        return new CreatureModel[0];
    }

    /**
     * Удаляет указанный токен регистрации из базы данных
     *
     * @param token токен, который надо удалить
     */
    public void removeRegistrationToken(String token) throws SQLException {
        connection.createStatement().execute("delete from registration_tokens " +
                "where token = " + token);
    }

    /**
     * Удаляет из базы данных все устаревшие токены регистрации.
     * Если нет соединения с базой данных, ничего не делает.
     */
    public void removeAllExpiredRegistrationTokens() {
        if (connection == null)
            return;
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "delete from registration_tokens " +
                            "where expires < ?"
            );
            statement.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            statement.execute();
        } catch (SQLException e) {
            logger.err("Во время очистки устаревших токенов регистрации произошла ошибка:\n" + e.toString());
        }
    }

    /**
     * Удаляет из базы данных все устаревшие токены сброса пароля.
     * Если нет соединения с базой данных, ничего не делает.
     */
    private void removeAllExpiredPasswordResetTokens() {
        if (connection == null)
            return;
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "delete from password_reset_tokens  " +
                            "where expires < ?"
            );
            statement.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            statement.execute();
        } catch (SQLException e) {
            logger.err("Во время очистки устаревших токенов сброса пароля произошла ошибка:\n" + e.toString());
        }
    }

    /**
     * Позволяет изменить пароль пользователя
     *
     * @param userid id пользователя
     *
     * @param token токен пользователя
     *
     * @param oldPassword старый пароль
     *
     * @param newPassword новый пароль
     *
     * @return Результат изменения
     */
    ServerResponse.ChangePassword changePassword(int userid, int token, String oldPassword, String newPassword) {
        if (connection == null)
            return ServerResponse.ChangePassword.DB_NOT_SUPPORTED;

        try {
            if (isUserAuthorized(userid, token, true)) {
                PreparedStatement statement = connection.prepareStatement(
                        "select * from users where id = ? and password_hash = ?"
                );

                if (newPassword.length() < 6 || Utils.isPasswordTooWeak(newPassword))
                    return ServerResponse.ChangePassword.INCORRECT_NEW_PASSWORD;

                statement.setInt(1, userid);
                statement.setBytes(2, hashPassword(oldPassword));
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    statement = connection.prepareStatement(
                            "update users set password_hash = ? where id = ?"
                    );
                    statement.setBytes(1, hashPassword(newPassword));
                    statement.setInt(2, userid);
                    statement.execute();
                    return ServerResponse.ChangePassword.OK;
                }
                return ServerResponse.ChangePassword.WRONG_PASSWORD;
            } else
                return ServerResponse.ChangePassword.WRONG_TOKEN;
        } catch (SQLException e) {
            logger.err("Во время изменения пароля произошла ошибка SQL: " + e.toString());
            return ServerResponse.ChangePassword.INTERNAL_ERROR;
        } catch (GeneralSecurityException e) {
            logger.err("Во время изменения пароля произошла ошибка безопасности: " + e.toString());
            return ServerResponse.ChangePassword.INTERNAL_ERROR;
        }
    }

    /**
     * Позволяет запросить сброс пароля
     *
     * @param email email пользователя, пароль которого надо сбросить
     *
     * @return результат запроса
     */
    ServerResponse.RequestPasswordReset requestPasswordReset(String email) {
        if (connection == null)
            return ServerResponse.RequestPasswordReset.DB_NOT_SUPPORTED;

        removeAllExpiredRegistrationTokens();

        try {
            PreparedStatement emailCheckingStatement = connection.prepareStatement(
                    "select * from users where email = ?"
            );
            emailCheckingStatement.setString(1, email);
            ResultSet users = emailCheckingStatement.executeQuery();

            if (users.next()) {
                int userid = users.getInt("id");
                int token = generatePasswordResetToken(userid);

                sendMail(email, "Сброс пароля",
                        "<p style=\"text-align: center; padding: 25px;\">" +
                                "Ваш ID " +
                                "<b style=\"padding: 12px; margin: 12px; background: antiquewhite; border-radius: 3px\">" + userid + "</b></p>" +
                                "<p style=\"text-align: center; padding: 25px;\">" +
                                "Ваш код сброса пароля " +
                                "<b style=\"padding: 12px; margin: 12px; background: antiquewhite; border-radius: 3px\">" + token + "</b></p>");
                return ServerResponse.RequestPasswordReset.OK;
            } else
                return ServerResponse.RequestPasswordReset.EMAIL_NOT_EXIST;
        } catch (MessagingException e) {
            logger.err("Возникла ошибка отправки сообщения во время запроса сброса пароля: " + e.toString());
            return ServerResponse.RequestPasswordReset.INTERNAL_ERROR;
        } catch (SQLException e) {
            logger.err("Произошла ошибка SQL во время запроса сброса пароля: " + e.toString());
            return ServerResponse.RequestPasswordReset.INTERNAL_ERROR;
        }
    }

    /**
     * Позволяет сбросить пароль после получения токена
     *
     * @param userid id пользователя, который запросил сброс
     *
     * @param token токен, полученный после запроса сброса
     *
     * @return результат сброса
     */
    ServerResponse.ResetPassword resetPassword(int userid, int token, String newPassword) {
        if (connection == null)
            return ServerResponse.ResetPassword.DB_NOT_SUPPORTED;
        removeAllExpiredPasswordResetTokens();

        try {
            PreparedStatement statement = connection.prepareStatement(
                    "select * from password_reset_tokens where token = ? and userid = ?"
            );

            if (Utils.isPasswordTooWeak(newPassword) || newPassword.length() < 6)
                return ServerResponse.ResetPassword.INCORRECT_PASSWORD;

            statement.setInt(1, token);
            statement.setInt(2, userid);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                statement = connection.prepareStatement(
                        "update users set password_hash = ? where id = ?"
                );
                statement.setBytes(1, hashPassword(newPassword));
                statement.setInt(2, userid);
                statement.execute();

                statement = connection.prepareStatement(
                        "delete from password_reset_tokens where token = ? and userid = ?"
                );
                statement.setInt(1, token);
                statement.setInt(2, userid);
                statement.execute();
                return ServerResponse.ResetPassword.OK;
            } else
                return ServerResponse.ResetPassword.WRONG_TOKEN;
        } catch (GeneralSecurityException e) {
            logger.err("Во время сброса пароля произошла ошибка безопасности: " + e.toString());
            return ServerResponse.ResetPassword.INTERNAL_ERROR;
        } catch (SQLException e) {
            logger.err("Во время сброса пароля произошла ошибка SQL: " + e.toString());
            return ServerResponse.ResetPassword.INTERNAL_ERROR;
        }
    }

    ServerResponse.RemoveCreature removeCreature(CreatureModel creatureModel, int userid, int token) {
//        ServerResponse.RemoveCreature result = new ServerResponse.RemoveCreature();
//        if (connection == null) {
//            result.type = ServerResponse.RemoveCreature.ResponseType.DB_NOT_SUPPORTED;
//            return result;
//        }
//        String name = creatureModel.getName();
//        int x = creatureModel.getX();
//        int y = creatureModel.getY();
//        int width  = creatureModel.getWidth();
//        int height = creatureModel.getHeight();
//        try {
//            if (isUserAuthorized(userid, token, true)) {
//                PreparedStatement statement = connection.prepareStatement(
//                        "delete from creatures where " +
//                                "name = ? and x = ? and y = ? and width = ? and height = ? and " +
//                                "ownerid = ?"
//                );
//                statement.setString(1, name);
//                statement.setInt(2, x);
//                statement.setInt(3, y);
//                statement.setInt(4, width);
//                statement.setInt(5, height);
//                statement.setInt(6, userid);
//                int updates = statement.executeUpdate();
//                result.type = ServerResponse.RemoveCreature.ResponseType.OK;
//                result.updates = updates;
//                return result;
//
//            } else {
//                result.type = ServerResponse.RemoveCreature.ResponseType.NOT_AUTHORIZED;
//                return result;
//            }
//        } catch (SQLException e) {
//            logger.err("Произошла ошибка SQL при удалении существа: " + e.toString());
//            result.type = ServerResponse.RemoveCreature.ResponseType.INTERNAL_ERROR;
//            return result;
//        }
        return null;
        // TODO
    }

    ServerResponse.AddCreature addCreature(CreatureModel creatureModel, Integer userid, Integer token) {
//        if (connection == null)
//            return ServerResponse.AddCreature.DB_NOT_SUPPORTED;
//        if (userid == null || token == null)
//            return ServerResponse.AddCreature.NOT_AUTHORIZED;
//        String name = creatureModel.getName();
//        int x = creatureModel.getX();
//        int y = creatureModel.getY();
//        int width  = creatureModel.getWidth();
//        int height = creatureModel.getHeight();
//        Timestamp created = new Timestamp(1000L* creatureModel.getCreated().toEpochSecond());
//        try {
//            if (isUserAuthorized(userid, token, true)) {
//                PreparedStatement statement = connection.prepareStatement(
//                        "select count(*) from creatures where ownerid = ?"
//                );
//                statement.setInt(1, userid);
//                ResultSet resultSet = statement.executeQuery();
//                if (resultSet.next() && resultSet.getInt(1) >= config.getMaxUserElements())
//                    return ServerResponse.AddCreature.NOT_ENOUGH_SPACE;
//
//                statement = connection.prepareStatement(
//                        "insert into creatures " +
//                                "(name, x, y, width, height, ownerid, created) " +
//                                "values (?, ?, ?, ?, ?, ?, ?)"
//                );
//                statement.setString(1, name);
//                statement.setInt(2, x);
//                statement.setInt(3, y);
//                statement.setInt(4, width);
//                statement.setInt(5, height);
//                statement.setInt(6, userid);
//                statement.setTimestamp(7, created);
//                statement.execute();
//                return ServerResponse.AddCreature.OK;
//            } else
//                return ServerResponse.AddCreature.NOT_AUTHORIZED;
//        } catch (SQLException e) {
//            logger.err("Произошла ошибка SQL при добавлении существа: " + e.toString());
//            return ServerResponse.AddCreature.INTERNAL_ERROR;
//        }
        return null;
        // TODO
    }

    /**
     * Проверяет, авторизован ли пользователь.
     * Пользователь считается авторизованным, если предоставил верную пару id/токен и если его токен не устарел.
     * Если нет соединения с базой данных, возвращает false.
     *
     * @param userId id пользователя
     *
     * @param token токен пользователя
     *
     * @param updateToken если установлен в true, время жизни токена будет
     *                    продлено в соответствии с настройками сервера
     *
     * @return true, если пользователь авторизован
     */
    public boolean isUserAuthorized(int userId, int token, boolean updateToken) throws SQLException {
        if (connection == null)
            return false;
        PreparedStatement statement = connection.prepareStatement(
                "select * from user_tokens where userid = ? and token = ?"
        );
        statement.setInt(1, userId);
        statement.setInt(2, token);
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) {
            Timestamp expires = resultSet.getTimestamp("expires");

            if (expires.before(new Timestamp(System.currentTimeMillis())))
                return false;

            if (updateToken)
                updateUserToken(userId, token);
            return true;
        }
        return false;
    }

    public HashSet<Properties> generateOnlineUsersList() throws SQLException {
        ResultSet resultSet = connection.createStatement().executeQuery("" +
                "select * from user_tokens inner join users on users.id = user_tokens.userid"
        );
        HashSet<Properties> result = new HashSet<>();

        while (resultSet.next()) {
            Properties properties = new Properties();

            properties.setProperty("id", resultSet.getString("id"));
            properties.setProperty("name", resultSet.getString("name"));
            properties.setProperty("color", String.format("#%06x", resultSet.getInt("color888")));

            result.add(properties);
        }
        return result;
    }

    /**
     * Генерирует код для сброса пароля и сохраняет в базу данных. Если такой код уже есть,
     * обновляет его время жизни.
     *
     * Внимание! Функция не выполняет проверку времени жизни токена.
     * Перед вызовом этой функции необходимо вызвать {@link #removeAllExpiredPasswordResetTokens()}
     *
     * @param userid id пользователя, для которого следует получить код сброса
     *
     * @return новый или обновленный код сброса
     *
     * @throws SQLException если что-то пойдёт не так
     */
    public int generatePasswordResetToken(int userid) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "select * from password_reset_tokens where userid = ?"
        );
        statement.setInt(1, userid);
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) {
            statement = connection.prepareStatement(
                    "update password_reset_tokens set expires = ?"
            );
            statement.setTimestamp(1, new Timestamp(System.currentTimeMillis() + config.getPasswordResetTokenTimeout()));
            statement.execute();
            return resultSet.getInt("token");
        } else {
            statement = connection.prepareStatement(
                    "insert into password_reset_tokens VALUES (?, ?, ?)"
            );
            int token = generateRandomToken(6);
            statement.setInt(1, token);
            statement.setInt(2, userid);
            statement.setTimestamp(3, new Timestamp(System.currentTimeMillis() + config.getPasswordResetTokenTimeout()));
            statement.execute();
            return token;
        }
    }
    /**
     * Генерирует токе регистрации и добавляет его в базу данных
     *
     * @param name Имя регистрируемого пользователя
     *
     * @param email Email регистрируемого пользователя
     *
     * @param password Пароль регистрируемого пользователя
     *
     * @return сгенерированный токен
     *
     * @throws SQLException Если что-то пойдёт не так
     * @throws GeneralSecurityException Если что-то совсем пойдёт не так
     */
    public int generateRegistrationToken(String name, String email, String password) throws SQLException, GeneralSecurityException {
        PreparedStatement statement = connection.prepareStatement(
                "insert into registration_tokens VALUES (?, ?, ?, ?, ?)"
        );
        int token = generateRandomToken(6);
        statement.setInt(1, token);
        statement.setString(2, name);
        statement.setString(3, email);
        statement.setBytes(4, hashPassword(password));
        statement.setTimestamp(
                5,
                new Timestamp(System.currentTimeMillis() + config.getRegistrationTokenTimeout())
        );
        statement.execute();
        return token;
    }

    /**
     * Генерирует токен, состоящий из случайного числа с заданным количеством цифр
     *
     * @param digits количество цифр в токене
     *
     * @return токен
     */
    private int generateRandomToken(int digits) {
        return (int)(Math.pow(10, digits - 1) + Math.random()*(Math.pow(10, digits) - Math.pow(10, digits - 1) - 1));
    }

    /**
     * Хеширует и солит пароль
     *
     * @param password пароль для хеширования
     *
     * @return хешированный пароль
     *
     * @throws GeneralSecurityException Если что-то пойдёт совсем не так
     */
    public byte[] hashPassword(String password) throws GeneralSecurityException {
        return SCrypt.scrypt(password.getBytes(), PASSWORD_SALT, 16384, 8, 1, 32);
    }

    /**
     * Отправляет письмо на указанный электронный адрес
     *
     * @param to кому отправить
     *
     * @param subject тема письма
     *
     * @param content содержимое письма
     *
     * @throws MessagingException Если что-то пойдёт не так
     */
    public void sendMail(String to, String subject, String content) throws MessagingException {
        MimeMessage message = new MimeMessage(mailSession);
        message.setFrom(config.getEmailFrom());
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
//        message.setSubject(subject, "UTF16");
        message.setContent(content, "text/html; charset=utf-16");

        Transport.send(message);
    }
}