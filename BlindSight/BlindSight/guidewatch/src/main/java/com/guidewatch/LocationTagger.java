package com.guidewatch;

import android.location.Location;

/**
 * Created by Henry on 17-Sep-16.
 */
public class LocationTagger {

    private final float ACCURACY_DECAYS_TIME = 0.1f;

    private final int SAMPLES_REQUIRED = 5;
    private int samples = 0;

    private boolean tagging = false;

    private Location tagged_location;

    private KalmanFilter tagLocationKalman = new KalmanFilter(ACCURACY_DECAYS_TIME);

    public LocationTagger(){

    }

    public Location getTagLocation() {
        return tagged_location;
    }

    public void tagLocation(){
        tagging = true;
    }

    public boolean tagging(){
        return tagging;
    }

    public void setTagLocation(Location location){
        if(tagged_location != null)
            resetTagLocation();

        tagLocationKalman.process(location);

        if(samples++ >= SAMPLES_REQUIRED) {
            location.setLatitude(tagLocationKalman.get_lat());
            location.setLongitude(tagLocationKalman.get_lng());
            location.setAccuracy(tagLocationKalman.get_accuracy());
            location.setTime(tagLocationKalman.get_TimeStamp());
            tagged_location = new Location(location);
            tagging = false;
        }
    }

    private void resetTagLocation(){
        samples = 1;
        tagLocationKalman = new KalmanFilter(ACCURACY_DECAYS_TIME);
        tagged_location = null;
    }
}
