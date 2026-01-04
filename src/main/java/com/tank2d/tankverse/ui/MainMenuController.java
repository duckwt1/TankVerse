package com.tank2d.tankverse.ui;

import com.tank2d.tankverse.core.GameClient;
import com.tank2d.tankverse.core.PacketListener;
import com.tank2d.tankverse.utils.Packet;
import com.tank2d.tankverse.utils.PacketType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.net.DatagramSocket;
import java.util.List;
import java.util.Map;

public class MainMenuController implements PacketListener {

    @FXML private Label lblTitle;
    @FXML private Button btnCreateRoom, btnJoinRoom, btnShop, btnInventory, btnSettings, btnQuit;
    private GameClient client;

    @FXML
    public void initialize() {
//        lblTitle.setText("PIXEL TANK ONLINE - MAIN MENU");

        btnCreateRoom.setOnAction(e -> onCreateRoom());
        btnJoinRoom.setOnAction(e -> onJoinRoom());
        btnShop.setOnAction(e -> onShop());
        btnInventory.setOnAction(e -> onInventory());
        btnSettings.setOnAction(e -> onSettings());
        btnQuit.setOnAction(e -> System.exit(0));
    }

    @FXML
    private void onCreateRoom() {
        Platform.runLater(() -> {
            CreateRoomController controller = UiNavigator.loadSceneWithController("create_room.fxml");
            controller.setClient(client);
            // Transfer listener to CreateRoomController
            client.setPacketListener(controller);
        });
    }


    private void onShop(){
        Platform.runLater(() -> {
            ShopController controller = UiNavigator.loadSceneWithController("shop.fxml");
            controller.setClient(client);
        });
    }

    private void onInventory(){
        Platform.runLater(() -> {
            InventoryController controller = UiNavigator.loadSceneWithController("inventory.fxml");
            controller.setClient(client);
            // Request inventory data from server
            controller.requestInventory();
        });
    }

    private void onJoinRoom() {
        Platform.runLater(() -> {
            JoinRoomController controller = UiNavigator.loadSceneWithController("join_room.fxml");
            controller.setClient(client);
        });
    }

    private void onSettings() {
        // TODO: load settings scene
        System.out.println("Settings clicked!");
//        UiNavigator.loadScene("settings.fxml");
    }

    private DatagramSocket udpSocket;

    public void setClient(GameClient client) {
        this.client = client;
        client.setPacketListener(this);


    }

    // ========== PacketListener Implementation ==========
    
    @Override
    public void onRoomListReceived(List<Map<String, Object>> rooms) {
        Platform.runLater(() -> {
            // TODO: Display room list in UI
            StringBuilder sb = new StringBuilder("Available rooms:\n");
            for (Map<String, Object> r : rooms) {
                sb.append("ID: ").append(r.get("id"))
                        .append(" | Name: ").append(r.get("name"))
                        .append(" | Players: ").append(r.get("players"))
                        .append("/").append(r.get("maxPlayers"))
                        .append(((Boolean) r.get("hasPassword")) ? " | 🔒" : "")
                        .append("\n");
            }
            System.out.println(sb.toString());
        });
    }
    
    @Override
    public void onDisconnected() {
        Platform.runLater(() -> {
            System.out.println("Disconnected from server");
            // Navigate back to login
            UiNavigator.loadScene("login.fxml");
        });
    }
}