package com.tank2d.tankverse.ui;

import com.tank2d.tankverse.core.GameClient;
import com.tank2d.tankverse.core.PacketListener;
import com.tank2d.tankverse.utils.AnimationHelper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.List;

public class CreateRoomController implements PacketListener {

    @FXML private TextField txtRoomName;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<Integer> cmbMaxPlayers;
    @FXML private Button btnCreate;
    @FXML private Button btnCancel;
    @FXML private VBox createRoomContainer;

    private GameClient client;

    public void setClient(GameClient client) {
        this.client = client;
        client.setPacketListener(this);
    }

    @FXML
    public void initialize() {
        // Fade in animation
        if (createRoomContainer != null) {
            createRoomContainer.setOpacity(0);
            AnimationHelper.fadeIn(createRoomContainer, 500);
        }
        
        cmbMaxPlayers.getSelectionModel().select(0);
        btnCreate.setOnAction(e -> onCreate());
        btnCancel.setOnAction(e -> onCancel());

        // add value to combobox
        ObservableList<Integer> playerOptions = FXCollections.observableArrayList(
                2, 4, 6, 8);

        cmbMaxPlayers.setItems(playerOptions);
        cmbMaxPlayers.setValue(6);
    }


    @FXML
    private void onCreate() {
        String roomName = txtRoomName.getText().trim();
        Integer maxPlayers = cmbMaxPlayers.getValue();
        String password = txtPassword.getText();

        if (roomName.isEmpty()) {
//            showAlert("Room name cannot be empty!");
            AnimationHelper.shake(txtRoomName);
            return;
        }
        
        if (maxPlayers == null) {
//            showAlert("Please select max players!");
            AnimationHelper.shake(cmbMaxPlayers);
            return;
        }

        AnimationHelper.showButtonLoading(btnCreate);
        client.createRoom(roomName, maxPlayers, password);
    }

    @FXML
    private void onCancel(){
        Platform.runLater(() -> {
            MainMenuController controller = UiNavigator.loadSceneWithController("main_menu.fxml");
            controller.setClient(client);
        });
    }
    
    // ========== PacketListener Implementation ==========
    
    @Override
    public void onRoomCreated(int roomId, String roomName, int maxPlayers, List<String> players) {
        Platform.runLater(() -> {
            AnimationHelper.hideButtonLoading(btnCreate, "Create");
            
            // Fade out before navigating
            AnimationHelper.fadeOut(createRoomContainer, 300, () -> {
                WaitingRoomController controller = UiNavigator.loadSceneWithController("waiting_room.fxml");
                controller.setClient(client);
                controller.setRoomData(roomId, roomName, maxPlayers);
                controller.setHost(true);
                controller.updatePlayerList(players);
                // Transfer listener to WaitingRoomController
                client.setPacketListener(controller);
            });
        });
    }
    
    @Override
    public void onDisconnected() {
        Platform.runLater(() -> {
            showAlert("Disconnected from server");
            UiNavigator.loadScene("login.fxml");
        });
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
