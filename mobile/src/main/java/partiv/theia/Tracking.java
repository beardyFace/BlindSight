package partiv.theia;

import java.util.ArrayList;

// This class tracks the user's position by adding their past position to a list
public class Tracking {

    private int current_step;
    private double azimuth;
    private Sensors sensors;
	// the user's past positions
    private ArrayList<Position> tracks = new ArrayList<>();

    Tracking(Sensors sensors)
    {
        this.sensors = sensors;
    }

	// add position to the list
    public void addPosition(Position p)
    {
        tracks.add(p);
    }

	// get the size of the list
    public int getSize()
    {
        return tracks.size();
    }

	// get a tracked position at a particular index
    public Position getPosition(int index)
    {
        return tracks.get(index);
    }

	// remove a tracked position from the list
    public void removePosition(int index)
    {
        tracks.remove(index);
    }

	// check for over steping (corner cutting)
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

	// correct the list of positions by removing tracks that are behind the user current position
    private void overStepCorrection(int x)
    {
        for(int i = x; i < tracks.size(); i++)
        {
            tracks.remove(i);
        }
    }

    public ArrayList<Position> getTrack(){if (tracks != null) {return tracks;} else{return null;}};

}
