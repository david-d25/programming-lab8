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

    void setTarget(ObservableList<CreatureModel> target) {
        this.target = target;

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
    }

    public HashMap<Integer, Color> getUserColors() {
        return userColors;
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

        if (getWidth() < getHeight()) {
            double ratio = getWidth()/(AREA_SIZE + PADDING*2);
            context.scale(ratio, ratio);
            context.translate(PADDING, (getHeight() - AREA_SIZE*ratio)/2 + PADDING);
        } else {
            double ratio = getHeight()/(AREA_SIZE + PADDING*2);
            context.scale(ratio, ratio);
            context.translate((getWidth() - AREA_SIZE*ratio)/2 + PADDING, PADDING);
        }

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
        context.restore();
    }

    /**
     * Специальный класс-обёртка для {@link CreatureModel}, предоставляющий
     * функционал для отображения и анимации
     */
    private class CreatureVisualBuffer {
        private CreatureModel origin;

        private int actualX;
        private int actualY;

        private double[] rayData = new double[64];

        private CreatureVisualBuffer(CreatureModel origin) {
            this.origin = origin;

            actualX = origin.getX();
            actualY = origin.getY();
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
                xPoints[i] = actualX + ray*Math.cos(1.0*i/rayDataLength*2*Math.PI);
                yPoints[i] = actualY + ray*Math.sin(1.0*i/rayDataLength*2*Math.PI);
            }

            context.fillPolygon(xPoints, yPoints, rayData.length);
            context.strokePolygon(xPoints, yPoints, rayData.length);

            context.setFill(Color.BLACK); // TODO
            context.setTextAlign(TextAlignment.CENTER);
            context.setFont(new Font(24));
            context.setTextBaseline(VPos.CENTER);
            context.fillText(origin.getName(), actualX, actualY);

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
                            new Point2D(origin.getX(), origin.getY()),
                            new Point2D(current.origin.getX(), current.origin.getY()),
                            origin.getRadius(),
                            current.origin.getRadius()
                    );
                    if (intersection.length == 2)
                        stuff.add(intersection);
                }

                for (Point2D[] line : stuff) {
                    Point2D intersection = Utils.getLinesIntersection(
                            new Point2D(actualX, actualY),
                            new Point2D(
                                    actualX + limit*Math.cos(1.0*i/rayDataLength*2*Math.PI),
                                    actualY + limit*Math.sin(1.0*i/rayDataLength*2*Math.PI)
                            ),
                            line[0], line[1]
                    );
                    if (intersection != null) {
                        double distance = new Point2D(actualX, actualY).distance(intersection);
                        if (distance < limit)
                            limit = distance;
                    }
                }

                if (rayData[i] > limit)
                    rayData[i] += (limit - rayData[i])/10f - 2;
            }
            actualX += (origin.getX() - actualX)/10f;
            actualY += (origin.getY() - actualY)/10f;
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
