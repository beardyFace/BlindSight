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

    public void removePosition(int index)
    {
        tracks.remove(index);
    }

    public int check(boolean outDoor, Position position)
    {
        for(int i = 0; i < tracks.size() - 1; i++)
        {
            if(tracks.get(i).distanceTo(outDoor ,position) >= 4)
            {
                overStepCorrection(i);
                return i;
            }
        }
        return -1;
    }

    private void overStepCorrection(int x)
    {
        for(int i = x; i < tracks.size(); i++)
        {
            tracks.remove(i);
        }
    }

    public ArrayList<Position> getTrack(){if (tracks != null) {return tracks;} else{return null;}};

}
