package com.tank2d.tankverse.effect;

public class Fragment {

    // world position
    public double x, y;

    // direction (unit vector)
    public double dirX, dirY;

    // distance đã bay
    public double dist;

    // speed
    public double speed;

    // lifetime
    public double life;

    // visual
    public double size;
    public double rotation;
    public double rotationSpeed;

    public Fragment(double cx, double cy, double radius) {

        // góc ngẫu nhiên
        double angle = Math.random() * Math.PI * 2;

        // vị trí ban đầu: gần tâm
        double startR = Math.random() * radius * 0.2;
        x = cx + Math.cos(angle) * startR;
        y = cy + Math.sin(angle) * startR;

        dirX = Math.cos(angle);
        dirY = Math.sin(angle);

        speed = radius * (1.5 + Math.random()); // bán kính càng lớn → nổ càng mạnh
        life = 0.6 + Math.random() * 0.4;

        size = 3 + Math.random() * 4;
        rotation = Math.random() * 360;
        rotationSpeed = (Math.random() - 0.5) * 600;
    }

    public void update(double dt) {

        // random nhẹ làm quỹ đạo không thẳng tắp
        double jitter = 0.3;
        dirX += (Math.random() - 0.5) * jitter * dt;
        dirY += (Math.random() - 0.5) * jitter * dt;

        // normalize lại hướng
        double len = Math.sqrt(dirX * dirX + dirY * dirY);
        dirX /= len;
        dirY /= len;

        x += dirX * speed * dt;
        y += dirY * speed * dt;

        rotation += rotationSpeed * dt;
        life -= dt;
    }

    public boolean isDead() {
        return life <= 0;
    }
}
