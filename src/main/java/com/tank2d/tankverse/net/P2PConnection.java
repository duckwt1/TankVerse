package com.tank2d.tankverse.net;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * UDP P2P Connection helper using Rendezvous server
 * Implements UDP Hole Punching
 */
public class P2PConnection {
    private final String rendezvousHost;
    private final int rendezvousPort;
    private final int roomId;
    private final String username;
    private DatagramSocket socket;
    private List<InetSocketAddress> peers = new ArrayList<>();

    public P2PConnection(String rendezvousHost, int rendezvousPort, int roomId, String username) {
        this.rendezvousHost = rendezvousHost;
        this.rendezvousPort = rendezvousPort;
        this.roomId = roomId;
        this.username = username;
    }

    /**
     * Register with rendezvous server and get peer addresses
     */
    public boolean connect() {
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(5000);

            InetAddress serverAddr = InetAddress.getByName(rendezvousHost);

            // Send registration to rendezvous server
            String registerMsg = "REGISTER " + roomId + " " + username;
            byte[] data = registerMsg.getBytes();
            DatagramPacket packet = new DatagramPacket(
                data, data.length, serverAddr, rendezvousPort
            );
            socket.send(packet);

            System.out.println("[P2P] Registered with rendezvous: " + registerMsg);

            // Receive peer list
            byte[] buffer = new byte[4096];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.receive(response);

            String reply = new String(response.getData(), 0, response.getLength()).trim();
            System.out.println("[P2P] Received from rendezvous: " + reply);

            if (reply.startsWith("PEERS ")) {
                parsePeers(reply.substring(6));
            }

            // Perform UDP hole punching - send hello to each peer
            for (InetSocketAddress peer : peers) {
                String hello = "HELLO " + username;
                byte[] helloData = hello.getBytes();
                DatagramPacket helloPacket = new DatagramPacket(
                    helloData, helloData.length, peer
                );
                socket.send(helloPacket);
                System.out.println("[P2P] Sent HELLO to peer: " + peer);
            }

            socket.setSoTimeout(0); // Remove timeout for normal operation
            return true;

        } catch (Exception e) {
            System.err.println("[P2P] Connection failed: " + e.getMessage());
            return false;
        }
    }

    private void parsePeers(String peersData) {
        // Format: "username:ip:port;username:ip:port;..."
        if (peersData.isEmpty()) {
            return;
        }

        String[] peerList = peersData.split(";");
        for (String peerInfo : peerList) {
            if (peerInfo.trim().isEmpty()) continue;

            String[] parts = peerInfo.split(":");
            if (parts.length == 3) {
                String peerName = parts[0];
                String peerIp = parts[1];
                int peerPort = Integer.parseInt(parts[2]);

                InetSocketAddress peerAddr = new InetSocketAddress(peerIp, peerPort);
                peers.add(peerAddr);
                System.out.println("[P2P] Found peer: " + peerName + " -> " + peerAddr);
            }
        }
    }

    public DatagramSocket getSocket() {
        return socket;
    }

    public List<InetSocketAddress> getPeers() {
        return peers;
    }

    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                // Unregister from rendezvous
                String unregisterMsg = "UNREGISTER " + roomId + " " + username;
                byte[] data = unregisterMsg.getBytes();
                InetAddress serverAddr = InetAddress.getByName(rendezvousHost);
                DatagramPacket packet = new DatagramPacket(
                    data, data.length, serverAddr, rendezvousPort
                );
                socket.send(packet);
                
                socket.close();
            }
        } catch (Exception e) {
            System.err.println("[P2P] Error disconnecting: " + e.getMessage());
        }
    }
}
