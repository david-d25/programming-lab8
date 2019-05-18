package ru.david.room.client;

import ru.david.room.*;
import ru.david.room.json.JSONEntity;
import ru.david.room.json.JSONObject;
import ru.david.room.json.JSONParseException;
import ru.david.room.json.JSONParser;

import java.io.*;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.nio.file.AccessDeniedException;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import static ru.david.room.GlobalConstants.ANSI_COLORS;

public class Client {
    private static ClientConfig config;
    private static UserInteractor interactor;

    private static Command previousCommand = null;

    private static long broadcastId = (long)(Math.random()*100000000);

    private static Runnable updatingRunnable = () -> {
        sendCommand("subscribe", broadcastId, true);

        while (!Thread.interrupted()) {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ignored) {}

            String response = sendCommand("pull_updates", broadcastId, true);
            String[] data = response.split(" ");
            if (data[0].equals("nop"))
                continue;
            try {
                if (Long.parseLong(data[1]) != broadcastId) {
                    switch (data[0]) {
                        case "logout":
                            interactor.alert(Utils.colorize("Пользователь [[blue]]" + data[1] + " [" + data[2] + "][[reset]] вышел"));
                            break;
                        case "login":
                            interactor.alert(Utils.colorize("Пользователь [[blue]]" + data[1] + " [" + data[2] + "][[reset]] вошел"));
                            break;
                        case "timeout":
                            interactor.alert(Utils.colorize("Пользователь [[blue]]" + data[1] + " [" + data[2] + "][[reset]] вышел по таймауту"));
                            break;
                    }
                } else {
                    switch (data[0]) {
                        case "logout":
                            interactor.alert("Вы вышли");
                            logout();
                            break;

                        case "login":
                            interactor.alert("Вы вошли");
                            break;
                        case "timeout":
                            interactor.alert("Вы вышли по таймауту");
                            logout();
                            break;
                    }
                }
            } catch (NumberFormatException ignored) {}
        }

        unsubscribe();
        sendCommand("unsubscribe", broadcastId, true);
    };

    private static Thread updatingThread;

    private static Integer token;
    private static Integer userId;
    private static String name;

    public static void main(String[] args) {
        try {
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
        } catch (UnsupportedEncodingException ignored) {}

        if (args.length > 0)
            initConfig(args[0]);
        else
            System.out.println("Файл настроек не указан\n");

        System.out.println();
        System.out.println(WelcomePhrases.getRandom());
        System.out.println();

        System.out.println("Введите команду:\n");

        interactor = new UserInteractor(System.in);

        subscribe();

        while (true) {
            try {
                String query = interactor.prompt(generatePromptMessage());
                if (query == null) return;
                String response = processCommand(query);
                if (response != null)
                    System.out.println(response);
//                System.out.println();
            } catch (IOException e) {
                System.out.println("Не удалось прочитать стандартный поток вввода: " + e.getMessage());
            }
        }
    }

    static String generatePromptMessage() {
        return name == null ? "> " : ANSI_COLORS.PURPLE + name + " [" + userId + "] > " + ANSI_COLORS.RESET;
    }

    /**
     * Выполняет первичную обработку команды. Может быть использован для команд,
     * выполняемых на клиенте без участия сервера
     *
     * @param query команда пользователя
     *
     * @return результат операции для вывода на экран
     *
     * @throws IOException Если что-то пойдет не так
     */
    private static String processCommand(String query) throws IOException {
        query = query.trim().replaceAll("\\s{2,}", " ");
        Command command = new Command(query);

        if (command.name.isEmpty())
            return "Введите команду";

        if (previousCommand == null)
            if (command.name.equals("again"))
                return Utils.colorize("again не должен быть первой командой. Введите [[blue]]help again[[reset]], чтобы узнать больше");
            else
                previousCommand = command;
        else if (command.name.equals("again"))
            command = previousCommand;
        else
            previousCommand = command;


        switch (command.name) {
            case "exit":
                System.exit(0);

            case "show":
                return doShow(command.argument);

            case "address":
                if (command.argument == null)
                    return "Адрес сервера: " + config.getServerHost() +
                            "\nЧтобы изменить адрес, введите его после команды";
                config.setServerHost(command.argument);
                return Utils.colorize("Установлен адрес [[blue]]" + config.getServerHost());

            case "port":
                if (command.argument == null)
                    return "Порт: " + config.getServerPort() +
                            "\nЧтобы изменить порт, введите его после команды";
                int newPort;
                try {
                    newPort = Integer.parseInt(command.argument);
                } catch (NumberFormatException e) {
                    newPort = -1;
                }
                if (newPort < 1 || newPort > 65535)
                    return "Порт должен быть числом от 1 до 65535";
                config.setServerPort(newPort);
                return Utils.colorize("Установлен порт [[blue]]" + config.getServerPort());

            case "add":
            case "remove":
            case "remove_greater":
                return doWithCreatureArgument(command.name, command.argument);

            case "reset_password":
                return doResetPassword(command.argument);

            case "register":
                if (command.argument == null)
                    return doRegister();
                else
                    try {
                        Integer intArg = new Integer(command.argument);
                        return sendCommand("accept_registration_token", intArg);
                    } catch (NumberFormatException e) {
                        return "Код подтверждения должен быть числом";
                    }

            case "change_password":
                return doChangePassword();

            case "login":
                return doLogin();

            case "logout":
                String result = sendCommand("logout", null, true);
                logout();
                return result;

            case "multiline":
                return Utils.colorize("Многострочные команды " + (interactor.toggleMultilineMode() ? "[[blue]]включены[[reset]]. " +
                        "Используйте " + UserInteractor.COMMAND_END_SYMBOL + " для завешения команды." : "[[blue]]выключены[[reset]]"));

            case "import":
                if (command.argument != null)
                    return doImport(command.argument);

                default:
                    return sendCommand(command.name, command.argument);
        }
    }

    /**
     * Выполняет первичную обработку команды reset_password
     *
     * @param argument аргумент команды
     *
     * @return строку для вывода на экран или null
     *
     * @throws IOException если что-то сломается
     */
    private static String doResetPassword(String argument) throws IOException {
        if (argument == null) {
            System.out.println("Сброс пароля");
            System.out.println("Введите 'q', чтобы выйти");

            String email = interactor.prompt("Email: ");
            if (email.equals("q"))
                return "Сброс пароля отменён";

            return sendCommand("request_password_reset", email, true);
        } else {
            int spaceIndex = argument.indexOf(" ");

            if (spaceIndex == -1)
                return "Должно быть два аргумента";

            String leftPiece = argument.substring(0, spaceIndex);
            String rightPiece = argument.substring(spaceIndex + 1);

            int userid, token;

            try {
                userid = Integer.parseInt(leftPiece);
                token = Integer.parseInt(rightPiece);
            } catch (NumberFormatException e) {
                return "Оба аргумента команды должны быть числом";
            }

            String password;

            while (true) {
                password = interactor.promptHidden("Новый пароль: ");
                if (password.length() < 6) {
                    System.out.println(Utils.colorize("Пароль должен быть не короче [[blue]]6[[reset]] символов"));
                    continue;
                }
                if (Utils.isPasswordTooWeak(password)) {
                    System.out.println("Даже первак разгадает ваш пароль. Подберите другой");
                    continue;
                }
                break;
            }

            return sendCommand("reset_password",
                    new String[] {Integer.toString(userid), Integer.toString(token), password},
                    true
            );
        }
    }

    private static void subscribe() {
        updatingThread = new Thread(updatingRunnable);
        updatingThread.start();
    }

    private static void unsubscribe() {
        if (updatingThread != null) {
            updatingThread.interrupt();
            updatingThread = null;
        }
    }

    /**
     * @return Конфигурация приложения
     */
    static ClientConfig getConfig() {
        return config;
    }

    /**
     * Удаляет сохранённые id пользователя и токен
     */
    private static void logout() {
        userId = null;
        token = null;
        name = null;
    }

    /**
     * @return Сохранённый ID пользователя
     */
    public static Integer getUserId() {
        return userId;
    }

    /**
     * @return Сохранённый токен пользователя
     */
    public static Integer getToken() {
        return token;
    }

    /**
     * Выполняет первичную обработку команды change_password
     *
     * @return строку для вывода на экран или null
     *
     * @throws IOException Если что-то пойдёт не так
     */
    private static String doChangePassword() throws IOException {
        System.out.println("Изменение пароля");
        System.out.println("Введите 'q', чтобы выйти");
        String oldPassword, newPassword;

        oldPassword = interactor.prompt("Текущий пароль: ");
        if (oldPassword.equals("q"))
            return "Изменение пароля отменено";
        newPassword = interactor.prompt("Новый пароль: ");
        if (newPassword.equals("q"))
            return "Изменение пароля отменено";

        return sendCommand("change_password", new String[] {oldPassword, newPassword});
    }

    /**
     * Выполняет первичную обработку команды login
     *
     * @return строку для вывода на экран или null
     *
     * @throws IOException Если что-то пойдёт не так
     */
    private static String doLogin() throws IOException {
        System.out.println("Вход в аккаунт");
        System.out.println("Введите 'q', чтобы выйти");
        String email, password;

        email = interactor.prompt("Email: ");
        if (email.equals("q"))
            return "Вход отменён";
        password = interactor.promptHidden("Пароль: ");
        if (password.equals("q"))
            return "Вход отменён";

        String response = sendCommand("login", new String[]{email, password}, true);
        try {
            JSONEntity responseEntity = JSONParser.parse(response);
            JSONObject responseObject = responseEntity.toObject(
                    "Сервер отправил данные в неверном формате " +
                            "(ожидается json-объект, получено: " + responseEntity.getTypeName() + ")"
            );
            if (responseObject.getItemNotNull("status", "Сервер не отправил статус ответа")
                    .toString("Сервер отправил статус ответа в неверном формате")
                    .getContent().equals("ok")) {
                Integer token = (int)responseObject.getItemNotNull("token", "Сервер не оправил токен")
                        .toNumber("Сервер отправил токен в неверном формате").getValue();
                Integer userId = (int)responseObject.getItemNotNull("userid", "Сервер не отправил user id")
                        .toNumber("Сервер отправил user id в неверном формате").getValue();
                String name = responseObject.getItemNotNull("name", "Сервер не отправил имя пользователя")
                        .toString("Сервер отправил имя пользователя в неверном формате").getContent();

                Client.token = token;
                Client.userId = userId;
                Client.name = name;

//                subscribe();

                return Utils.colorize("[[bright_green]]Вы вошли! Ваш ID: " + userId + "[[reset]]");
            } else
                return Utils.colorize("[[yellow]]" + responseObject.getItemNotNull("message", "Войти не удалось, но сервер не сказал почему")
                        .toString("Войти не удалось, но сервер отправил сообщение об ошибке в неверном формате").getContent() + "[[reset]]");
        } catch (IllegalStateException | NoSuchElementException e) {
            return e.getMessage();
        } catch (JSONParseException e) {
            return response;
        }
    }

    /**
     * Выполняет первичную обработку команды register на стороне клиента
     *
     * @return строку для вывода на экран или null
     *
     * @throws IOException если что-то пойдет не так
     */
    private static String doRegister() throws IOException {
        System.out.println("Регистрация нового пользователя");
        System.out.println("Введите 'q', чтобы выйти");

        String name, email, password, confirmPassword;

        while ((name = interactor.prompt("Имя: ")).length() < 2 && !name.equals("q"))
            System.out.println("Ваше имя не должно быть короче двух символов");
        if (name.equals("q"))
            return "Регистрация отменена";

        while ((email = interactor.prompt("Email: ")).length() == 0 || !Utils.isValidEmailAddress(email) && !email.equals("q"))
            System.out.println("Следует ввести валидный email");
        if (email.equals("q"))
            return "Регистрация отменена";

        while (true) {
            password = interactor.promptHidden("Пароль: ");
            if (password.equals("q"))
                return "Регистрация отменена";

            if (password.equals("you suck") && !name.equals("Ivan")) {
                System.out.println("Этот пароль можно использовать только избранным");
                continue;
            }
            if (password.equals(name)) {
                System.out.println(Utils.colorize("[[yellow]]Пароль не должен быть вашим именем[[reset]]"));
                continue;
            }
            if (password.equals(email)) {
                System.out.println(Utils.colorize("[[yellow]]Пароль не должен быть вашим адресом электронной почты[[reset]]"));
                continue;
            }
            if (password.length() < 6) {
                System.out.println(Utils.colorize("[[yellow]]Пароль должен содержать 6 или больше символов[[reset]]"));
                continue;
            }
            if (Utils.isPasswordTooWeak(password)) {
                System.out.println(Utils.colorize("[[yellow]]Даже ребёнок разгадает ваш пароль. Подберите более сложный[[reset]]"));
                continue;
            }
            break;
        }

        while (true) {
            confirmPassword = interactor.promptHidden("Повтоите пароль: ");
            if (confirmPassword.equals("q"))
                return "Регистрация отменена";

            if (!confirmPassword.equals(password)) {
                System.out.println("Пароли не совпадают");
                continue;
            }
            break;
        }

        return sendCommand("register", new String[] {name, email, password});
    }

    /**
     * Выполняет команду, аргумент которой является json-представлением экземпляра класса Creature
     *
     * @param command имя команжы
     *
     * @param jsonArgument аргумент команды
     *
     * @return результат выполнения
     */
    private static String doWithCreatureArgument(String command, String jsonArgument) {
        try {
            if (jsonArgument == null)
                return sendCommand(command, null);
            else {
                Creature[] creatures;
                try {
                    creatures = CreatureFactory.generate(jsonArgument);
                } catch (Exception e) {
                    return e.getMessage();
                }
                try (SocketChannel channel = SocketChannel.open()) {
                    channel.connect(new InetSocketAddress(config.getServerHost(), config.getServerPort()));
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos);

                    for (int i = 0; i < creatures.length; i++) {
                        // Making a Message instance and writing it to ByteArrayOutputStream
                        Message message = new Message(command, creatures[i], i + 1 == creatures.length);
                        message.setUserid(userId);
                        message.setToken(token);
                        oos.writeObject(message);
                    }

                    // Sending message using channel
                    ByteBuffer sendingBuffer = ByteBuffer.allocate(baos.size());
                    sendingBuffer.put(baos.toByteArray());
                    sendingBuffer.flip();
                    channel.write(sendingBuffer);

                    // Getting response
                    ObjectInputStream ois = new ObjectInputStream(channel.socket().getInputStream());
                    while (true) {
                        Message incoming = (Message) ois.readObject();
                        System.out.println(incoming.getMessage());
                        if (incoming.hasEndFlag())
                            break;
                    }
                } catch (UnresolvedAddressException e) {
                    return "Не удалось определить адрес сервера. Воспользуйтесь командой address, чтобы изменить адрес.";
                } catch (UnknownHostException e) {
                    return "Ошибка подключения к серверу: неизвестный хост. Воспользуйтесь командой address, чтобы изменить адрес";
                } catch (SecurityException e) {
                    return "Нет разрешения на подключение, проверьте свои настройки безопасности";
                } catch (ConnectException e) {
                    return "Нет соединения с сервером. Введите again, чтобы попытаться ещё раз, или измените адрес (команда address)";
                } catch (IOException e) {
                    return "Ошибка ввода-вывода: " + e;
                } catch (ClassNotFoundException e) {
                    return "Ошибка: клиент отправил данные в недоступном для клиента формате (" + e.getLocalizedMessage() + ")";
                }
            }
        } catch (Exception e) {
            return e.getMessage();
        }
        return "";
    }

    /**
     * Выполняет команлу show, вывод отправляет в System.out
     * @return пустую строку или сообщение (возможно, об ошибке)
     */
    private static String doShow(String argument) {
        try (SocketChannel channel = SocketChannel.open()) {
            channel.connect(new InetSocketAddress(config.getServerHost(), config.getServerPort()));

            if (argument != null && argument.equals("mine") && userId == null)
                return Utils.colorize("Сначала следует войти, введите [[blue]]login");

            // Creating a Message instance and writing it to ByteArrayOutputStream
            Message message = new Message("show", argument,true);
            message.setUserid(userId);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new ObjectOutputStream(baos).writeObject(message);

            // Sending the message to server using channel
            ByteBuffer byteBuffer = ByteBuffer.allocate(baos.size());
            byteBuffer.put(baos.toByteArray()).flip();
            channel.write(byteBuffer);

            ObjectInputStream ois = new ObjectInputStream(channel.socket().getInputStream());
            List<Creature> result = new LinkedList<>();

            // Reading the response
            while (!channel.socket().isClosed()) {
                Message incoming = (Message)ois.readObject();
                if (!incoming.hasAttachment())
                    break;
                if (incoming.getAttachment() instanceof Creature)
                    result.add((Creature)incoming.getAttachment());
                else
                    return "Сервер вернул данные в неверном формате";
                if (incoming.hasEndFlag())
                    break;
            }

            // Writing the response to System.out
            if (result.size() > 0) {
                if (argument != null && argument.equals("mine"))
                    System.out.println("Ваши существа:");
                else
                    System.out.println("Все существа: ");
                result.forEach(System.out::println);
            } else
                return "Тюряга пустая, господин";
            return "";
        } catch (UnresolvedAddressException e) {
            return "Не удалось определить адрес сервера. Воспользуйтесь командой address, чтобы изменить адрес.";
        } catch (UnknownHostException e) {
            return "Ошибка подключения к серверу: неизвестный хост. Воспользуйтесь командой address, чтобы изменить адрес";
        } catch (SecurityException e) {
            return "Нет разрешения на подключение, проверьте свои настройки безопасности";
        } catch (ConnectException e) {
            return "Нет соединения с сервером. Введите again, чтобы попытаться ещё раз, или измените адрес (команда address)";
        } catch (EOFException e) {
            return "";
        } catch (IOException e) {
            return "Ошибка ввода-вывода: " + e;
        } catch (ClassNotFoundException e) {
            return "Сервер отпавил класс, который не может прочитать клиент";
        }
    }

    /**
     * Формирует команду import и отправляет на сервер
     * @param filename имя файла, содержимое которого будет отправлено
     * @return пустую строку или сообщение об ошибке, если есть
     */
    private static String doImport(String filename) {
        try {
            String content = FileLoader.getFileContentInteractive(filename, true);
            return sendCommand("import", content);
        } catch (FileNotFoundException e) {
            return "Нет такого файла";
        } catch (AccessDeniedException e) {
            return "Нет доступа к файлу";
        } catch (IOException e) {
            return "Ошибка ввода-вывода: " + e.getMessage();
        } catch (Exception e) {
            return "Неизвестная ошибка: " + e.toString();
        }
    }

    /**
     * Работает как {@link #sendCommand(String, Serializable, boolean)}, но последний аргумент установлен в "false"
     *
     * @param name {@inheritDoc}
     *
     * @param argument {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    private static String sendCommand(String name, Serializable argument) {
        return sendCommand(name, argument, false);
    }

    /**
     * Отправляет команду на сервер, результат отправляет в {@link System#out},
     * использует каналы согласно условию задания
     *
     * @param name команда, которую нужно отправить
     *
     * @param argument аргумент команды
     *
     * @param returnAll заставляет возвратить весь результат вместо ого, чтобы отправить в {@link System#out}
     *
     * @return пустую строку или сообщение об ошибке, если есть
     */
    private static String sendCommand(String name, Serializable argument, boolean returnAll) {
        try (SocketChannel channel = SocketChannel.open()) {
            channel.connect(new InetSocketAddress(config.getServerHost(), config.getServerPort()));

            // Making a Message instance and writing it to ByteArrayOutputStream
            Message message = new Message(name, argument, true);
            message.setToken(token);
            message.setUserid(userId);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(message);

            // Sending message using channel
            ByteBuffer sendingBuffer = ByteBuffer.allocate(baos.size());
            sendingBuffer.put(baos.toByteArray());
            sendingBuffer.flip();
            new Thread(() -> {
                try {
                    channel.write(sendingBuffer);
                } catch (IOException e) {
                    System.out.println("Ошибка ввода/вывода: " + e.getMessage());
                }
            }).start();

            // Getting Message instance from response
            ObjectInputStream ois = new ObjectInputStream(channel.socket().getInputStream());
            if (returnAll) {
                StringBuilder builder = new StringBuilder();
                while (true) {
                    Message incoming = (Message) ois.readObject();
                    builder.append(incoming.getMessage());
                    if (incoming.hasEndFlag())
                        break;
                }
                return builder.toString();
            } else {
                while (true) {
                    Message incoming = (Message) ois.readObject();
                    System.out.println(incoming.getMessage());
                    if (incoming.hasEndFlag())
                        break;
                }
                return "";
            }
        } catch (UnresolvedAddressException e) {
            return "Не удалось определить адрес сервера. Воспользуйтесь командой address, чтобы изменить адрес.";
        } catch (UnknownHostException e) {
            return "Ошибка подключения к серверу: неизвестный хост. Воспользуйтесь командой address, чтобы изменить адрес";
        } catch (SecurityException e) {
            return "Нет разрешения на подключение, проверьте свои настройки безопасности";
        } catch (ConnectException e) {
            return "Нет соединения с сервером. Введите again, чтобы попытаться ещё раз, или измените адрес (команда address)";
        } catch (IOException e) {
            return "Ошибка ввода-вывода, обработка запроса прервана: " + e.toString();
        } catch (ClassNotFoundException e) {
            return "Ошибка: клиент отправил данные в недоступном для клиента формате (" + e.toString() + ")";
        }
    }

    /**
     * Приложение делает суецыд
     * @param message прощальное сообщение
     */
    private static void suicide(String message) {
        System.err.println(message);
        System.exit(-1);
    }

    /**
     * Конфигурирует клиент, загружая настройки из указанного файла
     * @param filename файл конфигурации
     */
    private static void initConfig(String filename) {
        System.out.println("Загрузка настроек...");
        try {
            config = ClientConfig.fromFile(filename);
        } catch (FileNotFoundException e) {
            suicide("Не удалось найти файл " + filename);
        } catch (AccessDeniedException e) {
            suicide("Нет получилось прочитать файл " + filename);
        } catch (IOException e) {
            suicide("Неизвестная ошибка ввода/вывода: " + e.toString());
        } catch (JSONParseException | NoSuchElementException | IllegalArgumentException | IllegalStateException e) {
            suicide(e.getMessage());
        }
        System.out.println("Настройки загружены");
    }
}