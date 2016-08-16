package com.example.henry.gpstest;

/**
 * Created by Henry on 05-Aug-16.
 */
public abstract class Helper {

    public static double round(double i, int v){
        return (Math.round(i/v) * v);
    }
}
