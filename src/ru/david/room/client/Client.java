package ru.david.room.client;

import javafx.application.Application;
import javafx.scene.control.Alert;
import ru.david.room.client.main.MainWindow;
import ru.david.room.json.JSONParseException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.AccessDeniedException;
import java.util.*;

/**
 * Клиент. Просто Клиент. Для Просто Сервера.
 */
public class Client {
    private static ClientConfig config;
    private static Locale currentLocale = Locale.getDefault();
    private static HashMap<Locale, ResourceBundle> resourceBundles = new HashMap<>();

    private static Socket socket;

    public static void main(String[] args) {
        try {
            initResourceBundles();
            config = ClientConfig.fromFile("config/client-config.json");
        } catch (FileNotFoundException e) {
            alertError("Couldn't start the client", "There's no config file: config/client-config.json");
        } catch (AccessDeniedException e) {
            alertError("Couldn't start the client", "There's no access to config file: config/client-config.json");
        } catch (IOException e) {
            alertError("Couldn't start the client", "I/O Error", e.toString());
        } catch (JSONParseException | NoSuchElementException | IllegalArgumentException | IllegalStateException e) {
            alertError("Couldn't start the client", e.getMessage());
        }

        Application.launch(MainWindow.class);
    }

    private static void initResourceBundles() {
        resourceBundles.clear();
        resourceBundles.put(
                currentLocale,
                ResourceBundle.getBundle("i18n/text", currentLocale)
        );
        resourceBundles.put(
                new Locale("es", "DO"),
                ResourceBundle.getBundle("i18n/text", new Locale("es", "DO"), new UTF8BundleControl())
        );
        resourceBundles.put(
                new Locale("it", "IT"),
                ResourceBundle.getBundle("i18n/text", new Locale("it", "IT"), new UTF8BundleControl())
        );
        resourceBundles.put(
                new Locale("ro", "RO"),
                ResourceBundle.getBundle("i18n/text", new Locale("ro", "RO"), new UTF8BundleControl())
        );
        resourceBundles.put(
                new Locale("ru", "RU"),
                ResourceBundle.getBundle("i18n/text", new Locale("ru", "RU"), new UTF8BundleControl())
        );
    }

    public static Socket getSocket() {
        return socket;
    }

    public static void setSocket(Socket socket) {
        Client.socket = socket;
    }

    public static HashMap<Locale, ResourceBundle> getResourceBundles() {
        return resourceBundles;
    }

    public static ResourceBundle currentResourceBundle() {
        return resourceBundles.get(currentLocale);
    }

    public static Locale getCurrentLocale() {
        return currentLocale;
    }

    public static void setCurrentLocale(Locale currentLocale) {
        Client.currentLocale = currentLocale;
    }

    public static ClientConfig getConfig() {
        return config;
    }

    private static void alertError(String title, String header) {
        alertError(title, header, null);
    }

    private static void alertError(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.show();
    }
}
