package com.tank2d.tankverse.ui;

import com.tank2d.tankverse.core.GameClient;
import com.tank2d.tankverse.core.PacketListener;
import com.tank2d.tankverse.entity.ShopItem;
import com.tank2d.tankverse.utils.AssetLoader;
import com.tank2d.tankverse.utils.Packet;
import com.tank2d.tankverse.utils.PacketType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShopController implements PacketListener {

    @FXML private TabPane tabPane;
    @FXML private GridPane gridShopItems;
    @FXML private GridPane gridShopTanks;
    @FXML private Label lblGold;
    @FXML private Label lblStatus;
    @FXML private Button btnBuy;
    
    // Item detail panel
    @FXML private VBox vboxDetailPanel;
    @FXML private ImageView imgDetailItem;
    @FXML private Label lblImagePlaceholder;
    @FXML private Label lblDetailName;
    @FXML private Label lblDetailPrice;
    @FXML private Label lblDetailStats;
    
    // Tank detail panel
    @FXML private VBox vboxDetailPanelTank;
    @FXML private ImageView imgDetailTank;
    @FXML private Label lblDetailTankName;
    @FXML private Label lblDetailTankPrice;
    @FXML private Label lblDetailTankStats;

    private GameClient client;
    private ShopItem selectedItem;
    private boolean isTankSelected = false;

    @FXML
    public void initialize() {
        // Debug: Check if all labels are initialized
        System.out.println("[Shop] Checking label initialization:");
        System.out.println("[Shop] lblDetailName: " + (lblDetailName != null ? "OK" : "NULL"));
        System.out.println("[Shop] lblDetailPrice: " + (lblDetailPrice != null ? "OK" : "NULL"));
        System.out.println("[Shop] lblDetailStats: " + (lblDetailStats != null ? "OK" : "NULL"));
        System.out.println("[Shop] lblDetailTankName: " + (lblDetailTankName != null ? "OK" : "NULL"));
        System.out.println("[Shop] lblDetailTankPrice: " + (lblDetailTankPrice != null ? "OK" : "NULL"));
        System.out.println("[Shop] lblDetailTankStats: " + (lblDetailTankStats != null ? "OK" : "NULL"));
        
        // Listen to tab changes to update button text
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                if (newTab.getText().contains("Items")) {
                    btnBuy.setText("PURCHASE ITEM");
                } else if (newTab.getText().contains("Tanks")) {
                    btnBuy.setText("PURCHASE TANK");
                }
            }
        });
    }
    
    // Card creation methods
    private VBox createItemCard(ShopItem item) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-border-radius: 5; " +
                      "-fx-background-radius: 5; -fx-padding: 6; -fx-cursor: hand;");
        card.setPrefSize(110, 140);
        card.setMaxSize(110, 140);
        
        // Image (placeholder icon)
        ImageView imgView = new ImageView();
        imgView.setFitWidth(60);
        imgView.setFitHeight(60);
        imgView.setPreserveRatio(true);
        
        // Load item image from HTTP server
        Image itemImage = AssetLoader.loadItemImage(item.name);
        imgView.setImage(itemImage);
        
        // Name
        Label lblName = new Label(item.name);
        lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-alignment: center;");
        lblName.setWrapText(true);
        lblName.setAlignment(Pos.CENTER);
        lblName.setMaxWidth(95);
        lblName.setMaxHeight(35);
        
        // Price with coin icon
        HBox priceBox = new HBox(2);
        priceBox.setAlignment(Pos.CENTER);
        ImageView coinIcon = new ImageView(new Image(getClass().getResourceAsStream("/com/tank2d/tankverse/icon/coin.png")));
        coinIcon.setFitWidth(12);
        coinIcon.setFitHeight(12);
        coinIcon.setPreserveRatio(true);
        Label lblPrice = new Label(String.valueOf(item.getFinalPrice()));
        lblPrice.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 11px; -fx-font-weight: bold;");
        priceBox.getChildren().addAll(coinIcon, lblPrice);
        
        card.getChildren().addAll(imgView, lblName, priceBox);
        
        // Click handler
        card.setOnMouseClicked(event -> {
            selectedItem = item;
            isTankSelected = false;
            displayItemDetail(item);
            lblStatus.setText("Selected: " + item.name + " | Price: " + item.getFinalPrice() + " 🪙");
            highlightCard(card, gridShopItems);
        });
        
        // Hover effect
        card.setOnMouseEntered(e -> {
            if (selectedItem != item || isTankSelected) {
                card.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #2196F3; -fx-border-width: 2; " +
                             "-fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 10; -fx-cursor: hand;");
            }
        });
        card.setOnMouseExited(e -> {
            if (selectedItem != item || isTankSelected) {
                card.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-border-radius: 5; " +
                             "-fx-background-radius: 5; -fx-padding: 10; -fx-cursor: hand;");
            }
        });
        
        return card;
    }

    private VBox createTankCard(ShopItem tank) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-border-radius: 5; " +
                      "-fx-background-radius: 5; -fx-padding: 6; -fx-cursor: hand;");
        card.setPrefSize(110, 140);
        card.setMaxSize(110, 140);
        
        // Tank Image
        ImageView imgView = new ImageView();
        imgView.setFitWidth(65);
        imgView.setFitHeight(65);
        imgView.setPreserveRatio(true);
        
        Image tankImage = loadTankImage(tank.name);
        if (tankImage != null) {
            imgView.setImage(tankImage);
        }
        
        // Name
        Label lblName = new Label(tank.name);
        lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-alignment: center;");
        lblName.setWrapText(true);
        lblName.setAlignment(Pos.CENTER);
        lblName.setMaxWidth(95);
        lblName.setMaxHeight(35);
        
        // Price with coin icon
        HBox priceBox = new HBox(2);
        priceBox.setAlignment(Pos.CENTER);
        ImageView coinIcon = new ImageView(new Image(getClass().getResourceAsStream("/com/tank2d/tankverse/icon/coin.png")));
        coinIcon.setFitWidth(12);
        coinIcon.setFitHeight(12);
        coinIcon.setPreserveRatio(true);
        Label lblPrice = new Label(String.valueOf(tank.price));
        lblPrice.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 11px; -fx-font-weight: bold;");
        priceBox.getChildren().addAll(coinIcon, lblPrice);
        
        card.getChildren().addAll(imgView, lblName, priceBox);
        
        // Click handler
        card.setOnMouseClicked(event -> {
            selectedItem = tank;
            isTankSelected = true;
            displayTankDetail(tank);
            lblStatus.setText("Selected: " + tank.name + " | Price: " + tank.price + " 🪙");
            highlightCard(card, gridShopTanks);
        });
        
        // Hover effect
        card.setOnMouseEntered(e -> {
            if (selectedItem != tank || !isTankSelected) {
                card.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #2196F3; -fx-border-width: 2; " +
                             "-fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 10; -fx-cursor: hand;");
            }
        });
        card.setOnMouseExited(e -> {
            if (selectedItem != tank || !isTankSelected) {
                card.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-border-radius: 5; " +
                             "-fx-background-radius: 5; -fx-padding: 10; -fx-cursor: hand;");
            }
        });
        
        return card;
    }

    private void highlightCard(VBox selectedCard, GridPane gridPane) {
        // Remove highlight from all cards in this grid
        gridPane.getChildren().forEach(node -> {
            if (node instanceof VBox) {
                node.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-border-radius: 5; " +
                            "-fx-background-radius: 5; -fx-padding: 10; -fx-cursor: hand;");
            }
        });
        
        // Highlight selected card
        selectedCard.setStyle("-fx-background-color: #e3f2fd; -fx-border-color: #2196F3; -fx-border-width: 2; " +
                            "-fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 10; -fx-cursor: hand;");
    }
    
    private void displayItemDetail(ShopItem item) {
        System.out.println("[Shop] Displaying item detail: " + item.name);
        System.out.println("[Shop] Item attributes: " + item.getAttributes());
        
        lblDetailName.setText(item.name);
        lblDetailPrice.setText(String.valueOf(item.getFinalPrice()));
        
        // Display stats
        if (item.getAttributes() != null && !item.getAttributes().isEmpty()) {
            StringBuilder stats = new StringBuilder();
            item.getAttributes().forEach((key, value) -> {
                String displayKey = key.toUpperCase();
                stats.append("• ").append(displayKey).append(": +").append(value.intValue()).append("\n");
            });
            lblDetailStats.setText(stats.toString());
        } else {
            lblDetailStats.setText("No special stats");
        }


        // Show discount if any
        if (item.discount > 0) {
            lblDetailPrice.setText(item.getFinalPrice() + 
                String.format(" (-%d%%)", (int)(item.discount * 100)));
        }
        
        // Load item image from HTTP server
        Image itemImage = AssetLoader.loadItemImage(item.name);
        imgDetailItem.setImage(itemImage);
        lblImagePlaceholder.setVisible(false);
    }
    
    private void displayTankDetail(ShopItem tank) {
        System.out.println("[Shop] Displaying tank detail: " + tank.name);
        System.out.println("[Shop] Tank attributes: " + tank.getAttributes());
        
        // Set name and price
        String tankName = tank.name;
        String tankPrice = String.valueOf(tank.price);
        
        System.out.println("[Shop] Setting lblDetailTankName to: " + tankName);
        System.out.println("[Shop] Setting lblDetailTankPrice to: " + tankPrice);
        
        lblDetailTankName.setText(tankName);
        lblDetailTankPrice.setText(tankPrice);
        
        // Display tank stats
        if (tank.getAttributes() != null && !tank.getAttributes().isEmpty()) {
            StringBuilder stats = new StringBuilder();
            Map<String, Double> attrs = tank.getAttributes();
            
            stats.append("⚡ Speed: ").append(attrs.getOrDefault("spd", 0.0).intValue()).append("\n");
            stats.append("🛡️ Armor: ").append(attrs.getOrDefault("defense", 0.0).intValue()).append("\n");
            stats.append("💥 Damage: ").append(attrs.getOrDefault("dmg", 0.0).intValue()).append("\n");
            stats.append("❤️ Health: ").append(attrs.getOrDefault("hp", 0.0).intValue()).append("\n");
            
            if (attrs.containsKey("reload_speed")) {
                stats.append("🔄 Reload: ").append(attrs.get("reload_speed").intValue()).append("\n");
            }
            
            if (attrs.containsKey("mp")) {
                stats.append("⭐ Mana: ").append(attrs.get("mp").intValue()).append("\n");
            }
            
            String statsText = stats.toString();
            System.out.println("[Shop] Setting lblDetailTankStats to: " + statsText);
            lblDetailTankStats.setText(statsText);
        } else {
            System.out.println("[Shop] No stats available for tank");
            lblDetailTankStats.setText("No stats available");
        }
        
        // Load tank image
        Image tankImage = loadTankImage(tank.name);
        if (tankImage != null) {
            imgDetailTank.setImage(tankImage);
            System.out.println("[Shop] Tank image loaded successfully");
        } else {
            imgDetailTank.setImage(null);
            System.out.println("[Shop] No tank image found");
        }
        
        System.out.println("[Shop] Tank detail display completed");
    }
    
    private Image loadTankImage(String tankName) {
        return AssetLoader.loadTankImage(tankName);
    }

    public void setClient(GameClient client) {
        this.client = client;
        client.setPacketListener(this);
        // Request both shop lists
        client.requestShopList();
        requestTankShopList();
    }
    
    private void requestTankShopList() {
        if (client != null) {
            Packet packet = new Packet(PacketType.TANK_SHOP_LIST);
            client.sendPacket(packet);
        }
    }

    // Nhấn BUY
    @FXML
    private void onBuy() {
        if (selectedItem == null) {
            lblStatus.setText("Please select an item first!");
            return;
        }

        if (client != null) {
            if (isTankSelected) {
                // Buy tank
                Packet packet = new Packet(PacketType.BUY_TANK);
                packet.data.put("tankId", selectedItem.id);
                client.sendPacket(packet);
            } else {
                // Buy item
                client.buyItem(selectedItem.id, 1);
            }
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
            // Update gold
            lblGold.setText(String.valueOf(gold));
            
            System.out.println("[Shop] Received " + items.size() + " items from server");
            
            // Clear grid
            gridShopItems.getChildren().clear();
            
            // Populate grid with item cards
            int column = 0;
            int row = 0;
            int itemsPerRow = 3;
            
            for (Map<String, Object> itemData : items) {
                int id = ((Number) itemData.get("id")).intValue();
                String name = (String) itemData.get("name");
                String description = (String) itemData.get("description");
                int price = ((Number) itemData.get("price")).intValue();
                double discount = ((Number) itemData.getOrDefault("discount", 0.0)).doubleValue();
                int stock = ((Number) itemData.getOrDefault("stock", 0)).intValue();
                
                // Parse dynamic attributes
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
                
                System.out.println("[Shop] Item: " + name + " | Attributes: " + attributes);
                
                ShopItem item = new ShopItem(id, name, description, price, discount, stock, attributes);
                
                // Create and add card
                VBox card = createItemCard(item);
                gridShopItems.add(card, column, row);
                
                column++;
                if (column >= itemsPerRow) {
                    column = 0;
                    row++;
                }
            }
            
            lblStatus.setText("Shop loaded! You have " + gold + " 🪙");
        });
    }

    // New callback for tank shop
    @Override
    public void onTankShopDataReceived(List<Map<String, Object>> tanks, int gold) {
        Platform.runLater(() -> {
            System.out.println("[Shop] Received " + tanks.size() + " tanks from server");
            
            // Clear grid
            gridShopTanks.getChildren().clear();
            
            // Populate grid with tank cards
            int column = 0;
            int row = 0;
            int tanksPerRow = 3;
            
            for (Map<String, Object> tankData : tanks) {
                int id = ((Number) tankData.get("id")).intValue();
                String name = (String) tankData.get("name");
                String description = (String) tankData.get("description");
                int price = ((Number) tankData.get("price")).intValue();
                
                // Parse tank attributes
                Map<String, Double> attributes = new java.util.HashMap<>();
                Object attrObj = tankData.get("attributes");
                if (attrObj instanceof Map) {
                    Map<?, ?> attrMap = (Map<?, ?>) attrObj;
                    for (Map.Entry<?, ?> entry : attrMap.entrySet()) {
                        String key = String.valueOf(entry.getKey());
                        double value = entry.getValue() instanceof Number ? 
                            ((Number) entry.getValue()).doubleValue() : 0.0;
                        attributes.put(key, value);
                    }
                }
                
                System.out.println("[Shop] Tank: " + name + " | Attributes: " + attributes);
                
                // Tanks don't have discount/stock in same way
                ShopItem tank = new ShopItem(id, name, description, price, 0.0, 1, attributes);
                
                // Create and add card
                VBox card = createTankCard(tank);
                gridShopTanks.add(card, column, row);
                
                column++;
                if (column >= tanksPerRow) {
                    column = 0;
                    row++;
                }
            }
        });
    }

    @Override
    public void onBuySuccess(int newGold, String message) {
        Platform.runLater(() -> {
            lblGold.setText(String.valueOf(newGold));
            lblStatus.setText("Purchase successful! " + message);
            
            // ✅ Reload shop lists để cập nhật
            if (client != null) {
                client.requestShopList();
                requestTankShopList();
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
