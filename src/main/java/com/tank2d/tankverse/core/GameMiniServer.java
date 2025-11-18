package com.tank2d.tankverse.core;

import com.tank2d.tankverse.entity.OtherPlayer;
import com.tank2d.tankverse.entity.Player;
import com.tank2d.tankverse.utils.PlayerState;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class GameMiniServer extends Thread {
    private final PlayPanel playPanel;
    private final int port;
    private boolean running = true;

    private final Map<String, InetSocketAddress> clients = new HashMap<>();

    public GameMiniServer(PlayPanel playPanel, int port) {
        this.playPanel = playPanel;
        this.port = port;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            socket.setBroadcast(true);
            byte[] buffer = new byte[2048];
            System.out.println("[MiniServer] Running on UDP port " + port);

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength()).trim();
                InetSocketAddress clientAddr = new InetSocketAddress(packet.getAddress(), packet.getPort());

                String[] parts = msg.split(" ");
                if (parts.length == 11 && parts[0].equals("UPDATE")) {
                    String username = parts[1];
                    double x = Double.parseDouble(parts[2]);
                    double y = Double.parseDouble(parts[3]);
                    double bodyAngle = Double.parseDouble(parts[4]);
                    double gunAngle = Double.parseDouble(parts[5]);
                    boolean up = parts[6].equals("1");
                    boolean down = parts[7].equals("1");
                    boolean left = parts[8].equals("1");
                    boolean right = parts[9].equals("1");
                    boolean backward = parts[10].equals("1");

                    PlayerState ps = new PlayerState(username, x, y, bodyAngle, gunAngle,
                            up, down, left, right, backward);

                    playPanel.updateOtherPlayer(ps);
                    clients.put(username, clientAddr);

                    // ✅ Broadcast to everyone including sender
                    StringBuilder stateMsg = new StringBuilder("STATE ");
                    for (OtherPlayer op : playPanel.players) {
                        stateMsg.append(op.getName()).append(" ")
                                .append(op.x).append(" ")
                                .append(op.y).append(" ")
                                .append(op.getBodyAngle()).append(" ")
                                .append(op.getGunAngle()).append(" ")
                                .append(op.up ? 1 : 0).append(" ")
                                .append(op.down ? 1 : 0).append(" ")
                                .append(op.left ? 1 : 0).append(" ")
                                .append(op.right ? 1 : 0).append(" ")
                                .append(op.backward ? 1 : 0).append("; ");
                    }

                    Player self = playPanel.getPlayer();
                    stateMsg.append(self.getName()).append(" ")
                            .append(self.getX()).append(" ")
                            .append(self.getY()).append(" ")
                            .append(self.getBodyAngle()).append(" ")
                            .append(self.getGunAngle()).append(" ")
                            .append(self.isUp() ? 1 : 0).append(" ")
                            .append(self.isDown() ? 1 : 0).append(" ")
                            .append(self.isLeft() ? 1 : 0).append(" ")
                            .append(self.isRight() ? 1 : 0).append(" ")
                            .append(self.isBackward() ? 1 : 0).append("; ");

                    byte[] data = stateMsg.toString().getBytes();
                    for (InetSocketAddress addr : clients.values()) {
                        DatagramPacket resp = new DatagramPacket(data, data.length, addr);
                        socket.send(resp);
                    }

                } else {
                    System.out.println("[MiniServer] Invalid message: " + msg);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopServer() {
        running = false;
    }
}
