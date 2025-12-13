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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;

public class WaitingRoomController implements PacketListener {

    @FXML private Label lblRoomName;
    @FXML private Label lblPlayerCount;
    @FXML private Label lblStatus;
    @FXML private ListView<String> listPlayers;
    
    // Map selection UI components - using new template-based design
    @FXML private VBox mapSelectionContainer;
    @FXML private ImageView imgCurrentMap;
    @FXML private Label lblCurrentMapName;
    @FXML private Label lblSelectedMap;
    @FXML private Button btnPrevMap;
    @FXML private Button btnNextMap;
    
    @FXML private Button btnReady;
    @FXML private Button btnStartGame;
    @FXML private Button btnLeaveRoom;

    private GameClient client;
    private int roomId;
    private String roomName;
    private int maxPlayers;
    private boolean isHost = true;
    private String selectedMap = "map1"; // Default map
    private int currentMapIndex = 0; // For carousel navigation (0=map1, 1=map2, 2=map3)
    private boolean isUpdatingFromServer = false; // Flag to prevent infinite loop

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
        
        // Show map selection for both host and players
        mapSelectionContainer.setVisible(true);
        
        // Hide next/prev buttons for non-host (cleaner look)
        btnPrevMap.setVisible(host);
        btnNextMap.setVisible(host);
    }

    @FXML
    public void initialize() {
        lblStatus.setText("Waiting for other players...");
        btnReady.setVisible(false);
        btnStartGame.setVisible(false);
        
        // Debug ListView
        if (listPlayers != null) {
            System.out.println("[WaitingRoom] ListView initialized successfully");
            listPlayers.setVisible(true);
            listPlayers.setManaged(true);
            // Add test item to verify ListView is working
            listPlayers.getItems().add("(Waiting for players...)");
        } else {
            System.out.println("[WaitingRoom] ERROR: ListView is NULL after initialization!");
        }
        
        // Initialize map display
        updateMapDisplay();
    }
    
    private void updateMapDisplay() {
        try {
            if (imgCurrentMap != null && lblCurrentMapName != null) {
                // Load map image
                Image mapImage = loadMapImage(currentMapIndex + 1);
                if (mapImage != null) {
                    imgCurrentMap.setImage(mapImage);
                }
                
                // Update map name
                String mapName = "Map " + (currentMapIndex + 1);
                lblCurrentMapName.setText(mapName);
                
                if (lblSelectedMap != null) {
                    lblSelectedMap.setText("Selected Map: " + mapName);
                }
            }
        } catch (Exception e) {
            System.out.println("[WaitingRoom] Could not update map display: " + e.getMessage());
        }
    }
    
    private Image loadMapImage(int mapId) {
        try {
            // Try to load preview image
            var stream = getClass().getResourceAsStream("/com/tank2d/tankverse/map_preview/map" + mapId + "_preview.png");
            if (stream != null) {
                return new Image(stream);
            }
        } catch (Exception e) {
            System.out.println("[WaitingRoom] Preview image not found for map " + mapId);
        }
        
        // Fallback: create placeholder text image or use default
        return createPlaceholderImage("Map " + mapId);
    }
    
    private Image createPlaceholderImage(String text) {
        // Create a simple colored placeholder
        // You can improve this with actual image generation if needed
        return null; // Will just show empty ImageView
    }
    
    private void showMapPreview(int index) {
        showMapPreview(index, true);
    }
    
    private void showMapPreview(int index, boolean sendToServer) {
        currentMapIndex = index;
        selectedMap = "map" + (index + 1);
        
        // Update the display
        updateMapDisplay();
        
        // Only send to server if this is a user action (not from server broadcast)
        if (sendToServer && isHost && client != null && !isUpdatingFromServer) {
            Packet packet = new Packet(com.tank2d.tankverse.utils.PacketType.SELECT_MAP);
            packet.data.put("map", selectedMap);
            client.sendPacket(packet);
            System.out.println("[WaitingRoom] Host selected map: " + selectedMap);
        }
    }
    
    @FXML
    private void onPreviousMap() {
        if (!isHost) return; // Safety check
        currentMapIndex = (currentMapIndex - 1 + 3) % 3; // Wrap around
        showMapPreview(currentMapIndex);
    }
    
    @FXML
    private void onNextMap() {
        if (!isHost) return; // Safety check
        currentMapIndex = (currentMapIndex + 1) % 3; // Wrap around
        showMapPreview(currentMapIndex);
    }
    
    @FXML
    private void onSelectMap1() {
        if (!isHost) return; // Safety check
        showMapPreview(0);
    }
    
    @FXML
    private void onSelectMap2() {
        if (!isHost) return; // Safety check
        showMapPreview(1);
    }
    
    @FXML
    private void onSelectMap3() {
        if (!isHost) return; // Safety check
        showMapPreview(2);
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
        System.out.println("[WaitingRoom] updatePlayerList called with: " + players + " (maxPlayers=" + maxPlayers + ")");
        Platform.runLater(() -> {
            if (listPlayers == null) {
                System.out.println("[WaitingRoom] ERROR: listPlayers is NULL!");
                return;
            }
            listPlayers.getItems().clear();
            if (players != null) {
                for (String playerName : players) {
                    System.out.println("[WaitingRoom] Adding player: " + playerName);
                    listPlayers.getItems().add(playerName);
                }
            }
            updatePlayerCount(players != null ? players.size() : 0, maxPlayers);
        });
    }

    public void addPlayer(String name) {
        Platform.runLater(() -> {
            if (!listPlayers.getItems().contains(name)) {
                listPlayers.getItems().add(name);
                updatePlayerCount(listPlayers.getItems().size(), maxPlayers);
            }
        });
    }

    public void removePlayer(String name) {
        Platform.runLater(() -> {
            listPlayers.getItems().remove(name);
            updatePlayerCount(listPlayers.getItems().size(), maxPlayers);
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
    public void onRoomUpdate(String message, List<String> players, int maxPlayers) {
        Platform.runLater(() -> {
            // Update maxPlayers if provided
            if (maxPlayers > 0) {
                this.maxPlayers = maxPlayers;
                System.out.println("[WaitingRoom] Updated maxPlayers to: " + maxPlayers);
            }
            
            lblStatus.setText("[ROOM] " + message);
            if (players != null) {
                updatePlayerList(players);
            }
        });
    }
    
    @Override
    public void onMapSelected(String mapName) {
        Platform.runLater(() -> {
            isUpdatingFromServer = true; // Set flag to prevent sending packet back
            selectedMap = mapName;
            // Update carousel to show the selected map
            int index = switch (mapName) {
                case "map1" -> 0;
                case "map2" -> 1;
                case "map3" -> 2;
                default -> 0;
            };
            showMapPreview(index, false); // Don't send to server when receiving broadcast
            lblStatus.setText("Host selected: Map " + (index + 1));
            System.out.println("[WaitingRoom] Received map selection from server: " + mapName);
            isUpdatingFromServer = false; // Reset flag
        });
    }

    @Override
    public void onGameStart(Packet p) {
        Platform.runLater(() -> {
            lblStatus.setText((String) p.data.get("msg"));
            
            String hostUsername = p.data.getOrDefault("isHost", "none").toString();
            String myUsername = client.getUserName();
            boolean isHost = hostUsername.equals(myUsername);
            
            System.out.println("===========================================");
            System.out.println("[START_GAME] MY USERNAME: " + myUsername);
            System.out.println("[START_GAME] HOST USERNAME: " + hostUsername);
            System.out.println("[START_GAME] AM I HOST? " + isHost);
            System.out.println("===========================================");
            
            List<Map<String, Object>> playersData = (List<Map<String, Object>>) p.data.get("players");
            
            // Get mapId from server
            int mapId = getInt(p.data.getOrDefault("mapId", 1), 1);
            System.out.println("[START_GAME] Selected Map ID: " + mapId);

            // Create the play panel with player list and mapId
            PlayPanel playPanel = new PlayPanel(client.getUserName(), playersData.size(), playersData, mapId);

            Stage stage = (Stage) lblStatus.getScene().getWindow();
            Scene scene = new Scene(playPanel);
            stage.setScene(scene);
            stage.show();

            // Get relay server info
            String relayHost = Constant.DEFAULT_RELAY_HOST;
            int relayPort = Constant.DEFAULT_RELAY_PORT;
            int roomId = getInt(p.data.getOrDefault("room_id", 1), 1);
            
            System.out.println("[Game] Relay Server: " + relayHost + ":" + relayPort + " (Room " + roomId + ")");

            // Start relay client (same for host and non-host)
            GameRelayClient relayClient = new GameRelayClient(playPanel, relayHost, relayPort, roomId);
            relayClient.start();
            System.out.println("[Game] Started GameRelayClient");

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