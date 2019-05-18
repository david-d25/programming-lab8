package ru.david.room.server;

import ru.david.room.Creature;
import ru.david.room.Message;
import ru.david.room.Utils;
import ru.david.room.json.JSONObject;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Решатель запроса. Обрабатывает один запрос от клиента.
 * Может принимать запросы, состоящие из нескольких сообщений,
 * тогда команда из каждого сообщения будет выполнена отдельно.
 * Порядок ответов сервера всегда соответствует порядку отправленных сообщений.
 */
class RequestResolver implements Runnable {
    private ServerController controller;

    private ObjectOutputStream out;
    private ObjectInputStream ois;
    private Socket socket;
    private Logger logger;

    private RequestResolver(Socket s, ServerController c, Logger l) {
        try {
            out = new ObjectOutputStream(s.getOutputStream());
            ois = new ObjectInputStream(
                    new LimitedInputStream(s.getInputStream(), Server.getConfig().getMaxRequestSize())
            );

            socket = s;
            logger = l;
            controller = c;

        } catch (IOException e) {
            l.err("Ошибка создания решателя запроса: " + e.toString());
        }
    }

    /**
     * Решает запрос в отдельном потоке
     * @param socket сокет клиента
     * @param controller контроллер, с которым будет выполняться взаимодействие в ходе обработки запроса
     * @param logger логгер, с которым будет выполняться взаимодействие в ходе обработки запроса
     */
    static void resolveAsync(Socket socket, ServerController controller, Logger logger) {
        new Thread(new RequestResolver(socket, controller, logger)).start();
    }

    /**
     * Собирает сообщения от клиента, чтобы начать их обработку
     */
    @Override
    public void run() {
        try {
            List<Message> messages = new LinkedList<>();

            while (true) {
                Object incoming = ois.readObject();

                if (incoming instanceof Message) {
                    messages.add((Message) incoming);
                    if (((Message) incoming).hasEndFlag())
                        break;
                } else {
                    sendEndMessage("Клиент отправил данные в неверном формате: получен экземпляр " + incoming.getClass().getName());
                    return;
                }
            }

            if (messages.size() == 1) {
                Message message = messages.get(0);
                if (message.getMessage().length() <= Server.getConfig().getMaxLoggableRequestSize())
                    logger.log("Запрос от " + socket.getInetAddress() + ": " + message.getMessage());
                else
                    logger.log("Запрос от " + socket.getInetAddress() + ", размер запроса: " + Utils.optimalInfoUnit(message.getMessage().length()));

                processMessage(message);
            } else {
                logger.log("Запрос из " + messages.size() + " сообщений от " + socket.getInetAddress());
                for (int i = 0; i < messages.size(); i++)
                    processMessage(messages.get(i), i + 1 == messages.size());
            }

        } catch (LimitAchievedException e) {
            logger.err("Клиент (" + socket.getInetAddress() + ") отправил слишком много данных, в запросе отказано");
            sendEndMessage("Ваш запрос слишком большой, он должен быть не больше " + Utils.optimalInfoUnit(Server.getConfig().getMaxRequestSize()));
        } catch (EOFException e) {
            logger.err("Сервер наткнулся на неожиданный конец");
            sendEndMessage("Не удалось обработать ваш запрос: в ходе чтения запроса сервер наткнулся на неожиданный конец данных");
        } catch (IOException e) {
            logger.err("Ошибка исполнения запроса: " + e.toString());
            sendEndMessage("На сервере произошла ошибка: " + e.toString());
        } catch (ClassNotFoundException e) {
            sendEndMessage("Клиент отправил данные в неверном формате");
        } catch (InternalError e) {
            logger.err("Внутренняя ошибка сервера: " + e.toString());
            sendEndMessage("Внутренняя ошибка сервера");
        }
    }

    /**
     * Отправляет сообщение, отмеченное как последнее
     * @param message текст сообщения
     */
    private void sendEndMessage(String message) {
        sendMessage(message, true);
    }

    /**
     * Отправляет сообщение с указанным флагом окончания
     * @param message текст сообщения
     * @param endFlag флаг окончания
     */
    private void sendMessage(String message, boolean endFlag) {
        try {
            out.writeObject(new Message(message, endFlag));
        } catch (IOException e) {
            logger.log("Ошибка отправки данных клиенту: " + e.getMessage());
        }
    }

    /**
     * Обрабатывает сообщение, отправляемый клиенту результат будет отмечен как последний
     * @param message сообщение
     */
    private void processMessage(Message message) {
        processMessage(message, true);
    }

    /**
     * Обрабатывает сообщение
     * @param message сообщение
     * @param endFlag если он true, результат обработки отправится клиенту как последний
     */
    private void processMessage(Message message, boolean endFlag) {
        if (message == null) {
            sendMessage("Задан пустой запрос", endFlag);
            return;
        }

        switch (message.getMessage()) {
            case "info":
                sendMessage(controller.getInfo(), endFlag);
                return;

            case "subscribe":
                if (message.hasAttachment() && message.getAttachment() instanceof Long) {
                    controller.getBroadcastingController().addListener((Long) message.getAttachment());
                    sendEndMessage("");
                }
                else
                    logger.log("subscribe error");
                return;

            case "unsubscribe":
                if (message.hasAttachment() && message.getAttachment() instanceof Long) {
                    controller.getBroadcastingController().removeListener((Long) message.getAttachment());
                    sendEndMessage("");
                }
                else
                    logger.log("unsubscribe error");
                return;


            case "pull_updates":
                if (message.hasAttachment() && message.getAttachment() instanceof Long) {
                    BroadcastingController.BroadcastingListener listener = controller.getBroadcastingController().getListener((Long)message.getAttachment());
                    if (listener != null)
                        listener.pullUpdates(out);
                } else
                    logger.log("pull_updates error");
                return;

            case "show":
                try {
                    Creature[] creatures;
                    if (message.hasAttachment() && message.getAttachment().equals("mine"))
                        creatures = controller.showCreatures(message.getUserid());
                    else
                        creatures = controller.showCreatures(null);
                    Arrays.sort(creatures);
                    for (int i = 0, creaturesLength = creatures.length; i < creaturesLength; i++)
                        out.writeObject(new Message("", creatures[i], i+1 == creaturesLength));
                    if (creatures.length == 0)
                        out.writeObject(new Message("", null));
                } catch (IOException e) {
                    logger.log("Ошибка исполнения запроса show: " + e.getLocalizedMessage());
                }
                return;

//            case "load":
//                if (!message.hasArgument()) {
//                    sendMessage("Имя не указано.\n" +
//                            "Введите \"help load\", чтобы узнать, как пользоваться командой", endFlag);
//                    return;
//                }
//                try {
//                    if (!(message.getAttachment() instanceof String)) {
//                        sendMessage("Клиент отправил запрос в неверном формате (аргумент сообщения должен быть строкой)", endFlag);
//                        return;
//                    }
//                    PleaseWaitMessage pleaseWaitMessage = new PleaseWaitMessage(out,
//                            "Ваш запрос в процессе обработки. Пожалуйста, подождите...",
//                            2000);
//                    hoosegow.clear();
//                    HoosegowStateController.loadState(
//                            hoosegow,
//                            FileLoader.getFileContentInteractive((String)message.getAttachment(), false)
//                    );
//                    pleaseWaitMessage.clear();
//                    sendMessage("Загрузка успешна! В тюряге " + hoosegow.getSize() + " существ", endFlag);
//                } catch (AccessDeniedException e) {
//                    sendMessage("Нет доступа для чтения", endFlag);
//                } catch (FileNotFoundException e) {
//                    sendMessage("Файл не найден", endFlag);
//                } catch (IOException e) {
//                    sendEndMessage("На сервере произошла ошибка чтения/записи");
//                } catch (SAXException | ParserConfigurationException e) {
//                    sendMessage("Ошибка обработки файла: " + e.getLocalizedMessage(), endFlag);
//                } catch (HoosegowOverflowException e) {
//                    sendMessage("В тюряге не осталось места, некоторые существа загрузились", endFlag);
//                }
//                return;
//
//            case "import":
//                if (!message.hasArgument()) {
//                    sendMessage("Имя не указано.\n" +
//                            "Введите \"help import\", чтобы узнать, как пользоваться командой", endFlag);
//                    return;
//                }
//                try {
//                    if (!(message.getAttachment() instanceof String)) {
//                        sendMessage("Клиент отправил запрос в неверном формате (аргумент сообщения должен быть строкой)", endFlag);
//                        return;
//                    }
//                    PleaseWaitMessage pleaseWaitMessage = new PleaseWaitMessage(out,
//                            "Ваш запрос в процессе обработки. Пожалуйста, подождите...",
//                            2000);
//                    HoosegowStateController.loadState(
//                            hoosegow,
//                            (String)message.getAttachment()
//                    );
//                    pleaseWaitMessage.clear();
//                    sendMessage("Загрузка успешна! В тюряге " + hoosegow.getSize() + " существ", endFlag);
//                } catch (IOException e) {
//                    sendEndMessage("На сервере произошла ошибка чтения/записи");
//                } catch (SAXException | ParserConfigurationException e) {
//                    sendMessage("Ошибка обработки файла: " + e.getLocalizedMessage(), endFlag);
//                } catch (HoosegowOverflowException e) {
//                    sendMessage("В тюряге не остмалось места, некоторые существа не загрузились", endFlag);
//                }
//                return;

//            case "remove_last":
//                if (hoosegow.getSize() == 0)
//                    sendMessage("Тюряга пуста, господин", endFlag);
//                sendMessage("Удалено это существо: " + hoosegow.removeLast(), endFlag);
//                return;
//
            case "add": {

                if (!message.hasAttachment()) {
                    sendMessage(helpFor(message.getMessage()), endFlag);
                    return;
                }
                if (!(message.getAttachment() instanceof Creature)) {
                    sendMessage("Клиент отправил данные в неверном формате (аргумент должен быть существом)", endFlag);
                    return;
                }
                Creature creature = (Creature) message.getAttachment();
                switch (controller.addCreature(creature, message.getUserid(), message.getToken())) {
                    case OK:
                        sendMessage("Существо " + creature.getName() + " добавлено", endFlag);
                        return;
                    case NOT_AUTHORIZED:
                        sendEndMessage(Utils.colorize("Вы не авторизованы. Чтобы войти, введите [[blue]]login"));
                        return;
                    case INTERNAL_ERROR:
                        sendEndMessage(Utils.colorize("[[red]]Возникла внутренняя ошибка сервера. Дайте создателю сервера пинка"));
                        return;
                    case DB_NOT_SUPPORTED:
                        sendEndMessage(Utils.colorize("[[yellow]]Сервер не подключён к базе данных"));
                        return;
                    case NOT_ENOUGH_SPACE:
                        sendEndMessage(Utils.colorize("[[yellow]]Вы создали слишком много существ.\n" +
                                "Сервер разрешает создать до [[blue]]" + Server.getConfig().getMaxUserElements() + "[[yellow]] существ"));
                        return;
                }
                return;
            }
//
            case "remove_greater":
                sendEndMessage(Utils.colorize("Админ потратил время на команду сброса пароля, а эту он сделать не успел :с\n\n" +
                        "[[bright_red]]" +
                        "               ..8888888..     ..8888888..\n" +
                        "             .8:::::::::::8. .8:::::::::::8.\n" +
                        "           .8:::::::::::::::8:::::::::::::::8.\n" +
                        "          .8:::::::::::::::::::::::::::::::::8.\n" +
                        "          8:::::::::::::::::::::::::::::::::::8\n" +
                        "          8:::::::::::::::::::::::::::::::::::8\n" +
                        "          8:::::::::::::::::::::::::::::::::::8\n" +
                        "          '8:::::::::::::::::::::::::::::::::8'\n" +
                        "           '8:::::::::::::::::::::::::::::::8'\n" +
                        "            '8:::::::::::::::::::::::::::::8'\n" +
                        "              '8:::::::::::::::::::::::::8'\n" +
                        "                '8:::::::::::::::::::::8'\n" +
                        "                  '8:::::::::::::::::8'\n" +
                        "                     '8:::::::::::8'\n" +
                        "                        '8:::::8'\n" +
                        "                           '8'"));
//
//
            case "remove":
                try {
                    if (!message.hasAttachment()) {
                        sendMessage(helpFor(message.getMessage()), endFlag);
                        return;
                    }
                    if (!(message.getAttachment() instanceof Creature)) {
                        sendMessage("Клиент отправил данные в неверном формате (аргумент должен быть существом)", endFlag);
                        return;
                    }
                    Creature creature = (Creature)message.getAttachment();
                    ServerController.ServerResponse.RemoveCreature result = controller.removeCreature(creature, message.getUserid(), message.getToken());
                    switch (result.type) {
                        case DB_NOT_SUPPORTED:
                            sendMessage(Utils.colorize("[[yellow]]Сервер не подключён к базе данных"), endFlag);
                            return;
                        case INTERNAL_ERROR:
                            sendMessage(Utils.colorize("[[red]]Возникла внутренняя ошибка сервера. Дайте создателю сервера пинка"), endFlag);
                            return;
                        case NOT_AUTHORIZED:
                            sendMessage(Utils.colorize("[[yellow]]Только избранные могут использовать эту команду.\n" +
                                    "[[blue]]Войдите[[yellow]] или [[blue]]зарегистрируйтесь[[yellow]]."), endFlag);
                            return;
                        case OK:
                            sendMessage(Utils.colorize("Удалено [[blue]]" + result.updates + "[[reset]] существ"), endFlag);
                            return;
                    }
                    return;
                } catch (Exception e) {
                    sendMessage("Не получилось создать существо: " + e.getMessage(), endFlag);
                }
                return;

            case "reset_password": {
                if (!(message.hasAttachment() && message.getAttachment() instanceof String[])) {
                    sendEndMessage("Клиент отправил данные в неверном формате");
                    return;
                }

                String[] data = (String[])message.getAttachment();
                if (data.length < 3) {
                    sendEndMessage("Клиент отправил данные в неверном формате");
                    return;
                }

                int userid, token;
                String newPassword;

                try {
                    userid = Integer.parseInt(data[0]);
                    token = Integer.parseInt(data[1]);
                    newPassword = data[2];
                } catch (NumberFormatException e) {
                    sendEndMessage("Клиент отправил данные в неверном формате");
                    return;
                }

                switch (controller.resetPassword(userid, token, newPassword)) {
                    case OK:
                        sendEndMessage(Utils.colorize("[[bright_green]]Теперь вы можете войти, используя новый пароль"));
                        return;
                    case INTERNAL_ERROR:
                        sendEndMessage(Utils.colorize(
                                "[[red]]Произошла внутренняя ошибка сервера. Дайте администратору сервера пинка под зад"
                        ));
                        return;
                    case DB_NOT_SUPPORTED:
                        sendEndMessage(Utils.colorize("[[yellow]]Сервер не подключён к базе данных"));
                        return;
                    case WRONG_TOKEN:
                        sendEndMessage(Utils.colorize("[[yellow]]Неверный токен"));
                        return;
                    case INCORRECT_PASSWORD:
                        sendEndMessage(Utils.colorize("[[yellow]]Слабый или неверный пароль. Подберите другой"));
                        return;
                }
                return;
            }

            case "request_password_reset": {
                if (!(message.hasAttachment() && message.getAttachment() instanceof String)) {
                    sendEndMessage("Клиент отправил данные в неверном формате");
                    return;
                }

                String email = (String) message.getAttachment();

                switch (controller.requestPasswordReset(email)) {
                    case OK:
                        sendEndMessage(Utils.colorize(
                                "[[bright_green]]На указанный адрес электронной почты отправлен ваш ID и код сброса\n" +
                                        "Введите [[blue]]reset_password <id> <код_сброса>[[bright_green]], чтобы сбросить свой пароль.\n" +
                                        "Код действителен [[blue]]" + Server.getConfig().getPasswordResetTokenTimeout()/60000d + "[[bright_green]] минут"
                        ));
                        return;
                    case INTERNAL_ERROR:
                        sendEndMessage(Utils.colorize(
                                "[[red]]Произошла внутренняя ошибка сервера. Дайте администратору сервера пинка под зад"
                        ));
                        return;
                    case DB_NOT_SUPPORTED:
                        sendEndMessage(Utils.colorize(
                                "[[yellow]]Сервер не подключён к базе данных"
                        ));
                        return;
                    case EMAIL_NOT_EXIST:
                        sendEndMessage(Utils.colorize(
                                "Такой email не зарегистрирован в системе"
                        ));
                        return;
                }
                return;
            }

            case "change_password":
                if (!(message.hasAttachment() && message.getAttachment() instanceof String[])) {
                    sendEndMessage("Клиент отправил данные в неверном формате");
                    return;
                }

                String[] passwords = (String[])message.getAttachment();
                if (passwords.length < 2 || passwords[0] == null || passwords[1] == null) {
                    sendEndMessage("Клиент отправил неполные данные");
                    return;
                }

                if (message.getUserid() == null || message.getToken() == null) {
                    sendEndMessage(Utils.colorize(
                            "[[yellow]]Вы не авторизованы. Введите " +
                                    "[[blue]]login[[yellow]]"+
                                    ", чтобы войти в систему[[reset]]"));
                    return;
                }

                switch (controller.changePassword(message.getUserid(), message.getToken(), passwords[0], passwords[1])) {
                    case OK:
                        sendEndMessage(Utils.colorize("[[bright_green]]Готово! Теперь мы можете войти, используя свой новый пароль[[reset]]"));
                        return;
                    case INCORRECT_NEW_PASSWORD:
                        sendEndMessage("Некорректный новый пароль. Попробуйте подобрать другой.");
                        return;
                    case WRONG_PASSWORD:
                        sendEndMessage(Utils.colorize("[[yellow]]Неверный старый пароль[[reset]]"));
                        return;
                    case INTERNAL_ERROR:
                        sendEndMessage(Utils.colorize("[[red]]Возникла внутренняя ошибка сервера. Обратитесь к администратору сервера[[reset]]"));
                        return;
                    case DB_NOT_SUPPORTED:
                        sendEndMessage(Utils.colorize("[[yellow]]Сервер не подключён к базе данных[[reset]]"));
                        return;
                    case WRONG_TOKEN:
                        sendEndMessage(Utils.colorize("[[yellow]]Вы не авторизованы. Введите " +
                                        "[[blue]]login[[yellow]]" +
                                        ", чтобы войти в систему[[reset]]"));
                        return;
                }
                return;

            case "login": {
                if (!(message.getAttachment() instanceof String[])) {
                    sendEndMessage("Клиент отправил данные в неверном формате");
                    return;
                }
                String[] attachment = (String[]) message.getAttachment();
                if (attachment.length < 2) {
                    sendEndMessage("Клиент отправил неполные данные");
                    return;
                }
                String email = attachment[0];
                String password = attachment[1];

                ServerController.ServerResponse.Login result = controller.login(email, password);
                JSONObject response = new JSONObject();
                switch (result.responseType) {
                    case OK:
                        response.putItem("status", "ok");
                        response.putItem("userid", result.userid);
                        response.putItem("token", result.token);
                        response.putItem("name", result.name);
                        break;
                    case WRONG_PASSWORD:
                        response.putItem("status", "error");
                        response.putItem("message", "Неверная пара email/пароль");
                        break;
                    case INTERNAL_ERROR:
                        response.putItem("status", "error");
                        response.putItem("message", "Возникла внутренняя ошибка сервера");
                        break;
                    case DB_NOT_SUPPORTED:
                        response.putItem("status", "error");
                        response.putItem("message", "Сервер не подключён к базе данных");
                        break;
                        default:
                            response.putItem("status", "error");
                            response.putItem("message", "Возникла необработанная ошибка сервера");
                }
                sendMessage(response.toString(), endFlag);
            }

            case "logout":
                if (message.getUserid() == null || message.getToken() == null) {
                    sendEndMessage("Вы уже вышли");
                    return;
                }
                controller.logout(message.getUserid(), message.getToken());
                sendEndMessage("Кыш отсюда.");
                return;

            case "register":
                if (!(message.getAttachment() instanceof String[])) {
                    sendEndMessage("Клиент отправил данные в неверном формате");
                    return;
                }
                String[] attachment = (String[])message.getAttachment();
                if (attachment.length < 3) {
                    sendEndMessage("Клиент отправил неполные данные");
                    return;
                }
                String name = attachment[0];
                String email = attachment[1];
                String password = attachment[2];
                switch (controller.register(name, email, password)) {
                    case OK:
                        sendEndMessage(Utils.colorize("[[bright_green]]На указанный адрес электронной почты отправлен код.\n" +
                                        "Введите register <код>, чтобы подтвердить регистрацию.\n" +
                                        "Код действителен " + Server.getConfig().getRegistrationTokenTimeout()/60000d + " минут[[reset]]"));
                        return;
                    case INCORRECT_NAME:
                        sendEndMessage(Utils.colorize("[[yellow]Вы пошли против системы и ввели неверное имя.[[reset]]"));
                        return;
                    case INCORRECT_EMAIL:
                        sendEndMessage(Utils.colorize("[[yellow]]Похоже, вы ввели невалидный email.[[reset]]"));
                        return;
                    case INCORRECT_PASSWORD:
                        sendEndMessage(Utils.colorize("[[yellow]]Некорректный пароль, подберите другой[[reset]]"));
                        return;
                    case ADDRESS_ALREADY_REGISTERED:
                        sendEndMessage(
                                Utils.colorize("[[yellow]]Этот email-адрес уже зарегистрирован.\n[[reset]]" +
                                "Если вы потеряли доступ к своему аккаунту, используйте [[blue]]reset_password[[reset]]"));
                        return;
                    case ADDRESS_IN_USE:
                        sendEndMessage(
                                Utils.colorize("[[yellow]]Кто-то уже регистрируется с этим адресом.\n[[reset]]"+
                                "Если это не вы, подождите [[blue]]" +
                                        Server.getConfig().getRegistrationTokenTimeout()/60000d + "[[reset]] мин. и попробуйте снова")
                        );
                        return;
                    case INTERNAL_ERROR:
                        sendEndMessage(
                                Utils.colorize("[[red]]Возникла внутренняя ошибка сервера. Пожалуйста, сообщите об этом адинистратору сервера.[[reset]]")
                        );
                        return;
                    case DB_NOT_SUPPORTED:
                        sendEndMessage(
                                Utils.colorize("[[yellow]]Сервер не подключён к базе данных. Обратитесь к администратору сервера и дайте ему пинка под зад.[[reset]]")
                        );
                        return;
                }
                return;

            case "accept_registration_token":
                if (message.hasAttachment() && message.getAttachment() instanceof Integer) {
                    switch (controller.acceptRegistrationToken((Integer)message.getAttachment())) {
                        case OK:
                            sendEndMessage("Готово. Теперь вы можете войти в свой аккаунт");
                            return;
                        case WRONG_TOKEN:
                            sendEndMessage("Неверный или устаревший токен");
                            return;
                        case DB_NOT_SUPPORTED:
                            sendEndMessage(
                                    Utils.colorize("[[YELLOW]]Сервер не подключён к базе данных. Обратитесь к администратору сервера и дайте ему пинка под зад.[[RESET]]")
                            );
                            return;
                        case INTERNAL_ERROR:
                            sendEndMessage(
                                    Utils.colorize("[[YELLOW]]Возникла внутренняя ошибка сервера. Пожалуйста, сообщите об этом адинистратору сервера.[[RESET]]")
                            );
                            return;
                    }
                } else {
                    sendEndMessage("Клиент отправил данные в неверном формате");
                    return;
                }

            case "?":
            case "ыыы":
            case "help":
            case "хелб":
            case "хелп":
            case "хэлб":
            case "хэлп":
            case "хлеб":
            case "чоита":
            case "шоцетаке":
            case "помогите":
            case "памагити":
            case "напомощь":
                if (!message.hasAttachment())
                    sendMessage(helpFor("help"), endFlag);
                else {
                    if (message.getAttachment() instanceof String)
                        sendMessage(helpFor((String) message.getAttachment()), endFlag);
                    else
                        sendMessage("Клент отправил данные в неверном формате (аргумент должен быть строкой)", endFlag);
                }
                return;

            default:
                if (message.getMessage().length() < 64)
                    sendMessage("Не могу понять команду " + message.getMessage() + ", введите help, чтобы получить помощь", endFlag);
                else
                    sendMessage("Не могу понять эту большую команду, введите help, чтобы получить помощь", endFlag);
        }
    }

    /**
     * Возвращает инструкции к команде
     * @param command команда, для которой нужна инструкция
     * @return инструкция к указанной команде
     */
    private static String helpFor(String command) {
        switch (command) {
            case "help":
                return  "Похоже, ты тут впервые, боец!\n" +
                        "Вот команды, которыми можно пользоваться:\n\n" +
                        "exit - свалить отсюда\n" +
                        "address [newAddress] - информация об адресе сервера. Если указан адрес, он будет заменён\n" +
                        "port [newPort] - информация о порте сервера. Если указан порт, он будет заменён\n" +
                        "again - выполнить предыдущую введённую команду\n" +
                        "info - информация о коллекции\n" +
                        "show - состояние тюряги\n" +
                        "register - зарегистрировать пользователя\n" +
                        "change_password - поменять пароль\n" +
                        "reset_password - сбросить пароль\n" +
                        "login - войти в аккаунт\n" +
                        "logout - выйти из аккаунта\n" +
//                        "remove_last - удалить последний элемент\n" +
                        "add {elem} - добавить элемент, обязательные поля: x и y, дополнительно width, height, name\n" +
//                        "remove_greater {elem} - удалить всех, кто круче указанного существа\n" +
                        "multiline - включить/выключить ввод в несколько строк\n" +
//                        "import {file} - добавить данные из файла клиента в коллекцию\n" +
//                        "load {file} - загрузить состояние коллекции из файла сервера\n" +
//                        "save {file} - сохранить состояние коллекции в файл сервера\n" +
                        "remove {elem} - удалить элемент\n" +
                        "help {command} - инструкция к команде\n" +
                        "help, ?, памагити, хелб, хэлп, и т.п. - показать этот текст";

            case "exit":
                return Utils.colorize("Введите [[BLUE]]exit[[RESET]], чтобы выйти");

            case "address":
                return  "Если вызвать эту команду, то можно узнать адрес, к которому клиент будет\n" +
                        "подключаться и отправлять команды. Если после команды указать новый адрес,\n" +
                        "то текущий адрес заменится им.\n\n" +
                        "Например:\n" +
                        "> address 192.168.1.100\n" +
                        "> address 127.0.0.1\n" +
                        "> address localhost";

            case "port":
                return  "Если вызвать эту команду, то можно узнать порт, по которому клиент будет\n" +
                        "подключаться и отправлять команды. Если после команды указать новый порт,\n" +
                        "то текущий порт заменится им. Порт должен быть в пределах от 1 до 65535.\n\n" +
                        "Например:\n" +
                        "> port 8080\n" +
                        "> port 80\n" +
                        "> port 21";

            case "again":
                return "Эта команда выполняет предыдущую введённую команду.\n" +
                        "Команда again никогда не бывает первой введённой командой и\n" +
                        "никогда не бывает \"предыдущей\" введённой командой.";

            case "info":
                return "Введите \"info\", чтобы узнать о количестве существ в тюряге, времени создания и типе используемой коллекции";

            case "show":
                return "Выводит список существ в тюряге";

            case "remove_last":
                return "Введите \"remove_last\", чтобы удалить последнее существо из тюряги";

            case "add":
                return  "Чтобы создать существо, введите \"add\", а затем json-объект,\n" +
                        "описывающий существо. Для описания существа можно использовать\n" +
                        "следующие параметры:\n" +
                        "x - число, обязательное, X-координата существа\n" +
                        "y - число, обязательное, Y-координата существа\n" +
                        "width - число, необязательное, ширина существа\n" +
                        "height - число, необязательное, высота существа\n" +
                        "name - строка, необязательное, имя существа\n\n" +
                        "Если вы хотите добавить сразу несколько существ, то можете вместо\n" +
                        "json-объекта указать json-массив из объектов, каждый из которых будет\n" +
                        "добавлен в коллекцию.\n\n" +
                        "Например:\n" +
                        "> add {\"x\": 12, \"y\": 34}\n" +
                        "> add {\"x\": 42, \"y\": -53.1, \"width\": 12, \"name\": \"Стёпа\"}\n" +
                        "> add [ {\"x\": 12, \"y\": 34}, {\"y\": 62, \"x\": 1, \"width\": 23} ]";

            case "remove_greater":
                return  "Чтобы удалить все существа, превосходящие нужный, введите \"remove_greater\", а\n" +
                        "затем json-объект, описывающий существо, с которым будет выполняться сравнение\n" +
                        "при удалении. Для описания существа можно использовать слудеющие параметры:\n" +
                        "x - число, обязательное, X-координата существа\n" +
                        "y - число, обязательное, Y-координата существа\n" +
                        "width - число, необязательное, ширина существа\n" +
                        "height - число, необязательное, высота существа\n" +
                        "name - строка, необязательное, имя существа\n\n" +
                        "Если вы хотите удалить сразу несколько существ, то можете вместо\n" +
                        "json-объекта указать json-массив из объектов, каждое существо будет\n" +
                        "обработано по отдельности.\n\n" +
                        "Например:\n" +
                        "> remove_greater {\"x\": 32, \"y\": 7}\n" +
                        "> remove_greater {\"x\": -5, \"y\": 0.1, \"width\": 1, \"name\": \"Василий\"}\n" +
                        "> remove_greater [ {\"x\": 12, \"y\": 34}, {\"y\": 62, \"x\": 1, \"width\": 23} ]";

            case "multiline":
                return  "Переключает режим многострочного ввода. Если многострочный ввод выключен, введите \"multiline\",\n" +
                        "чтобы включить его. После того, как вы включили многострочный режим, ваши команды будут\n" +
                        "отдельяться друг от друга знаком ';'.\n" +
                        "Чтобы выключить многострочный ввод, введите \"multiline;\". Обратите внимание, что в режиме\n" +
                        "многострочного ввода также нужен знак ';' после команды отключения многострочного ввода.";

            case "import":
                return  "Иногда бывает так, что нужно передать содержимое всего файла на сервер, где этого файла нет.\n" +
                        "Используйте команду \"import\", чтобы сделать это. После имени команды укажите файл,\n" +
                        "содержимое которого передастся на сервер. Обратите внимание, что, если в файле указана дата\n" +
                        "создания коллекции, текущая дата будет перезаписана. Кроме того, import не удаляет\n" +
                        "предыдущих существ из коллекции. Файл должен хранить данные в формате xml\n\n" +
                        "Например:\n" +
                        "> import client_file.xml";

            case "load":
                return  "Эта команда идентична команде import (введите \"help import\", чтобы узнать о ней), за\n" +
                        "исключением нескольких деталей:\n" +
                        "- load используется для загрузки файла сервера\n" +
                        "- load полностью перезаписывает состояние коллекции";

            case "register":
                return  "Используйте команду register, чтобы зарегистрировать нового пользователя. Форма регистрации\n" +
                        "содерэит поля: имя, email, пароль, повтор пароля. Введите эти данные и вы будете зарегистрированы.";

            case "change_password":
                return "Используйте команду change_password, чтобы изменить свой пароль. Обратите внимание, что для этого\n" +
                        "нужно быть авторизованным. Чтобы войти в систему, используйте login";

            case "remove":
                return  "Чтобы удалить существо, введите \"remove\", а затем json-объект,\n" +
                        "описывающий существо. Для описания существа можно использовать\n" +
                        "следующие параметры:\n" +
                        "x - число, X-координата существа\n" +
                        "y - число, Y-координата существа\n" +
                        "width - число, ширина существа\n" +
                        "height - число, высота существа\n" +
                        "name - строка, имя существа\n\n" +
                        "Если вы хотите удалить сразу несколько существ, то можете вместо\n" +
                        "json-объекта указать json-массив из объектов, каждый из которых будет\n" +
                        "удален из коллекции.\n\n" +
                        "Например:\n" +
                        "> remove {\"x\": 5, \"y\": 1}\n" +
                        "> remove {\"x\": 65536, \"y\": 123214, \"height\": 203, \"name\": \"Пётр I\"}\n" +
                        "> remove [ {\"x\": 12, \"y\": 34}, {\"y\": 62, \"x\": 1, \"width\": 23} ]";

                default:
                    return  "К сожалению, для такой команды нет справки";
        }
    }
}