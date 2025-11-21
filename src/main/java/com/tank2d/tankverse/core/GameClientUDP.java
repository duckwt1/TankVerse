package com.tank2d.tankverse.core;

import com.tank2d.tankverse.entity.Player;
import com.tank2d.tankverse.utils.PlayerState;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class GameClientUDP extends Thread {
    private final PlayPanel playPanel;
    private final String hostIp;
    private final int hostPort;
    private boolean running = true;
    private final String username;

    public GameClientUDP(PlayPanel playPanel, String hostIp, int hostPort) {
        this.playPanel = playPanel;
        this.hostIp = hostIp;
        this.hostPort = hostPort;
        this.username = playPanel.getPlayer().getName();
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName(hostIp);
            socket.setSoTimeout(1000);

            System.out.println("[ClientUDP] Connected to host " + hostIp + ":" + hostPort);

            byte[] buffer = new byte[4096];

            while (running) {
                Player player = playPanel.getPlayer();

                String msg = "UPDATE " + username + " " +
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
                socket.send(new DatagramPacket(data, data.length, address, hostPort));

                // Receive broadcast
                DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                try {
                    socket.receive(response);
                    String reply = new String(response.getData(), 0, response.getLength()).trim();
                    System.out.println(reply);
                    if (reply.startsWith("STATE")) {
                        String[] playersData = reply.substring(6).trim().split(";");
                        for (String pd : playersData) {
                            pd = pd.trim();
                            if (pd.isEmpty()) continue;
                            String[] p = pd.split(" ");
                            if (p.length != 10) continue;

                            String name = p[0];
                            if (name.equals(username)) continue;

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
                        }
                    }

                } catch (SocketTimeoutException ignored) {
                }

                Thread.sleep(50);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopClient() {
        running = false;
    }
}
