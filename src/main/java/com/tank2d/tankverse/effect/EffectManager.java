package com.tank2d.tankverse.effect;

import javafx.scene.canvas.GraphicsContext;

import java.util.ArrayList;
import java.util.List;

public class EffectManager {

    private final List<Effect> effects = new ArrayList<>();

    public void addEffect(Effect effect) {
        effects.add(effect);
    }

    public void spawnExplosion(double x, double y) {
        effects.add(new ExplosionEffect(x, y));
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

    public void clear() {
        effects.clear();
    }
}
