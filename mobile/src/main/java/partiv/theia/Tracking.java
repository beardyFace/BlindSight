package partiv.theia;

import java.util.ArrayList;

public class Tracking {

    private int current_step;
    private double azimuth;
    private Sensors sensors;
    private ArrayList<Position> tracks = new ArrayList<>();

    Tracking(Sensors sensors)
    {
        this.sensors = sensors;
    }

    public void addPosition(Position p)
    {
        tracks.add(p);
    }

    public int getSize()
    {
        return tracks.size();
    }

    public Position getPosition(int index)
    {
        return tracks.get(index);
    }




}
