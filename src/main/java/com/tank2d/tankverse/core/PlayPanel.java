// Pham Ngoc Duc - Lớp 23JIT - Trường VKU - MSSV: 23IT059
package com.tank2d.tankverse.core;

import com.tank2d.tankverse.entity.Entity;
import com.tank2d.tankverse.entity.OtherPlayer;
import com.tank2d.tankverse.entity.Player;
import com.tank2d.tankverse.map.MapLoader;
import com.tank2d.tankverse.utils.Constant;
import com.tank2d.tankverse.utils.PlayerState;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

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
    List<OtherPlayer> players = new ArrayList<>();
    private Player player;
    private AnimationTimer gameLoop;
    private MapLoader mapLoader;
    private boolean isHost = false;


    public PlayPanel(String userName, int playerCount, List<Map<String, Object>> playerDataList, int mapId) {
        this.userName = userName;
        
        // Use mapId from parameter
        System.out.println("🗺️ Loading map with ID: " + mapId);
        this.mapLoader = new MapLoader(mapId);
        this.canvas = new Canvas(Constant.SCREEN_WIDTH, Constant.SCREEN_HEIGHT);
        this.gc = canvas.getGraphicsContext2D();
        getChildren().add(canvas);

        this.entities = new ArrayList<>();
        this.players = new ArrayList<>();




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
            if (found.isAlive == false && playerState.hp > 30)
            {
                found.hp = playerState.hp;
                found.isAlive = true;
            }
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
        // Fixed timestep: 60 FPS
        final double FRAME_TIME = 1_000_000_000.0 / 60.0; // 16.666ms
        final double[] accumulator = {0};

        AnimationTimer gameLoop = new AnimationTimer() {
            private long lastTime = 0;

            @Override
            public void handle(long now) {
                if (lastTime == 0) {
                    lastTime = now;
                    return;
                }

                double delta = now - lastTime;
                lastTime = now;
                accumulator[0] += delta;

                // update game exactly 60 times per second
                while (accumulator[0] >= FRAME_TIME) {
                    updateFixed60FPS();
                    accumulator[0] -= FRAME_TIME;
                }

                draw(); // render as fast as possible
            }
        };

        gameLoop.start();
    }
    private void updateFixed60FPS() {

        player.update(this);

        mapLoader.eManager.update(0.016);
        for (OtherPlayer oP : players) {
            oP.update(this);
        }
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
        mapLoader.drawCollision(gc, this.player);
        player.draw(gc);
        mapLoader.eManager.draw(gc, player.x, player.y);
        //player.drawSolidArea(gc);
        //mapLoader.debugDrawTileCoordinates(gc, player);
        for (OtherPlayer oP : players) oP.draw(gc);
        mapLoader.drawTowers(gc, player);
        mapLoader.drawBullets(gc, player);
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
        return this.player.dmg;

    }
}
