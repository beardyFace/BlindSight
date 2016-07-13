package com.example.henry.gpstest;

/**
 * Created by Henry on 14-Jul-16.
 */
public class AccelerometerData {

    private double ax;
    private double ay;
    private double az;

    public AccelerometerData(double ax, double ay, double az){
        this.ax = ax;
        this.ay = ay;
        this.az = az;
    }

    public void update(double ax, double ay, double az){
        this.ax = ax;
        this.ay = ay;
        this.az = az;
    }
    //TODO
    //Add methods to judge wrist command states
    public String toString(){
        return "X: "+ ax +"\nY: "+ ay +"\nZ: "+ az;
    }
}
