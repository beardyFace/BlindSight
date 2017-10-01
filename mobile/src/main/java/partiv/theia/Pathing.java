package partiv.theia;

// Pathing class uses the tracked points and iterate through them
public class Pathing {
    private Position target;
    private Position current;
    private Tracking tracking;
    private int index;

    Pathing(Tracking tracking, Position current) {
        this.current = current;
        this.tracking = tracking;
        this.index = tracking.getSize() - 1;
        this.target = tracking.getPosition(index);
    }

	// get the current position
    public Position getCurrent()
    {
        return this.current;
    }

	// set the current position
    public void setCurrent(Position current)
    {
        this.current = current;
    }

	// move to the next target on the tracks
    public boolean next() {
        if(--index == -1)
        {
            return false;
        }
        target = tracking.getPosition(index);
        //tracking.removePosition(index + 1);
        return true;
    }

	// decrement index
    public void decrement()
    {
        this.index--;
    }

	// get the current target
    public Position getTarget()
    {
        return this.target;
    }



}
