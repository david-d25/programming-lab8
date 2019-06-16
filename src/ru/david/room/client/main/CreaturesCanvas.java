package ru.david.room.client.main;

import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import ru.david.room.CreatureModel;
import ru.david.room.Utils;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class CreaturesCanvas extends Canvas {
    private static final int AREA_SIZE = 1000;

    private ObservableList<CreatureModel> target;
    private Set<CreatureVisualBuffer> proxy = new HashSet<>();
    private Thread updatingThread = new Thread();

    void setTarget(ObservableList<CreatureModel> target) {
        this.target = target;

        updatingThread.interrupt();
        updatingThread = new Thread(() -> {
            try {
                long lastMillis = System.currentTimeMillis();
                while (true) {
                    Thread.sleep(1000 / 60);
                    update(System.currentTimeMillis() - lastMillis);
                    lastMillis = System.currentTimeMillis();
                }
            } catch (InterruptedException ignored) {
            }
        });
        updatingThread.setDaemon(true);
        updatingThread.start();
    }

    private void update(long delta) {
        for (CreatureModel model : target) {
            Optional<CreatureVisualBuffer> creatureBuffer = proxy.stream().filter((b) -> b.origin.getId() == model.getId()).findAny();
            if (creatureBuffer.isPresent())
                creatureBuffer.get().update(delta);
            else
                proxy.add(new CreatureVisualBuffer(model));
        }

        GraphicsContext context = getGraphicsContext2D();
        context.clearRect(0, 0, getWidth(), getHeight());
        context.save();

        if (getWidth() < getHeight()) {
            double ratio = getWidth()/AREA_SIZE;
            context.scale(ratio, ratio);
            context.translate(0, (getHeight() - AREA_SIZE*ratio)/2);
        } else {
            // TODO
            double ratio = getHeight()/AREA_SIZE;
            context.scale(ratio, ratio);
            context.translate((getWidth() - AREA_SIZE*ratio)/2, 0);
        }

        proxy.forEach((b) -> {
            // TODO: remove from proxy
            b.draw(context);
        });
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

        private double[] rayData = new double[32];

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
            context.setFill(Color.GREEN); // TODO
            context.setStroke(Color.GREEN.darker()); // TODO
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
            context.restore();
        }

        /**
         * Выполняет обновление буфера
         * @param delta шаг обновления
         */
        private void update(long delta) {
            for (int i = 0, rayDataLength = rayData.length; i < rayDataLength; i++) {
                rayData[i]++;
                double limit = origin.getRadius();

                Point2D[][] stuff = new Point2D[4][];
                stuff[0] = new Point2D[]{new Point2D(0, 0), new Point2D(AREA_SIZE, 0)};
                stuff[1] = new Point2D[]{new Point2D(AREA_SIZE, 0), new Point2D(AREA_SIZE, AREA_SIZE)};
                stuff[2] = new Point2D[]{new Point2D(AREA_SIZE, AREA_SIZE), new Point2D(0, AREA_SIZE)};
                stuff[3] = new Point2D[]{new Point2D(0, AREA_SIZE), new Point2D(0, 0)};

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
                    rayData[i] += (limit - rayData[i])/10f;
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
