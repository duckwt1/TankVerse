package com.tank2d.tankverse.effect;

import com.tank2d.tankverse.utils.Constant;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class ExplosionEffect extends Effect {

    private final double x;
    private final double y;
    private final double radius;

    private final List<Fragment> fragments = new ArrayList<>();

    public ExplosionEffect(double x, double y, double radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;

        int count = (int) (radius * 0.4); // radius càng lớn → nhiều mảnh

        for (int i = 0; i < count; i++) {
            fragments.add(new Fragment(x, y, radius));
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

            double screenX =
                    Constant.SCREEN_WIDTH / 2 + (f.x - camX);
            double screenY =
                    Constant.SCREEN_HEIGHT / 2 + (f.y - camY);

            gc.save();
            gc.translate(screenX, screenY);
            gc.rotate(f.rotation);
            gc.fillRect(
                    -f.size / 2,
                    -f.size / 2,
                    f.size,
                    f.size
            );
            gc.restore();
        }
    }
}
