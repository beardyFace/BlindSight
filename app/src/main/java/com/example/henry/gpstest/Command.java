package com.example.henry.gpstest;

/**
 * Created by Henry on 14-Jul-16.
 */
public enum Command {
    EMPTY,
    DISTANCE,
    ANGLE,
    TAG;

    public static Command getCommand(SensorData orientation){
        double pitch = orientation.getAy();
        double roll = orientation.getAz();

        if(Math.abs(pitch) < 10 && Math.abs(roll) < 5)
            return ANGLE;
        else if(pitch > 70 && pitch < 90)
            return DISTANCE;
        else if(pitch < -70 && pitch > -90)
            return TAG;

        return EMPTY;
    }
}
