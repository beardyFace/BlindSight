package partiv.theia;

public class Position {

    private double angle;
    private double x;
    private double y;

    Position(double x, double y, double angle)
    {
        this.x = x;
        this.y = y;
        this.angle = angle;
    }

    public void setPosition(double x, double y, double angle)
    {
        this.x = x;
        this.y = y;
        this.angle = angle;
    }

    public double getX()
    {
        return this.x;
    }

    public double getY()
    {
        return this.y;
    }

    public double getAngle()
    {
        return this.angle;
    }
}
