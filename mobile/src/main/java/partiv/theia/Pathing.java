package partiv.theia;

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

    public Position getCurrent()
    {
        return this.current;
    }

    public void setCurrent(Position current)
    {
        this.current = current;
    }

    public boolean next() {
        if(--index == -1)
        {
            return false;
        }
        target = tracking.getPosition(index);
        return true;
    }

    public Position getTarget()
    {
        return this.target;
    }



}
