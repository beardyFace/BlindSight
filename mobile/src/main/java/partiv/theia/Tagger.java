package partiv.theia;

// This class stores the user's tagged position and their tag's status
public class Tagger{
    public static final int TAG_SAMPLE_SIZE = 5;
    private Position tag_location;
    private boolean status = false;

    Tagger()
    {
    }

    public Position getPosition() {
        return tag_location;
    }

    public void setPosition(Position position)
    {
            this.tag_location = position;
            this.status = true;
    }

    public boolean status()
    {
        return this.status;
    }

    public void setStatus(boolean status)
    {
        this.status = status;
    }


}
