package ru.david.room.client;

import ru.david.room.Utils;

import java.io.*;

/**
 * Выполянет взаимодействие с пользователем
 */
public class UserInteractor {

    public static char COMMAND_END_SYMBOL = ';';

    private BufferedReader reader;
    private boolean multilineMode = false;

    /**
     * Создаёт экземпляр ${@link UserInteractor}
     *
     * @param streamToRead поток, из которого следует читать ввод пользователя
     */
    UserInteractor(InputStream streamToRead) {
        reader = new BufferedReader(new InputStreamReader(streamToRead));
    }


    /**
     * Выводит на экран сообщение и приглашает пользователя ввести что-то в ответ
     *
     * @param message сообщение, выводимое перед приглашением.
     *                Обратите внимание, что после вывода сообщения не последует перенос строки.
     *
     * @return сообщение, введённое пользователем
     *
     * @throws IOException Если проихойдёт ошибка ввода
     */
    String prompt(String message) throws IOException {
        System.out.print(message);
        return multilineMode ? getMultilineCommand() : reader.readLine();
    }

    /**
     * Выводит на экран сообщение. Если в данный момент работают функции {@link #prompt(String)} или
     * {@link #promptHidden(String)}, текст их приглашения перезаписывается новым сообщением.
     *
     * @param message сообщение
     */
    synchronized void alert(String message) {
        System.out.print("\r");
        System.out.println(message);
        System.out.print(Client.generatePromptMessage());
    }

    /**
     * Работает аналогично {@link UserInteractor#prompt(String)}, но скрывает вводимый пользователем ответ.
     * Может использоваться для ввода секретной инфомации, такой как пароли.
     *
     * @param message сообщение, выводимое перед приглашением
     *
     * @return сообщение, введённое пользователем
     *
     * @throws IOException Если проихойдёт ошибка ввода
     */
    synchronized String promptHidden(String message) throws IOException {
        Console console = System.console();
        if (console == null) {
            System.out.println(Utils.colorize(
                                "[[yellow]]Среда не позволяет скрыть ваш ввод. " +
                                        "Убедитесь, что никто не стоит за спиной.[[reset]]"
            ));
            return prompt(message);
        }
        char[] responseArray = console.readPassword(message);
        return new String(responseArray);
    }

    /**
     * Выводит на экран вопрос и приглашает пользователя сделать выбор между "Да" и "Нет"
     *
     * @param question Вопрос, задаваемый пользователю.
     *                 Обратите внимание, что после вывода вопроса не последует перенос строки.
     * @param defaultAnswer Результат, который вернёт функция, если пользователь ничего не ответит
     *
     * @return true, если пользователь выбрал "Да", иначе false
     *
     * @throws IOException Если проихойдёт ошибка ввода
     */
    public synchronized boolean confirm(String question, boolean defaultAnswer) throws IOException {
        System.out.print(question);
        while (true) {
            if (defaultAnswer)
                System.out.print(" (Y/n) ");
            else
                System.out.print(" (y/N) ");

            String response = reader.readLine();
            if (response.length() == 0)
                return defaultAnswer;

            char firstChar = response.charAt(0);
            if (firstChar == 'y' || firstChar == 'Y')
                return true;
            if (firstChar == 'n' || firstChar == 'N')
                return false;

            System.out.print("Следует ответить 'y' или 'n': ");
        }
    }

    /**
     * Вощвращает слелующую команду пользователя в многострочном режиме.
     * Символ конца команды будет проигнорирован, если он находится в строковос литерале.
     *
     * @return введённая пользователем команда
     *
     * @throws IOException если что-то пойдёт не так
     */
    private String getMultilineCommand() throws IOException {
        StringBuilder builder = new StringBuilder();
        char current;
        boolean inString = false;
        do {
            current = (char)reader.read();
            if (current != COMMAND_END_SYMBOL || inString)
                builder.append(current);
            if (current == '"')
                inString = !inString;
        } while (current != COMMAND_END_SYMBOL || inString);
        return builder.toString();
    }

    /**
     * Устанавливает режим чтения команды.
     *
     * Есть 2 режима чтения команды: однострочный и многострочный.
     *
     * В однострочном режиме строка = команда, т.е. командой считается массив
     * символов, оканчивающийся на символ перевода строки и возврата каретки,
     * при этом сам символ перевода строки (возврата каретки) не входит в команду.
     *
     * В многострочном режиме командой считается массив символов, оканчивающийся на
     * специальный символ конца команды {@link UserInteractor#COMMAND_END_SYMBOL}.
     *
     * @param mode если этот параметр установлен в true, команды будут читаться
     *             в многострочном режиме. Если этот параметр установлен в false,
     *             команды будут читать в однострочном режиме.
     */
    public void setMultilineMode(boolean mode) {
        multilineMode = mode;
    }

    /**
     * Переключает режим чтения команды.
     *
     * @see UserInteractor#setMultilineMode(boolean)
     *
     * @return новый режим чтения
     */
    public boolean toggleMultilineMode() {
        setMultilineMode(!multilineMode);
        return multilineMode;
    }
}
