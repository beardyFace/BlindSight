package partiv.theia;

import android.graphics.PointF;
import android.location.Location;
import android.util.Log;

// Position class is a representation of the user's position in two different systems (location and step counter) as a single class
public class Position {

    private Location location;
    private PointF position;
    private double angle;

	// constructor for outdoor
    Position(Location location, double angle)
    {
        this.location = location;
        this.angle = angle;
    }

	// constructor for indoor
    Position(PointF position, double angle)
    {
        this.position = position;
        this.angle = angle;
    }

	// gets the distance between two positions
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

	// gets the bearing between two positions
    public float bearingTo(boolean outDoor, Position position)
    {
        if(outDoor)
        {
            return (location.bearingTo(position.getLocation()) + 360) % 360;
        }
        else
        {
            return calculateBearing(this.position.x, position.getPosition().x, this.position.y, position.getPosition().y);
        }
    }

	// calcualte the distance between two points using the formula r^2 = x^2 + y^2
    private float calculateDistance(float x1, float x2, float y1, float y2)
    {
        return (float) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

	// calculate the bearing between two points using the arc tangent atan2(y, x) function
    private float calculateBearing(float x1, float x2, float y1, float y2)
    {
        Log.d("X1", Float.toString(x1));
        Log.d("Y1", Float.toString(y1));
        Log.d("X2", Float.toString(x2));
        Log.d("Y2", Float.toString(y2));
        return (float) ((Math.toDegrees(Math.atan2((y1 - y2), (x1 - x2)))) + 360) % 360;
    }

	// set the user's current outdoor position
    public void setPosition(Location location, double angle)
    {
        this.location = location;
        this.angle = angle;
    }

	// set the user's current indoor position
    public void setPosition(PointF position, double angle)
    {
        this.position = position;
        this.angle = angle;
    }

	// get the user's gps location
    public Location getLocation()
    {
        return this.location;
    }

	// get the user's position in x,y coordinate
    public PointF getPosition()
    {
        return this.position;
    }

	// get the user's azimuth angle
    public double getAngle()
    {
        return this.angle;
    }
}
