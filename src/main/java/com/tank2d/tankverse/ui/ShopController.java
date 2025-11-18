package com.tank2d.tankverse.ui;

import com.tank2d.tankverse.core.GameClient;
import com.tank2d.tankverse.core.PacketListener;
import com.tank2d.tankverse.entity.ShopItem;
import com.tank2d.tankverse.utils.ShopItemCell;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShopController implements PacketListener {

    @FXML private ListView<ShopItem> listShopItems;
    @FXML private Label lblGold;
    @FXML private Label lblStatus;
    @FXML private Button btnBuy;

    private GameClient client;
    private ShopItem selectedItem;

    @FXML
    public void initialize() {
        setupListView();
    }

    private void setupListView() {
        listShopItems.setCellFactory(lv -> new ShopItemCell());

        listShopItems.setOnMouseClicked(event -> {
            selectedItem = listShopItems.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                lblStatus.setText("Selected: " + selectedItem.name +
                        " | Final Price: " + selectedItem.getFinalPrice());
            }
        });
    }

    public void setClient(GameClient client) {
        this.client = client;
        client.setPacketListener(this);
        // Request shop list after setting client
        client.requestShopList();
    }

    // Nhấn BUY
    @FXML
    private void onBuy() {
        if (selectedItem == null) {
            lblStatus.setText("Please select an item first!");
            return;
        }

        if (client != null) {
            client.buyItem(selectedItem.id, 1);
        }
    }

    // Nút đóng shop
    @FXML
    private void onClose() {
        Platform.runLater(() -> {
            MainMenuController controller = UiNavigator.loadSceneWithController("main_menu.fxml");
            controller.setClient(client);
        });
    }

    // ========== PacketListener Implementation ==========

    @Override
    public void onShopListReceived(List<Map<String, Object>> items, int gold) {
        Platform.runLater(() -> {
            // ✅ Cập nhật gold
            lblGold.setText(String.valueOf(gold));
            
            // Cập nhật items
            List<ShopItem> shopItems = new ArrayList<>();
            for (Map<String, Object> itemData : items) {
                int id = ((Number) itemData.get("id")).intValue();
                String name = (String) itemData.get("name");
                String description = (String) itemData.get("description");
                int price = ((Number) itemData.get("price")).intValue();
                double discount = ((Number) itemData.getOrDefault("discount", 0.0)).doubleValue();
                int stock = ((Number) itemData.getOrDefault("stock", 0)).intValue();
                
                // ✅ Parse dynamic attributes
                Map<String, Double> attributes = new java.util.HashMap<>();
                Object attrObj = itemData.get("attributes");
                if (attrObj instanceof Map) {
                    Map<?, ?> attrMap = (Map<?, ?>) attrObj;
                    for (Map.Entry<?, ?> entry : attrMap.entrySet()) {
                        String key = String.valueOf(entry.getKey());
                        double value = entry.getValue() instanceof Number ? 
                            ((Number) entry.getValue()).doubleValue() : 0.0;
                        attributes.put(key, value);
                    }
                }
                
                ShopItem item = new ShopItem(id, name, description, price, discount, stock, attributes);
                shopItems.add(item);
            }
            listShopItems.getItems().setAll(shopItems);
            lblStatus.setText("Shop loaded! You have " + gold + " gold");
        });
    }

    @Override
    public void onBuySuccess(int newGold, String message) {
        Platform.runLater(() -> {
            lblGold.setText(String.valueOf(newGold));
            lblStatus.setText("Purchase successful! " + message);
            
            // ✅ Reload shop list để cập nhật stock
            if (client != null) {
                client.requestShopList();
            }
        });
    }

    @Override
    public void onBuyFail(String message) {
        Platform.runLater(() -> {
            lblStatus.setText("Purchase failed: " + message);
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
