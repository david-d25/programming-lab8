package ru.david.room.json;

/**
 * Абстрация для любой json-сущеости (объект, массив или любое значение)
 * @see JSONArray
 * @see JSONObject
 * @see JSONNumber
 * @see JSONBoolean
 * @see JSONString
 */
public class JSONEntity {
    protected JSONType type;

    /**
     * @return {@link JSONEntity#toFormattedString(int)} с отступом 2
     */
    public final String toFormattedString() {
        return toFormattedString(2);
    }

    /**
     * @param tabSize размер отступа
     * @return Тот же, что {@link JSONEntity#toString()}, но с отступами и переносами для читаемости
     */
    public final String toFormattedString(int tabSize) {
        return toFormattedString(tabSize, 0);
    }

    protected String toFormattedString(int tabSize, int depth) {
        return toString();
    }

    protected String getPaddingString(int tabSize, int depth) {
        StringBuilder padding = new StringBuilder();
        for (int i = 0; i < tabSize*depth; i++)
            padding.append(' ');
        return padding.toString();
    }

    /**
     * @return тип этой json-сущеости
     */
    public JSONType getType() {
        return type;
    }

    /**
     * @return Строка с типом этой json-сущности на русском языке
     */
    public String getTypeName() {
        return getType().getName();
    }

    /**
     * @return true, если эта json-сущность представляет собой объект
     */
    public boolean isObject() {
        return type == JSONType.OBJECT;
    }

    /**
     * @return true, если эта json-сущность представляет собой массив
     */
    public boolean isArray() {
        return type == JSONType.ARRAY;
    }

    /**
     * @return true, если эта json-сущность представляет собой логический тип
     */
    public boolean isBoolean() {
        return type == JSONType.BOOLEAN;
    }

    /**
     * @return true, если эта json-сущность представляет собой число
     */
    public boolean isNumber() {
        return type == JSONType.NUMBER;
    }

    /**
     * @return true, если эта json-сущность представляет собой строку
     */
    public boolean isString() {
        return type == JSONType.STRING;
    }

    /**
     * Приводит эту json-сущность к json-объекту
     * @return Эта сущность в виде json-объекта
     * @throws IllegalStateException В случае, если эта json-сущность не может быть приведена к json-объекту
     */
    public JSONObject toObject() throws IllegalStateException {
        return toObject("Невозможно создать объект из сущности с типом " + getTypeName());
    }

    /**
     * Приводит эту json-сущность к json-объекту
     * @param onError Сообщение исключения, которое будет вызвано в случае невозможности приведения
     * @return Эта сущность в виде json-объекта
     * @throws IllegalStateException В случае, если эта json-сущность не может быть приведена к json-объекту
     */
    public JSONObject toObject(String onError) throws IllegalStateException {
        if (isObject())
            return (JSONObject)this;
        throw new IllegalStateException(onError);
    }

    /**
     * Приводит эту json-сущность к json-массиву
     * @return Эта сущность в виде json-массива
     * @throws IllegalStateException В случае, если эта json-сущность не может быть приведена к json-массиву
     */
    public JSONArray toArray() throws IllegalStateException {
        return toArray("Невозможно создать массив из сущности с типом " + getTypeName());
    }

    /**
     * Приводит эту json-сущность к json-массиву
     * @param onError Сообщение исключения, которое будет вызвано в случае невозможности приведения
     * @return Эта сущность в виде json-массива
     * @throws IllegalStateException В случае, если эта json-сущность не может быть приведена к json-массиву
     */
    public JSONArray toArray(String onError) throws IllegalStateException {
        if (isArray())
            return (JSONArray)this;
        throw new IllegalStateException(onError);
    }

    /**
     * Приводит эту json-сущность к объекту-инкапсулятору логического типа json
     * @return Эта сущность в виде объекта-инкапсулятора логического типа json
     * @throws IllegalStateException В случае, если эта json-сущность не может быть приведена к
     * объекту-инкапсулятору логического типа json
     */
    public JSONBoolean toBoolean() throws IllegalStateException {
        return toBoolean("Невозможно создать булев тип из сущности с типом " + getTypeName());
    }

    /**
     * Приводит эту json-сущность к объекту-инкапсулятору логического типа json
     * @param onError Сообщение исключения, которое будет вызвано в случае невозможности приведения
     * @return Эта сущность в виде объекта-инкапсулятора логического типа json
     * @throws IllegalStateException В случае, если эта json-сущность не может быть приведена к объекту-инкапсулятору логического типа json
     */
    public JSONBoolean toBoolean(String onError) throws IllegalStateException {
        if (isBoolean())
            return (JSONBoolean) this;
        throw new IllegalStateException(onError);
    }

    /**
     * Приводит эту json-сущность к json-числу
     * @return Эта сущность в виде json-числа
     * @throws IllegalStateException В случае, если эта json-сущность не может быть приведена к json-числу
     */
    public JSONNumber toNumber() throws IllegalStateException {
        return toNumber("Невозможно создать число из сущености с типом " + getTypeName());
    }

    /**
     * Приводит эту json-сущность к json-числу
     * @param onError Сообщение исключения, которое будет вызвано в случае невозможности приведения
     * @return Эта сущность в виде json-числа
     * @throws IllegalStateException В случае, если эта json-сущность не может быть приведена к json-числу
     */
    public JSONNumber toNumber(String onError) throws IllegalStateException {
        if (isNumber())
            return (JSONNumber) this;
        throw new IllegalStateException(onError);
    }

    /**
     * Приводит эту json-сущность к json-строке
     * @param onError Сообщение исключения, которое будет вызвано в случае невозможности приведения
     * @return Эта сущность в виде json-строки
     * @throws IllegalStateException В случае, если эта json-сущность не может быть приведена к json-строке
     */
    public JSONString toString(String onError) throws IllegalStateException {
        if (isString())
            return (JSONString) this;
        throw new IllegalStateException(onError);
    }
}