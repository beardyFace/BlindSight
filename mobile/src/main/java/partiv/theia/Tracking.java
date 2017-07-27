package partiv.theia;

import java.util.ArrayList;

public class Tracking {

    private int current_step;
    private double azimuth;
    private Sensors sensors;
    private ArrayList<Position> tracks = new ArrayList<Position>();

    Tracking(Sensors sensors)
    {
        this.sensors = sensors;
    }

    public void addPosition(Position p)
    {
        tracks.add(p);
    }



}
