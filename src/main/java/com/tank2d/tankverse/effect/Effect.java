package com.tank2d.tankverse.effect;


import javafx.scene.canvas.GraphicsContext;

public abstract class Effect {

    protected boolean finished = false;

    /** update logic (dt: seconds) */
    public abstract void update(double dt);

    /** draw effect (world → screen chuyển ở ngoài) */
    public abstract void draw(GraphicsContext gc,
                              double camX,
                              double camY);

    public boolean isFinished() {
        return finished;
    }
}
