package com.example.henry.gpstest;

/**
 * Created by Henry on 06-Jul-16.
 */
public class Coordinate
{
    public final double X;
    public final double Y;

    public Coordinate(double lat, double lon){
        this.X = 0;
        this.Y = 0;
    }

    public double distanceFromCoord(Coordinate other){
        double xx = X * other.X;
        double yy = Y * other.Y;
        return Math.sqrt(xx + yy);
    }
}
