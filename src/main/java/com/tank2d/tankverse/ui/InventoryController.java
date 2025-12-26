package com.tank2d.tankverse.ui;

import com.tank2d.tankverse.core.GameClient;
import com.tank2d.tankverse.core.PacketListener;
import com.tank2d.tankverse.utils.Packet;
import com.tank2d.tankverse.utils.PacketType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.List;
import java.util.Map;

public class InventoryController implements PacketListener {


    @FXML private StackPane rootPane;
    @FXML private Label lblTitle;
    @FXML private Label lblGold;
    @FXML private GridPane gridInventory;
    @FXML private ImageView imgEquippedTank;
    @FXML private Label lblEquippedTankName;
    @FXML private ProgressBar pbHealth;
    @FXML private ProgressBar pbDamage;
    @FXML private ProgressBar pbSpeed;
    @FXML private Button btnUseItem;
    @FXML private Button btnBack;

    private GameClient client;
    private Map<String, Object> selectedItem;
    private Map<String, Object> equippedTank;
    private List<Map<String, Object>> allTanks;

    @FXML
    public void initialize() {
        // Set up back button
        btnBack.setOnAction(e -> onBack());
        
        // Set up use item button
        btnUseItem.setOnAction(e -> onUseItem());
        btnUseItem.setDisable(true); // Initially disabled
    }

    public void setClient(GameClient client) {
        this.client = client;
        client.setPacketListener(this);
        
        // Request inventory data
        requestInventory();
    }

    public void requestInventory() {
        Packet p = new Packet(PacketType.INVENTORY_REQUEST);
        client.sendPacket(p);
    }

    @Override
    public void onInventoryReceived(List<Map<String, Object>> tanks, List<Map<String, Object>> items, int gold, Map<String, Object> equippedTank) {
        Platform.runLater(() -> {
            lblGold.setText(String.valueOf(gold));
            this.allTanks = tanks;
            this.equippedTank = equippedTank;
            displayEquippedTank(equippedTank);
            displayInventoryItems(items);
        });
    }

    private void updateInventory(Map<String, Object> data) {
        // Update gold
        int gold = (int) data.get("gold");
        lblGold.setText(String.format("%,d 💰", gold));

        // Update equipped tank
        if (data.containsKey("equippedTank")) {
            equippedTank = (Map<String, Object>) data.get("equippedTank");
            displayEquippedTank(equippedTank);
        }

        // Update inventory items
        List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
        displayInventoryItems(items);
    }

    private void displayEquippedTank(Map<String, Object> tank) {
        if (tank == null) return;

        String tankName = (String) tank.get("name");
        lblEquippedTankName.setText(tankName);

        // Load tank image
        Image tankImage = loadTankImage(tankName);
        if (tankImage != null) {
            imgEquippedTank.setImage(tankImage);
        }

        // Update stats
        Map<String, Double> attributes = (Map<String, Double>) tank.get("attributes");
        if (attributes != null) {
            pbHealth.setProgress(Math.min(attributes.getOrDefault("health", 100.0) / 200.0, 1.0));
            pbDamage.setProgress(Math.min(attributes.getOrDefault("damage", 50.0) / 100.0, 1.0));
            pbSpeed.setProgress(Math.min(attributes.getOrDefault("speed", 5.0) / 10.0, 1.0));
        }

        // Add click handler to select new tank
        imgEquippedTank.setOnMouseClicked(e -> showTankSelectionOverlay());
        lblEquippedTankName.setOnMouseClicked(e -> showTankSelectionOverlay());
    }

    private void displayInventoryItems(List<Map<String, Object>> items) {
        gridInventory.getChildren().clear();

        int row = 0;
        int col = 0;
        final int maxCols = 4;

        for (Map<String, Object> item : items) {
            VBox itemCard = createItemCard(item);
            gridInventory.add(itemCard, col, row);

            col++;
            if (col >= maxCols) {
                col = 0;
                row++;
            }
        }
    }

    private VBox createItemCard(Map<String, Object> item) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-border-radius: 5; " +
                      "-fx-background-radius: 5; -fx-padding: 8; -fx-cursor: hand;");
        card.setPrefSize(120, 120);
        card.setMaxSize(120, 120);

        // Item icon (placeholder)
        ImageView imgView = new ImageView();
        imgView.setFitWidth(50);
        imgView.setFitHeight(50);
        imgView.setPreserveRatio(true);

        // Try to load item icon
        try {
            Image itemImage = new Image(getClass().getResourceAsStream("/com/tank2d/tankverse/icon/item.png"));
            imgView.setImage(itemImage);
        } catch (Exception e) {
            // Use colored background
            imgView.setStyle("-fx-background-color: #4CAF50;");
        }

        // Item name
        Label lblName = new Label((String) item.get("name"));
        lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 10px; -fx-text-alignment: center;");
        lblName.setWrapText(true);
        lblName.setAlignment(Pos.CENTER);
        lblName.setMaxWidth(100);
        lblName.setMaxHeight(30);

        // Quantity
        int quantity = ((Number) item.get("quantity")).intValue();
        Label lblQuantity = new Label("x" + quantity);
        lblQuantity.setStyle("-fx-font-size: 9px; -fx-text-fill: #666;");

        card.getChildren().addAll(imgView, lblName, lblQuantity);

        // Click handler
        card.setOnMouseClicked(event -> {
            selectedItem = item;
            btnUseItem.setDisable(false);
            btnUseItem.setText("USE " + ((String) item.get("name")).toUpperCase());
            highlightCard(card);
        });

        // Hover effect
        card.setOnMouseEntered(e -> {
            if (selectedItem != item) {
                card.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #2196F3; -fx-border-width: 2; " +
                             "-fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8; -fx-cursor: hand;");
            }
        });
        card.setOnMouseExited(e -> {
            if (selectedItem != item) {
                card.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-border-radius: 5; " +
                             "-fx-background-radius: 5; -fx-padding: 8; -fx-cursor: hand;");
            }
        });

        return card;
    }

    private void highlightCard(VBox card) {
        // Reset all cards
        for (var child : gridInventory.getChildren()) {
            if (child instanceof VBox) {
                child.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-border-radius: 5; " +
                              "-fx-background-radius: 5; -fx-padding: 8; -fx-cursor: hand;");
            }
        }

        // Highlight selected card
        card.setStyle("-fx-background-color: #e3f2fd; -fx-border-color: #2196F3; -fx-border-width: 3; " +
                     "-fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8; -fx-cursor: hand;");
    }

    private Image loadTankImage(String tankName) {
        try {
            String imagePath = "/com/tank2d/tankverse/tank/" + tankName.toLowerCase().replace(" ", "_") + ".png";
            return new Image(getClass().getResourceAsStream(imagePath));
        } catch (Exception e) {
            // Return default tank image or null
            try {
                return new Image(getClass().getResourceAsStream("/com/tank2d/tankverse/tank/default_tank.png"));
            } catch (Exception ex) {
                return null;
            }
        }
    }

    @FXML
    private void onUseItem() {
        if (selectedItem == null) return;

        // TODO: Implement item usage logic
        // For now, just show a message
        System.out.println("Using item: " + selectedItem.get("name"));
        
        // You might want to send a packet to server for item usage
        // Packet p = new Packet(PacketType.USE_ITEM);
        // p.data.put("itemId", selectedItem.get("itemId"));
        // client.sendPacket(p);
    }

    @Override
    public void onDisconnected() {
        Platform.runLater(() -> {
            UiNavigator.loadScene("login.fxml");
        });
    }

    @FXML
    private void onBack() {
        Platform.runLater(() -> {
            MainMenuController controller = UiNavigator.loadSceneWithController("main_menu.fxml");
            if (client != null) {
                controller.setClient(client);
            }
        });
    }

    private void showTankSelectionOverlay() {
        if (allTanks == null || allTanks.isEmpty()) return;

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.7);");
        overlay.setPrefSize(rootPane.getWidth(), rootPane.getHeight());

        VBox panel = new VBox(25);
        panel.setAlignment(Pos.CENTER);
        panel.setMaxWidth(650);
        panel.setMaxHeight(Region.USE_PREF_SIZE);

        panel.setStyle("-fx-background-color: #f4e8c1; " +
                "-fx-padding: 35; " +
                "-fx-background-radius: 15; " +
                "-fx-border-radius: 12; " +
                "-fx-border-color: #704214; " +
                "-fx-border-width: 6; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 20, 0, 0, 10);");

        Label title = new Label("CHOOSE YOUR UNIT");
        title.setFont(Font.font("System Bold", 32));
        title.setTextFill(Color.web("#704214"));
        DropShadow ds = new DropShadow();
        ds.setColor(Color.web("#8B4513"));
        ds.setRadius(2);
        title.setEffect(ds);

        // Kẻ một đường phân cách nhỏ
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #8a6f47; -fx-opacity: 0.3;");
        sep.setPrefWidth(500);

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setAlignment(Pos.CENTER);

        int col = 0, row = 0;
        final int maxCol = 3;

        for (Map<String, Object> tank : allTanks) {
            VBox card = createTankCard(tank, overlay);
            grid.add(card, col, row);

            col++;
            if (col >= maxCol) {
                col = 0;
                row++;
            }
        }

        Button btnClose = new Button("CLOSE");
        btnClose.setStyle("-fx-background-color: linear-gradient(to bottom, #d95e61, #c54245); " +
                "-fx-border-color: #8b2e31; -fx-border-width: 2; " +
                "-fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 8; -fx-cursor: hand;");
        btnClose.setPrefWidth(120);
        btnClose.setOnAction(e -> rootPane.getChildren().remove(overlay));

        panel.getChildren().addAll(title, sep, grid, btnClose);
        overlay.getChildren().add(panel);

        overlay.setOnMouseClicked(e -> rootPane.getChildren().remove(overlay));
        panel.setOnMouseClicked(e -> e.consume());

        rootPane.getChildren().add(overlay);
    }

    private VBox createTankCard(Map<String, Object> tank, StackPane overlay) {

        int tankId = ((Number) tank.get("tankId")).intValue();
        String name = (String) tank.get("name");

        ImageView img = new ImageView();
        img.setFitWidth(140);
        img.setFitHeight(100);
        img.setPreserveRatio(true);
        img.setImage(loadTankImage(name));

        Label lbl = new Label(name);
        lbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        VBox card = new VBox(10, img, lbl);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(10));
        card.setPrefSize(180, 150);

        boolean isEquipped = equippedTank != null
                && ((Number) equippedTank.get("tankId")).intValue() == tankId;

        card.setStyle(isEquipped
                ? "-fx-background-color: #16a085; -fx-background-radius: 8;"
                : "-fx-background-color: #34495e; -fx-background-radius: 8; -fx-cursor: hand;"
        );

        if (!isEquipped) {
            card.setOnMouseClicked(e -> {
                equipTank(tankId);
                rootPane.getChildren().remove(overlay);
            });
        }

        return card;
    }

    private void equipTank(int tankId) {
        Packet p = new Packet(PacketType.EQUIP_TANK);
        p.data.put("tankId", tankId);
        client.sendPacket(p);
    }

    @Override
    public void onEquipTankSuccess(int tankId) {
        Platform.runLater(() -> {
            // Find the new equipped tank from allTanks
            for (Map<String, Object> tank : allTanks) {
                if (((Number) tank.get("tankId")).intValue() == tankId) {
                    equippedTank = tank;
                    displayEquippedTank(equippedTank);
                    break;
                }
            }
        });
    }

    @Override
    public void onEquipTankFail(String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Equip Tank Failed");
            alert.setContentText(msg);
            alert.show();
        });
    }
}
