package com.tank2d.tankverse.core;

import com.tank2d.tankverse.entity.Player;
import com.tank2d.tankverse.utils.PlayerState;

import java.io.IOException;
import java.net.*;

/**
 * UDP Relay Client - connects to GameRelayServer for realtime game state synchronization
 * Works for both host and non-host players
 */
public class GameRelayClient extends Thread {
    private final PlayPanel playPanel;
    private final String relayHost;
    private final int relayPort;
    private final int roomId;
    private boolean running = true;
    private final String username;
    private DatagramSocket socket;

    public GameRelayClient(PlayPanel playPanel, String relayHost, int relayPort, int roomId) {
        this.playPanel = playPanel;
        this.relayHost = relayHost;
        this.relayPort = relayPort;
        this.roomId = roomId;
        this.username = playPanel.getPlayer().getName();
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(100);
            InetAddress serverAddr = InetAddress.getByName(relayHost);
            
            System.out.println("========================================");
            System.out.println("[RelayClient] Starting connection...");
            System.out.println("[RelayClient] Host: " + relayHost);
            System.out.println("[RelayClient] Port: " + relayPort);
            System.out.println("[RelayClient] Room: " + roomId);
            System.out.println("[RelayClient] Username: " + username);
            System.out.println("========================================");

            // Send JOIN message
            String joinMsg = "JOIN " + roomId + " " + username;
            System.out.println("[RelayClient] Sending: " + joinMsg);
            sendUDP(joinMsg, serverAddr);
            
            // Start receiver thread
            new Thread(this::receiveLoop).start();
            
            // Start sender loop
            int updateCount = 0;
            while (running) {
                Player player = playPanel.getPlayer();

                // Send UPDATE: "UPDATE roomId username x y bodyAngle gunAngle up down left right backward"
                String msg = "UPDATE " + roomId + " " + username + " " +
                        player.getX() + " " +
                        player.getY() + " " +
                        player.getBodyAngle() + " " +
                        player.getGunAngle() + " " +
                        (player.isUp() ? 1 : 0) + " " +
                        (player.isDown() ? 1 : 0) + " " +
                        (player.isLeft() ? 1 : 0) + " " +
                        (player.isRight() ? 1 : 0) + " " +
                        (player.isBackward() ? 1 : 0);

                sendUDP(msg, serverAddr);
                
                updateCount++;
                if (updateCount % 100 == 0) {
                    System.out.println("[RelayClient] Sent " + updateCount + " UPDATEs");
                }

                Thread.sleep(16); // ~60 updates per second for smooth gameplay
            }
            
            // Send LEAVE when stopping
            sendUDP("LEAVE " + roomId + " " + username, serverAddr);
            System.out.println("[RelayClient] Sent LEAVE");

        } catch (Exception e) {
            System.err.println("[RelayClient] Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }
    
    private void receiveLoop() {
        byte[] buffer = new byte[4096];
        int receiveCount = 0;
        System.out.println("[RelayClient] Receive loop started");
        try {
            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    receiveCount++;
                    String msg = new String(packet.getData(), 0, packet.getLength()).trim();
                    
                    if (receiveCount <= 5 || receiveCount % 100 == 0) {
                        System.out.println("[RelayClient] Received #" + receiveCount + ": " + msg.substring(0, Math.min(50, msg.length())));
                    }
                    
                    if (msg.startsWith("JOINED")) {
                        System.out.println("[RelayClient] ✓ " + msg);
                    } else if (msg.startsWith("STATE")) {
                        parseState(msg);
                    }
                } catch (SocketTimeoutException ignored) {
                    // Timeout is normal, continue
                }
            }
        } catch (Exception e) {
            if (running) {
                System.err.println("[RelayClient] Receive error: " + e.getMessage());
            }
        }
        System.out.println("[RelayClient] Receive loop ended (total received: " + receiveCount + ")");
    }
    
    private void sendUDP(String message, InetAddress serverAddr) {
        try {
            byte[] data = message.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddr, relayPort);
            socket.send(packet);
        } catch (Exception e) {
            System.err.println("[RelayClient] Send error: " + e.getMessage());
        }
    }

    private void parseState(String stateMsg) {
        // STATE username1 x y bodyAngle gunAngle up down left right backward; username2 ...;
        String[] playersData = stateMsg.substring(6).trim().split(";");
        
        System.out.println("[RelayClient] Received STATE with " + playersData.length + " players");
        
        for (String pd : playersData) {
            pd = pd.trim();
            if (pd.isEmpty()) continue;
            
            String[] p = pd.split(" ");
            if (p.length != 10) {
                System.err.println("[RelayClient] Invalid player data (expected 10 parts, got " + p.length + "): " + pd);
                continue;
            }

            String name = p[0];
            System.out.println("[RelayClient] Processing player: " + name + " (self: " + username + ")");
            
            if (name.equals(username)) {
                // Skip self
                System.out.println("[RelayClient] Skipping self: " + name);
                continue;
            }

            try {
                double x = Double.parseDouble(p[1]);
                double y = Double.parseDouble(p[2]);
                double body = Double.parseDouble(p[3]);
                double gun = Double.parseDouble(p[4]);
                boolean up = p[5].equals("1");
                boolean down = p[6].equals("1");
                boolean left = p[7].equals("1");
                boolean right = p[8].equals("1");
                boolean backward = p[9].equals("1");

                System.out.println("[RelayClient] Updating other player: " + name + " at (" + x + ", " + y + ")");
                
                // Always update for realtime P2P gameplay
                playPanel.updateOtherPlayer(
                    new PlayerState(name, x, y, body, gun, up, down, left, right, backward)
                );
            } catch (NumberFormatException e) {
                System.err.println("[RelayClient] Parse error for player " + name + ": " + e.getMessage());
            }
        }
    }

    public void stopClient() {
        running = false;
        cleanup();
    }
    
    private void cleanup() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
