// Pham Ngoc Duc - Lớp 23JIT - Trường VKU - MSSV: 23IT059
package com.tank2d.tankverse.entity;

import com.tank2d.tankverse.core.PlayPanel;
import com.tank2d.tankverse.map.MapLoader;
import com.tank2d.tankverse.object.Bullet;
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
    private Image dieImage;
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

    public int kill = 0;
    public int die = 0;
    //public int support = 0;

    // Tốc độ di chuyển và tốc độ xoay
    private final double rotateSpeed = 2.5; // độ/khung hình
    public int maxHp;
    public int hp;
    public int lastHp;
    public int mp;
    public int dmg;
    public int defense;
    public int crit;
    public int range;
    public int bullet = 100;
    // Pivot của nòng
    private double gunPivotX;
    private double gunPivotY;
    // ===== RESPAWN SYSTEM =====
    private boolean isRespawning = false;
    private long respawnEndTime = 0;          // thời điểm kết thúc countdown
    private final int RESPAWN_SECONDS = 10;   // 10s
    // ===== LIFE SYSTEM =====
    private boolean isOut = false;
    private long outEndTime = 0;
    private final int OUT_SECONDS = 3;
    // ===== WIN SYSTEM =====
    private boolean isWin = false;
    private long winEndTime = 0;
    private final int WIN_SECONDS = 3;

    // spawn position (có thể set từ ngoài)
    private double spawnX;
    private double spawnY;
    int maxLife = 3;

    public Player(double x, double y, Polygon solidArea, double speed, MapLoader mapLoader, String playerName) {
        super(x, y, solidArea, speed, mapLoader);
        //initSolidArea();
        this.playerName = playerName;
        this.mapLoader = mapLoader;
        getImages();
        maxHp = 30;
        hp = maxHp;
        lastHp = hp;
        bullet = 100;
        dmg = 5;
        this.spawnX = x;
        this.spawnY = y;
        spawnRandomInCenter(mapLoader);

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
            bodyImage = new Image(getClass().getResourceAsStream("/com/tank2d/tankverse/tank/heavy_tank.png"));
            gunImage = new Image(getClass().getResourceAsStream("/com/tank2d/tankverse/gun/gun1.png"));
            dieImage = new Image(getClass().getResourceAsStream("/com/tank2d/tankverse/tank/die.png"));

            gunPivotX = gunImage.getWidth() / 4; // vị trí xoay nòng
            gunPivotY = gunImage.getHeight() / 2;

            System.out.println(" Loaded Player images successfully.");
        } catch (Exception e) {
            System.out.println(" Error loading images for Player:");
            e.printStackTrace();
        }
    }

    @Override
    public void update(PlayPanel panel) {
        if (isWin) {
            updateWinCountdown(panel);
            return;
        }
        if (isOut) {
            updateOutCountdown(panel);
            return;
        }
        if (!this.isAlive) {
            updateRespawnCountdown();
            return;
        }


        if (action == Constant.ACTION_SHOOT)
        {
            shootBullet();
            action = Constant.ACTION_CHARGE;
        }
        Bullet collide = mapLoader.checkPlayerBulletCollision(this);
        if (collide != null) {
            int damage = (panel.getDamage(collide.ownerName));

            hp -= damage;
            if (hp <= 0 && this.isAlive) {
                hp = 0;
                this.isAlive = false;
                this.die++;
                mapLoader.eManager.spawnTankExplosion(this.x, this.y, this.bodyImage, 100);
                if (die >= maxLife) {
                    startOutCountdown();   // ❗ HẾT MẠNG
                } else {
                    startRespawnCountdown(); // còn mạng → hồi sinh
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

        initSolidArea();

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
            //System.out.println("collide");
            return;
        }
        if (mapLoader.checkPlayerBulletCollision(this) != null)
        {
            System.out.println("get hit");
        }
        double nextX = x + dx;
        double nextY = y + dy;

        if (!willCollide(nextX, nextY, bodyAngle)) {
            x = nextX;
            y = nextY;
        } else {
            startBounce(dx, dy);
        }
        checkWinCondition(panel);



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

    private void startRespawnCountdown() {
        isRespawning = true;
        respawnEndTime = System.currentTimeMillis() + RESPAWN_SECONDS * 1000L;

        // (tuỳ chọn) reset input để khỏi tiếp tục di chuyển khi sống lại
        up = down = left = right = backward = false;
    }

    private void updateRespawnCountdown() {
        if (!isRespawning) return;

        long now = System.currentTimeMillis();
        if (now >= respawnEndTime) {
            respawn();
        }
    }
    private void updateWinCountdown(PlayPanel panel) {
        long now = System.currentTimeMillis();
        if (now >= winEndTime) {
            panel.forceQuitGame(); // về main menu
        }
    }

    private void checkWinCondition(PlayPanel panel) {
        if (isWin || isOut) return;
        if (!this.isAlive) return;

        boolean allEnemyDead = true;

        for (var op : panel.players) {
            if (op.isAlive && op.hp > 0 || op.dieCount < 3) {
                allEnemyDead = false;
                break;
            }
        }

        if (allEnemyDead && panel.players.size() > 0) {
            startWinCountdown();
        }
    }
    private void startWinCountdown() {
        isWin = true;
        winEndTime = System.currentTimeMillis() + WIN_SECONDS * 1000L;

        // khóa input
        up = down = left = right = backward = false;
    }


    private void updateOutCountdown(PlayPanel panel) {
        long now = System.currentTimeMillis();
        if (now >= outEndTime) {
            panel.forceQuitGame(); // <<< thoát game
        }
    }


    private void respawn() {
        // reset stats
        this.isAlive = true;
        this.isRespawning = false;

        this.hp = this.maxHp;
        this.lastHp = this.hp;
        this.bullet = 100;

        // reset position to spawn
        this.x = spawnX;
        this.y = spawnY;
        spawnRandomInCenter(mapLoader);

        // reset angles (tuỳ bạn)
        // this.bodyAngle = 0;
        // this.gunAngle = 0;

        // reset input
        up = down = left = right = backward = false;

        // rebuild collision poly
        initSolidArea();
    }


    private void startBounce(double dx, double dy) {


        // Vector bật ngược lại
        double backX = -dx;
        double backY = -dy;

        double len = Math.sqrt(backX * backX + backY * backY);
        if (len == 0) return;

        backX /= len;
        backY /= len;

        // Tốc độ bật lại ban đầu (cảm giác mạnh)
        double initialBounceSpeed = 12;


    }



    @Override
    public void draw(GraphicsContext gc) {
        if (bodyImage == null || gunImage == null) return;
        double centerX = Constant.SCREEN_WIDTH / 2.0;
        double centerY = Constant.SCREEN_HEIGHT / 2.0;
        double bodyW = bodyImage.getWidth();
        double bodyH = bodyImage.getHeight();

        if (this.isAlive) {


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
            drawHp(gc);
        } else {
            // vẽ xác chết nằm giữa
            gc.drawImage(dieImage, centerX - bodyW / 2, centerY - bodyH / 2);

            // vẽ overlay hồi sinh
            drawRespawnOverlay(gc);
        }

        // --- Debug ---
        gc.setFill(Color.WHITE);
        gc.fillText(playerName, centerX - 20, centerY - bodyH / 2 - 5);
        gc.setFill(Color.RED);
        gc.fillText(String.format("x: %.1f, y: %.1f  angle: %.1f°", x, y, bodyAngle), 10, 20);
        drawKDA(gc);
        if (isOut) {
            drawOutOverlay(gc);
            return;
        }
        if (isWin) {
            drawWinOverlay(gc);
            return;
        }




    }
    private void drawWinOverlay(GraphicsContext gc) {
        long now = System.currentTimeMillis();
        long remainMs = Math.max(0, winEndTime - now);
        int remainSec = (int) Math.ceil(remainMs / 1000.0);

        gc.save();

        // nền mờ
        gc.setFill(Color.rgb(0, 0, 0, 0.70));
        gc.fillRect(0, 0, Constant.SCREEN_WIDTH, Constant.SCREEN_HEIGHT);

        // text
        String line1 = "VICTORY";
        String line2 = "Returning to lobby in " + remainSec + "s";

        gc.setFill(Color.GOLD);
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.EXTRA_BOLD, 56));
        drawCenteredText(gc, line1, Constant.SCREEN_HEIGHT / 2.0 - 20);

        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 24));
        drawCenteredText(gc, line2, Constant.SCREEN_HEIGHT / 2.0 + 30);

        gc.restore();
    }

    private void drawOutOverlay(GraphicsContext gc) {
        long now = System.currentTimeMillis();
        long remainMs = Math.max(0, outEndTime - now);
        int remainSec = (int) Math.ceil(remainMs / 1000.0);

        gc.save();

        // nền mờ
        gc.setFill(Color.rgb(0, 0, 0, 0.75));
        gc.fillRect(0, 0, Constant.SCREEN_WIDTH, Constant.SCREEN_HEIGHT);

        // text chính
        String line1 = "YOU ARE OUT";
        String line2 = "Leaving game in " + remainSec + "s";

        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 48));
        drawCenteredText(gc, line1, Constant.SCREEN_HEIGHT / 2.0 - 20);

        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 24));
        drawCenteredText(gc, line2, Constant.SCREEN_HEIGHT / 2.0 + 30);

        gc.restore();
    }
    private void drawCenteredText(GraphicsContext gc, String text, double y) {
        javafx.scene.text.Text t = new javafx.scene.text.Text(text);
        t.setFont(gc.getFont());
        double w = t.getLayoutBounds().getWidth();
        gc.fillText(text, (Constant.SCREEN_WIDTH - w) / 2.0, y);
    }
    public void spawnRandomInCenter(MapLoader mapLoader) {

        // ===== CONFIG =====
        int maxTry = 200; // số lần thử tối đa
        double centerRatio = 0.4; // 40% vùng trung tâm

        double mapW = mapLoader.width * Constant.TILESIZE;
        double mapH = mapLoader.height * Constant.TILESIZE;

        // vùng center
        double minX = mapW * (0.5 - centerRatio / 2);
        double maxX = mapW * (0.5 + centerRatio / 2);
        double minY = mapH * (0.5 - centerRatio / 2);
        double maxY = mapH * (0.5 + centerRatio / 2);

        for (int i = 0; i < maxTry; i++) {

            double rx = minX + Math.random() * (maxX - minX);
            double ry = minY + Math.random() * (maxY - minY);

            // thử với bodyAngle hiện tại
            Polygon testPoly = buildSolidArea(rx, ry, bodyAngle);

            Polygon old = this.solidArea;
            this.solidArea = testPoly;

            boolean collide = mapLoader.checkCollision(rx, ry, this);

            this.solidArea = old;

            if (!collide) {
                // ===== SPAWN THÀNH CÔNG =====
                this.x = rx;
                this.y = ry;
                this.spawnX = rx;
                this.spawnY = ry;

                initSolidArea();
                System.out.println("✅ Player spawned at (" + rx + ", " + ry + ")");
                return;
            }
        }

        // ===== FALLBACK =====
        System.err.println("⚠️ Cannot find valid spawn position, using default spawn.");
        this.x = spawnX;
        this.y = spawnY;
        initSolidArea();
    }


    private void drawHp(GraphicsContext gc)
    {
        double centerX = Constant.SCREEN_WIDTH / 2.0;
        double centerY = Constant.SCREEN_HEIGHT / 2.0;
        double bodyW = bodyImage.getWidth();
        double bodyH = bodyImage.getHeight();
        // ===== DRAW HP BAR =====
        double barWidth = 40;
        double barHeight = 6;

        double hpPercent = (double) hp / maxHp;
        double lastHpPercent = (double) lastHp / maxHp;

        double barX = centerX - barWidth / 2;
        double barY = centerY - bodyH / 2 - 15;

        gc.setFill(Color.DARKGRAY);
        gc.fillRect(barX, barY, barWidth, barHeight);

        gc.setFill(Color.RED);
        gc.fillRect(barX, barY, barWidth * lastHpPercent, barHeight);

        gc.setFill(Color.LIMEGREEN);
        gc.fillRect(barX, barY, barWidth * hpPercent, barHeight);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeRect(barX, barY, barWidth, barHeight);

    }
    private void startOutCountdown() {
        isOut = true;
        outEndTime = System.currentTimeMillis() + OUT_SECONDS * 1000L;

        // disable input
        up = down = left = right = backward = false;
    }

    private void drawRespawnOverlay(GraphicsContext gc) {
        if (!isRespawning) return;

        long now = System.currentTimeMillis();
        long remainMs = Math.max(0, respawnEndTime - now);
        int remainSec = (int) Math.ceil(remainMs / 1000.0);

        // nền mờ toàn màn
        gc.save();
        gc.setFill(Color.rgb(0, 0, 0, 0.60));
        gc.fillRect(0, 0, Constant.SCREEN_WIDTH, Constant.SCREEN_HEIGHT);

        // text giữa màn
        String line1 = "YOU DIED";
        String line2 = "Respawn in " + remainSec + "s";

        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 48));
        javafx.scene.text.Text t1 = new javafx.scene.text.Text(line1);
        t1.setFont(gc.getFont());
        double w1 = t1.getLayoutBounds().getWidth();

        gc.fillText(line1, (Constant.SCREEN_WIDTH - w1) / 2.0, Constant.SCREEN_HEIGHT / 2.0 - 20);

        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 24));
        javafx.scene.text.Text t2 = new javafx.scene.text.Text(line2);
        t2.setFont(gc.getFont());
        double w2 = t2.getLayoutBounds().getWidth();

        gc.fillText(line2, (Constant.SCREEN_WIDTH - w2) / 2.0, Constant.SCREEN_HEIGHT / 2.0 + 25);

        gc.restore();
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
    private void drawKDA(GraphicsContext gc) {

        String kdaText = kill + "  |  " + die;

        // ===== POSITION: CENTER TOP =====
        double centerX = Constant.SCREEN_WIDTH / 2.0;
        double y = 30; // khoảng cách từ top xuống

        gc.save();

        // ===== FONT =====
        double fontSize = 22;
        gc.setFont(javafx.scene.text.Font.font(
                "Arial",
                javafx.scene.text.FontWeight.BOLD,
                fontSize
        ));

        // ===== MEASURE TEXT =====
        javafx.scene.text.Text temp = new javafx.scene.text.Text(kdaText);
        temp.setFont(gc.getFont());
        double textW = temp.getLayoutBounds().getWidth();
        double textH = temp.getLayoutBounds().getHeight();

        // ===== BOX SIZE =====
        double padX = 18;
        double padY = 8;

        double boxW = textW + padX * 2;
        double boxH = textH + padY * 2;

        double boxX = centerX - boxW / 2;
        double boxY = y;

        // ===== DRAW BACKGROUND =====
        gc.setFill(Color.rgb(0, 0, 0, 0.55));
        gc.fillRoundRect(boxX, boxY, boxW, boxH, 16, 16);

        // ===== BORDER =====
        gc.setStroke(Color.rgb(255, 255, 255, 0.6));
        gc.setLineWidth(2);
        gc.strokeRoundRect(boxX, boxY, boxW, boxH, 16, 16);

        // ===== DRAW TEXT =====
        gc.setFill(Color.WHITE);
        gc.fillText(
                kdaText,
                centerX - textW / 2,
                boxY + boxH / 2 + textH / 4
        );

        gc.restore();
    }


    // ---- Phím điều khiển ----
    public void setUp(boolean value) { up = value;
        //System.out.println("hello")
    }
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

        //System.out.println("► Player " + playerName + " bắn đạn loại " + 1 + " con lai " + this.bullet + "vie dan");
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
