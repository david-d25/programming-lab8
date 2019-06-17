package ru.david.room.client.main;

import javafx.animation.AnimationTimer;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import ru.david.room.CreatureModel;
import ru.david.room.Utils;

import java.util.*;

public class CreaturesCanvas extends Canvas {
    private static final int AREA_SIZE = 1000;
    private static final int PADDING = 50;

    private ObservableList<CreatureModel> target;
    private Set<CreatureVisualBuffer> proxy = new HashSet<>();
    private Thread updatingThread = new Thread();
    private HashMap<Integer, Color> userColors = new HashMap<>();
    private CreatureVisualBuffer selected = null;

    private CreatureSelectingListener listener = model -> {};

    void setTarget(ObservableList<CreatureModel> target) {
        this.target = target;

        selected = null;
        proxy.clear();

        updatingThread.interrupt();
        updatingThread = new Thread(() -> {
            try {
                long lastMillis = System.currentTimeMillis();
                while (true) {
                    Thread.sleep(1000 / 30);
                    update(System.currentTimeMillis() - lastMillis);
                    lastMillis = System.currentTimeMillis();
                }
            } catch (InterruptedException ignored) {
            }
        });
        updatingThread.setDaemon(true);
        updatingThread.start();

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                draw();
            }
        };
        timer.start();

        setOnMouseClicked(e -> onClicked(e.getX(), e.getY()));
    }

    HashMap<Integer, Color> getUserColors() {
        return userColors;
    }

    void selectCreature(CreatureModel model) {
        if (model == null) {
            selected = null;
            return;
        }

        for (CreatureVisualBuffer current : proxy) {
            if (current.origin.getId() == model.getId()) {
                selected = current;
                return;
            }
        }
    }

    void setSelectingListener(CreatureSelectingListener listener) {
        this.listener = listener;
    }

    private void onClicked(double x, double y) {
        double scale = getScale();
        Point2D translate = getTranslate();

        // Матрица преобразований
        double  a = 1, b = 0,
                c = 0, d = 1,
                e = 0, f = 0;

        // Перенос масшабирования и смещения на матрицу
        a /= scale;
        d /= scale;

        e -= translate.getX();
        f -= translate.getY();

        // Преобразование координат матрицей
        x = a*x + c*y + e;
        y = b*c + d*y + f;

        for (CreatureVisualBuffer current : proxy) {
            if (    Math.sqrt(  Math.pow(current.origin.getX() - x, 2) +
                                Math.pow(current.origin.getY() - y, 2)
                    ) < current.origin.getRadius()
            ) {
                listener.selected(current.origin);
                return;
            }
        }
        listener.selected(null);
        selected = null;
    }

    private void update(long delta) {
        for (CreatureModel model : target) {
            Optional<CreatureVisualBuffer> creatureBuffer = proxy.stream().filter((b) -> b.origin.getId() == model.getId()).findAny();
            if (creatureBuffer.isPresent())
                creatureBuffer.get().update(delta);
            else
                proxy.add(new CreatureVisualBuffer(model));
        }

        // TODO: remove from proxy
    }

    private void draw() {
        GraphicsContext context = getGraphicsContext2D();
        context.clearRect(0, 0, getWidth(), getHeight());
        context.save();

        double scale = getScale();
        Point2D translate = getTranslate();

        context.scale(scale, scale);
        context.translate(translate.getX(), translate.getY());

        proxy.forEach((b) -> b.draw(context));
        context.setStroke(Color.BLACK);
        context.setLineWidth(2);
        context.strokePolygon(
                new double[] {
                        0, AREA_SIZE, AREA_SIZE, 0, 0
                },
                new double[] {
                        0, 0, AREA_SIZE, AREA_SIZE, 0
                }, 5
        );

        if (selected != null)
            selected.drawSelectionOutline(context);
        context.restore();
    }

    /**
     * @return масштаб для вписывания в размер
     */
    private double getScale() {
        if (getWidth() < getHeight())
            return getWidth()/(AREA_SIZE + PADDING*2);
        else
            return getHeight()/(AREA_SIZE + PADDING*2);
    }

    /**
     * @return смещение для вписывания в размер
     */
    private Point2D getTranslate() {
        double scale = getScale();
        if (getWidth() < getHeight())
            return new Point2D(PADDING, (getHeight() - AREA_SIZE*scale)/2 + PADDING);
        else
            return new Point2D((getWidth() - AREA_SIZE*scale)/2 + PADDING, PADDING);
    }

    /**
     * Специальный класс-обёртка для {@link CreatureModel}, предоставляющий
     * функционал для отображения и анимации
     */
    private class CreatureVisualBuffer {
        private CreatureModel origin;

        private int visualX;
        private int visualY;

        private double[] rayData = new double[63];

        private CreatureVisualBuffer(CreatureModel origin) {
            this.origin = origin;

            visualX = origin.getX();
            visualY = origin.getY();
        }

        /**
         * Рисует целевое существо
         * @param context контекст холста
         */
        private void draw(GraphicsContext context) {
            context.save();

            Color color = Color.rgb(128, 128, 128, .5);

            if (userColors.containsKey(origin.getOwnerid()))
                color = userColors.get(origin.getOwnerid());

            context.setFill(color);
            context.setStroke(color.darker());
            context.setLineWidth(10);

            double[] xPoints = new double[rayData.length];
            double[] yPoints = new double[rayData.length];

            for (int i = 0, rayDataLength = rayData.length; i < rayDataLength; i++) {
                double ray = rayData[i];
                xPoints[i] = visualX + ray*Math.cos(1.0*i/rayDataLength*2*Math.PI);
                yPoints[i] = visualY + ray*Math.sin(1.0*i/rayDataLength*2*Math.PI);
            }

            context.fillPolygon(xPoints, yPoints, rayData.length);
            context.strokePolygon(xPoints, yPoints, rayData.length);

            context.setFill(Color.BLACK); // TODO
            context.setTextAlign(TextAlignment.CENTER);
            context.setFont(new Font(24));
            context.setTextBaseline(VPos.CENTER);
            context.fillText(origin.getName(), visualX, visualY);

            context.restore();
        }

        /**
         * Рисует рамку выбора целевого существа
         * @param context контекст золста
         */
        private void drawSelectionOutline(GraphicsContext context) {
            context.save();

            Color color = Color.rgb(255, 154, 0, .75);

            context.setLineWidth(4);
            context.setStroke(color);
            context.setLineDashes(10);
            context.setLineDashOffset(10);

            context.strokeRect(
                    visualX - origin.getRadius(),
                    visualY - origin.getRadius(),
                    origin.getRadius()*2,
                    origin.getRadius()*2
            );

            context.restore();
        }

        /**
         * Выполняет обновление буфера
         * @param delta шаг обновления
         */
        private void update(long delta) {
            for (int i = 0, rayDataLength = rayData.length; i < rayDataLength; i++) {
                rayData[i] += 2;
                double limit = origin.getRadius();

                // Линии столкновения
                ArrayList<Point2D[]> stuff = new ArrayList<>(proxy.size() + 4);
                stuff.add(new Point2D[]{new Point2D(0, 0), new Point2D(AREA_SIZE, 0)});
                stuff.add(new Point2D[]{new Point2D(AREA_SIZE, 0), new Point2D(AREA_SIZE, AREA_SIZE)});
                stuff.add(new Point2D[]{new Point2D(AREA_SIZE, AREA_SIZE), new Point2D(0, AREA_SIZE)});
                stuff.add(new Point2D[]{new Point2D(0, AREA_SIZE), new Point2D(0, 0)});

                for (CreatureVisualBuffer current : proxy) {
                    if (current.origin == origin)   // Не обрабатывать столкновение с самим собой
                        continue;

                    Point2D[] intersection = Utils.getCirclesIntersection(
                            new Point2D(visualX, visualY),
                            new Point2D(current.visualX, current.visualY),
                            origin.getRadius(),
                            current.origin.getRadius()
                    );
                    if (intersection.length == 2)
                        stuff.add(intersection);
                }

                for (Point2D[] line : stuff) {
                    Point2D intersection = Utils.getLinesIntersection(
                            new Point2D(visualX, visualY),
                            new Point2D(
                                    visualX + limit*Math.cos(1.0*i/rayDataLength*2*Math.PI),
                                    visualY + limit*Math.sin(1.0*i/rayDataLength*2*Math.PI)
                            ),
                            line[0], line[1]
                    );
                    if (intersection != null) {
                        double distance = new Point2D(visualX, visualY).distance(intersection);
                        if (distance < limit)
                            limit = distance;
                    }
                }

                if (rayData[i] > limit)
                    rayData[i] += (limit - rayData[i])/4f - 2;
            }
            visualX += (origin.getX() - visualX)/25f;
            visualY += (origin.getY() - visualY)/25f;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || obj.getClass() != getClass())
                return false;
            return origin.getId() == (((CreatureVisualBuffer)obj).origin.getId());
        }

        @Override
        public int hashCode() {
            return origin.hashCode();
        }
    }
}
