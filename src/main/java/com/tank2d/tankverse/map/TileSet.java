package com.tank2d.tankverse.map;

import javafx.scene.image.Image;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class TileSet {
    public int firstgid;
    public String source;
    public Image image;
    public int tileWidth, tileHeight, columns, tileCount;
    // collision polygons keyed by localId (0..tileCount-1)
    public Map<Integer, Polygon> collisionByLocalId = new HashMap<>();
}
