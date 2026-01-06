// Pham Ngoc Duc - Lớp 23JIT - Trường VKU - MSSV: 23IT059
package com.tank2d.tankverse.core;

import com.tank2d.tankverse.entity.BotPlayer;
import com.tank2d.tankverse.entity.Entity;
import com.tank2d.tankverse.entity.OtherPlayer;
import com.tank2d.tankverse.entity.Player;
import com.tank2d.tankverse.map.MapLoader;
import com.tank2d.tankverse.ui.MainMenuController;
import com.tank2d.tankverse.ui.UiNavigator;
import com.tank2d.tankverse.utils.Constant;
import com.tank2d.tankverse.utils.PlayerState;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tank2d.tankverse.utils.DataTypeParser.toInt;

public class PlayPanel extends Pane implements Runnable {
    private String userName;
    private final Canvas canvas;
    private final GraphicsContext gc;
    private final List<Entity> entities;
    public List<OtherPlayer> players = new ArrayList<>();
    public Player player;
    private AnimationTimer gameLoop;
    private MapLoader mapLoader;
    private boolean isHost = false;
    public GameClient client;
    private boolean running = false;

    // Bot system
    private BotManager botManager;

    public PlayPanel(String userName, int playerCount, List<Map<String, Object>> playerDataList, int mapId, GameClient client) {
        this.userName = userName;
        this.client = client;
        // Use mapId from parameter
        System.out.println("🗺️ Loading map with ID: " + mapId);
        this.mapLoader = new MapLoader(mapId, this);
        this.canvas = new Canvas(Constant.SCREEN_WIDTH, Constant.SCREEN_HEIGHT);
        this.gc = canvas.getGraphicsContext2D();
        getChildren().add(canvas);
        addQuitButton();

        this.entities = new ArrayList<>();
        this.players = new ArrayList<>();

        // Initialize bot manager
        this.botManager = new BotManager(mapLoader);

        // Nếu chưa có player nào, tạo 1 mặc định
        if (this.player == null) {
            this.player = new Player(0, 0, new Polygon(), 3, mapLoader, userName);
//            players.add(player);
//            entities.add(player);
            System.out.println("[PlayPanel] No player data provided, created default.");
        }

        setupControls();
        setFocusTraversable(true);
        requestFocus();
    }

    public void updateOtherPlayer(PlayerState playerState) {
        OtherPlayer found = null;
        for (OtherPlayer op : players) {
            if (op.getName().equals(playerState.userName) || op.getName().equals(this.userName)) {
                found = op;
                break;
            }
        }

        if (found == null) {
            // create new if not exist
            Polygon solid = new Polygon();
            OtherPlayer newOp = new OtherPlayer(playerState.x, playerState.y, solid, 3, mapLoader, playerState.userName, this.player);
            players.add(newOp);
            //entities.add(newOp);
            System.out.println("[PlayPanel] Added new player: " + playerState.userName);
        } else {
            //System.out.println("update data of " + found.getName());
            found.setX(playerState.x);
            found.setY(playerState.y);
            found.setBodyAngle(playerState.bodyAngle);
            found.setGunAngle(playerState.gunAngle);
            found.setUp(playerState.up);
            found.setDown(playerState.down);
            found.setRight(playerState.right);
            found.setLeft(playerState.left);
            found.setBackward(playerState.backward);
            if (found.isAlive == false && playerState.hp >= 15)
            {
                found.hp = playerState.hp;
                found.isAlive = true;
            }
            else if (found.isAlive == true)
                found.hp = playerState.hp;
            found.bullet = playerState.bullet;
            found.action = playerState.action;
        }
    }




    private void setupControls() {
        // mouse and key handlers (delegate to your methods)
        setOnMouseMoved(this::onMouseMoved);

        // Try to handle keys on the Panel itself
        setOnKeyPressed(e -> {
            //System.out.println("KeyPressed on PlayPanel: " + e.getCode());
            onKeyPressed(e);
        });
        setOnKeyReleased(e -> {
            //System.out.println("KeyReleased on PlayPanel: " + e.getCode());
            onKeyReleased(e);
        });

        // Make sure PlayPanel is focusable and request focus at right time
        setFocusTraversable(true);
        setOnMouseClicked(e -> {
            requestFocus(); // clicking will give focus
            player.action = Constant.ACTION_SHOOT;
            //System.out.println("PlayPanel clicked -> requestFocus()");
        });

        // When scene/window shows, request focus automatically
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((o2, oldWindow, newWindow) -> {
                    if (newWindow != null) {
                        newWindow.showingProperty().addListener((o3, wasShowing, isShowing) -> {
                            if (isShowing) {
                                javafx.application.Platform.runLater(() -> {
                                    requestFocus();
                                    System.out.println("Requested focus on PlayPanel after show()");
                                });
                            }

                        });
                    }
                });

                // also attach scene-level handlers as fallback
                newScene.setOnKeyPressed(e -> {
                    // scene-level receives even if pane isn't focused
                    System.out.println("Scene key pressed: " + e.getCode());
                    onKeyPressed(e);
                });
                newScene.setOnKeyReleased(e -> {
                    onKeyReleased(e);
                });
            }
        });

        // debug focus changes
        focusedProperty().addListener((obs, oldV, newV) -> System.out.println("PlayPanel focus = " + newV));
    }


    private void onKeyPressed(KeyEvent e) {
        KeyCode code = e.getCode();
        switch (code) {
            case W -> player.setUp(true);
            case S -> player.setDown(true);
            case A -> player.setLeft(true);
            case D -> player.setRight(true);
            case SPACE -> player.setBackward(true);
        }
    }


    private void onKeyReleased(KeyEvent e) {
        KeyCode code = e.getCode();
        switch (code) {
            case W -> player.setUp(false);
            case S -> player.setDown(false);
            case A -> player.setLeft(false);
            case D -> player.setRight(false);
            case SPACE -> player.setBackward(false);
        }
    }

    private void onMouseMoved(MouseEvent e) {
        player.onMouseMoved(e);
    }

    public void setHost(boolean value) {
        this.isHost = value;
    }

    public Player getPlayer() {
        return player;
    }

    public List<Entity> getEntities() {
        return entities;
    }

    // Called when remote data received from mini server
    public void applyRemoteState(double x, double y, double angle) {
        player.setX(x);
        player.setY(y);
        // we could smooth interpolate here later
    }

    @Override
    public void run() {
        if (running) return; // ❗ chống start nhiều lần
        running = true;

        final double FRAME_TIME = 1_000_000_000.0 / 60.0;
        final double[] accumulator = {0};

        gameLoop = new AnimationTimer() {
            private long lastTime = 0;

            @Override
            public void handle(long now) {
                if (!running) return;

                if (lastTime == 0) {
                    lastTime = now;
                    return;
                }

                double delta = now - lastTime;
                lastTime = now;
                accumulator[0] += delta;

                while (accumulator[0] >= FRAME_TIME) {
                    updateFixed60FPS();
                    accumulator[0] -= FRAME_TIME;
                }

                draw();
            }
        };

        gameLoop.start();
    }

    public void stopGame() {
        System.out.println("[PlayPanel] STOP GAME");

        running = false;

        if (gameLoop != null) {
            gameLoop.stop();
            gameLoop = null;
        }

        // clear data
        players.clear();
        entities.clear();

        setOnKeyPressed(null);
        setOnKeyReleased(null);
        setOnMouseMoved(null);
    }

    private void updateFixed60FPS() {

        player.update(this);

        mapLoader.eManager.update(0.016);
        for (OtherPlayer oP : players) {
            oP.update(this);
        }

        // Update bots
        botManager.updateAll(this);

        mapLoader.updateBullets(this.player);
        mapLoader.updateTowers(player, this);

    }


//    private void update() {
//        player.update();
//        for (OtherPlayer oP : players)
//        {
//            oP.update();
//        }
//        //for (Entity e : entities) e.update();
//    }

    private void draw() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        mapLoader.draw(gc, this.player);
        //mapLoader.drawCollision(gc, this.player);
        player.draw(gc);
        mapLoader.eManager.draw(gc, player.x, player.y);
        //player.drawSolidArea(gc);
        //mapLoader.debugDrawTileCoordinates(gc, player);
        for (OtherPlayer oP : players) oP.draw(gc);

        // Draw bots
        botManager.drawAll(gc, player);

        mapLoader.drawTowers(gc, player);
        mapLoader.drawBullets(gc, player);
    }

    public void forceQuitGame() {
        stopGame();

        javafx.application.Platform.runLater(() -> {
            MainMenuController controller =
                    UiNavigator.loadSceneWithController("main_menu.fxml");
            controller.setClient(client);
            client.setPacketListener(controller);
        });
    }

    public int getDamage(String name)
    {
        //OtherPlayer result = null;
        System.out.println("search name: " + name);
        for (OtherPlayer a : players) {
            System.out.println("search tartget: " + a.getName());

            if (name.equals(a.getName())) {
                //result = a;
                return a.dmg;
            }
        }

        // Check if it's a bot
        int botDamage = botManager.getDamage(name);
        if (botDamage > 0) {
            return botDamage;
        }

        return this.player.dmg;

    }

    // ===== PUBLIC BOT METHODS =====

    /**
     * Spawn bot(s) for testing
     */
    public void spawnBot() {
        botManager.spawnBot();
    }

    public void spawnBots(int count) {
        System.out.println("[PlayPanel] 🤖 Received request to spawn " + count + " bots near player");
        botManager.spawnBotsNearPlayer(count, player);
        System.out.println("[PlayPanel] ✅ Bot spawn complete. Total bots: " + botManager.getBotCount());
    }

    public void spawnBotAt(double x, double y, String name) {
        botManager.spawnBotAt(x, y, name);
    }

    public void clearBots() {
        botManager.clearAllBots();
    }

    public BotManager getBotManager() {
        return botManager;
    }
    
    /**
     * Update remote bot state (for P2P sync)
     */
    public void updateRemoteBot(String botName, double x, double y, double bodyAngle, double gunAngle, int hp, boolean isAlive) {
        var bot = botManager.getBot(botName);
        
        if (bot == null) {
            // Bot doesn't exist, create it
            bot = new com.tank2d.tankverse.entity.BotPlayer(x, y, mapLoader, botName);
            botManager.addBot(bot);
            System.out.println("[PlayPanel] Created remote bot: " + botName);
        }
        
        // Update state
        bot.setRemoteState(x, y, bodyAngle, gunAngle, hp, isAlive);
    }

    private void addQuitButton() {
        Button btnQuit = new Button("QUIT GAME");
        btnQuit.setPrefSize(140, 45);
        btnQuit.setLayoutX(Constant.SCREEN_WIDTH - 160);
        btnQuit.setLayoutY(20);

        btnQuit.setStyle("""
        -fx-background-color: #c54245;
        -fx-text-fill: white;
        -fx-font-weight: bold;
        -fx-font-size: 14;
        -fx-background-radius: 10;
        -fx-cursor: hand;
    """);

        btnQuit.setOnAction(e -> showQuitConfirm());

        getChildren().add(btnQuit);
    }

    private void showQuitConfirm() {

        // nền mờ che game
        Pane overlay = new Pane();
        overlay.setPrefSize(Constant.SCREEN_WIDTH, Constant.SCREEN_HEIGHT);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.6);");

        // hộp confirm
        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setPrefSize(360, 180);
        box.setLayoutX((Constant.SCREEN_WIDTH - 360) / 2);
        box.setLayoutY((Constant.SCREEN_HEIGHT - 180) / 2);
        box.setStyle("""
        -fx-background-color: #f4e8c1;
        -fx-border-color: #704214;
        -fx-border-width: 4;
        -fx-background-radius: 12;
        -fx-border-radius: 12;
    """);

        Label lbl = new Label("❓ Do you really want to quit the game?");
        lbl.setStyle("""
        -fx-font-size: 15;
        -fx-font-weight: bold;
        -fx-text-fill: #704214;
    """);

        Button btnYes = new Button("YES");
        Button btnNo  = new Button("NO");

        btnYes.setPrefSize(110, 40);
        btnNo.setPrefSize(110, 40);

        btnYes.setStyle("""
        -fx-background-color: #c54245;
        -fx-text-fill: white;
        -fx-font-weight: bold;
        -fx-background-radius: 8;
    """);

        btnNo.setStyle("""
        -fx-background-color: #7a9c8e;
        -fx-text-fill: white;
        -fx-font-weight: bold;
        -fx-background-radius: 8;
    """);

        // YES → quit game
        btnYes.setOnAction(e -> {
            stopGame();

            MainMenuController controller =
                    UiNavigator.loadSceneWithController("main_menu.fxml");
            controller.setClient(client);
            client.setPacketListener(controller);
        });

        // NO → đóng hộp
        btnNo.setOnAction(e -> getChildren().remove(overlay));

        HBox buttons = new HBox(20, btnYes, btnNo);
        buttons.setAlignment(Pos.CENTER);

        box.getChildren().addAll(lbl, buttons);
        overlay.getChildren().add(box);

        getChildren().add(overlay);
    }
}