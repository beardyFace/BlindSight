package partiv.theia;

import android.graphics.PointF;
import android.location.Location;

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
