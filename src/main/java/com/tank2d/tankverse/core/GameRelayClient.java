package com.tank2d.tankverse.core;

import com.tank2d.tankverse.entity.Player;
import com.tank2d.tankverse.utils.Constant;
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
            socket.setSoTimeout(1);  // ultra low latency
            serverAddr = InetAddress.getByName(relayHost);

            // === JOIN ROOM ===
            sendRaw("JOIN " + roomId + " " + username);
            System.out.println("[RelayClient] JOIN sent");

            // Try JOIN ACK (not required)
            try {
                byte[] buf = new byte[256];
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                socket.receive(pkt);
                System.out.println("[RelayClient] ACK: " +
                        new String(pkt.getData(), 0, pkt.getLength()));
            } catch (SocketTimeoutException ignored) {}

            // ==== Start Send & Receive Threads ====
            Thread sendThread = new Thread(this::sendLoop, "SEND_THREAD");
            Thread recvThread = new Thread(this::receiveLoop, "RECV_THREAD");

            sendThread.start();
            recvThread.start();

            sendThread.join();
            recvThread.join();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (socket != null) socket.close();
        }
    }

    //==========================================================
    // SEND LOOP — FIXED 60 FPS
    //==========================================================
    private void sendLoop() {
        long interval = 10; // 60fps
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
                        (player.isBackward() ? 1 : 0) + " " +
                        player.hp + " " +
                        player.bullet + " " +
                        player.action;


                sendRaw(msg);
                last = now;
                if (player.action == Constant.ACTION_CHARGE)
                {
                    player.action = Constant.ACTION_NONE;
                }
                //count++;
                //if (count % 200 == 0)
                    //System.out.println("[RelayClient] Sent " + count + " updates");
            }

            try { Thread.sleep(1); } catch (Exception ignored) {}
        }

        sendRaw("LEAVE " + roomId + " " + username);
        System.out.println("[RelayClient] LEAVE sent");
    }

    //==========================================================
    // RECEIVE LOOP — NO DELAY
    //==========================================================
    private void receiveLoop() {
        byte[] buffer = new byte[4096];

        while (running) {
            try {
                DatagramPacket pkt = new DatagramPacket(buffer, buffer.length);
                socket.receive(pkt);

                String msg = new String(pkt.getData(), 0, pkt.getLength()).trim();

                // accept STATE only
                if (msg.startsWith("STATE ")) {
                    parseState(msg);
                }

            } catch (SocketTimeoutException ignored) {
                // no packet this tick
            } catch (Exception e) {
                System.err.println("[RelayClient] Receive error: " + e.getMessage());
            }
        }
    }

    //==========================================================
    // SEND RAW UDP
    //==========================================================
    private void sendRaw(String text) {
        try {
            byte[] data = text.getBytes();
            DatagramPacket p = new DatagramPacket(data, data.length, serverAddr, relayPort);
            socket.send(p);
        } catch (Exception e) {
            System.err.println("[RelayClient] Send error: " + e.getMessage());
        }
    }

    //==========================================================
    // SAFE PARSE — NEVER CRASH
    //==========================================================
    private void parseState(String msg) {

        try {
            // 1) Must be at least "STATE X"
            if (msg.length() < 7) return;

            // 2) Extract body
            String body = msg.substring(6).trim();
            if (body.isEmpty()) return;

            // 3) Split players
            String[] players = body.split(";");

            for (String pd : players) {
                if (pd == null) continue;
                pd = pd.trim();
                if (pd.isEmpty()) continue;

                // Format: username x y body gun up down left right backward
                String[] p = pd.split(" ");

                // EXPECTING 13 FIELDS: name x y body gun up down left right backward hp ammo action
                if (p.length < 13) {
                    System.err.println("[RelayClient] Warning: malformed STATE: " + Arrays.toString(p));
                    continue;
                }


                String name = p[0];
                if (name.equals(username)) continue; // skip self

                PlayerState ps = new PlayerState(
                        name,
                        Double.parseDouble(p[1]),
                        Double.parseDouble(p[2]),
                        Double.parseDouble(p[3]),
                        Double.parseDouble(p[4]),
                        p[5].equals("1"),
                        p[6].equals("1"),
                        p[7].equals("1"),
                        p[8].equals("1"),
                        p[9].equals("1"),
                        Integer.parseInt(p[10]),
                        Integer.parseInt(p[11]),
                        Integer.parseInt(p[12])
                );

                playPanel.updateOtherPlayer(ps);
            }

        } catch (Exception e) {
            System.err.println("[RelayClient] ParseState error: " + e.getMessage());
            // We ignore the bad pack et and continue normally.
        }
    }

    //==========================================================
    public void stopClient() {
        running = false;
    }
}
