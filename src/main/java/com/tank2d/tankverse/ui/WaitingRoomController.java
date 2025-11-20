package com.tank2d.tankverse.ui;

import com.tank2d.tankverse.core.*;
import com.tank2d.tankverse.net.P2PConnection;
import com.tank2d.tankverse.utils.Constant;
import com.tank2d.tankverse.utils.Packet;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;

public class WaitingRoomController implements PacketListener {

    @FXML private Label lblRoomName;
    @FXML private Label lblPlayerCount;
    @FXML private Label lblStatus;
    @FXML private ListView<String> listPlayers;
    @FXML private Button btnReady;
    @FXML private Button btnStartGame;
    @FXML private Button btnLeaveRoom;

    private GameClient client;
    private int roomId;
    private String roomName;
    private int maxPlayers;
    private boolean isHost = true;

    public void setClient(GameClient client) {
        this.client = client;
        client.setPacketListener(this);
    }

    public void setRoomData(int roomId, String roomName, int maxPlayers) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.maxPlayers = maxPlayers;
        lblRoomName.setText("Room: " + roomName + " (ID: " + roomId + ")");
    }

    public void setHost(boolean host) {
        this.isHost = host;
        btnStartGame.setVisible(host);
        btnReady.setVisible(!host);
    }

    @FXML
    public void initialize() {
        lblStatus.setText("Waiting for other players...");
        btnReady.setVisible(false);
        btnStartGame.setVisible(false);
    }

    @FXML
    private void onReady() {
        if (client == null) return;
        lblStatus.setText("You are ready!");
        client.setReady(true);
    }

    @FXML
    private void onStartGame() {
        if (client == null) return;
        lblStatus.setText("Starting game...");
        client.startGame();
    }

    @FXML
    private void onLeaveRoom() {
        if (client == null) return;
        client.leaveRoom();

        // Navigate back to main menu with client
        Platform.runLater(() -> {
            MainMenuController controller = UiNavigator.loadSceneWithController("main_menu.fxml");
            controller.setClient(client);
        });
    }

    public void updatePlayerList(List<String> players) {
        System.out.println("[WaitingRoom] updatePlayerList called with: " + players);
        System.out.println("[WaitingRoom] listPlayers is null? " + (listPlayers == null));
        Platform.runLater(() -> {
            if (listPlayers == null) {
                System.out.println("[WaitingRoom] ERROR: listPlayers is NULL!");
                return;
            }
            listPlayers.getItems().clear();
            for (String playerName : players) {
                System.out.println("[WaitingRoom] Adding player: " + playerName);
                listPlayers.getItems().add(playerName);
            }
            updatePlayerCount(players.size(), maxPlayers);
        });
    }

    public void addPlayer(String name) {
        Platform.runLater(() -> {
            if (!listPlayers.getItems().contains(name)) {
                listPlayers.getItems().add(name);
                updatePlayerCount(listPlayers.getItems().size(), 4);
            }
        });
    }

    public void removePlayer(String name) {
        Platform.runLater(() -> {
            listPlayers.getItems().remove(name);
            updatePlayerCount(listPlayers.getItems().size(), 4);
        });
    }

    public void updatePlayerCount(int current, int max) {
        Platform.runLater(() -> lblPlayerCount.setText("Players: " + current + "/" + max));
    }

    public void updateStatus(String msg) {
        Platform.runLater(() -> lblStatus.setText(msg));
    }

    // ✅ Helper: safely parse int or double to int
    private int getInt(Object obj, int defaultValue) {
        if (obj == null) return defaultValue;
        if (obj instanceof Integer) return (int) obj;
        if (obj instanceof Double) return ((Double) obj).intValue();
        if (obj instanceof String) {
            try {
                return (int) Double.parseDouble((String) obj);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    @Override
    public void onRoomJoined(int roomId, String roomName, int maxPlayers, List<String> players) {
        Platform.runLater(() -> {
            setRoomData(roomId, roomName, maxPlayers);
            setHost(false);
            updatePlayerList(players);
        });
    }

    @Override
    public void onRoomUpdate(String message, List<String> players) {
        Platform.runLater(() -> {
            lblStatus.setText("[ROOM] " + message);
            if (players != null) {
                updatePlayerList(players);
            }
        });
    }

    @Override
    public void onGameStart(Packet p) {
        Platform.runLater(() -> {
            lblStatus.setText((String) p.data.get("msg"));
            System.out.println(p.data.toString());
            boolean isHost = p.data.getOrDefault("isHost", "none").equals(client.getUserName());
            List<Map<String, Object>> playersData = (List<Map<String, Object>>) p.data.get("players");

            // Create the play panel with player list
            PlayPanel playPanel = new PlayPanel(client.getUserName(), playersData.size(), playersData);

            Stage stage = (Stage) lblStatus.getScene().getWindow();
            Scene scene = new Scene(playPanel);
            stage.setScene(scene);
            stage.show();

            int udpPort = getInt(p.data.getOrDefault("host_udp_port", 4001), 4001);
            String hostIp = p.data.getOrDefault("host_ip", "127.0.0.1").toString();

            // Try P2P with UDP hole punching via Rendezvous server
            lblStatus.setText("Connecting P2P...");
            
            new Thread(() -> {
                try {
                    P2PConnection p2p = new P2PConnection(
                        Constant.RENDEZVOUS_HOST,
                        Constant.RENDEZVOUS_PORT,
                        roomId,
                        client.getUserName()
                    );
                    
                    boolean connected = p2p.connect();
                    
                    Platform.runLater(() -> {
                        if (connected && !p2p.getPeers().isEmpty()) {
                            // P2P connection successful
                            lblStatus.setText("P2P Connected!");
                            
                            if (isHost) {
                                // Host runs mini server for P2P
                                GameMiniServer miniServer = new GameMiniServer(playPanel, udpPort);
                                miniServer.start();
                                System.out.println("[P2P] Host started GameMiniServer on port " + udpPort);
                            } else {
                                // Client connects to host via P2P
                                GameClientUDP udpClient = new GameClientUDP(playPanel, hostIp, udpPort);
                                udpClient.start();
                                System.out.println("[P2P] Client connected to host via P2P");
                            }
                        } else {
                            // P2P failed, fallback to direct connection
                            lblStatus.setText("P2P failed, using direct connection");
                            System.out.println("[P2P] Fallback to direct connection");
                            
                            if (isHost) {
                                GameMiniServer miniServer = new GameMiniServer(playPanel, udpPort);
                                miniServer.start();
                            } else {
                                GameClientUDP udpClient = new GameClientUDP(playPanel, hostIp, udpPort);
                                udpClient.start();
                            }
                        }
                    });
                    
                } catch (Exception e) {
                    System.err.println("[P2P] Error: " + e.getMessage());
                    e.printStackTrace();
                    
                    Platform.runLater(() -> {
                        // Fallback to original P2P method
                        if (isHost) {
                            GameMiniServer miniServer = new GameMiniServer(playPanel, udpPort);
                            miniServer.start();
                        } else {
                            GameClientUDP udpClient = new GameClientUDP(playPanel, hostIp, udpPort);
                            udpClient.start();
                        }
                    });
                }
            }).start();

            new Thread(playPanel).start();
        });
    }


    @Override
    public void onDisconnected() {
        Platform.runLater(() -> {
            lblStatus.setText("Disconnected from server");
            UiNavigator.loadScene("login.fxml");
        });
    }
}