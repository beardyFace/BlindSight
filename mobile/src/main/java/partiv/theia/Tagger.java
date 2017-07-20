package partiv.theia;

import android.location.Location;
import android.util.Log;

public class Tagger{
    public static final int TAG_SAMPLE_SIZE = 5;
    private Location tag_location;
    private boolean status = false;

    Tagger()
    {
    }

    public Location getLocation() {
        return tag_location;
    }

    public void setLocation(Location location)
    {
            this.tag_location = location;
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
