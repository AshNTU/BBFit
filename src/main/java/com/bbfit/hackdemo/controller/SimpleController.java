package com.bbfit.hackdemo.controller;

import android.util.Log;

import com.segway.robot.algo.Pose2D;
import com.segway.robot.sdk.locomotion.sbv.Base;

/**
 * This is a simple example on how to make the robot move correctly
 */
public class SimpleController{
    private static final String TAG = "SimpleController";
    private double[] referenceDistance;
    private double[] referenceVelocity;
    private RobotPose startRobotPose;
    RobotPose curRobotPose;
    private RobotPose targetRobotPose;
    private ControlThread processThread;
    private int referenceGroupSize;
    double driveV;
    double driveW;
    private double arriveDistance,arriveOrientation;
    private double controllerFrequency;
    private simplePID_controller angleVelocityController;

    private DistanceAndOrientation toTargetDistanceAndOrientation,toStartDistanceAndOrientation;
    private double distanceFromeStartToEnd;
    private StateListener mStateListener;
    private Base mBase;

    public int trackStatus=1;

    class DistanceAndOrientation{
        double d;
        double a;
    }

    public SimpleController(SimpleController.StateListener finishListener, Base base){
        mStateListener = finishListener;
        mBase = base;
        controllerFrequency = 0.1;//10 Hz
        referenceGroupSize = 5;

        referenceDistance = new double[referenceGroupSize];
        referenceDistance[0] = 5.0;
        referenceDistance[1] = 3.0;
        referenceDistance[2] = 2.0;
        referenceDistance[3] = 1.0;
        referenceDistance[4] = 0.5;

        referenceVelocity = new double[referenceGroupSize];
        referenceVelocity[0] = 0.6;
        referenceVelocity[1] = 0.4;
        referenceVelocity[2] = 0.3;
        referenceVelocity[3] = 0.2;
        referenceVelocity[4] = 0.15;

        //accuricy offset should be tune this value
        arriveDistance = 0.8;
        //accuricy offset should be tune this value, here is rads, which means 0.05 rad = 2.866 degree
        arriveOrientation = 0.1;
        startRobotPose = new RobotPose();
        curRobotPose = new RobotPose();
        targetRobotPose = new RobotPose();

        updateCurrentRobotPose();
        setStartRobotPose();
        angleVelocityController = new simplePID_controller();
        angleVelocityController.init();
        toTargetDistanceAndOrientation = new DistanceAndOrientation();
        toStartDistanceAndOrientation = new DistanceAndOrientation();
    }

    Base getBase() {
        return mBase;
    }

    void finish() {
        if (mStateListener != null) {
            mStateListener.onFinish();
        }
    }

    public void updatePoseAndDistance(){
        Log.d(TAG, "updatePoseAndDistance");
        updateCurrentRobotPose();
        updateStartRobotPose();
        updateCurrentDistance();
    }

    public void updateandFollow(){
        Log.d(TAG, "updatePoseAndDistance");
        updateCurrentRobotPose();
        updateStartRobotPose();
        trackStatus = processTargetRobotPose();
        updateCurrentDistance();
    }

    public void startProcess(){
        if (processThread == null){
            processThread = new ControlThread(this);
            processThread.start();
            Log.d(TAG, "Process started");
        } else {
            Log.d(TAG, "Process null");
        }
    }

    public void updateCurrentDistance(){
        DistanceAndOrientation startToTarget = calculateDistanceAndOrientationBetweenPoses(startRobotPose,targetRobotPose);
        distanceFromeStartToEnd = startToTarget.d;
        Log.d(TAG, "Target DISTANCE " + distanceFromeStartToEnd);

    }

    public void setControllerFrequency(double frequency){
        controllerFrequency = frequency;
    }

    public double getControllerFrequency() {
        return controllerFrequency;
    }

    public void updateCurrentRobotPose(){
        Pose2D curPose = mBase.getOdometryPose(-1);
        curRobotPose.update(curPose.getX(), curPose.getY(), curPose.getTheta());
    }

    public void updateStartRobotPose(){
        startRobotPose = curRobotPose;
    }

    public void setTargetRobotPose(double targetX, double targetY, double targetOrientation){
        targetRobotPose.update(targetX, targetY, targetOrientation);
    }

    public void resetTargetRobotPose(){
        Pose2D curPose = mBase.getOdometryPose(-1);
        targetRobotPose.update(curPose.getX(), curPose.getY(), curPose.getTheta());
    }

    public void setStartRobotPose(){
        Pose2D curPose = mBase.getOdometryPose(-1);
        startRobotPose.update(curPose.getX(), curPose.getY(), curPose.getTheta());
    }

    int processTargetRobotPose(){
        updateCurrentRobotPose();
        updateDistanceAndOrientationToTargetPose();
        updateDistanceAndOrientationToStartPose();
        if(arriveOrOverTargetPose() == 0 ){
            pickUpSpeed(toTargetDistanceAndOrientation.d);
            pidAngleSpeed(toTargetDistanceAndOrientation.a - curRobotPose.orientation);
            return 2;
        } else {
            driveV = 0.0;
            if(arriveTargetOrientationOrentation() ==0){
                pidAngleSpeed(targetRobotPose.orientation - curRobotPose.orientation);
                return 1;
            } else {
                driveW = 0.0;
                return 0;
            }
        }
    }

    int arriveOrOverTargetPose(){
        if(toTargetDistanceAndOrientation.d < arriveDistance){
            Log.d(TAG, " arrive Target  " + toTargetDistanceAndOrientation.d);
            return 1;//arrive the target
        }
        if(toStartDistanceAndOrientation.d > distanceFromeStartToEnd){
            double lengthOver;
            lengthOver = toStartDistanceAndOrientation.d - distanceFromeStartToEnd;
            Log.d(TAG, " over Target  " + lengthOver);
            return 2; //running too fast , and incase of over the target.
        }
        return 0;
    }

    int arriveTargetOrientationOrentation() {
        if((targetRobotPose.orientation - curRobotPose.orientation) < arriveOrientation){
            return 1;//arrived
        } else {
            return 0; // not
        }
    }


    DistanceAndOrientation calculateDistanceAndOrientationBetweenPoses(RobotPose p1, RobotPose p2){
        double dlt_x, dlt_y;
        dlt_x = p2.x - p1.x;
        dlt_y = p2.y - p1.y;
        DistanceAndOrientation distanceAndOrientation = new DistanceAndOrientation();
        distanceAndOrientation.d  = Math.sqrt(dlt_x * dlt_x + dlt_y * dlt_y);
        distanceAndOrientation.a = Math.atan(dlt_y/dlt_x);
        return distanceAndOrientation;
    }

    void updateDistanceAndOrientationToTargetPose(){
        toTargetDistanceAndOrientation = calculateDistanceAndOrientationBetweenPoses(curRobotPose,targetRobotPose);
        Log.d(TAG, "ToTarget info " + " D is " + toTargetDistanceAndOrientation.d + "  A is " + toTargetDistanceAndOrientation.a);
    }

    void updateDistanceAndOrientationToStartPose(){
        toStartDistanceAndOrientation = calculateDistanceAndOrientationBetweenPoses(startRobotPose, curRobotPose);
    }

    void pickUpSpeed(double d){
        for (int i =0;  i < referenceGroupSize; i++){
            if (d > referenceDistance[i]){
                driveV = referenceVelocity[i];
                break;
            }
        }
    }

    void pidAngleSpeed(double a){
        driveW = angleVelocityController.generateOutput(a);
    }

    void stopDuringProcesssTargetRobotPose(){
        mBase.stop();
        processThread.stopRun();
    }

    class simplePID_controller{  // here is a simple sample for just using a PID controller, here is P only
        double kp,kd;
        double ki, integrator;
        double error;
        double lastError;
        double outputThreshold;
        double output;

        void init(){
            //safe value for turning (Angle velocity), plz not change them without a good understand of cybernetics
            kp = 0.5;
            ki = 0.0;
            kd = 0.0;//P only
            outputThreshold = 0.6;  //max is 0.6 for turning
            resetController();
        }

        void resetController(){
            integrator  = 0.0;
        }

        void limitIntegration(){
            if (integrator > 0.2){
                integrator = 0.2;
            }
            if (integrator < -0.2){
                integrator = -0.2;
            }
        }

        void limitOutput(){
            if(output > outputThreshold){
                output = outputThreshold;
            }
            if(output < -outputThreshold){
                output = -outputThreshold;
            }
        }

        double generateOutput(double err){
            error = err / 3.1415926;

            output = kp * error;
            integrator += error;
            limitIntegration();

            double deltaError = error - lastError;
            output += kd * deltaError / controllerFrequency;

            output += ki * integrator / controllerFrequency;
            limitOutput();

            lastError = error;
            Log.d(TAG, "Angle PID" + " output is " + output + "  err is " + error);
            return output;
        }

    }

    public interface StateListener {
        void onFinish();
    }
}
