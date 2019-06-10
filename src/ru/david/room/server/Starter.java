package ru.david.room.server;

import ru.david.room.json.JSONParseException;

import java.io.*;
import java.nio.file.AccessDeniedException;
import java.util.NoSuchElementException;

/**
 * Слушай сюда, салага!
 * Этот класс является один из важнейших вещей не только в этой лабе,
 * но и, может, во всей вселенной. Без этого класса разные части этого костыльного
 * сервера никогда не соберутся и не заработают. Этот класс, как тот сумасшедший дядя Вася
 * из соседней парадной, запускает всё и вся, даже если это совершенно нерабочая рухлядь,
 * отважный класс-герой под незамысловатым именем {@link Starter} запустит этот мешок с болтами,
 * какой бы ценой это ему не обошлось! Это очень полезный и важный класс, уважай и чти его.
 */
public class Starter {
    public static void main(String[] args) {
        try {
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
        } catch (UnsupportedEncodingException ignored) {}

        if (args.length == 0)
            die("Следует указать файл настроек сервера");

        Hub hub = new Hub();

        ServerConfig config = initConfig(args[0]);

        if (config == null) {
            System.err.println("Совершенно внезапно config не настроился");
            System.exit(-1);
        }

        Logger logger = initLogger(config);

        ServerController controller = new ServerController();
        hub.setController(controller);
        controller.onHubConnected(hub);

        ClientPool pool = new ClientPool();
        hub.setClientPool(pool);
        pool.onHubConnected(hub);

        Server server = new Server();
        hub.setServer(server);
        server.onHubConnected(hub);

        RequestResolver requestResolver = new RequestResolver();
        hub.setRequestResolver(requestResolver);
        requestResolver.onHubConnected(hub);

        hub.setConfig(config);
        hub.setLogger(logger);

        requestResolver.onHubReady();
        controller.onHubReady();
        server.onHubReady();
        pool.onHubReady();
    }

    /**
     * Сервер выводит на экран прощадьный текст и делает суецыд
     * @param message предсмертное сообщение
     */
    private static void die(String message) {
        System.err.println(message);
        System.exit(-1);
    }

    /**
     * Инициализирует логгер для сервера.
     * При необходимости создаёт все директории, отсутствующие в пути создаваемых файлов.
     *
     * @param config настройки сервера
     *
     * @return логгер
     */
    private static Logger initLogger(ServerConfig config) {
        try {
            if (new File(config.getOutLogFile()).getParentFile().mkdirs())
                System.out.println("Автоматически созданы директории для создания файла стандартного вывода: " + config.getOutLogFile());
            if (new File(config.getErrLogFile()).getParentFile().mkdirs())
                System.out.println("Автоматически созданы директории для создания файла вывода ошибок: " + config.getErrLogFile());

            return new Logger(
                    new PrintStream(new TeeOutputStream(System.out, new FileOutputStream(config.getOutLogFile())), true, "UTF-8"),
                    new PrintStream(new TeeOutputStream(System.err, new FileOutputStream(config.getErrLogFile())), true, "UTF-8")
            );
        } catch (SecurityException e) {
            die("Ошибка безопасности: " + e);
        } catch (IOException e) {
            die("Ошибка записи логов: " + e.getMessage());
        }
        return null;
    }

    /**
     * Инициализирует конфигурирование сервера из файла
     *
     * @param filename имя файла, из которого следует прочитать конфигурацию
     *
     * @return настройки сервера
     */
    private static ServerConfig initConfig(String filename) {
        try {
            return ServerConfig.fromFile(filename);
        } catch (FileNotFoundException e) {
            die("Не удалось найти файл " + filename);
        } catch (AccessDeniedException e) {
            die("Нет получилось прочитать файл " + filename);
        } catch (IOException e) {
            die("Неизвестная ошибка ввода/вывода: " + e.toString());
        } catch (JSONParseException | NoSuchElementException | IllegalArgumentException | IllegalStateException e) {
            die(e.getMessage());
        }
        return null;
    }
}
