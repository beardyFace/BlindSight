package partiv.theia;

import android.location.Location;
import android.util.Log;

public class Tagger{

    private Location tag_location;

    Tagger() {
        Log.d("tagger run", "tagger ran");
    }

    public Location getLocation() {
        return tag_location;
    }

    public void setLocation(Location location) {
        this.tag_location = location;
    }

    public float tag() {
        float currentLocation;
        currentLocation = 2;
        return currentLocation;
    }

}
