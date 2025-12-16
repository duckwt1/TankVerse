package com.tank2d.tankverse.map;

// Pham Ngoc Duc - Lớp 23JIT - Trường VKU - MSSV: 23IT059

import com.tank2d.tankverse.entity.Entity;
import com.tank2d.tankverse.entity.Player;
import com.tank2d.tankverse.object.Bullet;
import com.tank2d.tankverse.utils.Constant;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.awt.geom.Area;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;

public class MapLoader {

    public int id;
    public String name;
    public ArrayList<Layer> layers = new ArrayList<>();
    public ArrayList<TileSet> tileSets = new ArrayList<>();
    public Tile[] tiles;
    public int width;
    public int height;
    public Polygon testCollisionP;
    private ArrayList<Bullet> bullets = new ArrayList<>();

    public MapLoader(int id) {
        this.id = id;
        loadMap("/com/tank2d/tankverse/map/map" + id + ".tmj");
    }

    private void loadMap(String mapPath) {
        try {
            System.out.println("🗺️ Loading map: " + mapPath);
            var inputStream = getClass().getResourceAsStream(mapPath);
            if (inputStream == null) {
                System.err.println("❌ Map file not found: " + mapPath);
                return;
            }

            String jsonString = new String(inputStream.readAllBytes());
            JSONObject json = new JSONObject(jsonString);

            int width = json.getInt("width");
            int height = json.getInt("height");
            this.width = width;
            this.height = height;

            // ===== Load layers =====
            JSONArray jsonLayers = json.getJSONArray("layers");
            for (int i = 0; i < jsonLayers.length(); i++) {
                JSONObject jLayer = jsonLayers.getJSONObject(i);
                if (!jLayer.getString("type").equals("tilelayer")) continue;

                Layer layer = new Layer();
                layer.id = jLayer.getInt("id");
                layer.visible = jLayer.optBoolean("visible", true);
                JSONArray data = jLayer.getJSONArray("data");

                layer.data = new int[height][width];
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int idx = y * width + x;
                        layer.data[y][x] = data.getInt(idx);
                    }
                }
                layers.add(layer);
            }
            layers.sort(Comparator.comparingInt(l -> l.id));
            System.out.println("✅ Loaded " + layers.size() + " layers.");

            // ===== Load tilesets =====
            JSONArray jsonTilesets = json.getJSONArray("tilesets");
            int maxGid = 0;

            for (int i = 0; i < jsonTilesets.length(); i++) {
                JSONObject jTs = jsonTilesets.getJSONObject(i);

                TileSet ts = new TileSet();
                ts.firstgid = jTs.getInt("firstgid");
                String rawSource = jTs.getString("source");
                String tsxPath = "/com/tank2d/tankverse/tileset/" + new File(rawSource).getName();
                var tsxStream = getClass().getResourceAsStream(tsxPath);
                if (tsxStream == null) {
                    System.err.println("❌ Tileset not found: " + tsxPath);
                    continue;
                }

                // Parse .tsx (XML)
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(tsxStream);
                doc.getDocumentElement().normalize();

                Element tilesetElement = doc.getDocumentElement();
                ts.tileWidth = Integer.parseInt(tilesetElement.getAttribute("tilewidth"));
                ts.tileHeight = Integer.parseInt(tilesetElement.getAttribute("tileheight"));
                ts.columns = Integer.parseInt(tilesetElement.getAttribute("columns"));
                ts.tileCount = Integer.parseInt(tilesetElement.getAttribute("tilecount"));

                NodeList imageNodes = tilesetElement.getElementsByTagName("image");
                if (imageNodes.getLength() > 0) {
                    Element imageElement = (Element) imageNodes.item(0);
                    String imageSource = imageElement.getAttribute("source");
                    String imageFileName = new File(imageSource).getName();
                    String tilePath = "/com/tank2d/tankverse/tile/" + imageFileName;
                    var tileImageStream = getClass().getResourceAsStream(tilePath);
                    if (tileImageStream != null) {
                        ts.image = new Image(tileImageStream);
                    }
                    tileSets.add(ts);
                    maxGid = Math.max(maxGid, ts.firstgid + ts.tileCount - 1);
                    System.out.println("🧩 Loaded tileset image: " + imageFileName + " firstgid=" + ts.firstgid);
                }

                // ===== Load collision polygons defined inside this tileset (.tsx)
                NodeList tileCollisionList = tilesetElement.getElementsByTagName("tile");
                for (int k = 0; k < tileCollisionList.getLength(); k++) {
                    Element tileElement = (Element) tileCollisionList.item(k);
                    int localId = Integer.parseInt(tileElement.getAttribute("id")); // local id inside tileset

                    NodeList objectGroupList = tileElement.getElementsByTagName("objectgroup");
                    if (objectGroupList.getLength() == 0) continue;
                    Element objectGroup = (Element) objectGroupList.item(0);
                    NodeList objectList = objectGroup.getElementsByTagName("object");
                    if (objectList.getLength() == 0) continue;
                    Element objectElement = (Element) objectList.item(0);

                    float offsetX = 0f;
                    float offsetY = 0f;
                    if (objectElement.hasAttribute("x")) offsetX = Float.parseFloat(objectElement.getAttribute("x"));
                    if (objectElement.hasAttribute("y")) offsetY = Float.parseFloat(objectElement.getAttribute("y"));

                    NodeList polygonList = objectElement.getElementsByTagName("polygon");
                    if (polygonList.getLength() == 0) continue;
                    Element polygonElement = (Element) polygonList.item(0);

                    String pointsString = polygonElement.getAttribute("points").trim();
                    String[] pointPairs = pointsString.split(" ");

                    int[] xPoints = new int[pointPairs.length];
                    int[] yPoints = new int[pointPairs.length];
                    for (int p = 0; p < pointPairs.length; p++) {
                        String[] xy = pointPairs[p].split(",");
                        float px = Float.parseFloat(xy[0]);
                        float py = Float.parseFloat(xy[1]);
                        // polygon stored in tile-local coordinates (including object's offset)
                        xPoints[p] = Math.round(offsetX + px);
                        yPoints[p] = Math.round(offsetY + py);
                    }

                    Polygon poly = new Polygon(xPoints, yPoints, pointPairs.length);
                    ts.collisionByLocalId.put(localId, poly);
                } // end tileCollisionList

                System.out.println("⚙️ tileset.firstgid=" + ts.firstgid + " collision local count=" + ts.collisionByLocalId.size());
            } // end tilesets loop

            // ===== Build global tiles[] indexed by GID and attach collision polygons =====
            if (maxGid < 0) maxGid = 0;
            tiles = new Tile[maxGid + 1];

            for (TileSet ts : tileSets) {
                for (int local = 0; local < ts.tileCount; local++) {
                    int gid = ts.firstgid + local;
                    Tile t = new Tile();
                    t.gid = gid;

                    // Crop the sprite for this tile
                    int sx = (local % ts.columns) * ts.tileWidth;
                    int sy = (local / ts.columns) * ts.tileHeight;
                    try {
                        t.image = new WritableImage(ts.image.getPixelReader(), sx, sy, ts.tileWidth, ts.tileHeight);
                    } catch (Exception ex) {
                        t.image = null;
                    }

                    // If this tileset defined a collision polygon for this local id -> attach to global gid
                    Polygon localPoly = ts.collisionByLocalId.get(local);
                    if (localPoly != null) {
                        // store polygon (tile-local coords) on tile
                        t.collision = true;
                        // Clone the polygon so later modifications don't affect original
                        Polygon cloned = new Polygon(localPoly.xpoints, localPoly.ypoints, localPoly.npoints);
                        t.solidPolygon = cloned;
                    }

                    // Place into tiles[] (some gids might be unused -> remain null)
                    if (gid >= 0 && gid < tiles.length) tiles[gid] = t;
                }
            }

            // Debug: print all GIDs that have collision polygon
            StringBuilder sb = new StringBuilder("Collision GIDs: ");
            for (int g = 0; g < tiles.length; g++) {
                if (tiles[g] != null && tiles[g].collision) sb.append(g).append(", ");
            }
            System.out.println(sb.toString());

            System.out.println("✅ Map " + id + " loaded successfully (maxGid=" + maxGid + ").");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private Tile exist(int id, ArrayList<Tile> list) {
        for (Tile tile : list) {
            if (tile.gid == id) return tile;
        }
        return null;
    }

    public void draw(GraphicsContext gc, Player player) {
        if (tiles == null || layers.isEmpty()) return;

        int playerTileX = (int) (player.x / Constant.TILESIZE);
        int playerTileY = (int) (player.y / Constant.TILESIZE);

        int halfCols = Constant.SCREEN_COL / 2;
        int halfRows = Constant.SCREEN_ROW / 2;

        for (Layer layer : layers) {
            if (!layer.visible) continue;

            for (int y = playerTileY - halfRows; y <= playerTileY + halfRows; y++) {
                for (int x = playerTileX - halfCols; x <= playerTileX + halfCols; x++) {
                    if (y < 0 || y >= layer.data.length) continue;
                    if (x < 0 || x >= layer.data[0].length) continue;

                    int gid = layer.data[y][x];
                    if (gid == 0 || tiles[gid] == null || tiles[gid].image == null) continue;

                    double screenX = (x * Constant.TILESIZE) - player.x + Constant.SCREEN_WIDTH / 2.0;
                    double screenY = (y * Constant.TILESIZE) - player.y + Constant.SCREEN_HEIGHT / 2.0;

                    gc.drawImage(tiles[gid].image, screenX, screenY, Constant.TILESIZE, Constant.TILESIZE);
                }
            }
        }

        // Debug player info
        gc.setFill(Color.WHITE);
        gc.fillText("Player: (" + player.x + ", " + player.y + ")", 10, 20);

        if (testCollisionP != null) {
            gc.setStroke(Color.MAGENTA);
            gc.setLineWidth(2);

            // Chuyển polygon sang mảng double[]
            double[] xs = new double[testCollisionP.npoints];
            double[] ys = new double[testCollisionP.npoints];

            for (int i = 0; i < testCollisionP.npoints; i++) {
                xs[i] = testCollisionP.xpoints[i];
                ys[i] = testCollisionP.ypoints[i];
            }

            gc.strokePolygon(xs, ys, testCollisionP.npoints);
        }
        //drawBullets(gc);
    }
    public void drawBullets(GraphicsContext gc, Player player) {
        for (Bullet b : bullets) {
            b.draw(gc, player);
        }
    }
    public void addBullet(Bullet b) {
        bullets.add(b);
    }

    public ArrayList<Bullet> getBullets() {
        return bullets;
    }

    public void updateBullets(Player player) {
        for (int i = 0; i < bullets.size(); i++) {
            Bullet b = bullets.get(i);
            b.update(player, this);

            if (!b.isActive()) {
                bullets.remove(i);
                i--;
            }
        }
    }
    public boolean checkBulletCollision(Polygon bulletPoly) {

        int minTileX = (int)(bulletPoly.getBounds().x / Constant.TILESIZE);
        int minTileY = (int)(bulletPoly.getBounds().y / Constant.TILESIZE);
        int maxTileX = (int)((bulletPoly.getBounds().x + bulletPoly.getBounds().width) / Constant.TILESIZE);
        int maxTileY = (int)((bulletPoly.getBounds().y + bulletPoly.getBounds().height) / Constant.TILESIZE);

        for (Layer layer : layers) {
            if (!layer.visible) continue;

            for (int ty = minTileY; ty <= maxTileY; ty++) {
                for (int tx = minTileX; tx <= maxTileX; tx++) {

                    if (ty < 0 || ty >= height || tx < 0 || tx >= width)
                        continue;

                    int gid = layer.data[ty][tx];
                    if (gid <= 0 || gid >= tiles.length) continue;

                    Tile tile = tiles[gid];
                    if (tile == null || !tile.collision || tile.solidPolygon == null)
                        continue;

                    // Build tile polygon world-space
                    Polygon tilePoly = new Polygon(
                            tile.solidPolygon.xpoints,
                            tile.solidPolygon.ypoints,
                            tile.solidPolygon.npoints
                    );
                    tilePoly.translate(
                            tx * Constant.TILESIZE,
                            ty * Constant.TILESIZE
                    );

                    Area a = new Area(bulletPoly);
                    a.intersect(new Area(tilePoly));

                    if (!a.isEmpty()) return true; // HIT
                }
            }
        }

        return false;
    }
    public Bullet checkPlayerBulletCollision(Entity player) {

        Polygon playerPoly = player.solidArea;
        if (playerPoly == null) return null;

        for (Bullet b : bullets) {
            if (!b.isActive()) continue;

            Polygon bulletPoly = b.getPolygon(); // ⚠ đổi tên nếu bạn dùng tên khác
            if (bulletPoly == null) continue;

            Area a = new Area(playerPoly);
            a.intersect(new Area(bulletPoly));

            if (!a.isEmpty()) {
                b.setActive(false);   // bullet biến mất
                return b;          // player trúng đạn
            }
        }
        return null;
    }


    public boolean checkCollision(double x, double y, Entity player) {
        Polygon playerPoly = player.solidArea;

        // Quét các tile trong vùng player
        int minTileX = (int) ((x - Constant.TILESIZE) / Constant.TILESIZE);
        int minTileY = (int) ((y - Constant.TILESIZE) / Constant.TILESIZE);
        int maxTileX = (int) ((x + Constant.TILESIZE) / Constant.TILESIZE);
        int maxTileY = (int) ((y + Constant.TILESIZE) / Constant.TILESIZE);

        for (Layer layer : layers) {
            if (!layer.visible) continue;

            for (int ty = minTileY; ty <= maxTileY; ty++) {
                for (int tx = minTileX; tx <= maxTileX; tx++) {

                    if (ty < 0 || ty >= height || tx < 0 || tx >= width)
                        continue;

                    int gid = layer.data[ty][tx];
                    if (gid <= 0 || gid >= tiles.length) continue;

                    Tile tile = tiles[gid];
                    if (tile == null || !tile.collision || tile.solidPolygon == null)
                        continue;

                    // Tạo polygon thế giới của tile
                    Polygon tilePoly = new Polygon(
                            tile.solidPolygon.xpoints,
                            tile.solidPolygon.ypoints,
                            tile.solidPolygon.npoints
                    );
                    tilePoly.translate(
                            tx * Constant.TILESIZE,
                            ty * Constant.TILESIZE
                    );

                    // Dùng Area để kiểm tra chính xác polygon vs polygon
                    Area a = new Area(playerPoly);
                    a.intersect(new Area(tilePoly));

                    if (!a.isEmpty()) {
                        return true; // CHẠM
                    }
                }
            }
        }


        return false;
    }






    private javafx.scene.shape.Polygon toFxPolygon(Polygon awtPoly) {
        javafx.scene.shape.Polygon fx = new javafx.scene.shape.Polygon();
        for (int i = 0; i < awtPoly.npoints; i++) {
            fx.getPoints().addAll(
                    (double) awtPoly.xpoints[i],
                    (double) awtPoly.ypoints[i]
            );
        }
        return fx;
    }

    public void drawCollision(GraphicsContext gc, Player player) {
        if (tiles == null || layers.isEmpty()) return;
        gc.setStroke(Color.RED);
        gc.setLineWidth(2);

        double px = player.x;
        double py = player.y;

        int playerTileX = (int) (px / Constant.TILESIZE);
        int playerTileY = (int) (py / Constant.TILESIZE);
        int halfCols = Constant.SCREEN_COL / 2;
        int halfRows = Constant.SCREEN_ROW / 2;

        for (Layer layer : layers) {
            if (!layer.visible) continue;
            for (int ty = playerTileY - halfRows; ty <= playerTileY + halfRows; ty++) {
                for (int tx = playerTileX - halfCols; tx <= playerTileX + halfCols; tx++) {
                    if (ty < 0 || ty >= layer.data.length) continue;
                    if (tx < 0 || tx >= layer.data[0].length) continue;

                    int gid = layer.data[ty][tx];
                    if (gid <= 0 || gid >= tiles.length) continue;
                    Tile tile = tiles[gid];
                    if (tile == null || !tile.collision || tile.solidPolygon == null) continue;

                    Polygon poly = tile.solidPolygon;
                    for (int i = 0; i < poly.npoints; i++) {
                        int nxt = (i + 1) % poly.npoints;
                        double worldX1 = poly.xpoints[i] + tx * Constant.TILESIZE;
                        double worldY1 = poly.ypoints[i] + ty * Constant.TILESIZE;
                        double worldX2 = poly.xpoints[nxt] + tx * Constant.TILESIZE;
                        double worldY2 = poly.ypoints[nxt] + ty * Constant.TILESIZE;

                        double screenX1 = worldX1 - px + Constant.SCREEN_WIDTH / 2.0;
                        double screenY1 = worldY1 - py + Constant.SCREEN_HEIGHT / 2.0;
                        double screenX2 = worldX2 - px + Constant.SCREEN_WIDTH / 2.0;
                        double screenY2 = worldY2 - py + Constant.SCREEN_HEIGHT / 2.0;

                        gc.strokeLine(screenX1, screenY1, screenX2, screenY2);
                    }
                }
            }
        }

        // draw player's solid area too
        //player.drawSolidArea(gc);
    }


    public void debugDrawTileCoordinates(GraphicsContext gc, Player player) {
        gc.setFill(Color.YELLOW);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.setFont(javafx.scene.text.Font.font(10));

        int playerTileX = (int) (player.x / Constant.TILESIZE);
        int playerTileY = (int) (player.y / Constant.TILESIZE);

        int halfCols = Constant.SCREEN_COL / 2;
        int halfRows = Constant.SCREEN_ROW / 2;

        for (int ty = playerTileY - halfRows; ty <= playerTileY + halfRows; ty++) {
            for (int tx = playerTileX - halfCols; tx <= playerTileX + halfCols; tx++) {

                // out of map
                if (ty < 0 || ty >= height || tx < 0 || tx >= width)
                    continue;

                double worldX = tx * Constant.TILESIZE;
                double worldY = ty * Constant.TILESIZE;

                double screenX = worldX - player.x + Constant.SCREEN_WIDTH / 2.0;
                double screenY = worldY - player.y + Constant.SCREEN_HEIGHT / 2.0;

                String label = tx + "," + ty;

                gc.strokeText(label, screenX + 4, screenY + 12);
                gc.fillText(label,   screenX + 4, screenY + 12);
            }
        }
    }



}
