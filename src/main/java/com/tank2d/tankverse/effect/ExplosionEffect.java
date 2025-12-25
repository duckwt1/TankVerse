package com.tank2d.tankverse.effect;

import com.tank2d.tankverse.utils.Constant;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class ExplosionEffect extends Effect {

    private final List<Fragment> fragments = new ArrayList<>();

    // center (world)
    private final double x;
    private final double y;

    public ExplosionEffect(double x, double y) {
        this.x = x;
        this.y = y;

        int count = 30 + (int)(Math.random() * 20); // explosion to

        for (int i = 0; i < count; i++) {
            fragments.add(new Fragment(x, y));
        }
    }

    @Override
    public void update(double dt) {
        fragments.removeIf(f -> {
            f.update(dt);
            return f.isDead();
        });

        if (fragments.isEmpty()) {
            finished = true;
        }
    }

    @Override
    public void draw(GraphicsContext gc,
                     double camX,
                     double camY) {

        gc.setFill(Color.ORANGE);

        for (Fragment f : fragments) {
            double sx = Constant.SCREEN_WIDTH / 2 + (f.x - camX);
            double sy = Constant.SCREEN_HEIGHT / 2 + (f.y - camY);

            gc.save();
            gc.translate(sx, sy);
            gc.rotate(f.rotation);
            gc.fillRect(
                    -f.width / 2,
                    -f.height / 2,
                    f.width,
                    f.height
            );
            gc.restore();
        }
    }


    class Fragment {

        // world position
        public double x, y;

        // velocity
        public double vx, vy;

        // rotation
        public double rotation;
        public double rotationSpeed;

        // lifetime
        public double life;

        // size
        public double width;
        public double height;

        public Fragment(double x, double y) {
            this.x = x;
            this.y = y;

            double angle = Math.random() * Math.PI * 2;
            double speed = 200 + Math.random() * 300; // mạnh → bay xa

            vx = Math.cos(angle) * speed;
            vy = Math.sin(angle) * speed;

            rotation = Math.random() * 360;
            rotationSpeed = (Math.random() - 0.5) * 720;

            width = 4 + Math.random() * 4;
            height = 2 + Math.random() * 3;

            life = 0.8 + Math.random() * 0.6;
        }

        public void update(double dt) {
            vy += 700 * dt;          // gravity
            x += vx * dt;
            y += vy * dt;
            rotation += rotationSpeed * dt;
            life -= dt;
        }

        public boolean isDead() {
            return life <= 0;
        }
    }

}
