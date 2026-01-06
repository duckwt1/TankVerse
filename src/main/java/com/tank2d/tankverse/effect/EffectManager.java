package com.tank2d.tankverse.effect;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.util.ArrayList;
import java.util.List;

public class EffectManager {

    private final List<Effect> effects = new ArrayList<>();

    public void add(Effect effect) {
        effects.add(effect);
    }

    public void spawnExplosion(double x, double y, double radius) {
        effects.add(new ExplosionEffect(x, y, radius));
    }

    public void update(double dt) {
        effects.removeIf(e -> {
            e.update(dt);
            return e.isFinished();
        });
    }

    public void draw(GraphicsContext gc,
                     double camX,
                     double camY) {
        for (Effect e : effects) {
            e.draw(gc, camX, camY);
        }
    }
    public void spawnTankExplosion(
            double x,
            double y,
            Image tankImage,
            double radius
    ) {
        add(new ImageExplosionEffect(
                x,
                y,
                tankImage,
                36, // number of pieces
                radius
        ));
    }

    public void clear() {
        effects.clear();
    }
}
