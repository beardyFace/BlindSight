package partiv.theia;

import android.graphics.PointF;
import android.location.Location;
import android.util.Log;

public class Position {

    private Location location;
    private PointF position;
    private double angle;

    Position(Location location, double angle)
    {
        this.location = location;
        this.angle = angle;
    }

    Position(PointF position, double angle)
    {
        this.position = position;
        this.angle = angle;
    }

    public float distanceTo(boolean outDoor, Position position)
    {
        if(outDoor)
        {
            return location.distanceTo(position.getLocation());
        }
        else
        {
            return calculateDistance(this.position.x, position.getPosition().x, this.position.y, position.getPosition().y);
        }
    }

    public float bearingTo(boolean outDoor, Position position)
    {
        if(outDoor)
        {
            return location.bearingTo(position.getLocation());
        }
        else
        {
            return calculateBearing(this.position.x, position.getPosition().x, this.position.y, position.getPosition().y);
        }
    }

    private float calculateDistance(float x1, float x2, float y1, float y2)
    {
        return (float) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    private float calculateBearing(float x1, float x2, float y1, float y2)
    {
        Log.d("X1", Float.toString(x1));
        Log.d("Y1", Float.toString(y1));
        Log.d("X2", Float.toString(x2));
        Log.d("Y2", Float.toString(y2));
        return (float) ((Math.toDegrees(Math.atan2((y1 - y2), (x1 - x2)))) + 360 + 90) % 360;
    }

    public void setPosition(Location location, double angle)
    {
        this.location = location;
        this.angle = angle;
    }

    public void setPosition(PointF position, double angle)
    {
        this.position = position;
        this.angle = angle;
    }

    public Location getLocation()
    {
        return this.location;
    }

    public PointF getPosition()
    {
        return this.position;
    }


    public double getAngle()
    {
        return this.angle;
    }
}
