package ru.david.room;

import java.io.Serializable;

/**
 * Класс, использующийся для передачи сообщений между клиентом и сервером,
 * используется для соответствия требованию о сериализации передаваемых объектов
 * и для решения проблемы с разделением команды и её объектов-аргументов
 */
public class Message implements Serializable {
    private String message;
    private Serializable attachment;
    private boolean endFlag;

    private Integer token;
    private Integer userid;

    /**
     * Создаёт сообщение с указанным текстовым запросом, объектом-приложением и флагом окончания
     * @param message текстовый запрос
     * @param attachment объект-приложение, прикреплённый к сообщению
     * @param endFlag флаг окончания
     */
    public Message(String message, Serializable attachment, boolean endFlag) {
        this.message = message;
        this.attachment = attachment;
        this.endFlag = endFlag;
    }

    /**
     * Создаёт сообщение с указанным текстовым запросом и объектом-приложением
     * @param message текстовый запрос
     * @param attachment объект-приложение, прикреплённый к сообщению
     */
    public Message(String message, Serializable attachment) {
        this(message, attachment, false);
    }

    /**
     * Создаёт сообщение с указанным текстовым запросом и флагом окончания
     * @param message текстовый запрос
     * @param endFlag флаг окончания
     */
    public Message(String message, boolean endFlag) {
        this(message, null, endFlag);
    }

    /**
     * Создаёт сообщение с указанным текстовым запросом
     * @param message текствый запрос
     */
    public Message(String message) {
        this(message, null, false);
    }

    /**
     * @return текстовый запрос сообщения
     */
    public String getMessage() {
        return message;
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
     * @return true, если сообщение отмечено как последнее (если установлен флаг окончания)
     */
    public boolean hasEndFlag() {
        return endFlag;
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
