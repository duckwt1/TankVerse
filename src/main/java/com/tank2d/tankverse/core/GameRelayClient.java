package com.tank2d.tankverse.core;

import com.tank2d.tankverse.entity.Player;
import com.tank2d.tankverse.utils.PlayerState;

import java.net.*;
import java.util.Arrays;

public class GameRelayClient extends Thread {

    private final PlayPanel playPanel;
    private final String relayHost;
    private final int relayPort;
    private final int roomId;
    private final String username;
    private volatile boolean running = true;

    private DatagramSocket socket;
    private InetAddress serverAddr;

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
            socket.setSoTimeout(200);
            serverAddr = InetAddress.getByName(relayHost);

            // Send JOIN
            sendRaw("JOIN " + roomId + " " + username);
            System.out.println("[RelayClient] JOIN sent");

            // Try to get JOINED ack
            try {
                byte[] buf = new byte[256];
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                socket.receive(pkt);
                System.out.println("[RelayClient] ACK: " +
                        new String(pkt.getData(), 0, pkt.getLength()));
            } catch (SocketTimeoutException ignored) {}

            // Start sender + receiver threads
            Thread sendThread = new Thread(this::sendLoop, "SendThread");
            Thread recvThread = new Thread(this::receiveLoop, "ReceiveThread");

            sendThread.start();
            recvThread.start();

            // Wait until finished
            sendThread.join();
            recvThread.join();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (socket != null) socket.close();
        }
    }

    //====================================================
    // SEND LOOP (20–60 FPS)
    //====================================================
    private void sendLoop() {
        long interval = 16; // ~60 fps
        long last = System.currentTimeMillis();

        int count = 0;

        while (running) {
            long now = System.currentTimeMillis();
            if (now - last >= interval) {

                Player player = playPanel.getPlayer();

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

                sendRaw(msg);
                count++;

                if (count % 200 == 0)
                    System.out.println("[RelayClient] Sent " + count + " updates");

                last = now;
            }

            try { Thread.sleep(1); } catch (Exception ignored) {}
        }

        // send LEAVE when stopping
        sendRaw("LEAVE " + roomId + " " + username);
        System.out.println("[RelayClient] LEAVE sent");
    }

    //====================================================
    // RECEIVE LOOP
    //====================================================
    private void receiveLoop() {
        byte[] buffer = new byte[4096];

        while (running) {
            try {
                DatagramPacket pkt = new DatagramPacket(buffer, buffer.length);
                socket.receive(pkt);

                String msg = new String(pkt.getData(), 0, pkt.getLength()).trim();

                if (msg.startsWith("STATE"))
                    parseState(msg);

            } catch (SocketTimeoutException ignored) {
                // no packet in this frame
            } catch (Exception e) {
                System.err.println("[RelayClient] Receive error: " + e.getMessage());
            }
        }
    }

    //====================================================
    // SEND RAW UDP
    //====================================================
    private void sendRaw(String text) {
        try {
            byte[] data = text.getBytes();
            DatagramPacket p = new DatagramPacket(data, data.length, serverAddr, relayPort);
            socket.send(p);
        } catch (Exception e) {
            System.err.println("[RelayClient] Send error: " + e.getMessage());
        }
    }

    //====================================================
    // PROCESS STATE PACKET
    //====================================================
    private void parseState(String msg) {
        String[] players = msg.substring(6).trim().split(";");

        for (String pd : players) {
            pd = pd.trim();
            if (pd.isEmpty()) continue;

            String[] p = pd.split(" ");
            if (p.length != 10) {
                System.err.println("[RelayClient] Invalid state packet: " +
                        Arrays.toString(p));
                continue;
            }

            if (p[0].equals(username)) continue;

            try {
                PlayerState ps = new PlayerState(
                        p[0],
                        Double.parseDouble(p[1]),
                        Double.parseDouble(p[2]),
                        Double.parseDouble(p[3]),
                        Double.parseDouble(p[4]),
                        p[5].equals("1"),
                        p[6].equals("1"),
                        p[7].equals("1"),
                        p[8].equals("1"),
                        p[9].equals("1")
                );

                playPanel.updateOtherPlayer(ps);

            } catch (Exception ex) {
                System.err.println("[RelayClient] Parse fail: " + ex);
            }
        }
    }

    //====================================================
    public void stopClient() {
        running = false;
    }
}
