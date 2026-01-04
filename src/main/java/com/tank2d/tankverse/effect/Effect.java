package com.tank2d.tankverse.effect;

import javafx.scene.canvas.GraphicsContext;

public abstract class Effect {

    protected boolean finished = false;

    public abstract void update(double dt);

    public abstract void draw(GraphicsContext gc,
                              double camX,
                              double camY);

    public boolean isFinished() {
        return finished;
    }
}
