package com.tank2d.tankverse.entity;

import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class RendezvousServer {
    // Map: clientName -> SocketAddress (InetAddress + port)
    private static ConcurrentHashMap<String, InetSocketAddress> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        int port = 5000; // server lắng nghe cổng 5000
        DatagramSocket sock = new DatagramSocket(port);
        System.out.println("Rendezvous Server running on port " + port);

        byte[] buf = new byte[1024];

        while (true) {
            DatagramPacket p = new DatagramPacket(buf, buf.length);
            sock.receive(p);

            String s = new String(p.getData(), 0, p.getLength()).trim();
            InetSocketAddress addr = new InetSocketAddress(p.getAddress(), p.getPort());

            // Protocol: "REGISTER:clientName:peerName"
            if (s.startsWith("REGISTER:")) {
                String[] parts = s.split(":", 3);
                if (parts.length < 3) {
                    System.out.println("Invalid register: " + s);
                    continue;
                }
                String clientName = parts[1];
                String peerName = parts[2];

                System.out.println("Register from " + clientName + " (peer=" + peerName + ") -> " + addr);
                clients.put(clientName, addr);

                // If peer already registered, send info to both
                if (clients.containsKey(peerName)) {
                    InetSocketAddress peerAddr = clients.get(peerName);

                    // send peer info to clientName
                    String infoToClient = String.format("PEER:%s:%s:%d", peerName,
                            peerAddr.getAddress().getHostAddress(), peerAddr.getPort());
                    byte[] data1 = infoToClient.getBytes();
                    DatagramPacket send1 = new DatagramPacket(data1, data1.length, addr.getAddress(), addr.getPort());
                    sock.send(send1);

                    // send client info to peerName
                    String infoToPeer = String.format("PEER:%s:%s:%d", clientName,
                            addr.getAddress().getHostAddress(), addr.getPort());
                    byte[] data2 = infoToPeer.getBytes();
                    DatagramPacket send2 = new DatagramPacket(data2, data2.length,
                            peerAddr.getAddress(), peerAddr.getPort());
                    sock.send(send2);

                    System.out.println("Exchanged info: " + clientName + " <--> " + peerName);

                    // Optionally remove them so new pairs can register (or keep, depending use-case)
                    clients.remove(clientName);
                    clients.remove(peerName);
                } else {
                    // Peer not present yet, wait
                    String ack = "WAITING";
                    sock.send(new DatagramPacket(ack.getBytes(), ack.length(), addr.getAddress(), addr.getPort()));
                }
            } else {
                System.out.println("Unknown message: " + s + " from " + addr);
            }
        }
    }
}
