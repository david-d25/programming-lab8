package ru.david.room.client;

import ru.david.room.FileLoader;
import ru.david.room.json.JSONEntity;
import ru.david.room.json.JSONObject;
import ru.david.room.json.JSONParseException;
import ru.david.room.json.JSONParser;

import java.io.IOException;
import java.util.NoSuchElementException;

class ClientConfig {
    private ClientConfig() {}

    private String serverHost;
    private int serverPort;

    /**
     * Возвращает новый экземпляр {@link ClientConfig}, в который загружены
     * настройки из указанного файла. Файл должен быть в формате JSON.
     * @param filename файл, из которого следует загрузить настройки
     * @return экземпляр {@link ClientConfig} с настройками из файла
     * @throws IOException В случае ошибки ввода/вывода
     * @throws JSONParseException В случае синтаксической ошибки json
     * @throws NoSuchElementException Когда отсутствует какой-то обязательный параметр
     * @throws IllegalArgumentException Когда какой-то параметр имеет неверное значение
     * @throws IllegalStateException Когда какой-то параметр имеет неверный тип
     */
    static ClientConfig fromFile(String filename) throws IOException, JSONParseException, NoSuchElementException, IllegalArgumentException, IllegalStateException {
        ClientConfig config = new ClientConfig();

        String configContent = FileLoader.getFileContent(filename);

        JSONObject object = JSONParser.parse(configContent).toObject(
                "Файл должен содержать объект в формате JSON"
        );

        // Getting entities
        JSONEntity serverHostEntity = object.getItemNotNull(
                "server_host",
                "Хост сервера не указан. Укажите его в параметре 'server_host'"
        );
        JSONEntity serverPortEntity = object.getItemNotNull(
                "server_port",
                "Порт сервера не указан. Укажите его в параметре 'server_port'"
        );

        // Extracting variables
        String serverHost = serverHostEntity.toString(
                "Хост сервера должен быть строкой, но это " + serverHostEntity.getTypeName() + "\n" +
                        "Проверьте значение параметра 'server_host'"
        ).getContent();

        int serverPort = (int)serverPortEntity.toNumber(
                "Порт сервера должен быть числом, но это " + serverPortEntity.getTypeName() + "\n" +
                        "Проверьте значение параметра 'server_port'"
        ).getValue();

        // Setting variables
        config.setServerHost(serverHost);
        config.setServerPort(serverPort);

        return config;
    }

    /**
     * @return хост сервера, к которому следует подключиться
     */
    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String host) {
        this.serverHost = host;
    }

    /**
     * @return порт сервера, по которому следует подключиться
     */
    public int getServerPort() {
        return serverPort;
    }

    /**
     * Устанавливает порт сервера, по которому следует подключиться.
     * Порт должен находиться в пределах от 1 до 65535.
     * @param port номер порта
     */
    public void setServerPort(int port) {
        if (port < 1 || port > 65535)
            throw new IllegalArgumentException("Порт должен быть в пределах от 1 до 65535");
        this.serverPort = port;
    }
}
