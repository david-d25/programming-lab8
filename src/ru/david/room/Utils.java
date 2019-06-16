package ru.david.room;

import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;

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
     * Находит точки пересечения двух окружностей.
     * Возвращает пустой массив, если точек пересечения нет или центры окружностей совпадают.
     * Размер возвращаемого массив может быть 0, 1 или 2.
     *
     * <strong>This function is imported and not guaranteed to work</strong>
     *
     * @param c1 центр окружности 1
     * @param c2 центр окружности 2
     * @param r1 радиус окружности 1
     * @param r2 радиус окружности 2
     * @return массив точек пересечения
     */
    public static Point2D[] getCirclesIntersection(Point2D c1, Point2D c2, double r1, double r2) {
        if (c1.distance(c2) > r1 + r2)  // Окружности слишком далеко
            return new Point2D[0];

        if (c1.equals(c2)) // Центры окружностей совпадают
            return new Point2D[0];

        double c = c2.getX() - c1.getX();
        double d = c2.getY() - c1.getY();

        double A = -2*c;
        double B = -2*d;
        double C = r1*r1 - r2*r2 + c*c + d*d;

        double ro = Math.abs(C)/Math.sqrt(A*A + B*B);

        if (ro > r1)
            return new Point2D[0];

        double x0 = -A*C/(A*A+B*B);
        double y0 = -B*C/(A*A+B*B);

        if (ro == r1)
            return new Point2D[] {new Point2D(
                    x0 + c1.getX(),
                    y0 + c1.getY()
            )};

        double p = Math.sqrt(r1*r1 - (C*C)/(A*A+B*B));
        double k = Math.sqrt((p*p)/(A*A+B*B));

        return new Point2D[] {
                new Point2D(
                        x0 + B*k + c1.getX(),
                        y0 - A*k + c1.getY()
                ),
                new Point2D(
                        x0 - B*k + c1.getX(),
                        y0 + A*k + c1.getY()
                )
        };
    }

    /**
     * Понятия не имею как, но функция возвращает точку пересечения двух отрезков.
     * Если точки нет, возвращает null.
     *
     * @param a1 точка отрезка 1
     * @param a2 точка отрезка 1
     * @param b1 точка отрезка 2
     * @param b2 точка отрезка 2
     * @return точка пересечения или null
     */
    public static Point2D getLinesIntersection(Point2D a1, Point2D a2, Point2D b1, Point2D b2) {
        double angle1Ratio = (a2.getY() - a1.getY())/(a2.getX() - a1.getX());
        double angle2Ratio = (b2.getY() - b1.getY())/(b2.getX() - b1.getX());

        if (    angle1Ratio != angle1Ratio || angle2Ratio != angle2Ratio ||
                Double.isInfinite(angle1Ratio) && Double.isInfinite(angle2Ratio) ||
                (angle1Ratio == angle2Ratio)
        ) return null;

        // Если какой-то отрезон вертикальный
        if (Double.isInfinite(angle1Ratio) || Double.isInfinite(angle2Ratio)) {
            double resultX, resultY, offset;
            Point2D p1, p2;

            if (Double.isInfinite(angle1Ratio)) {
                resultX = a1.getX();
                offset = b1.getY() - angle2Ratio * b1.getX();
                resultY = angle2Ratio * resultX + offset;
                p1 = b1;
                p2 = b2;
            } else {
                resultX = b1.getX();
                offset = a1.getY() - angle1Ratio * a1.getX();
                resultY = angle1Ratio * resultX + offset;
                p1 = a1;
                p2 = a2;
            }

            if (    (p1.getX() < resultX && p2.getX() < resultX) ||
                    (p1.getX() > resultX && p2.getX() > resultX)
            ) return null;

            double avgY = (p1.getY() + p2.getY())/2;
            double height = Math.abs(p1.getY() - p2.getY());

            if (Math.abs(resultY - avgY) <= height/2)
                return new Point2D(resultX, resultY);
            else
                return null;
        }

        double offsetB = b1.getY() - angle2Ratio * b1.getX();
        double offsetA = a1.getY() - angle1Ratio * a1.getX();

        // Точка пересечения двух ПРЯМЫХ, образованных отрезками
        double resultX = (offsetB - offsetA)/(angle1Ratio - angle2Ratio);
        double resultY = angle1Ratio * resultX + offsetA;

        // Проверяем, что точка лежит в обоих отрезках
        Point2D avg1Point = new Point2D((a1.getX() + a2.getX())/2, (a1.getY() + a2.getY())/2);
        Dimension2D rectA = new Dimension2D(Math.abs(a1.getX() - a2.getX()), Math.abs(a1.getY() - a2.getY()));
        Point2D avg2Point = new Point2D((b1.getX() + b2.getX())/2, (b1.getY() + b2.getY())/2);
        Dimension2D rectB = new Dimension2D(Math.abs(b1.getX() - b2.getX()), Math.abs(b1.getY() - b2.getY()));

        if (    (Math.abs(resultX - avg1Point.getX()) <= rectA.getWidth()/2) &&
                (Math.abs(resultY - avg1Point.getY()) <= rectA.getHeight()/2) &&
                (Math.abs(resultX - avg2Point.getX()) <= rectB.getWidth()/2) &&
                (Math.abs(resultY - avg2Point.getY()) <= rectB.getHeight()/2)
        ) return new Point2D(resultX, resultY);
        return null;
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
