package com.tank2d.tankverse.entity;

import java.net.*;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);

        System.out.print("Local bind port (0 for random): ");
        int localPort = Integer.parseInt(sc.nextLine().trim());

        System.out.print("Your name: ");
        String myName = sc.nextLine().trim();

        System.out.print("Peer name you want to connect to: ");
        String peerName = sc.nextLine().trim();

        System.out.print("Rendezvous server IP: ");
        String serverIp = sc.nextLine().trim();

        int serverPort = 5000;

        DatagramSocket socket = new DatagramSocket(localPort);
        socket.setSoTimeout(0); // blocking receive

        InetAddress serverAddr = InetAddress.getByName(serverIp);

        // 1) Register to rendezvous server
        String reg = "REGISTER:" + myName + ":" + peerName;
        byte[] regb = reg.getBytes();
        DatagramPacket regPacket = new DatagramPacket(regb, regb.length, serverAddr, serverPort);
        socket.send(regPacket);
        System.out.println("Sent registration to server: " + reg);

        // 2) Wait for server to send peer info
        byte[] buf = new byte[1024];
        DatagramPacket recv = new DatagramPacket(buf, buf.length);

        InetAddress peerAddr = null;
        int peerPort = -1;
        System.out.println("Waiting for peer info from rendezvous server...");

        while (true) {
            socket.receive(recv);
            String msg = new String(recv.getData(), 0, recv.getLength()).trim();
            if (msg.startsWith("PEER:")) {
                // Format: PEER:peerName:ip:port
                String[] parts = msg.split(":", 4);
                if (parts.length == 4) {
                    String receivedPeerName = parts[1];
                    String ip = parts[2];
                    peerPort = Integer.parseInt(parts[3]);
                    peerAddr = InetAddress.getByName(ip);
                    System.out.println("Got peer info -> " + receivedPeerName + " @ " + ip + ":" + peerPort);
                    break;
                }
            } else if (msg.equals("WAITING")) {
                System.out.println("Server: waiting for peer to register...");
            } else {
                System.out.println("Server: " + msg);
            }
        }

        // 3) Start listener thread for incoming messages (from peer or server)
        InetAddress finalPeerAddr = peerAddr;
        int finalPeerPort = peerPort;
        Thread listener = new Thread(() -> {
            byte[] inBuf = new byte[2048];
            DatagramPacket p = new DatagramPacket(inBuf, inBuf.length);
            try {
                while (true) {
                    socket.receive(p);
                    String r = new String(p.getData(), 0, p.getLength());
                    InetSocketAddress from = new InetSocketAddress(p.getAddress(), p.getPort());
                    System.out.println("\n[Recv from " + from + "] " + r);
                    System.out.print("You: ");
                }
            } catch (Exception e) {
                System.out.println("Listener stopped: " + e.getMessage());
            }
        });
        listener.setDaemon(true);
        listener.start();

        // 4) Hole punching: send several small packets to peer to open NAT mappings
        System.out.println("Starting hole punching to peer...");
        byte[] punch = "PUNCH".getBytes();
        for (int i = 0; i < 10; i++) {
            try {
                DatagramPacket punchPkt = new DatagramPacket(punch, punch.length, finalPeerAddr, finalPeerPort);
                socket.send(punchPkt);
            } catch (Exception e) {
                System.out.println("Punch send error: " + e.getMessage());
            }
            Thread.sleep(300);
        }
        System.out.println("Punch packets sent. Try typing messages now.");

        // 5) Chat loop: read user input and send to peer
        while (true) {
            System.out.print("You: ");
            String line = sc.nextLine();
            if (line == null) break;
            if (line.equalsIgnoreCase("/quit")) {
                System.out.println("Quitting.");
                break;
            }

            byte[] out = line.getBytes();
            DatagramPacket outPkt = new DatagramPacket(out, out.length, finalPeerAddr, finalPeerPort);
            socket.send(outPkt);
        }

        socket.close();
        sc.close();
    }
}
