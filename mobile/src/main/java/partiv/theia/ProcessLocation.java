package partiv.theia;

import android.location.Location;

import java.util.ArrayList;

// This class averages the locations that are received by the gps services
public class ProcessLocation {

    private ArrayList<Double> lats = new ArrayList<Double>();
    private ArrayList<Double> longs = new ArrayList<Double>();
    private String provider;
    private float accuracy;

    public void addLocation(Location location)
    {
        lats.add(location.getLatitude());
        longs.add(location.getLongitude());
        this.provider = location.getProvider();
        this.accuracy = location.getAccuracy();
    }

    public Location average()
    {
        double final_lat = 0;
        double final_long = 0;
        for(double lat : lats)
        {
            final_lat += lat;
        }
        final_lat = final_lat / lats.size();
        for(double longd : longs)
        {
            final_long += longd;
        }
        final_long = final_long / longs.size();

        Location location = new Location(this.provider);

        location.setLatitude(final_lat);
        location.setLongitude(final_long);
        location.setAccuracy(this.accuracy);
        return location;
    }

    public void clear()
    {
        lats.clear();
        longs.clear();
    }
}
