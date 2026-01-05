// Bot Manager - Quản lý tất cả bots trong game
package com.tank2d.tankverse.core;

import com.tank2d.tankverse.entity.BotPlayer;
import com.tank2d.tankverse.entity.Player;
import com.tank2d.tankverse.map.MapLoader;
import javafx.scene.canvas.GraphicsContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Quản lý spawn, update, và draw tất cả bots
 */
public class BotManager {
    
    private List<BotPlayer> bots = new ArrayList<>();
    private MapLoader mapLoader;
    private Random random = new Random();
    private int botCounter = 0;
    
    public BotManager(MapLoader mapLoader) {
        this.mapLoader = mapLoader;
    }
    
    /**
     * Spawn một bot mới tại vị trí random
     */
    public void spawnBot() {
        double spawnX = 200 + random.nextDouble() * 800;
        double spawnY = 200 + random.nextDouble() * 800;
        
        botCounter++;
        String botName = "Bot_" + botCounter;
        
        BotPlayer bot = new BotPlayer(spawnX, spawnY, mapLoader, botName);
        bots.add(bot);
        
        System.out.println("[BotManager] Spawned " + botName + " at (" + spawnX + ", " + spawnY + ")");
    }
    
    /**
     * Spawn nhiều bots
     */
    public void spawnBots(int count) {
        for (int i = 0; i < count; i++) {
            spawnBot();
        }
    }
    
    /**
     * Spawn bot tại vị trí cụ thể
     */
    public void spawnBotAt(double x, double y, String customName) {
        String botName = customName != null ? customName : "Bot_" + (++botCounter);
        
        BotPlayer bot = new BotPlayer(x, y, mapLoader, botName);
        bots.add(bot);
        
        System.out.println("[BotManager] Spawned " + botName + " at (" + x + ", " + y + ")");
    }
    
    /**
     * Update tất cả bots
     */
    public void updateAll(PlayPanel panel) {
        for (BotPlayer bot : bots) {
            bot.update(panel);
        }
    }
    
    /**
     * Draw tất cả bots (relative to camera)
     */
    public void drawAll(GraphicsContext gc, Player camera) {
        for (BotPlayer bot : bots) {
            bot.draw(gc, camera);
        }
    }
    
    /**
     * Xóa bot theo tên
     */
    public boolean removeBot(String botName) {
        return bots.removeIf(bot -> bot.getName().equals(botName));
    }
    
    /**
     * Xóa tất cả bots
     */
    public void clearAllBots() {
        bots.clear();
        botCounter = 0;
        System.out.println("[BotManager] Cleared all bots");
    }
    
    /**
     * Lấy số lượng bots hiện tại
     */
    public int getBotCount() {
        return bots.size();
    }
    
    /**
     * Lấy danh sách bots
     */
    public List<BotPlayer> getBots() {
        return bots;
    }
    
    /**
     * Lấy bot theo tên
     */
    public BotPlayer getBot(String name) {
        for (BotPlayer bot : bots) {
            if (bot.getName().equals(name)) {
                return bot;
            }
        }
        return null;
    }
    
    /**
     * Lấy damage của bot (cho collision system)
     */
    public int getDamage(String botName) {
        BotPlayer bot = getBot(botName);
        return bot != null ? bot.getDmg() : 0;
    }
}
