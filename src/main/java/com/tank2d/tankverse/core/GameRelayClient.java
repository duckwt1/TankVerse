package com.tank2d.tankverse.core;

import com.tank2d.tankverse.entity.Player;
import com.tank2d.tankverse.utils.PlayerState;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

/**
 * Unified UDP client that connects to GameRelayServer
 * Works for both host and non-host players
 */
public class GameRelayClient extends Thread {
    private final PlayPanel playPanel;
    private final String relayHost;
    private final int relayPort;
    private final int roomId;
    private boolean running = true;
    private final String username;

    public GameRelayClient(PlayPanel playPanel, String relayHost, int relayPort, int roomId) {
        this.playPanel = playPanel;
        this.relayHost = relayHost;
        this.relayPort = relayPort;
        this.roomId = roomId;
        this.username = playPanel.getPlayer().getName();
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName(relayHost);
            socket.setSoTimeout(1000);

            // Send JOIN message
            String joinMsg = "JOIN " + roomId + " " + username;
            byte[] joinData = joinMsg.getBytes();
            socket.send(new DatagramPacket(joinData, joinData.length, address, relayPort));
            System.out.println("[RelayClient] Sent JOIN to " + relayHost + ":" + relayPort + " (Room " + roomId + ")");
            
            // Wait for JOINED ACK
            byte[] ackBuffer = new byte[256];
            DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
            try {
                socket.receive(ackPacket);
                String ack = new String(ackPacket.getData(), 0, ackPacket.getLength()).trim();
                System.out.println("[RelayClient] Received: " + ack);
            } catch (SocketTimeoutException e) {
                System.err.println("[RelayClient] No JOIN ACK received (timeout)");
            }

            byte[] buffer = new byte[4096];
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

                byte[] data = msg.getBytes();
                socket.send(new DatagramPacket(data, data.length, address, relayPort));
                
                updateCount++;
                if (updateCount % 100 == 0) {
                    System.out.println("[RelayClient] Sent " + updateCount + " UPDATEs");
                }

                // Receive STATE broadcast from relay server
                DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                try {
                    socket.receive(response);
                    String reply = new String(response.getData(), 0, response.getLength()).trim();
                    
                    if (reply.startsWith("STATE")) {
                        parseState(reply);
                    }

                } catch (SocketTimeoutException ignored) {
                    // No state received this cycle, continue
                }

                Thread.sleep(50); // 20 updates per second
            }
            
            // Send LEAVE when stopping
            String leaveMsg = "LEAVE " + roomId + " " + username;
            byte[] leaveData = leaveMsg.getBytes();
            socket.send(new DatagramPacket(leaveData, leaveData.length, address, relayPort));
            System.out.println("[RelayClient] Sent LEAVE");

        } catch (Exception e) {
            System.err.println("[RelayClient] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void parseState(String stateMsg) {
        // STATE username1 x y bodyAngle gunAngle up down left right backward; username2 ...;
        String[] playersData = stateMsg.substring(6).trim().split(";");
        
        for (String pd : playersData) {
            pd = pd.trim();
            if (pd.isEmpty()) continue;
            
            String[] p = pd.split(" ");
            if (p.length != 10) {
                System.err.println("[RelayClient] Invalid player data (expected 10 parts): " + pd);
                continue;
            }

            String name = p[0];
            if (name.equals(username)) {
                // Skip self
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
    }
}
