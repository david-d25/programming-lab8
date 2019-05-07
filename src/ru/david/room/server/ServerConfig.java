package ru.david.room.server;

import ru.david.room.FileLoader;
import ru.david.room.json.JSONEntity;
import ru.david.room.json.JSONObject;
import ru.david.room.json.JSONParseException;
import ru.david.room.json.JSONParser;

import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * Этот класс используется для управления настройками сервера
 */
public class ServerConfig {
    private ServerConfig() {}

    private int port;

    private int maxRequestSize;
    private int maxLoggableRequestSize;
    private int maxUserElements;

    private String databaseHost;
    private int databasePort;

    private String databaseName;
    private String tableNamesPrefix;

    private String databaseUser;
    private String databasePassword;

    private long registrationTokenTimeout;
    private long userTokenTimeout;

    private String outLogFile;
    private String errLogFile;

    private String jdbcDriver;
    private String jdbcLangProtocol;

    /**
     * Возвращает новый экземпляр ServerConfig, в который загружены
     * настройки из указанного файла. Файл должен быть в формате JSON.
     * @param filename файл, из которого следует загрузить настройки
     * @return экземпляр ServerConfig с настройками из файла
     * @throws IOException В случае ошибки ввода/вывода
     * @throws JSONParseException В случае синтаксической ошибки json
     * @throws NoSuchElementException Когда отсутствует какой-то обязательный параметр
     * @throws IllegalArgumentException Когда какой-то параметр имеет неверное значение
     * @throws IllegalStateException Когда какой-то параметр имеет неверный тип
     */
    static ServerConfig fromFile(String filename) throws IOException, JSONParseException, NoSuchElementException, IllegalArgumentException, IllegalStateException {
        ServerConfig result = new ServerConfig();

        String configContent = FileLoader.getFileContent(filename);

        JSONObject object = JSONParser.parse(configContent).toObject(
                "Файл должен содержать объект в формате JSON"
        );

        // Getting entities
        JSONEntity portEntity = object.getItemNotNull(
                "port",
                "Порт не указан, укажите его в параметре 'port'"
        );

        JSONEntity maxRequestSizeEntity = object.getItemNotNull(
                "max_request_size",
                "Макс. размер запроса не указан, укажите его в параметре 'max_request_size'"
        );
        JSONEntity maxLoggableRequestSizeEntity = object.getItemNotNull(
                "max_loggable_request_size",
                "Макс. логгируемый размер запроса не указан, укажите его в параметре 'max_loggable_request_size'"
        );
        JSONEntity maxUserElementsEntity = object.getItemNotNull(
                "max_user_elements",
                "Макс. кол-во пользовательских элементов не указан, укажите его в параметре 'max_collection_elements'"
        );

        JSONEntity databaseHostEntity = object.getItemNotNull(
                "db_host",
                "Хост базы данных не указан, укажите его в параметре 'db_host'"
        );
        JSONEntity databasePortEntity = object.getItemNotNull(
                "db_port",
                "Порт базы данных не указан, укажите его в параметре 'db_port'"
        );

        JSONEntity databaseNameEntity = object.getItemNotNull(
                "db_name",
                "Имя базы данных не указано, укажите его в параметре 'db_name'"
        );
        JSONEntity tableNamesPrefixEntity = object.getItemNotNull(
                "table_names_prefix",
                "Префикс таблиц не указан, укажите его в параметре 'table_names_prefix'. " +
                        "Если вы не хотите устанавливать префикс именам таблиц, укажите пустую строку."
        );

        JSONEntity databaseUserEntity = object.getItemNotNull(
                "db_user",
                "Имя пользователя базы данных не указано, укажите его в параметре 'db_user'"
        );
        JSONEntity databasePasswordEntity = object.getItemNotNull(
                "db_password",
                "Пароль базы данных не указан, укажите его в параметре 'db_password'. " +
                        "Если пароль не требуется для входа, укажите пустую строку."
        );

        JSONEntity registrationTokenTimeoutEntity = object.getItemNotNull(
                "registration_token_timeout",
                "Время жизни токена регистрации не указано, укажите его в параметре 'registration_token_timeout'"
        );
        JSONEntity userTokenTimeoutEntity = object.getItemNotNull(
                "user_token_timeout",
                "Время жизни токена пользователя не указано, укажите его в параметре 'user_token_timeout'"
        );

        JSONEntity outLogFileEntity = object.getItemNotNull(
                "out_log_file",
                "Файл вывода сервера не указан, укажите его имя в параметре 'out_log_file'"
        );
        JSONEntity errLogFileEntity = object.getItemNotNull(
                "err_log_file",
                "Файл вывода ошибок сервера не указан, укажите его имя в параметре 'err_log_file'"
        );

        JSONEntity jdbcDriverEntity = object.getItemNotNull(
                "jdbc_driver",
                "Драйвер базы данных не указан, укажите его в параметре 'jdbc_driver'"
        );
        JSONEntity jdbcLangProtocolEntity = object.getItemNotNull(
                "jdbc_lang_protocol",
                "Протокол языка базы данных не указан, укажите его в параметре 'jdbc_lang_protocol'"
        );

        // Extracting Java-type variables
        int port = (int)portEntity.toNumber(
                "Порт должен быть числом, но это " + portEntity.getTypeName() + ".\n" +
                        "Проверьте значение параметра 'port'"
        ).getValue();

        int maxRequestSize = (int)maxRequestSizeEntity.toNumber(
                "Макс. размер запроса должен быть числом, но это " + maxRequestSizeEntity.getTypeName() + ".\n" +
                        "Проверьте значение параметра 'max_request_size'"
        ).getValue();

        int maxLoggableRequestSize = (int)maxLoggableRequestSizeEntity.toNumber(
                "Макс. логгируемый размер запроса должен быть числом, но это " + maxLoggableRequestSizeEntity.getTypeName() + ".\n" +
                        "Проверьте значение параметра 'max_loggable_request_size'"
        ).getValue();

        int maxUserElements = (int)maxUserElementsEntity.toNumber(
                "Макс. размер коллекции должен быть числом, но это " + maxUserElementsEntity.getTypeName() + ".\n" +
                        "Проверьте значение параметра 'max_collection_elements'"
        ).getValue();

        String databaseHost = databaseHostEntity.toString(
                "Хост базы данных должен быть строкой, но это " + databaseHostEntity.getTypeName() + ".\n" +
                        "Проверьте значение параметра 'db_host'"
        ).getContent();

        int databasePort = (int)databasePortEntity.toNumber(
                "Порт базы данных должен быть числом, но это " + databasePasswordEntity.getTypeName() + ".\n" +
                        "Проверьте значение параметра 'db_port'"
        ).getValue();

        String databaseName = databaseNameEntity.toString(
                "Имя базы данных должно быть строкой, но это " + databaseNameEntity.getTypeName() + ".\n" +
                        "Проверьте значение параметра 'db_name'"
        ).getContent();

        String tableNamesPrefix = tableNamesPrefixEntity.toString(
                "Префикс таблиц базы данных должен быть строкой, но это " + tableNamesPrefixEntity.getTypeName() + ".\n" +
                        "Проверьте значение параметра 'table_names_prefix'"
        ).getContent();

        String databaseUser = databaseUserEntity.toString(
                "Имя пользователя базы данных должно быть строкой, но это " + databaseUserEntity.getTypeName() + ".\n" +
                        "Проверьте значение параметра 'db_user'"
        ).getContent();

        String databasePassword = databasePasswordEntity.toString(
                "Пароль пользователя базы данных должен быть строкой, но это " + databasePasswordEntity.getTypeName() + ".\n" +
                        "Проверьте значение парамтра 'db_password'"
        ).getContent();

        long registrationTokenTimeout = (long)registrationTokenTimeoutEntity.toNumber(
                "Время жизни токена регистрации должно быть числом, но это " + registrationTokenTimeoutEntity.getTypeName() + ".\n" +
                        "Проверьте значение параметра 'registration_token_timeout'"
        ).getValue();

        long userTokenTimeout = (long)userTokenTimeoutEntity.toNumber(
                "Время жизни токена пользователя должно быть числом, но это " + userTokenTimeoutEntity.getTypeName() + ".\n" +
                        "Проверьте значение параметра 'user_token_timeout'"
        ).getValue();

        String outLogFile = outLogFileEntity.toString(
                "Имя файла журнала сервера должно быть строкой, но это " + outLogFileEntity.getTypeName() + ".\n" +
                        "Проверьте значение параметра 'out_log_file'"
        ).getContent();

        String errLogFile = errLogFileEntity.toString(
                "Имя файла ошибок сервера должно быть строкой, но это " + errLogFileEntity.getTypeName() + ".\n" +
                        "Проверье значение параметра 'err_log_file'"
        ).getContent();

        String jdbcDriver = jdbcDriverEntity.toString(
                "Драйвер базы данных должен быть строкой, но это " + jdbcDriverEntity.getTypeName() + ".\n" +
                        "Проверье значение параметра 'jdbc_driver'"
        ).getContent();

        String jdbcLangProtocol = jdbcLangProtocolEntity.toString(
                "Язык протокола базы данных должен быть строкой, но это " + jdbcLangProtocolEntity.getTypeName() + ".\n" +
                        "Проверье значение параметра 'jdbc_lang_protocol'"
        ).getContent();

        // Setting variables
        result.setPort(port);
        result.setMaxRequestSize(maxRequestSize);
        result.setMaxLoggableRequestSize(maxLoggableRequestSize);
        result.setMaxUserElements(maxUserElements);
        result.setDatabaseHost(databaseHost);
        result.setDatabasePort(databasePort);
        result.setDatabaseName(databaseName);
        result.setTableNamesPrefix(tableNamesPrefix);
        result.setDatabaseUser(databaseUser);
        result.setDatabasePassword(databasePassword);
        result.setRegistrationTokenTimeout(registrationTokenTimeout);
        result.setUserTokenTimeout(userTokenTimeout);
        result.setOutLogFile(outLogFile);
        result.setErrLogFile(errLogFile);
        result.setJdbcDriver(jdbcDriver);
        result.setJdbcLangProtocol(jdbcLangProtocol);

        return result;
    }

    /**
     * @return Порт, который следует слушать серверу
     */
    public int getPort() {
        return port;
    }

    /**
     * Устанавливает порт, который следует слушать серверу.
     * Порт должен находиться в пределах от 1 до 65535.
     * @param port номер порта
     */
    public void setPort(int port) {
        if (port < 1 || port > 65535)
            throw new IllegalArgumentException("Порт должен быть в пределах от 1 до 65535");
        this.port = port;
    }

    /**
     * @return Максимальный размер запроса, который следует обрабатывать серверу
     */
    public int getMaxRequestSize() {
        return maxRequestSize;
    }

    public void setMaxRequestSize(int maxRequestSize) {
        this.maxRequestSize = maxRequestSize;
    }

    /**
     * Возвращает максимальный размер запроса, который следует отображать в логах.
     * Если размер запроса больше указанного значения, в логах рекомендуется
     * указать лишь размер запроса.
     * @return Максимальный размер логгируемого запроса
     */
    public int getMaxLoggableRequestSize() {
        return maxLoggableRequestSize;
    }

    public void setMaxLoggableRequestSize(int maxLoggableRequestSize) {
        this.maxLoggableRequestSize = maxLoggableRequestSize;
    }

    /**
     * @return максимальное количество элементов коллекции, которое следует
     * сохранять серверу
     */
    public int getMaxUserElements() {
        return maxUserElements;
    }

    public void setMaxUserElements(int maxUserElements) {
        this.maxUserElements = maxUserElements;
    }

    /**
     * @return хост базы данных к которой следует подключаться серверу
     */
    public String getDatabaseHost() {
        return databaseHost;
    }

    public void setDatabaseHost(String databaseHost) {
        this.databaseHost = databaseHost;
    }

    /**
     * @return порт базы данных, к которой следует подключаться серверу
     */
    public int getDatabasePort() {
        return databasePort;
    }

    /**
     * Устанавливает порт базы данных, к которой следует подключиться серверу
     * @param databasePort номер порта
     */
    public void setDatabasePort(int databasePort) {
        if (port < 1 || port > 65535)
            throw new IllegalArgumentException("Порт должен быть в пределах от 1 до 65535");
        this.databasePort = databasePort;
    }

    /**
     * @return Имя базы данных, к которой следует подключиться серверу
     */
    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    /**
     * @return Префикс имён таблиц, используемых сервером
     */
    public String getTableNamesPrefix() {
        return tableNamesPrefix;
    }

    public void setTableNamesPrefix(String tableNamesPrefix) {
        this.tableNamesPrefix = tableNamesPrefix;
    }

    /**
     * @return Имя пользователя базы данных, с которым работает сервер
     */
    public String getDatabaseUser() {
        return databaseUser;
    }

    public void setDatabaseUser(String databaseUser) {
        this.databaseUser = databaseUser;
    }

    /**
     * @return Пароль базы данных, с которым работает сервер
     */
    public String getDatabasePassword() {
        return databasePassword;
    }

    public void setDatabasePassword(String databasePassword) {
        this.databasePassword = databasePassword;
    }

    /**
     * @return Время жизни токена регистрации
     */
    public long getRegistrationTokenTimeout() {
        return registrationTokenTimeout;
    }

    public void setRegistrationTokenTimeout(long registrationTokenTimeout) {
        this.registrationTokenTimeout = registrationTokenTimeout;
    }

    /**
     * @return Время жизни токена пользователя
     */
    public long getUserTokenTimeout() {
        return userTokenTimeout;
    }

    public void setUserTokenTimeout(long userTokenTimeout) {
        this.userTokenTimeout = userTokenTimeout;
    }

    /**
     * @return Имя файла, в который будет записан стандартный лог сервера
     */
    public String getOutLogFile() {
        return outLogFile;
    }

    public void setOutLogFile(String outLogFile) {
        this.outLogFile = outLogFile;
    }

    /**
     * @return Имя файла, в который будут записаны ошибки сервера
     */
    public String getErrLogFile() {
        return errLogFile;
    }

    public void setErrLogFile(String errLogFile) {
        this.errLogFile = errLogFile;
    }

    /**
     * @return Драйвер базы данных
     */
    public String getJdbcDriver() {
        return jdbcDriver;
    }

    public void setJdbcDriver(String jdbcDriver) {
        this.jdbcDriver = jdbcDriver;
    }

    /**
     * @return Протокол языка базы данных, который следует использовать при подключении
     */
    public String getJdbcLangProtocol() {
        return jdbcLangProtocol;
    }

    public void setJdbcLangProtocol(String jdbcLangProtocol) {
        this.jdbcLangProtocol = jdbcLangProtocol;
    }
}
