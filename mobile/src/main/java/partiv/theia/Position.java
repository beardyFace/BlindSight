package partiv.theia;

import android.location.Location;

public class Position {

    private Location location;
    private double angle;

    Position(Location location, double angle)
    {
        this.location = location;
        this.angle = angle;
    }

    public void setPosition(Location location, double angle)
    {
        this.location = location;
        this.angle = angle;
    }

    public Location getLocation()
    {
        return this.location;
    }


    public double getAngle()
    {
        return this.angle;
    }
}
