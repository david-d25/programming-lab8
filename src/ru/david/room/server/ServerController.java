package ru.david.room.server;

import java.sql.*;

class ServerController {
    private ServerConfig config;
    private Logger logger;
    private Connection connection;

    ServerController(ServerConfig c, Logger l) {
        config = c;
        logger = l;

        try {
            Class.forName(config.getJdbcDriver());
        } catch (ClassNotFoundException | NullPointerException e) {
            logger.err(
                    "Не удалось найти драйвер базы данных " + config.getJdbcDriver() + ". " +
                    "Сервер будет работать, но без поддержки некоторых команд."
            );
        }

        initConnection();
        initTables();
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
                "id serial primary key, name varchar, x integer, y integer, width integer, height integer, owner integer, created timestamp"
        );
    }

    /**
     * Создаёт таблицу с указанными именем и структурой, если её ещё нет.
     * К имени таблицы автоматически прибавляется префикс из конфигурации сервера.
     * @param name имя таблицы без префикса
     * @param structure описание структуры таблицы в sql-формате (имена колонн и их типы)
     */
    private void autoCreateTable(String name, String structure) {
        String prefix = config.getTableNamesPrefix();
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            if (
                    !metaData.getTables(
                            null,
                            null,
                            prefix + "creatures",
                            new String[]{"TABLE"}
                    ).next()
            ) {
                connection.createStatement().execute("create table if not exists " + prefix + "creatures (" + structure + ")");
                logger.log("Создана таблица " + prefix + "creatures");
            }
        } catch (SQLException e) {
            logger.err("Не получилось создать таблицу " + name + ": " + e.getMessage());
        }
    }

    /**
     * @return Информация о сервере в читабельном виде
     */
    String getInfo() {
        return "Test string";
    }

    // TODO: Register
    // TODO: Login
    // TODO: Accept register token
    // TODO: Reset password
    // TODO: Add & Remove & Remove_greater & Remove_less & Remove_all
    // TODO: Subscribe_to_changes
    // TODO: Unsubscribe_from_changes
}
