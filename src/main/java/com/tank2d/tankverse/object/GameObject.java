package com.tank2d.tankverse.object;

import com.tank2d.tankverse.entity.Player;
import com.tank2d.tankverse.map.MapLoader;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public abstract class GameObject {
    protected double x;
    protected double y;
    protected Image image;

    public GameObject(double x, double y, Image image) {
        this.x = x;
        this.y = y;
        this.image = image;
    }

    public abstract void update(Player player, MapLoader map);

    public double getX() { return x; }
    public double getY() { return y; }


    public abstract void draw(GraphicsContext gc, Player player);
}
