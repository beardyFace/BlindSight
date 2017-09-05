package partiv.theia;


import android.location.Location;

public class KalmanFilter {
    private final float MinAccuracy = 1;

    private float Q_metres_per_second;

    private String provider;
    private long timeStamp_milliseconds;
    private double lat;
    private double lng;
    private float variance; // P matrix.  Negative means object uninitialised.  NB: units irrelevant, as long as same units used throughout

    public KalmanFilter(float Q_metres_per_second) {
        this.Q_metres_per_second = Q_metres_per_second;
        variance = -1;
    }

    public long get_TimeStamp() {
        return timeStamp_milliseconds;
    }

    public double get_lat() {
        return lat;
    }

    public double get_lng() {
        return lng;
    }

    public float get_accuracy() {
        return (float)Math.sqrt(variance);
    }

    public void setState(double lat, double lng, float accuracy, long TimeStamp_milliseconds) {
        this.lat=lat;
        this.lng=lng;
        this.variance = accuracy * accuracy;
        this.timeStamp_milliseconds =TimeStamp_milliseconds;
    }

    public Location returnLocation(){
        Location location = new Location(this.provider);
        location.setLatitude(get_lat());
        location.setLongitude(get_lng());
        location.setAccuracy(get_accuracy());
        return location;
    }

    public void process(Location location){
        double lat_measurement = location.getLatitude();
        double lng_measurement = location.getLongitude();
        float accuracy = location.getAccuracy();
        long timeStamp_milliseconds = location.getTime();
        this.provider = location.getProvider();

        if (accuracy < MinAccuracy) accuracy = MinAccuracy;
        if (variance < 0) {
            // if variance < 0, object is unitialised, so initialise with current values
            setState(lat_measurement, lng_measurement, accuracy, timeStamp_milliseconds);
        } else {
            // else apply Kalman filter methodology

            long timeInc_milliseconds = timeStamp_milliseconds - this.timeStamp_milliseconds;
            if (timeInc_milliseconds > 0) {
                // time has moved on, so the uncertainty in the current position increases
                variance += timeInc_milliseconds * Q_metres_per_second * Q_metres_per_second / 1000;
                this.timeStamp_milliseconds = timeStamp_milliseconds;
                // TO DO: USE VELOCITY INFORMATION HERE TO GET A BETTER ESTIMATE OF CURRENT POSITION
            }

            // Kalman gain matrix K = Covarariance * Inverse(Covariance + MeasurementVariance)
            // NB: because K is dimensionless, it doesn't matter that variance has different units to lat and lng
            float K = variance / (variance + accuracy * accuracy);
            // apply K
            lat += K * (lat_measurement - lat);
            lng += K * (lng_measurement - lng);
            // new Covarariance  matrix is (IdentityMatrix - K) * Covarariance
            variance = (1 - K) * variance;
        }
    }
}
