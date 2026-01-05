package com.tank2d.tankverse.utils;

import javafx.scene.image.Image;

import java.io.InputStream;
import java.net.URL;

/**
 * Simple utility to load tank and item images from HTTP server
 */
public class AssetLoader {
    private static final String SERVER_URL = "http://localhost:8080/assets";
    
    /**
     * Load tank image by name from HTTP server
     * Falls back to embedded placeholder if not found
     */
    public static Image loadTankImage(String tankName) {
        try {
            // Convert tank name to filename: "VIP Tank" -> "vip_tank.png"
            String fileName = tankName.toLowerCase().replace(" ", "_") + ".png";
            String url = SERVER_URL + "/tanks/" + fileName;
            
            System.out.println("[AssetLoader] Loading tank: " + url);
            return new Image(url, true); // background loading
            
        } catch (Exception e) {
            System.err.println("[AssetLoader] Failed to load tank image: " + tankName);
            return getPlaceholderTank();
        }
    }
    
    /**
     * Load item image by name from HTTP server
     * Falls back to embedded placeholder if not found
     */
    public static Image loadItemImage(String itemName) {
        try {
            // Convert item name to filename: "Health Potion" -> "health_potion.png"
            String fileName = itemName.toLowerCase().replace(" ", "_") + ".png";
            String url = SERVER_URL + "/items/" + fileName;
            
            System.out.println("[AssetLoader] Loading item: " + url);
            return new Image(url, true); // background loading
            
        } catch (Exception e) {
            System.err.println("[AssetLoader] Failed to load item image: " + itemName);
            return getPlaceholderItem();
        }
    }
    
    /**
     * Get placeholder tank image from embedded resources
     */
    private static Image getPlaceholderTank() {
        try {
            InputStream stream = AssetLoader.class.getResourceAsStream("/com/tank2d/tankverse/tank/tank1.png");
            if (stream != null) {
                return new Image(stream);
            }
        } catch (Exception e) {
            // Ignore
        }
        // Return empty image if no placeholder found
        return new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");
    }
    
    /**
     * Get placeholder item image from embedded resources
     */
    private static Image getPlaceholderItem() {
        try {
            InputStream stream = AssetLoader.class.getResourceAsStream("/com/tank2d/tankverse/icon/health.png");
            if (stream != null) {
                return new Image(stream);
            }
        } catch (Exception e) {
            // Ignore
        }
        // Return empty image if no placeholder found
        return new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");
    }
    
    /**
     * Update server URL if needed (for remote servers)
     */
    public static void setServerUrl(String url) {
        // Could make SERVER_URL non-final and update it here
        System.out.println("[AssetLoader] Note: Server URL is hardcoded to localhost:8080");
    }
}
