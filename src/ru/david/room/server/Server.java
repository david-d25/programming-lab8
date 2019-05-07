package ru.david.room.server;

import ru.david.room.json.JSONParseException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.AccessDeniedException;
import java.util.NoSuchElementException;

/**
 * Главный класс сервера. Содержит точку входа.
 */
public class Server {
    private static ServerConfig config;

    private static ServerSocket serverSocket;
    private static Logger logger;

    public static void main(String[] args) {
        try {
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
        } catch (UnsupportedEncodingException ignored) {}

        if (args.length == 0)
            die("Следует указать файл настроек сервера");
        initConfig(args[0]);
        initLogger();

        ServerController controller = new ServerController(config, logger);

        try {
            serverSocket = new ServerSocket(config.getPort());
            logger.log("Сервер запущен и слушает порт " + config.getPort() + "...");
        } catch (IOException e) {
            logger.err("Ошибка создания серверного сокета (" + e.getMessage() + "), приложение будет остановлено.");
            System.exit(1);
        }
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                RequestResolver.resolveAsync(clientSocket, controller, logger);
            } catch (IOException e) {
                logger.err("Ошибка подключения: " + e.getMessage());
            }
        }
    }

    /**
     * Инициализирует логгер для сервера.
     * Перед вызовом этого метода сервер должен быть сконфигурирован (должен быть вызван {@link Server#initConfig(String)}).
     * При необходимости создаёт все директории, отсутствующие в пути создаваемых файлов.
     */
    private static void initLogger() {
        try {
            if (new File(config.getOutLogFile()).getParentFile().mkdirs())
                System.out.println("Автоматически созданы директории для создания файла стандартного вывода: " + config.getOutLogFile());
            if (new File(config.getErrLogFile()).getParentFile().mkdirs())
                System.out.println("Автоматически созданы директории для создания файла вывода ошибок: " + config.getErrLogFile());

            logger = new Logger(
                    new PrintStream(new TeeOutputStream(System.out, new FileOutputStream(config.getOutLogFile())), true, "UTF-8"),
                    new PrintStream(new TeeOutputStream(System.err, new FileOutputStream(config.getErrLogFile())), true, "UTF-8")
            );
        } catch (SecurityException e) {
            die("Ошибка безопасности: " + e);
        } catch (IOException e) {
            die("Ошибка записи логов: " + e.getMessage());
        }
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
     * Инициализирует конфигурирование сервера из файла
     * @param filename имя файла, из которого следует прочитать конфигурацию
     */
    private static void initConfig(String filename) {
        System.out.println("Настройка сервера...");
        try {
            config = ServerConfig.fromFile(filename);
        } catch (FileNotFoundException e) {
            die("Не удалось найти файл " + filename);
        } catch (AccessDeniedException e) {
            die("Нет получилось прочитать файл " + filename);
        } catch (IOException e) {
            die("Неизвестная ошибка ввода/вывода: " + e.toString());
        } catch (JSONParseException | NoSuchElementException | IllegalArgumentException | IllegalStateException e) {
            die(e.getMessage());
        }
        System.out.println("Сервер настроен");
    }
}