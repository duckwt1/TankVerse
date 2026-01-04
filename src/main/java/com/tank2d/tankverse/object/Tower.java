package com.tank2d.tankverse.object;

import com.tank2d.tankverse.entity.Player;
import com.tank2d.tankverse.map.MapLoader;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class Tower extends GameObject {
    public Tower(double x, double y, Image image) {
        super(x, y, image);
    }

    @Override
    public void update(Player player, MapLoader map) {

    }

    @Override
    public void draw(GraphicsContext gc, Player player) {

    }
}
