package ru.david.room;

import ru.david.room.json.*;

public class CreatureFactory {
    /**
     * Создаёт экземпляр класса Creature по заданному json-объекту
     * @param object json-объект, представляющий экземпляр класса Creature
     * @return экземпляр класса Creature
     */
    private static Creature generate(JSONObject object) {
        JSONEntity x = object.getItem("x");
        JSONEntity y = object.getItem("y");

        if (x == null)
            throw new IllegalArgumentException("Требуется параметр 'x', но он не указан");
        if (y == null)
            throw new IllegalArgumentException("Требуется параметр 'y', но он не указан");

        Creature creature = new Creature(
                (int)x.toNumber("Параметр 'x' должен быть числом, но это " + x.getTypeName()).getValue(),
                (int)y.toNumber("Параметр 'y' должен быть числом, но это " + y.getTypeName()).getValue());

        JSONEntity width = object.getItem("width");
        if (width != null)
            creature.setWidth((int)width.toNumber("Указанная ширина должна быть числом, но это " + width.getTypeName()).getValue());

        JSONEntity height = object.getItem("height");
        if (height != null)
            creature.setHeight((int)(height.toNumber("Указанная вцысота должна быть числом, но это " + height.getTypeName())).getValue());

        JSONEntity name = object.getItem("name");
        if (name != null)
            creature.setName(name.toString("Указанное имя должно быть строкой, но это " + name.getTypeName()).getContent());

        return creature;
    }

    /**
     * Создаёт существа их json-представления. Если получен json-объект, сгенерируется одно существо.
     * Если получен json-массив объектов, будет прочтён каждый объект внутри массива и возвращён
     * массив существ, сгенерированных из каждого объекта
     * @param json json-представление
     * @return массив существ
     * @throws Exception если что-то пойдет не так
     */
    public static Creature[] generate(String json) throws Exception {
        JSONEntity entity;

        try {
            entity = JSONParser.parse(json);
        } catch (JSONParseException e) {
            throw new JSONParseException(e.getMessage());
        }

        if (entity == null)
            throw new IllegalArgumentException("Требуется json-объект, но получен null");

        if (entity.isObject())
            return new Creature[]{generate(entity.toObject())};
        else if (entity.isArray()) {
            JSONArray array = entity.toArray();
            Creature[] creatures = new Creature[array.size()];
            for (int i = 0; i < array.size(); i++)
                creatures[i] = generate(
                        array.getItem(i).toObject(
                                "Все элементы массива должны быть объектами, но элемент с индексом " + i + " имеет тип " + array.getItem(i).getTypeName()
                        )
                );
            return creatures;
        } else
            throw new IllegalArgumentException("Нужен json-объект или json-массив, но вместо него " + entity.getTypeName());
    }
}
