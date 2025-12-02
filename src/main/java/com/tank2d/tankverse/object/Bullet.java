package com.tank2d.tankverse.object;

import com.tank2d.tankverse.utils.Constant;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class Bullet extends GameObject {

    private double angle;          // Hướng bay (radians)
    private double speed = 12;     // Tốc độ bay
    private String ownerName;      // Player bắn ra

    private boolean active = true; // Khi đạn chạm tường/mục tiêu → inactive

    public Bullet(double x, double y, double angle, String ownerName, Image bulletImage) {
        super(x, y, bulletImage);
        this.angle = angle;
        this.ownerName = ownerName;
    }

    @Override
    public void update() {
        if (!active) return;

        double dx = Math.cos(angle) * speed;
        double dy = Math.sin(angle) * speed;

        x += dx;
        y += dy;

        // Ra ngoài màn hình → biến mất
        if (x < 0 || x > Constant.MAP_WIDTH || y < 0 || y > Constant.MAP_HEIGHT) {
            active = false;
        }
    }

    @Override
    public void draw(GraphicsContext gc) {
        if (!active) return;

        double w = image.getWidth();
        double h = image.getHeight();

        gc.save();
        gc.translate(x, y);
        gc.rotate(Math.toDegrees(angle));
        gc.drawImage(image, -w / 2, -h / 2);
        gc.restore();
    }

    public boolean isActive() {
        return active;
    }

    public void destroy() {
        active = false;
    }

    public String getOwnerName() {
        return ownerName;
    }
}
