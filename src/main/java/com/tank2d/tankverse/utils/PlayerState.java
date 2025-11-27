package com.tank2d.tankverse.utils;

import java.io.Serializable;

public class PlayerState implements Serializable {
    public String userName;
    public double x;
    public double y;
    public double bodyAngle;
    public double gunAngle;
    public boolean up;
    public boolean down;
    public boolean left;
    public boolean right;
    public boolean backward;
    //public boolean isMoving;
    public int hp = 0;
    public int bullet = 3;
    public int action = 0;

    public PlayerState(String userName,
                       double x,
                       double y,
                       double bodyAngle,
                       double gunAngle,
                       boolean up,
                       boolean down,
                       boolean left,
                       boolean right,
                       boolean backward,
                       int hp,
                       int bullet,
                       int action) {

        this.userName = userName;
        this.x = x;
        this.y = y;
        this.bodyAngle = bodyAngle;
        this.gunAngle = gunAngle;

        this.up = up;
        this.down = down;
        this.left = left;
        this.right = right;
        this.backward = backward;

        this.hp = hp;
        this.bullet = bullet;
        this.action = action;
    }


    public PlayerState() {}
}
