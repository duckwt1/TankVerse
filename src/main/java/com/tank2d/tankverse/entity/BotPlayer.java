// Bot Tank AI - Simple Implementation
package com.tank2d.tankverse.entity;

import com.tank2d.tankverse.core.PlayPanel;
import com.tank2d.tankverse.map.MapLoader;
import com.tank2d.tankverse.object.Bullet;
import com.tank2d.tankverse.utils.Constant;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.awt.*;
import java.util.Random;

/**
 * Bot Tank với AI đơn giản
 * States: IDLE, CHASE, ATTACK, WANDER
 */
public class BotPlayer extends Entity {
    
    // ===== BOT INFO =====
    private final String botName;
    private Image bodyImage;
    private Image gunImage;
    private Image dieImage;
    
    // ===== ANGLES & MOVEMENT =====
    private double bodyAngle = 0;
    private double gunAngle = 0;
    private final double rotateSpeed = 2.0;
    
    // ===== COLLISION =====
    public int solidAreaX = 12;
    public int solidAreaY = 16;
    public int solidWidth = 40;
    public int solidHeight = 40;
    
    // ===== STATS =====
    public int maxHp = 25;
    public int hp = 25;
    public int lastHp = 25;
    public int dmg = 4;
    public int bullet = 999; // unlimited for bots
    public int kill = 0;
    public int die = 0;
    
    // ===== RESPAWN =====
    private boolean isRespawning = false;
    private long respawnEndTime = 0;
    private final int RESPAWN_SECONDS = 10;
    private double spawnX;
    private double spawnY;
    
    // ===== AI STATE MACHINE =====
    private enum BotState {
        IDLE,       // Đứng yên, quét tìm target
        WANDER,     // Đi lang thang random
        CHASE,      // Đuổi theo target
        ATTACK,     // Bắn target
        RETREAT     // Chạy trốn khi HP thấp
    }
    
    private BotState currentState = BotState.IDLE;
    private Player targetPlayer = null;
    
    // ===== AI PARAMETERS =====
    private final double DETECTION_RANGE = 400;    // Tầm phát hiện
    private final double ATTACK_RANGE = 350;       // Tầm bắn
    private final double SAFE_DISTANCE = 150;      // Khoảng cách an toàn
    private final double RETREAT_HP_PERCENT = 0.3; // Retreat khi HP < 30%
    
    // ===== AI TIMERS =====
    private long lastShootTime = 0;
    private final long SHOOT_COOLDOWN = 1000;      // 1 giây/phát
    private long stateChangeTime = 0;
    private final long WANDER_DURATION = 3000;     // Wander 3 giây
    
    // ===== WANDER MOVEMENT =====
    private double wanderTargetX;
    private double wanderTargetY;
    private Random random = new Random();
    
    // ===== GUN PIVOT =====
    private double gunPivotX;
    private double gunPivotY;
    
    public BotPlayer(double x, double y, MapLoader mapLoader, String botName) {
        super(x, y, null, 2.0, mapLoader); // speed = 2.0
        this.botName = botName;
        this.spawnX = x;
        this.spawnY = y;
        this.mapLoader = mapLoader;
        
        getImages();
        initSolidArea();
        
        // Random initial angle
        this.bodyAngle = random.nextDouble() * 360;
        this.gunAngle = Math.toRadians(bodyAngle);
    }
    
    @Override
    public void getImages() {
        try {
            bodyImage = new Image(getClass().getResourceAsStream("/com/tank2d/tankverse/tank/tank2.png"));
            gunImage = new Image(getClass().getResourceAsStream("/com/tank2d/tankverse/gun/gun1.png"));
            dieImage = new Image(getClass().getResourceAsStream("/com/tank2d/tankverse/tank/die.png"));
            
            gunPivotX = gunImage.getWidth() / 4;
            gunPivotY = gunImage.getHeight() / 2;
            
            System.out.println("[Bot] Loaded images for: " + botName);
        } catch (Exception e) {
            System.err.println("[Bot] Error loading images: " + e.getMessage());
        }
    }
    
    @Override
    public void initSolidArea() {
        solidArea = new Polygon();
        double rad = Math.toRadians(bodyAngle);
        
        double[][] corners = {
            { solidAreaX - 32, solidAreaY - 32 },
            { solidAreaX + solidWidth - 32, solidAreaY - 32 },
            { solidAreaX + solidWidth - 32, solidAreaY + solidHeight - 32 },
            { solidAreaX - 32, solidAreaY + solidHeight - 32 }
        };
        
        for (double[] c : corners) {
            double rx = c[0] * Math.cos(rad) - c[1] * Math.sin(rad);
            double ry = c[0] * Math.sin(rad) + c[1] * Math.cos(rad);
            solidArea.addPoint((int)(x + rx), (int)(y + ry));
        }
    }
    
    @Override
    public void update(PlayPanel panel) {
        if (!isAlive) {
            updateRespawnCountdown();
            return;
        }
        
        // Check if hit by bullet
        Bullet collide = mapLoader.checkPlayerBulletCollision(this);
        if (collide != null) {
            int damage = panel.getDamage(collide.ownerName);
            hp -= damage;
            
            if (hp <= 0 && isAlive) {
                hp = 0;
                isAlive = false;
                die++;
                startRespawnCountdown();
                return;
            }
        }
        
        // Update HP bar smoothly
        if (lastHp > hp) {
            lastHp -= 1;
            if (lastHp < hp) lastHp = hp;
        }
        
        // AI Update
        updateAI(panel);
        
        // Rebuild collision area
        initSolidArea();
    }
    
    /**
     * Main AI Logic
     */
    private void updateAI(PlayPanel panel) {
        // Find nearest player
        targetPlayer = findNearestPlayer(panel);
        
        // Check if should retreat
        if (shouldRetreat()) {
            currentState = BotState.RETREAT;
        }
        // State machine
        else if (targetPlayer != null) {
            double distToTarget = distanceTo(targetPlayer);
            
            if (distToTarget <= ATTACK_RANGE) {
                currentState = BotState.ATTACK;
            } else if (distToTarget <= DETECTION_RANGE) {
                currentState = BotState.CHASE;
            } else {
                currentState = BotState.WANDER;
            }
        } else {
            currentState = BotState.WANDER;
        }
        
        // Execute state behavior
        switch (currentState) {
            case IDLE:
                doIdle();
                break;
            case WANDER:
                doWander();
                break;
            case CHASE:
                doChase();
                break;
            case ATTACK:
                doAttack();
                break;
            case RETREAT:
                doRetreat();
                break;
        }
    }
    
    /**
     * IDLE: Stand still, rotate gun randomly
     */
    private void doIdle() {
        gunAngle += Math.toRadians(1);
    }
    
    /**
     * WANDER: Move randomly
     */
    private void doWander() {
        long now = System.currentTimeMillis();
        
        // Pick new wander target every 3 seconds
        if (now - stateChangeTime > WANDER_DURATION) {
            wanderTargetX = x + (random.nextDouble() - 0.5) * 400;
            wanderTargetY = y + (random.nextDouble() - 0.5) * 400;
            stateChangeTime = now;
        }
        
        // Move toward wander target
        moveToward(wanderTargetX, wanderTargetY);
    }
    
    /**
     * CHASE: Follow target player
     */
    private void doChase() {
        if (targetPlayer == null) return;
        
        moveToward(targetPlayer.getX(), targetPlayer.getY());
        aimAt(targetPlayer.getX(), targetPlayer.getY());
    }
    
    /**
     * ATTACK: Shoot at target
     */
    private void doAttack() {
        if (targetPlayer == null) return;
        
        // Aim at target
        aimAt(targetPlayer.getX(), targetPlayer.getY());
        
        // Strafe around target (circle movement)
        double angleToTarget = Math.atan2(targetPlayer.getY() - y, targetPlayer.getX() - x);
        double strafeAngle = angleToTarget + Math.PI / 2; // 90 degrees offset
        
        double strafeX = x + Math.cos(strafeAngle) * speed;
        double strafeY = y + Math.sin(strafeAngle) * speed;
        
        if (!willCollide(strafeX, strafeY, bodyAngle)) {
            x = strafeX;
            y = strafeY;
        }
        
        // Shoot
        tryShoot();
    }
    
    /**
     * RETREAT: Run away from target
     */
    private void doRetreat() {
        if (targetPlayer == null) return;
        
        // Run opposite direction
        double angleToTarget = Math.atan2(targetPlayer.getY() - y, targetPlayer.getX() - x);
        double fleeAngle = angleToTarget + Math.PI; // 180 degrees
        
        double fleeX = x + Math.cos(fleeAngle) * speed;
        double fleeY = y + Math.sin(fleeAngle) * speed;
        
        moveToward(fleeX, fleeY);
        
        // Still aim backward at target
        aimAt(targetPlayer.getX(), targetPlayer.getY());
    }
    
    /**
     * Move toward a position
     */
    private void moveToward(double targetX, double targetY) {
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance < 5) return; // Already there
        
        // Calculate target angle
        double targetAngle = Math.toDegrees(Math.atan2(dy, dx));
        
        // Rotate body smoothly toward target
        double angleDiff = normalizeAngle(targetAngle - bodyAngle);
        
        if (Math.abs(angleDiff) > rotateSpeed) {
            bodyAngle += Math.signum(angleDiff) * rotateSpeed;
        } else {
            bodyAngle = targetAngle;
        }
        
        bodyAngle = normalizeAngle(bodyAngle);
        
        // Move forward
        double rad = Math.toRadians(bodyAngle);
        double nextX = x + Math.cos(rad) * speed;
        double nextY = y + Math.sin(rad) * speed;
        
        if (!willCollide(nextX, nextY, bodyAngle)) {
            x = nextX;
            y = nextY;
        } else {
            // Try to navigate around obstacle
            bodyAngle += 30; // Turn 30 degrees
        }
    }
    
    /**
     * Aim gun at target
     */
    private void aimAt(double targetX, double targetY) {
        gunAngle = Math.atan2(targetY - y, targetX - x);
    }
    
    /**
     * Try to shoot with cooldown
     */
    private void tryShoot() {
        long now = System.currentTimeMillis();
        
        if (now - lastShootTime >= SHOOT_COOLDOWN) {
            shootBullet();
            lastShootTime = now;
        }
    }
    
    /**
     * Shoot bullet
     */
    private void shootBullet() {
        double spawnX = x + Math.cos(gunAngle) * 30;
        double spawnY = y + Math.sin(gunAngle) * 30;
        
        Bullet b = new Bullet(spawnX, spawnY, gunAngle, botName, 1);
        mapLoader.addBullet(b);
        
        //System.out.println("[Bot] " + botName + " fired!");
    }
    
    /**
     * Find nearest player
     */
    private Player findNearestPlayer(PlayPanel panel) {
        Player nearest = null;
        double minDist = Double.MAX_VALUE;
        
        // Tìm player gần nhất (panel.getPlayer() hoặc từ list)
        Player mainPlayer = panel.player;
        
        if (mainPlayer != null && mainPlayer.isAlive) {
            double dist = distanceTo(mainPlayer);
            if (dist < DETECTION_RANGE && dist < minDist) {
                nearest = mainPlayer;
                minDist = dist;
            }
        }
        
        return nearest;
    }
    
    /**
     * Calculate distance to target
     */
    private double distanceTo(Player target) {
        double dx = target.getX() - x;
        double dy = target.getY() - y;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Check if should retreat
     */
    private boolean shouldRetreat() {
        return (double) hp / maxHp < RETREAT_HP_PERCENT;
    }
    
    /**
     * Collision detection
     */
    public boolean willCollide(double testX, double testY, double angle) {
        Polygon old = this.solidArea;
        this.solidArea = buildSolidArea(testX, testY, angle);
        
        boolean result = mapLoader.checkCollision(testX, testY, this);
        
        this.solidArea = old;
        return result;
    }
    
    /**
     * Build collision polygon at test position
     */
    private Polygon buildSolidArea(double testX, double testY, double angleDeg) {
        Polygon p = new Polygon();
        double rad = Math.toRadians(angleDeg);
        
        double[][] corners = {
            { solidAreaX - 32, solidAreaY - 32 },
            { solidAreaX + solidWidth - 32, solidAreaY - 32 },
            { solidAreaX + solidWidth - 32, solidAreaY + solidHeight - 32 },
            { solidAreaX - 32, solidAreaY + solidHeight - 32 }
        };
        
        for (double[] c : corners) {
            double rx = c[0] * Math.cos(rad) - c[1] * Math.sin(rad);
            double ry = c[0] * Math.sin(rad) + c[1] * Math.cos(rad);
            p.addPoint((int)(testX + rx), (int)(testY + ry));
        }
        
        return p;
    }
    
    /**
     * Normalize angle to -180 to 180
     */
    private double normalizeAngle(double angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }
    
    /**
     * Respawn system
     */
    private void startRespawnCountdown() {
        isRespawning = true;
        respawnEndTime = System.currentTimeMillis() + RESPAWN_SECONDS * 1000L;
    }
    
    private void updateRespawnCountdown() {
        if (!isRespawning) return;
        
        long now = System.currentTimeMillis();
        if (now >= respawnEndTime) {
            respawn();
        }
    }
    
    private void respawn() {
        isAlive = true;
        isRespawning = false;
        hp = maxHp;
        lastHp = hp;
        bullet = 999;
        
        // Respawn at spawn point
        x = spawnX;
        y = spawnY;
        
        bodyAngle = random.nextDouble() * 360;
        gunAngle = Math.toRadians(bodyAngle);
        
        initSolidArea();
    }
    
    /**
     * Draw bot (relative to camera/player)
     */
    public void draw(GraphicsContext gc, Player camera) {
        if (bodyImage == null || gunImage == null) return;
        
        // Calculate screen position relative to camera
        double screenX = x - camera.getX() + Constant.SCREEN_WIDTH / 2.0;
        double screenY = y - camera.getY() + Constant.SCREEN_HEIGHT / 2.0;
        
        // Don't draw if off-screen
        if (screenX < -100 || screenX > Constant.SCREEN_WIDTH + 100 ||
            screenY < -100 || screenY > Constant.SCREEN_HEIGHT + 100) {
            return;
        }
        
        if (isAlive) {
            // Draw body
            gc.save();
            gc.translate(screenX, screenY);
            gc.rotate(bodyAngle);
            gc.drawImage(bodyImage, -bodyImage.getWidth() / 2, -bodyImage.getHeight() / 2);
            gc.restore();
            
            // Draw gun
            gc.save();
            gc.translate(screenX, screenY);
            gc.rotate(Math.toDegrees(gunAngle));
            gc.drawImage(gunImage, -gunPivotX, -gunPivotY);
            gc.restore();
            
            // Draw HP bar
            drawHpBar(gc, screenX, screenY);
            
            // Draw name
            gc.setFill(Color.YELLOW);
            gc.fillText(botName, screenX - 20, screenY - 35);
            
        } else {
            // Draw dead sprite
            gc.drawImage(dieImage, screenX - bodyImage.getWidth() / 2, 
                        screenY - bodyImage.getHeight() / 2);
        }
    }
    
    /**
     * Draw HP bar above bot
     */
    private void drawHpBar(GraphicsContext gc, double screenX, double screenY) {
        double barWidth = 40;
        double barHeight = 6;
        double barX = screenX - barWidth / 2;
        double barY = screenY - 30;
        
        double hpPercent = (double) hp / maxHp;
        double lastHpPercent = (double) lastHp / maxHp;
        
        // Background
        gc.setFill(Color.DARKGRAY);
        gc.fillRect(barX, barY, barWidth, barHeight);
        
        // Yellow bar (lastHp - smooth transition)
        gc.setFill(Color.YELLOW);
        gc.fillRect(barX, barY, barWidth * lastHpPercent, barHeight);
        
        // Green bar (current hp)
        gc.setFill(Color.LIME);
        gc.fillRect(barX, barY, barWidth * hpPercent, barHeight);
        
        // Border
        gc.setStroke(Color.BLACK);
        gc.strokeRect(barX, barY, barWidth, barHeight);
    }
    
    // ===== GETTERS =====
    public String getName() {
        return botName;
    }
    
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
    
    public int getHp() {
        return hp;
    }
    
    public int getDmg() {
        return dmg;
    }
    
    public Polygon getWorldSolidArea() {
        return solidArea;
    }
}
