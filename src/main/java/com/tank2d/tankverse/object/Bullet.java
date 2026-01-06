package com.tank2d.tankverse.object;

import com.tank2d.tankverse.entity.Player;
import com.tank2d.tankverse.map.MapLoader;
import com.tank2d.tankverse.utils.Constant;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.awt.*;

public class Bullet extends GameObject {

    private double angle;
    private double speed = 12;
    public String ownerName;
    private boolean active = true;
    private Polygon polygon;
    public Bullet(double x, double y, double angle, String ownerName, int bulletId) {
        super(x, y, new Image(Bullet.class.getResourceAsStream(
                "/com/tank2d/tankverse/bullet/bullet" + bulletId + ".png"
        )));
        System.out.println(" da tao dan thanh cong");
        this.angle = angle;
        this.ownerName = ownerName;
    }

    @Override
    public void update(Player player, MapLoader map) {
        if (!active) return;

        // move
        x += Math.cos(angle) * speed;
        y += Math.sin(angle) * speed;

        // Bullet polygon = 1 điểm, nhưng convert thành polygon 2x2 để tránh lỗi
        polygon = new java.awt.Polygon(
                new int[]{(int)x, (int)x+2, (int)x+2, (int)x},
                new int[]{(int)y, (int)y, (int)y+2, (int)y+2},
                4
        );

        // Kiểm tra collision
        if (map.checkBulletCollision(polygon)) {
            active = false;
            map.eManager.spawnExplosion(this.x, this.y, 20);
            //System.out.println("💥 Bullet hit a wall!");
        }

        // Out of screen
        if (x < 0 || x > map.width * Constant.TILESIZE ||
                y < 0 || y > map.height * Constant.TILESIZE) {
            active = false;
        }
    }


    @Override
    public void draw(GraphicsContext gc, Player player) {
        if (!active) return;

        double screenX = x - player.x + Constant.SCREEN_WIDTH / 2.0;
        double screenY = y - player.y + Constant.SCREEN_HEIGHT / 2.0;

        double w = image.getWidth();
        double h = image.getHeight();

        gc.save();
        gc.translate(screenX, screenY);
        gc.rotate(Math.toDegrees(angle));
        gc.drawImage(image, -w/2, -h/2);
        gc.restore();
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Polygon getPolygon() {
        return polygon;
    }

    public boolean isActive() {
        return active;
    }
}


