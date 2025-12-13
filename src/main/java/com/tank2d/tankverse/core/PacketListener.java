package com.tank2d.tankverse.core;

import com.tank2d.tankverse.utils.Packet;

import java.util.List;
import java.util.Map;

/**
 * Interface for handling packets received from server.
 * Controllers should implement this to handle server responses.
 */
public interface PacketListener {
    
    // Authentication events
    default void onLoginSuccess(String message) {}
    default void onLoginFail(String message) {}
    default void onRegisterSuccess(String message) {}
    default void onRegisterFail(String message) {}
    
    // Room events
    default void onRoomCreated(int roomId, String roomName, int maxPlayers, List<String> players) {}
    default void onRoomJoined(int roomId, String roomName, int maxPlayers, List<String> players) {}
    default void onRoomUpdate(String message, List<String> players, int maxPlayers) {}
    default void onRoomListReceived(List<Map<String, Object>> rooms) {}
    default void onMapSelected(String mapName) {}
    
    // Game events
    default void onGameStart(Packet p) {}
    
    // Shop events
    default void onShopListReceived(List<Map<String, Object>> items, int gold) {}
    default void onBuySuccess(int newGold, String message) {}
    default void onBuyFail(String message) {}
    
    // Inventory events
    default void onInventoryReceived(List<Map<String, Object>> tanks, List<Map<String, Object>> items, int gold) {}
    default void onTankShopDataReceived(List<Map<String, Object>> tanks, int gold) {}
    default void onEquipTankSuccess(int tankId) {}
    default void onEquipTankFail(String msg) {}
    
    // Connection events
    default void onDisconnected() {}
}
