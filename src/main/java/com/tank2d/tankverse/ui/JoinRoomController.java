package com.tank2d.tankverse.ui;

import com.tank2d.tankverse.core.GameClient;
import com.tank2d.tankverse.core.PacketListener;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.Map;

import static com.tank2d.tankverse.utils.DataTypeParser.toInt;

public class JoinRoomController implements PacketListener {
    @FXML private TextField txtSearch;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblStatus;
    @FXML private TableView<Map<String, Object>> tableRooms;
    @FXML private TableColumn<Map<String, Object>, String> colRoomName;
    @FXML private TableColumn<Map<String, Object>, String> colPlayers;
    @FXML private TableColumn<Map<String, Object>, String> colMaxPlayers;
    @FXML private TableColumn<Map<String, Object>, String> colStatus;
    @FXML private TableColumn<Map<String, Object>, String> colLocked;
    @FXML private HBox passwordBox;

    private final ObservableList<Map<String, Object>> roomList = FXCollections.observableArrayList();
    private final ObservableList<Map<String, Object>> filteredRoomList = FXCollections.observableArrayList();
    private GameClient client;
    private Map<String, Object> selectedRoom;

    public void setClient(GameClient client) {
        this.client = client;
        client.setPacketListener(this);
        requestRoomList();
    }

    @FXML
    public void initialize() {
        colRoomName.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(data.getValue().get("name")))
        );
        colPlayers.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(toInt(data.getValue().get("players"))))
        );
        colMaxPlayers.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(toInt(data.getValue().get("maxPlayers"))))
        );
        colStatus.setCellValueFactory(data -> {
            int current = toInt(data.getValue().get("players"));
            int max = toInt(data.getValue().get("maxPlayers"));
            String status = current >= max ? "FULL" : "OPEN";
            return new SimpleStringProperty(status);
        });
        colLocked.setCellValueFactory(data ->
                new SimpleStringProperty(
                        Boolean.TRUE.equals(data.getValue().get("hasPassword")) ? "🔒" : ""
                )
        );

        tableRooms.setItems(filteredRoomList);
        tableRooms.setOnMouseClicked(this::onRoomSelect);

        // Hide password box initially
        passwordBox.setVisible(false);

        // Add search listener
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> filterRooms(newValue));
    }

    private void onRoomSelect(MouseEvent e) {
        selectedRoom = tableRooms.getSelectionModel().getSelectedItem();
        if (selectedRoom == null) return;

        boolean hasPassword = Boolean.TRUE.equals(selectedRoom.get("hasPassword"));
        passwordBox.setVisible(hasPassword);

        int current = toInt(selectedRoom.get("players"));
        int max = toInt(selectedRoom.get("maxPlayers"));
        boolean isFull = current >= max;

        if (isFull) {
            lblStatus.setText("⚠️ This room is full!");
        } else {
            lblStatus.setText("✅ Selected: " + selectedRoom.get("name"));
        }
    }

    @FXML
    private void onRefresh() {
        requestRoomList();
    }

    @FXML
    private void onJoin() {
        if (selectedRoom == null) {
            lblStatus.setText("⚠️ Please select a room first!");
            showAlert("Please select a room!");
            return;
        }

        int current = toInt(selectedRoom.get("players"));
        int max = toInt(selectedRoom.get("maxPlayers"));
        if (current >= max) {
            lblStatus.setText("⚠️ This room is full!");
            showAlert("This room is full!");
            return;
        }

        int roomId = toInt(selectedRoom.get("id"));
        boolean hasPassword = Boolean.TRUE.equals(selectedRoom.get("hasPassword"));
        String password = txtPassword.getText().trim();

        if (hasPassword && password.isEmpty()) {
            lblStatus.setText("🔒 Room requires password!");
            showAlert("Please enter room password!");
            return;
        }

        lblStatus.setText("⏳ Joining room...");
        client.joinRoom(roomId, password);
    }

    @FXML
    private void onBack() {
        Platform.runLater(() -> {
            MainMenuController controller = UiNavigator.loadSceneWithController("main_menu.fxml");
            controller.setClient(client);
        });
    }

    private void requestRoomList() {
        if (client == null) {
            lblStatus.setText("Client not connected!");
            return;
        }

        client.requestRoomList();
        lblStatus.setText("🔄 Requesting room list...");
    }

    public void updateRoomList(Iterable<Map<String, Object>> rooms) {
        Platform.runLater(() -> {
            roomList.clear();
            for (Map<String, Object> room : rooms) {
                roomList.add(room);
            }
            lblStatus.setText("✅ Found " + roomList.size() + " room(s)");
        });
    }

    @Override
    public void onRoomListReceived(List<Map<String, Object>> rooms) {
        Platform.runLater(() -> {
            roomList.clear();
            roomList.addAll(rooms);
            filterRooms(txtSearch.getText());
            lblStatus.setText("✅ Found " + rooms.size() + " room(s)");
        });
    }

    private void filterRooms(String searchText) {
        filteredRoomList.clear();

        if (searchText == null || searchText.trim().isEmpty()) {
            filteredRoomList.addAll(roomList);
        } else {
            String lowerSearch = searchText.toLowerCase();
            for (Map<String, Object> room : roomList) {
                String roomName = String.valueOf(room.get("name")).toLowerCase();
                if (roomName.contains(lowerSearch)) {
                    filteredRoomList.add(room);
                }
            }
        }

        if (filteredRoomList.isEmpty() && !roomList.isEmpty()) {
            lblStatus.setText("❌ No rooms match your search");
        }
    }

    @Override
    public void onRoomJoined(int roomId, String roomName, int maxPlayers, List<String> players) {
        Platform.runLater(() -> {
            WaitingRoomController controller = UiNavigator.loadSceneWithController("waiting_room.fxml");
            controller.setClient(client);
            controller.setRoomData(roomId, roomName, maxPlayers);
            controller.setHost(false);
            controller.updatePlayerList(players);
        });
    }

    @Override
    public void onLoginFail(String message) {
        // Server uses LOGIN_FAIL for join room errors too
        Platform.runLater(() -> {
            lblStatus.setText("❌ " + message);
            showAlert(message);
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

    // 🔒 Safe numeric conversion for all JSON numeric fields

}
