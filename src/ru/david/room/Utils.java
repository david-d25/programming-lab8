package ru.david.room;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.david.room.GlobalConstants.ANSI_COLORS.RESET;

public class Utils {
    /**
     * Проверяет валидность email-адреса
     *
     * @param email Адрес, который надо проверить
     *
     * @return true, если адрес валидный
     */
    public static boolean isValidEmailAddress(String email) {
        boolean result = true;
        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
        } catch (AddressException ex) {
            result = false;
        }
        return result;
    }

    /**
     * Превращает вот_такой_текст в ВотТакойТекст
     * @param s исходный текст
     * @return результат
     */
    public static String toCamelCase(String s){
        String[] parts = s.split("_");
        StringBuilder camelCaseString = new StringBuilder();
        for (String part : parts){
            camelCaseString.append(toProperCase(part));
        }
        return camelCaseString.toString();
    }

    private static String toProperCase(String s) {
        return s.substring(0, 1).toUpperCase() +
                s.substring(1).toLowerCase();
    }

    /**
     * Проверяет, не является ли пароль <b>слишком</b> простым
     *
     * @param password пароль для проверки
     *
     * @return true, если пароль <b>слишком</b> простой
     */
    public static boolean isPasswordTooWeak(String password) {
        String[] weakPasswords = new String[] {
                "123456", "1234567", "12345678", "123456789", "1234567890",
                "234567", "2345678", "23456789", "24567890",
                "345678", "3456789", "34567890",
                "456789", "4567890",
                "567890", "monkey", "654321",
                "0987654321", "987654321", "87654321",
                "7654321", "654321", "qwerty",
                "qwertyuiop", "zxcvbn", "zxcvbnm",
                "asdfghjkl", "password", "111111",
                "sunshine", "iloveyou", "princess",
                "admin", "welcome", "666666",
                "abc123", "football", "123123",
                "!@#$%^&*"
        };
        for (String testPassword : weakPasswords) {
            if (password.equals(testPassword))
                return true;
        }
        return false;
    }

    /**
     * Раскрашивает текст, заменяя подсроки-маркеры специальными ASCII-кодами
     * Подстрокой-маркером считается текст, заключенный в двойные прямоугольные скобки.
     * Например: [[RED]], [[BG_BLUE]]
     *
     * Информация о цветах получается из {@link GlobalConstants.ANSI_COLORS}
     *
     * @param source исходная строка с кодами
     *
     * @return раскрашенный текст
     */
    public static String colorize(String source) {
        try {
            StringBuilder result = new StringBuilder(source);
            HashMap<String, String> colorsMap = new HashMap<>();
            Field[] colorFields = GlobalConstants.ANSI_COLORS.class.getDeclaredFields();
            for (Field field : colorFields) {
                if (field.getType() == String.class)
                    colorsMap.put(field.getName(), (String)field.get(GlobalConstants.ANSI_COLORS.class));
            }
            String regex = "\\[\\[\\w+]]";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(result);
            while (matcher.find()) {
                String colorName = result.substring(matcher.start()+2, matcher.end()-2).toUpperCase();
                String color = colorsMap.get(colorName);
                if (color != null) {
                    result.replace(matcher.start(), matcher.end(), color);
                    matcher.region(matcher.start(), result.length());
                }
            }
            return result.toString() + RESET;
        } catch (IllegalAccessException e) {
            return source;
        }
    }
}
