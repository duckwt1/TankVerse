package com.tank2d.tankverse.entity;

import com.tank2d.tankverse.map.MapLoader;
import com.tank2d.tankverse.utils.Constant;
import com.tank2d.tankverse.utils.PlayerState;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;

import java.awt.*;

public class OtherPlayer extends Entity{
    private final String playerName;
    private Image bodyImage;
    private Image gunImage;

    // Góc thân (hiện tại) và góc mục tiêu (khi nhấn phím)
    private double bodyAngle = 0;
    private double targetAngle = 0;
    private double gunAngle = 0;
    public boolean backward; // trạng thái phím lùi
    private MapLoader mapLoader;
    public int solidAreaX = 8;
    public int solidAreaY = 16;
    public int solidWidth = (int) (Constant.TILESIZE * Constant.CHAR_SCALE - solidAreaX * 2);
    public int solidHeight = (int) (Constant.TILESIZE * Constant.CHAR_SCALE - solidAreaY);
    // Trạng thái phím
    public boolean up;
    public boolean down;
    public boolean left;
    public boolean right;

    // Tốc độ di chuyển và tốc độ xoay
    private final double moveSpeed = 2.5;
    private final double rotateSpeed = 2.0; // độ/khung hình

    // Pivot của nòng
    private double gunPivotX;
    private double gunPivotY;
    private Player player;

    public OtherPlayer(double x, double y, Polygon solidArea, double speed, MapLoader mapLoader, String playerName, Player player) {
        super(x, y, solidArea, speed, mapLoader);
        this.playerName = playerName;
        this.mapLoader = mapLoader;
        this.player = player;
        getImages();

    }

    @Override
    public void getImages() {
        try {
            bodyImage = new Image(getClass().getResourceAsStream("/com/tank2d/tankverse/tank/tank2.png"));
            gunImage = new Image(getClass().getResourceAsStream("/com/tank2d/tankverse/gun/gun1.png"));

            gunPivotX = gunImage.getWidth() / 4;
            gunPivotY = gunImage.getHeight() / 2;

            System.out.println("✅ Loaded OtherPlayer images successfully.");
        } catch (Exception e) {
            System.out.println("❌ Error loading images for OtherPlayer:");
            e.printStackTrace();
        }
    }

    @Override
    public void update() {
        double dx = 0, dy = 0;

        if (up) dy -= moveSpeed;
        if (down) dy += moveSpeed;
        if (left) dx -= moveSpeed;
        if (right) dx += moveSpeed;

        double newX = x;
        double newY = y;

        if (dx != 0 || dy != 0) {
            // Tính góc đích (mục tiêu)
            targetAngle = Math.toDegrees(Math.atan2(dy, dx));
            double diff = normalizeAngle(targetAngle - bodyAngle);

            if (Math.abs(diff) < rotateSpeed) {
                bodyAngle = targetAngle;
            } else {
                bodyAngle += Math.signum(diff) * rotateSpeed;
            }

            // Tiến theo hướng thân
            double rad = Math.toRadians(bodyAngle);
            newX += Math.cos(rad) * moveSpeed;
            newY += Math.sin(rad) * moveSpeed;

        } else if (backward) {
            // Nếu nhấn SPACE → lùi
            double rad = Math.toRadians(bodyAngle);
            newX -= Math.cos(rad) * moveSpeed;
            newY -= Math.sin(rad) * moveSpeed;
        }

        // ---- Check collision ----
        if (!mapLoader.checkCollision(newX, newY, this)) {
            x = newX;
            y = newY;
        } else {
            // Nếu có va chạm, bạn có thể thêm hiệu ứng dừng hoặc trượt nhẹ
            // System.out.println("Collision detected!");
        }
    }


    @Override
    public void draw(GraphicsContext gc) {
        if (bodyImage == null || gunImage == null || player == null) return;

        // --- Define visible range around main player (the "camera view") ---

        double halfW = Constant.SCREEN_WIDTH / 2.0;
        double halfH = Constant.SCREEN_HEIGHT / 2.0;

        // Player’s camera center
        double camX = player.getX();
        double camY = player.getY();

        // Skip drawing if outside camera range (with small buffer)
        double buffer = 100; // draw slightly beyond screen edge to prevent pop-in
        if (x < camX - halfW - buffer || x > camX + halfW + buffer ||
                y < camY - halfH - buffer || y > camY + halfH + buffer) {
            return; // Not visible → skip draw
        }

        // --- Compute screen position relative to camera ---
        double screenX = halfW + (x - camX);
        double screenY = halfH + (y - camY);

        double bodyW = bodyImage.getWidth();
        double bodyH = bodyImage.getHeight();

        // --- Draw tank body ---
        gc.save();
        gc.translate(screenX, screenY);
        gc.rotate(bodyAngle);
        gc.drawImage(bodyImage, -bodyW / 2, -bodyH / 2);
        gc.restore();

        // --- Draw gun ---
        gc.save();
        Affine transform = new Affine();
        transform.appendTranslation(screenX, screenY);
        transform.appendRotation(Math.toDegrees(gunAngle));
        gc.setTransform(transform);
        gc.drawImage(gunImage, -gunPivotX, -gunPivotY);
        gc.restore();

        // --- Draw player name & debug info ---
        gc.setFill(Color.WHITE);
        gc.fillText(playerName, screenX - 20, screenY - bodyH / 2 - 5);
    }


    /** Giúp giữ góc trong khoảng [-180, 180] */
    private double normalizeAngle(double angle) {
        angle %= 360;
        if (angle > 180) angle -= 360;
        if (angle < -180) angle += 360;
        return angle;
    }

    /** Xoay nòng theo chuột */
    public void onMouseMoved(MouseEvent e) {
        double centerX = Constant.SCREEN_WIDTH / 2.0;
        double centerY = Constant.SCREEN_HEIGHT / 2.0;
        double dx = e.getX() - centerX;
        double dy = e.getY() - centerY;
        gunAngle = Math.atan2(dy, dx);
    }

    // ---- Phím điều khiển ----
    public void setUp(boolean value) { up = value;
       // System.out.println("hello"
        ;}
    public void setDown(boolean value) { down = value; }
    public void setLeft(boolean value) { left = value; }
    public void setRight(boolean value) { right = value; }
    public void setBackward(boolean value) { backward = value; }
    public void setX(double x)
    {
        this.x = x;
    }
    public double getGunAngle() {
        return gunAngle;
    }

    public void setGunAngle(double angle) {
        this.gunAngle = angle;
    }

    public void setY(double y)
    {
        this.y = y;
    }

    public double getX() {
        return this.x;
    }
    public double getY()
    {
        return this.y;
    }

    public double getBodyAngle() {
        return bodyAngle;
    }
    public void applyRemoteState(PlayerState state) {
        this.x = state.x;
        this.y = state.y;
        this.bodyAngle = state.bodyAngle;
        this.gunAngle = state.gunAngle;
        this.up = state.up;
        this.down = state.down;
        this.left = state.left;
        this.right = state.right;
        this.backward = state.backward;
        this.backward = state.backward;
    }
    public void setBodyAngle(double bodyAngle) {
        this.bodyAngle = bodyAngle;
    }
    public String getName() {
        return playerName;
    }

}
