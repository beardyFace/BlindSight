package com.example.henry.gpstest;

/**
 * Created by Henry on 14-Jul-16.
 */
public class SensorData {

    private double ax;
    private double ay;
    private double az;

    public SensorData(double ax, double ay, double az){
        this.ax = ax;
        this.ay = ay;
        this.az = az;
    }

    public void update(double ax, double ay, double az){
        this.ax = ax;
        this.ay = ay;
        this.az = az;
    }

    public double getAx(){
        return ax;
    }

    public double getAy(){
        return ay;
    }

    public double getAz(){
        return az;
    }

    public String toString(){
        return "X: "+ Helper.round(ax, 2) +"\nY: "+ Helper.round(ay, 2) +"\nZ: "+ Helper.round(az, 2);
    }
}
