package com.tank2d.tankverse.effect;


import javafx.scene.image.Image;

public class ImageFragment {

    public double x, y;
    public double vx, vy;
    public double rotation;
    public double rotationSpeed;
    public double life = 1.2; // seconds
    public double size;

    public Image image;

    public ImageFragment(
            double x,
            double y,
            Image image,
            double forceRadius
    ) {
        this.x = x;
        this.y = y;
        this.image = image;
        this.size = image.getWidth();

        double angle = Math.random() * Math.PI * 2;
        double speed = 120 + Math.random() * forceRadius * 3;

        vx = Math.cos(angle) * speed;
        vy = Math.sin(angle) * speed;

        rotation = Math.random() * 360;
        rotationSpeed = (Math.random() - 0.5) * 720;
    }

    public void update(double dt) {
        life -= dt;

        x += vx * dt;
        y += vy * dt;
        vy += 600 * dt; // gravity
        rotation += rotationSpeed * dt;
    }

    public boolean isDead() {
        return life <= 0;
    }
}
