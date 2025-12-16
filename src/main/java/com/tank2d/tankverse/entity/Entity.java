package com.tank2d.tankverse.entity;

import com.tank2d.tankverse.core.PlayPanel;
import com.tank2d.tankverse.map.MapLoader;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.awt.*;

public abstract class Entity {
    public double x;
    public double y;
    public Polygon solidArea;
    public double speed;
    public Image[] images;
    public int spriteNum = 0;
    public MapLoader mapLoader;
    public int action = 0;
    public Entity(double x, double y, Polygon solidArea, double speed, MapLoader mapLoader) {
        this.x = x;
        this.y = y;
        this.solidArea = solidArea;
        this.speed = speed;
        this.mapLoader = mapLoader;
    }
    public void initSolidArea(){};
    public void getImages() {}
    public void update() {}


    public abstract void update(PlayPanel panel);

    public void draw(GraphicsContext gc) {

    }
    public void drawCollision(GraphicsContext gc) {
        if (solidArea == null) {
            System.out.println("you didnt create a solid polygon");
            return;
        }

        // Lấy số điểm
        int n = solidArea.npoints;

        double[] xs = new double[n];
        double[] ys = new double[n];

        // Chuyển polygon AWT sang tọa độ màn hình JavaFX
        for (int i = 0; i < n; i++) {
            xs[i] = solidArea.xpoints[i] + x;
            ys[i] = solidArea.ypoints[i] + y;
        }

        // Vẽ viền polygon
        gc.setLineWidth(2);
        gc.setStroke(javafx.scene.paint.Color.RED);
        gc.strokePolygon(xs, ys, n);
    }

}
