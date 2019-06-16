package ru.david.room;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Модель существа с координатами, размером, именем и временем создания.
 */
public class CreatureModel implements Serializable, Comparable<CreatureModel> {
    private int x, y;
    private float radius;
    private long id;
    private int ownerid;
    private String name;
    private ZonedDateTime created;

    private CreatureModel() {}

    public CreatureModel(long id, int x, int y, float radius, int ownerid, String name) {
        this.x = x;
        this.y = y;
        this.radius = radius;

        this.id = id;
        this.name = name;
        this.ownerid = ownerid;
        this.created = ZonedDateTime.now();
    }

    /**
     * Создаёт существо на основе объекта {@link ResultSet}
     * @param set объект с данными
     * @return экземпляр существа
     * @throws SQLException если что-то пойдёт не так
     */
    public static CreatureModel fromResultSet(ResultSet set) throws SQLException {
        CreatureModel result = new CreatureModel();
        result.id = set.getLong("id");
        result.x = set.getInt("x");
        result.y = set.getInt("y");
        result.radius = set.getFloat("radius");
        result.name = set.getString("name");
        result.ownerid = set.getInt("ownerid");
        result.created = ZonedDateTime.ofInstant(set.getTimestamp("created").toInstant(), ZoneId.systemDefault());
        return result;
    }

    public void setFromCreatureModel(CreatureModel model) {
        id = model.id;
        x = model.x;
        y = model.y;
        radius = model.radius;
        ownerid = model.ownerid;
        name = model.name;
        created = model.created;

    }

    /**
     * Уникальный идентифкатор существа. Можно использовать для сравнения существ.
     * У двух одинаковых по параметрам существ может быть разный идентификатор.
     * У двух разных существ не может быть один идентифкатор.
     *
     * @return уникальный идентификатор существа
     */
    public long getId() {
        return id;
    }

    /**
     * @return имя существа
     */
    public String getName() {
        return name;
    }

    /**
     * @return x-координата существа
     */
    public int getX() {
        return x;
    }

    /**
     * @return y-координата существа
     */
    public int getY() {
        return y;
    }

    /**
     * @return радиус существа
     */
    public float getRadius() {
        return radius;
    }

    /**
     * @return id владельца существа
     */
    public int getOwnerid() {
        return ownerid;
    }

    /**
     * @return время создания существа
     */
    public ZonedDateTime getCreated() {
        return created;
    }

    /**
     * Определяет числовой эквивалент "крутости" существа
     * @return крутость существа
     */
    private int getCoolness() {
        return getX() + getY();
    }

    @Override
    public int compareTo(CreatureModel o) {
        return getCoolness() - o.getCoolness();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || o.getClass() != getClass()) return false;
        if (this == o) return true;
        CreatureModel c = (CreatureModel) o;
        return  c.getName().equals(getName()) &&
                c.getX() == getX() &&
                c.getY() == getY() &&
                c.getRadius() == getRadius() &&
                c.getId() == getId() &&
                c.getOwnerid() == c.getOwnerid() &&
                c.getCreated().equals(getCreated());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, getX(), getY(), getRadius(), getId(), getOwnerid(), getCreated());
    }
}
