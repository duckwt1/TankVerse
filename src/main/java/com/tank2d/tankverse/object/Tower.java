package com.tank2d.tankverse.object;

import com.tank2d.tankverse.entity.Player;
import com.tank2d.tankverse.map.MapLoader;
import com.tank2d.tankverse.utils.Constant;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.awt.*;

public class Tower extends GameObject {
    public int maxHp = 50;
    public int hp = maxHp;

    private Rectangle hitbox; // world-space
    private boolean alive = true;

    public Tower(double x, double y, int towerId) {

        super(x, y, new Image(Bullet.class.getResourceAsStream(
                "/com/tank2d/tankverse/tower/tower" + towerId + ".png"
        )));

        // hitbox theo size ảnh (world-space, neo giữa)
        int w = (int) image.getWidth();
        int h = (int) image.getHeight();
        hitbox = new Rectangle((int)(x - w/2), (int)(y - h/2), w, h);
    }
    @Override
    public void update(Player player, MapLoader map) {
        // tower đứng yên -> chỉ cần sync hitbox theo x,y (nếu sau này tower có move)
        if (!alive) return;
        int w = (int) image.getWidth();
        int h = (int) image.getHeight();
        hitbox.setBounds((int)(x - w/2), (int)(y - h/2), w, h);
    }
    public void takeDamage(int dmg) {
        if (!alive) return;
        hp -= dmg;
        if (hp <= 0) {
            hp = 0;
            alive = false;
        }
    }
    public boolean isAlive() {
        return alive;
    }

    public Rectangle getHitbox() {
        return hitbox;
    }

    @Override
    public void draw(GraphicsContext gc, Player player) {
        if (!alive) return;

        // world -> screen theo camera player
        double screenX = x - player.x + Constant.SCREEN_WIDTH / 2.0;
        double screenY = y - player.y + Constant.SCREEN_HEIGHT / 2.0;

        double w = image.getWidth();
        double h = image.getHeight();

        // vẽ tower (neo giữa)
        gc.drawImage(image, screenX - w/2, screenY - h/2);

        // vẽ HP bar trên tower
        double barW = 50;
        double barH = 6;
        double hpPct = (double) hp / maxHp;

        double barX = screenX - barW/2;
        double barY = screenY - h/2 - 12;

        gc.setFill(Color.rgb(0,0,0,0.6));
        gc.fillRect(barX, barY, barW, barH);

        gc.setFill(Color.LIMEGREEN);
        gc.fillRect(barX, barY, barW * hpPct, barH);

        gc.setStroke(Color.BLACK);
        gc.strokeRect(barX, barY, barW, barH);
    }
}
