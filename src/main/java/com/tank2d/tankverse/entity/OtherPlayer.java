package com.tank2d.tankverse.entity;

import com.tank2d.tankverse.core.PlayPanel;
import com.tank2d.tankverse.map.MapLoader;
import com.tank2d.tankverse.object.Bullet;
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
    private Image dieImage;
    // Góc thân (hiện tại) và góc mục tiêu (khi nhấn phím)
    private double bodyAngle = 0;
    private double targetAngle = 0;
    private double gunAngle = 0;
    public boolean backward; // trạng thái phím lùi
    private MapLoader mapLoader;
    public int solidAreaX = 12;
    public int solidAreaY = 16;
    public int solidWidth = (int) (Constant.PLAYER_TILE_SIZE * Constant.CHAR_SCALE - solidAreaX * 2 );
    public int solidHeight = (int) (Constant.PLAYER_TILE_SIZE * Constant.CHAR_SCALE - solidAreaY * 2);
    // Trạng thái phím
    public boolean up;
    public boolean down;
    public boolean left;
    public boolean right;

    private boolean isBouncing = false;
    private long bounceEndTime = 0;

    private double bounceVX = 0;
    private double bounceVY = 0;
    public int maxHp;
    public int hp;
    public int lastHp;
    public int mp;
    public int dmg;
    public int defense;
    public int crit;
    public int range;
    public int bullet = 0;

    // Tốc độ di chuyển và tốc độ xoay
   // private final double moveSpeed = 2.5;
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
        maxHp = 100;
        hp = maxHp;
        lastHp = hp;
        bullet = 4;
        dmg = 5;

    }

    @Override
    public void getImages() {
        try {
            bodyImage = new Image(getClass().getResourceAsStream("/com/tank2d/tankverse/tank/tank2.png"));
            gunImage = new Image(getClass().getResourceAsStream("/com/tank2d/tankverse/gun/gun1.png"));
            dieImage = new Image(getClass().getResourceAsStream("/com/tank2d/tankverse/tank/die.png"));
            gunPivotX = gunImage.getWidth() / 4;
            gunPivotY = gunImage.getHeight() / 2;

            System.out.println("✅ Loaded OtherPlayer images successfully.");
        } catch (Exception e) {
            System.out.println("❌ Error loading images for OtherPlayer:");
            e.printStackTrace();
        }
    }
    @Override
    public void initSolidArea() {
        solidArea = new Polygon();

        double rad = Math.toRadians(bodyAngle);

        double[][] corners = {
                { solidAreaX - bodyImage.getWidth()/2,  solidAreaY - bodyImage.getHeight()/2 },
                { solidAreaX + solidWidth - bodyImage.getWidth()/2,  solidAreaY - bodyImage.getHeight()/2 },
                { solidAreaX + solidWidth - bodyImage.getWidth()/2,  solidAreaY + solidHeight - bodyImage.getHeight()/2 },
                { solidAreaX - bodyImage.getWidth()/2,  solidAreaY + solidHeight - bodyImage.getHeight()/2 }
        };

        for (double[] c : corners) {
            double rx = c[0] * Math.cos(rad) - c[1] * Math.sin(rad);
            double ry = c[0] * Math.sin(rad) + c[1] * Math.cos(rad);

            solidArea.addPoint((int)(x + rx), (int)(y + ry));
        }
    }

    @Override
    public void update(PlayPanel panel) {
        if (this.isAlive == false)
        {
            return;
        }
        if (this.action == Constant.ACTION_CHARGE || this.action == Constant.ACTION_SHOOT)
        {
            //System.out.println("avasvcs");
            shootBullet();
            this.action = Constant.ACTION_NONE;
        }
        initSolidArea();

        Bullet collide = mapLoader.checkPlayerBulletCollision(this);
        if (collide != null) {
            int damage = panel.getDamage(collide.ownerName);

            hp -= damage;
            System.out.println("o player " + hp);
            if (hp < 0){
                hp = 0;
                this.isAlive = false;
                if (collide.ownerName.equals(player.getName()))
                {
                    player.kill ++;
                }
            }

        }
        // ===== HP SMOOTH UPDATE =====
        if (lastHp > hp) {
            lastHp -= hp / 100; // tốc độ tụt (tăng số này nếu muốn tụt nhanh hơn)
            if (lastHp < hp) {
                lastHp = hp;
            }
        }

        double dx = 0, dy = 0;

        if (up)    dy -= speed;
        if (down)  dy += speed;
        if (left)  dx -= speed;
        if (right) dx += speed;

        // BACKWARD (SPACE)
        if (backward) {
            double rad = Math.toRadians(bodyAngle);
            dx = -Math.cos(rad) * speed;
            dy = -Math.sin(rad) * speed;
        }

        // ===== ROTATION =====
        if (!backward && (dx != 0 || dy != 0)) {  // <= FIXED

            targetAngle = Math.toDegrees(Math.atan2(dy, dx));
            double diff = normalizeAngle(targetAngle - bodyAngle);

            double newAngle = bodyAngle + Math.signum(diff) * rotateSpeed;

            if (!willCollide(x, y, newAngle)) {
                bodyAngle = newAngle;
            } else {
                resolveRotationCollision();
                if (!willCollide(x, y, newAngle)) {
                    bodyAngle = newAngle;
                } else {
                    return;
                }
            }

            double rad = Math.toRadians(bodyAngle);
            dx = Math.cos(rad) * speed;
            dy = Math.sin(rad) * speed;
        }

        // ===== MOVEMENT =====
        if (willCollide(x + dx, y + dy, bodyAngle)) {
            System.out.println("collide");
            return;
        }

        double nextX = x + dx;
        double nextY = y + dy;

        if (!willCollide(nextX, nextY, bodyAngle)) {
            x = nextX;
            y = nextY;
        } else {
            //startBounce(dx, dy);
        }
    }
    private void startBounce(double dx, double dy) {

        isBouncing = true;
        bounceEndTime = System.currentTimeMillis() + 500; // 0.5s

        // Vector bật ngược lại
        double backX = -dx;
        double backY = -dy;

        double len = Math.sqrt(backX * backX + backY * backY);
        if (len == 0) return;

        backX /= len;
        backY /= len;

        // Tốc độ bật lại ban đầu (cảm giác mạnh)
        double initialBounceSpeed = 12;

        bounceVX = backX * initialBounceSpeed;
        bounceVY = backY * initialBounceSpeed;
    }

    private void resolveRotationCollision() {

        // đẩy nhẹ sang các hướng để thoát va chạm
        double[] pushX = { 1, -1,  0,  0, 1, -1 };
        double[] pushY = { 0,  0,  1, -1, 1, -1 };

        for (int i = 0; i < pushX.length; i++) {

            double testX = x + pushX[i];
            double testY = y + pushY[i];

            if (!willCollide(testX, testY, bodyAngle)) {
                // nếu thoát collision → cập nhật vị trí
                x = testX;
                y = testY;
                return;
            }
        }
    }


    @Override
    public void draw(GraphicsContext gc) {
        if (bodyImage == null || gunImage == null || player == null) return;

        double halfW = Constant.SCREEN_WIDTH / 2.0;
        double halfH = Constant.SCREEN_HEIGHT / 2.0;

        double camX = player.getX();
        double camY = player.getY();

        double buffer = 100;
        if (x < camX - halfW - buffer || x > camX + halfW + buffer ||
                y < camY - halfH - buffer || y > camY + halfH + buffer) {
            return;
        }

        double screenX = halfW + (x - camX);
        double screenY = halfH + (y - camY);

        double bodyW = bodyImage.getWidth();
        double bodyH = bodyImage.getHeight();

        // ===== Draw body (with rotation) =====
        gc.save();
        try {
            gc.translate(screenX, screenY);
            gc.rotate(bodyAngle);

            if (this.isAlive) {
                gc.drawImage(bodyImage, -bodyW / 2, -bodyH / 2);
            } else {
                gc.drawImage(dieImage, -bodyW / 2, -bodyH / 2);
            }
        } finally {
            gc.restore();
        }

        // ===== Draw gun (only if alive) =====
        if (this.isAlive) {
            gc.save();
            try {
                Affine transform = new Affine();
                transform.appendTranslation(screenX, screenY);
                transform.appendRotation(Math.toDegrees(gunAngle));
                gc.setTransform(transform);
                gc.drawImage(gunImage, -gunPivotX, -gunPivotY);
            } finally {
                gc.restore();
            }

            drawHp(gc, screenX, screenY, bodyH);
        }

        // ===== Draw name (NO transform active) =====
        gc.setFill(Color.WHITE);
        gc.fillText(playerName, screenX - 20, screenY - bodyH / 2 - 5);
    }

    private void drawHp(GraphicsContext gc,
                           double screenX,
                           double screenY,
                           double bodyH) {

        double barWidth = 40;
        double barHeight = 6;

        double hpPercent = (double) hp / maxHp;
        double lastHpPercent = (double) lastHp / maxHp;

        // Vị trí HP bar (trên đầu tank)
        double barX = screenX - barWidth / 2;
        double barY = screenY - bodyH / 2 - 12;

        // nền
        gc.setFill(Color.rgb(50, 50, 50, 0.8));
        gc.fillRect(barX, barY, barWidth, barHeight);

        // lastHp (tụt chậm – đỏ)
        gc.setFill(Color.RED);
        gc.fillRect(barX, barY, barWidth * lastHpPercent, barHeight);

        // hp thật (xanh)
        gc.setFill(Color.LIMEGREEN);
        gc.fillRect(barX, barY, barWidth * hpPercent, barHeight);

        // viền
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeRect(barX, barY, barWidth, barHeight);
    }



    /** Giúp giữ góc trong khoảng [-180, 180] */
    private double normalizeAngle(double angle) {
        angle %= 360;
        if (angle > 180) angle -= 360;
        if (angle < -180) angle += 360;
        return angle;
    }
    public boolean willCollide(double testX, double testY, double bodyAngle) {

        // Lưu lại solidArea cũ
        Polygon old = this.solidArea;

        // Tạo solidArea tạm
        this.solidArea = buildSolidArea(testX, testY, bodyAngle);

        // Kiểm tra collision
        boolean result = mapLoader.checkCollision(testX, testY, this);

        // Khôi phục solidArea cũ
        this.solidArea = old;

        return result;
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
    public void shootBullet() {
        if (bullet <= 0) {
            System.out.println("Hết đạn!");
            return;
        }

        bullet--; // trừ đạn

        double spawnX = x + Math.cos(gunAngle) * 30;
        double spawnY = y + Math.sin(gunAngle) * 30;

        // bullet là id đạn, ví dụ 1,2,3...
        Bullet b = new Bullet(spawnX, spawnY, gunAngle, playerName, 1);

        mapLoader.addBullet(b);

        System.out.println("► Player " + playerName + " bắn đạn loại " + this.bullet);
    }

    public Polygon buildSolidArea(double testX, double testY, double angleDeg) {
        Polygon p = new Polygon();

        double rad = Math.toRadians(angleDeg);

        double[][] corners = {
                { solidAreaX - bodyImage.getWidth()/2,  solidAreaY - bodyImage.getHeight()/2 },
                { solidAreaX + solidWidth - bodyImage.getWidth()/2,  solidAreaY - bodyImage.getHeight()/2 },
                { solidAreaX + solidWidth - bodyImage.getWidth()/2,  solidAreaY + solidHeight - bodyImage.getHeight()/2 },
                { solidAreaX - solidAreaX - bodyImage.getWidth()/2,  solidAreaY + solidHeight - bodyImage.getHeight()/2 }
        };

        for (double[] c : corners) {
            double rx = c[0] * Math.cos(rad) - c[1] * Math.sin(rad);
            double ry = c[0] * Math.sin(rad) + c[1] * Math.cos(rad);

            p.addPoint((int)(testX + rx), (int)(testY + ry));
        }

        return p;
    }
    public void setBodyAngle(double bodyAngle) {
        this.bodyAngle = bodyAngle;
    }
    public String getName() {
        return playerName;
    }

}
