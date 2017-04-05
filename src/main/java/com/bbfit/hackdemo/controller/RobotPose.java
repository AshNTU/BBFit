package com.bbfit.hackdemo.controller;

/**
 * Created by Yusen.QIN on 2017/2/6.
 */

public class RobotPose {
        double x;
        double y;
        double orientation;

    void update(double updateX, double updateY,double updateOrientation){
        x = updateX;
        y = updateY;
        orientation = updateOrientation;
    }
}
