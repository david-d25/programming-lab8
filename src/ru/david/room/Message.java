package ru.david.room;

import java.io.Serializable;

/**
 * Класс, использующийся для передачи сообщений между клиентом и сервером,
 * используется для соответствия требованию о сериализации передаваемых объектов
 * и для решения проблемы с разделением команды и её объектов-аргументов
 */
public class Message implements Serializable {
    private String text;
    private Serializable attachment;

    private Integer token;
    private Integer userid;

    /**
     * Создаёт сообщение с указанным текстовым запросом и объектом-приложением
     * @param text текстовый запрос
     * @param attachment объект-приложение, прикреплённый к сообщению
     */
    public Message(String text, Serializable attachment) {
        this.text = text;
        this.attachment = attachment;
    }
    /**
     * Создаёт сообщение с указанным текстовым запросом
     * @param text текствый запрос
     */
    public Message(String text) {
        this(text, null);
    }

    /**
     * @return текстовый запрос сообщения
     */
    public String getText() {
        return text;
    }

    /**
     * @return объект-приложение, прикреплённое к сообщению
     */
    public Serializable getAttachment() {
        return attachment;
    }

    /**
     * Устанавливает приложение к сообщению
     *
     * @param attachment объект-приложение
     */
    public void setAttachment(Serializable attachment) {
        this.attachment = attachment;
    }

    /**
     * @return true, если к сообщению прикреплено объект-приложение
     */
    public boolean hasAttachment() {
        return attachment != null;
    }

    /**
     * @return токен пользователя для его аутентификации
     */
    public Integer getToken() {
        return token;
    }

    public void setToken(Integer token) {
        this.token = token;
    }

    /**
     * @return идентификатор пользователя
     */
    public Integer getUserid() {
        return userid;
    }

    public void setUserid(Integer userid) {
        this.userid = userid;
    }
}
