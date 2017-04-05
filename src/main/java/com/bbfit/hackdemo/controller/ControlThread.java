package com.bbfit.hackdemo.controller;

import android.util.Log;

/**
 * This is a simple example on how to move the robot correctly
 */
class ControlThread extends Thread {
    private static final String TAG = "ControlThread";
    private SimpleController mSimpleController;

    public ControlThread(SimpleController simpleController) {
        mSimpleController = simpleController;
    }

    private boolean mStop = false;

    public void stopRun() {
        mSimpleController.stopDuringProcesssTargetRobotPose();
        mStop = true;
    }

    @Override
    public void run() {
        super.run();
        while (!mStop) {
            Log.d(TAG, "sub thread running"
                    + " V is " + mSimpleController.driveV
                    + "  W is " + mSimpleController.driveW);

            Log.d(TAG, "Cur Pose is "
                    + " X is " + mSimpleController.curRobotPose.x
                    + " Y is " + mSimpleController.curRobotPose.y
                    + " O is " + mSimpleController.curRobotPose.orientation);
            if (mSimpleController.processTargetRobotPose() == 0){
                Log.d(TAG, "subthread finished");
                mStop = true;
            }
            mSimpleController.getBase().setLinearVelocity((float)mSimpleController.driveV);
            mSimpleController.getBase().setAngularVelocity((float)mSimpleController.driveW);
            try {
                sleep((long) (1 / mSimpleController.getControllerFrequency()));
            } catch (InterruptedException ignored) {
            }
        }
        mSimpleController.finish();
    }
}