// Pham Ngoc Duc - Lớp 23JIT - Trường VKU - MSSV: 23IT059
package com.tank2d.tankverse.entity;

import com.tank2d.tankverse.map.MapLoader;
import com.tank2d.tankverse.utils.Constant;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;

import java.awt.*;

public class Player extends Entity {
    private final String playerName;
    private Image bodyImage;
    private Image gunImage;

    // Góc thân (hiện tại) và góc mục tiêu (khi nhấn phím)
    private double bodyAngle = 0;
    private double targetAngle = 0;
    private double gunAngle = 0;
    private boolean backward; // trạng thái phím lùi
    private MapLoader mapLoader;
    public int solidAreaX = 12;
    public int solidAreaY = 16;
    public int solidWidth = (int) (Constant.PLAYER_TILE_SIZE * Constant.CHAR_SCALE - solidAreaX * 2 );
    public int solidHeight = (int) (Constant.PLAYER_TILE_SIZE * Constant.CHAR_SCALE - solidAreaY * 2);
    // Trạng thái phím
    private boolean up, down, left, right;

    // Tốc độ di chuyển và tốc độ xoay
    private final double moveSpeed = 2.5;
    private final double rotateSpeed = 6.0; // độ/khung hình

    // Pivot của nòng
    private double gunPivotX;
    private double gunPivotY;

    public Player(double x, double y, Polygon solidArea, double speed, MapLoader mapLoader, String playerName) {
        super(x, y, solidArea, speed, mapLoader);
        //initSolidArea();
        this.playerName = playerName;
        this.mapLoader = mapLoader;
        getImages();

    }
    public void initSolidArea() {
        solidArea = new Polygon();

        double rad = Math.toRadians(bodyAngle);

        // Tank center is the pivot
        double cx = x;
        double cy = y;

        // 4 corners BEFORE rotation (relative to body center)
        double[][] corners = {
                { solidAreaX - bodyImage.getWidth()/2,  solidAreaY - bodyImage.getHeight()/2 },
                { solidAreaX + solidWidth - bodyImage.getWidth()/2,  solidAreaY - bodyImage.getHeight()/2 },
                { solidAreaX + solidWidth - bodyImage.getWidth()/2,  solidAreaY + solidHeight - bodyImage.getHeight()/2 },
                { solidAreaX - bodyImage.getWidth()/2,  solidAreaY + solidHeight - bodyImage.getHeight()/2 }
        };

        // Apply rotation to all corners
        for (double[] c : corners) {
            double rx = c[0] * Math.cos(rad) - c[1] * Math.sin(rad);
            double ry = c[0] * Math.sin(rad) + c[1] * Math.cos(rad);

            solidArea.addPoint((int)(cx + rx), (int)(cy + ry));
        }
    }




    @Override
    public void getImages() {
        try {
            bodyImage = new Image(getClass().getResourceAsStream("/com/tank2d/tankverse/tank/tank2.png"));
            gunImage = new Image(getClass().getResourceAsStream("/com/tank2d/tankverse/gun/gun1.png"));

            gunPivotX = gunImage.getWidth() / 4; // vị trí xoay nòng
            gunPivotY = gunImage.getHeight() / 2;

            System.out.println(" Loaded Player images successfully.");
        } catch (Exception e) {
            System.out.println(" Error loading images for Player:");
            e.printStackTrace();
        }
    }

    @Override
    public void update() {
        initSolidArea();
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
             //Nếu có va chạm, bạn có thể thêm hiệu ứng dừng hoặc trượt nhẹ
             System.out.println("Collision detected!");
        }
    }


    @Override
    public void draw(GraphicsContext gc) {
        if (bodyImage == null || gunImage == null) return;

        double centerX = Constant.SCREEN_WIDTH / 2.0;
        double centerY = Constant.SCREEN_HEIGHT / 2.0;

        double bodyW = bodyImage.getWidth();
        double bodyH = bodyImage.getHeight();

        // --- Vẽ thân (xoay mượt theo hướng di chuyển) ---
        gc.save();
        gc.translate(centerX, centerY);
        gc.rotate(bodyAngle);
        gc.drawImage(bodyImage, -bodyW / 2, -bodyH / 2);
        gc.restore();

        // --- Vẽ nòng ---
        gc.save();
        Affine transform = new Affine();
        transform.appendTranslation(centerX, centerY);
        transform.appendRotation(Math.toDegrees(gunAngle));
        gc.setTransform(transform);
        gc.drawImage(gunImage, -gunPivotX, -gunPivotY);
        gc.restore();

        // --- Debug ---
        gc.setFill(Color.WHITE);
        gc.fillText(playerName, centerX - 20, centerY - bodyH / 2 - 5);
        gc.setFill(Color.RED);
        gc.fillText(String.format("x: %.1f, y: %.1f  angle: %.1f°", x, y, bodyAngle), 10, 20);
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
        //System.out.println("hello")
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

    public boolean isUp() {
        return up;
    }

    public boolean isDown() {
        return down;
    }

    public String getName() {
        return playerName;
    }

    public boolean isLeft() {
        return left;
    }

    public boolean isRight() {
        return right;
    }

    public boolean isBackward() {
        return backward;
    }

    public double getBodyAngle() {
        return bodyAngle;
    }

    public MapLoader getMapLoader() {
        return this.mapLoader;
    }

    public void setBodyAngle(double bodyAngle) {
        this.bodyAngle = bodyAngle;
    }

    public Polygon getWorldSolidArea() {
        Polygon worldPoly = new Polygon();

        for (int i = 0; i < solidArea.npoints; i++) {
            int worldX = (int) (x + solidArea.xpoints[i]);
            int worldY = (int) (y + solidArea.ypoints[i]);
            worldPoly.addPoint(worldX, worldY);
        }

        return worldPoly;
    }
    public void drawSolidArea(GraphicsContext gc) {
        if (solidArea == null) return;

        gc.setStroke(Color.LIME);
        gc.setLineWidth(2);

        double centerX = Constant.SCREEN_WIDTH / 2.0;
        double centerY = Constant.SCREEN_HEIGHT / 2.0;

        for (int i = 0; i < solidArea.npoints; i++) {
            // Convert world → screen
            double sx1 = centerX + (solidArea.xpoints[i] - x);
            double sy1 = centerY + (solidArea.ypoints[i] - y);

            int j = (i + 1) % solidArea.npoints;

            double sx2 = centerX + (solidArea.xpoints[j] - x);
            double sy2 = centerY + (solidArea.ypoints[j] - y);

            gc.strokeLine(sx1, sy1, sx2, sy2);
        }
    }


}
