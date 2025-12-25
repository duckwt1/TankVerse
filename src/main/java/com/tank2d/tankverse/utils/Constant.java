package com.tank2d.tankverse.utils;

public class Constant {
    public static final int TILESIZE = 32;
    public static final int CHAR_SCALE = 1;
    public static final int SCALE = 3;
    public static final int SCREEN_COL = 32;
    public static final int SCREEN_ROW = 24;
    public static final int SCREEN_WIDTH = TILESIZE * SCREEN_COL;
    public static final int SCREEN_HEIGHT = TILESIZE * SCREEN_ROW;
    public static final int FPS = 60;

    public static final int PLAYER_HEIGHT = 48;
    public static final int PLAYER_WIDTH = 32;
    public static final int PLAYER_TILE_SIZE = 64;

    public static final int ACTION_NONE = 0;
    public static final int ACTION_SHOOT = 1;
    public static final int ACTION_CHARGE = 2;
    public static final int ACTION_ABC = 3;

    // Server Configuration (Playit.gg - TCP + UDP)
//    public static final String DEFAULT_SERVER_HOST = "per-mg.gl.at.ply.gg";
//    //public static final String DEFAULT_SERVER_HOST = "restaurants-expenditures.gl.at.ply.gg";
//
//    public static final int DEFAULT_SERVER_PORT = 41181;
    //public static final int DEFAULT_SERVER_PORT = 11640;


    public static final String LAN_SERVER_HOST = "26.0.26.202"; // ĐỔI THEO MÁY BẠN
    public static final int LAN_SERVER_PORT = 11640;


    public static final String DEFAULT_RELAY_HOST = "try-crystal.gl.at.ply.gg";
    public static final int DEFAULT_RELAY_PORT = 65455;


    public static final int SHOOT = 1;
    public static final int SPEEDUP = 2;
    public static final int EXPLODE = 3;

}
