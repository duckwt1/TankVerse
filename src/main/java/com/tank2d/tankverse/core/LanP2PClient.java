package com.tank2d.tankverse.core;

import com.tank2d.tankverse.utils.Constant;
import com.tank2d.tankverse.utils.PlayerState;

import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LanP2PClient extends Thread {

    private final PlayPanel playPanel;
    private final List<Map<String, Object>> peers;
    private final DatagramSocket socket;
    private volatile boolean running = true;

    // name -> last state
    private final Map<String, PlayerState> remotePlayers = new ConcurrentHashMap<>();

    public LanP2PClient(PlayPanel panel,
                        DatagramSocket socket,
                        List<Map<String,Object>> peers) {
        this.socket = socket; // dùng socket đã bind từ WaitingRoom
        this.peers = peers;
        this.playPanel = panel;
    }

    @Override
    public void run() {
        try {
            socket.setSoTimeout(1);

            // ===== LOG MY INFO =====
            System.out.println("=================================");
            System.out.println("[P2P] MY NAME : " + playPanel.getPlayer().getName());
            System.out.println("[P2P] MY UDP  : " +
                    socket.getLocalAddress().getHostAddress() +
                    ":" + socket.getLocalPort());

            System.out.println("[P2P] PEERS FROM SERVER (" + peers.size() + "):");
            for (Map<String, Object> p : peers) {
                System.out.println("  - " + p.get("name") +
                        " @ " + p.get("ip") + ":" + p.get("udpPort"));
            }
            System.out.println("=================================");

            // ===== SEND HELLO (HOLE PUNCH) =====
            for (Map<String, Object> peer : peers) {
                String name = peer.get("name").toString();
                if (name.equals(playPanel.getPlayer().getName())) continue;

                InetAddress ip = InetAddress.getByName(peer.get("ip").toString());
                int port = ((Number) peer.get("udpPort")).intValue();

                System.out.println("[P2P] HELLO -> " + name + " " + ip + ":" + port);
                send("HELLO " + playPanel.getPlayer().getName(), ip, port);
            }

            new Thread(this::sendLoop, "P2P-SEND").start();
            new Thread(this::recvLoop, "P2P-RECV").start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= SEND STATE =================
    private void sendLoop() {
        long last = System.currentTimeMillis();

        while (running) {
            long now = System.currentTimeMillis();
            if (now - last >= 16) { // ~60 FPS
                var p = playPanel.getPlayer();

                String msg = "STATE " +
                        p.getName() + " " +
                        p.getX() + " " +
                        p.getY() + " " +
                        p.getBodyAngle() + " " +
                        p.getGunAngle() + " " +
                        (p.isUp() ? 1 : 0) + " " +
                        (p.isDown() ? 1 : 0) + " " +
                        (p.isLeft() ? 1 : 0) + " " +
                        (p.isRight() ? 1 : 0) + " " +
                        (p.isBackward() ? 1 : 0) + " " +
                        p.hp + " " +
                        p.bullet + " " +
                        p.action;

                for (Map<String, Object> peer : peers) {
                    if (peer.get("name").equals(p.getName())) continue;

                    try {
                        send(
                                msg,
                                InetAddress.getByName(peer.get("ip").toString()),
                                ((Number) peer.get("udpPort")).intValue()
                        );
                    } catch (Exception ignored) {}
                }
                if (p.action != Constant.ACTION_NONE)
                {
                    p.action = Constant.ACTION_NONE;
                }
                last = now;
            }
            try { Thread.sleep(1); } catch (Exception ignored) {}
        }
    }

    // ================= RECEIVE =================
    private void recvLoop() {
        byte[] buf = new byte[2048];

        while (running) {
            try {
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                socket.receive(pkt);

                String msg = new String(pkt.getData(), 0, pkt.getLength()).trim();
                InetAddress realIp = pkt.getAddress();
                int realPort = pkt.getPort();

                //System.out.println("[P2P] RECV from " +
               //         realIp.getHostAddress() + ":" + realPort + " -> " + msg);

                if (msg.startsWith("HELLO ")) {
                    String peerName = msg.substring(6).trim();
                    onHello(peerName, realIp, realPort);
                }
                else if (msg.startsWith("HELLO_ACK ")) {
                //    System.out.println("[P2P] HELLO_ACK from " +
                //            msg.substring(10).trim());
                }
                else if (msg.startsWith("STATE ")) {
                    parseState(msg.substring(6));
                }

            } catch (SocketTimeoutException ignored) {
            } catch (Exception e) {
                System.err.println("[P2P] recv error " + e.getMessage());
            }
        }
    }

    // ================= HELLO HANDLER =================
    private void onHello(String name, InetAddress ip, int port) {
//        System.out.println("[P2P] HELLO FROM " + name +
//                " @ " + ip.getHostAddress() + ":" + port);

        for (Map<String, Object> peer : peers) {
            if (peer.get("name").equals(name)) {
                // 🔥 OVERRIDE ENDPOINT THE RIGHT WAY
                peer.put("ip", ip.getHostAddress());
                peer.put("udpPort", port);

//                System.out.println("[P2P] UPDATED PEER " + name +
//                        " -> " + ip.getHostAddress() + ":" + port);

                send("HELLO_ACK " + playPanel.getPlayer().getName(), ip, port);
                return;
            }
        }

        // fallback (rare)
        Map<String, Object> p = new HashMap<>();
        p.put("name", name);
        p.put("ip", ip.getHostAddress());
        p.put("udpPort", port);
        peers.add(p);

        send("HELLO_ACK " + playPanel.getPlayer().getName(), ip, port);
    }

    // ================= STATE PARSER =================
    private void parseState(String data) {
        try {
            String[] p = data.split(" ");
            if (p.length < 13) return;

            String name = p[0];
            if (name.equals(playPanel.getPlayer().getName())) return;

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

        } catch (Exception e) {
            System.err.println("[P2P] parse error");
        }
    }

    private void send(String msg, InetAddress ip, int port) {
        try {
            byte[] d = msg.getBytes();
            socket.send(new DatagramPacket(d, d.length, ip, port));
        } catch (Exception ignored) {}
    }
}
