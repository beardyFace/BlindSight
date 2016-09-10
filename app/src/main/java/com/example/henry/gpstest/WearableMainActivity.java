package com.example.henry.gpstest;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.app.Activity;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import android.os.Vibrator;

import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.wearable.Wearable;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.Collection;

public class WearableMainActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        SensorEventListener,
        BeaconConsumer {

    //App operation

    //hold watch facing up and flat to check angle towards goal
        //will vibrate when facing less than 10 degrees from goal

    //rotate wrist forwards to get distance
        //if > 10m away
            //vibrate once per 10m away
        //else if < 10m vibrate "harder" as get closer
            //pulse three times in quick succession to indicate close
            //vibrate once per 1m away


    //Android device control variables
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;
    private GoogleApiClient mGoogleApiClient;
    private BeaconManager beaconManager;
    private Vibrator vibrator;
//    private PowerManager powerManager;
    private SensorManager mSensorManager;
    //////////////////////

    //Program variables
    public static final int ACCURACY_DECAYS_TIME = 5; // Metres per second
    private KalmanFilter kalmanFilter = new KalmanFilter(ACCURACY_DECAYS_TIME);

    private Command current_command = Command.EMPTY;

    //Volatile data variables
    private volatile Location current_location;
    private volatile Location tagged_location;

//    private volatile SensorData current_acceleration = new SensorData(0, 0, 0);
//    private volatile SensorData current_compass = new SensorData(0, 0, 0);
    private volatile SensorData orientation = new SensorData(0, 0, 0);
    private volatile Beacon distance_beacon;
    //////////////////////

    //UI display
    private EditText text;
    private Button tag_button;
    private DrawView drawView;

//    private final Handler handler = new Handler();
    private final Runnable command_runner = new Runnable() {
        @Override
        public void run() {
            while(true) {
                updateFeedback();
                waitMS(100);
            }
        }
    };

    private Thread command_thread = new Thread(command_runner);
    private void scheduleMain(){
        command_thread.start();
    }
//    private volatile boolean isRunning = false;
//    private final Timer timer = new Timer();
//    private final TimerTask task = new TimerTask() {
//        @Override
//        public void run() {
//            if(!isRunning) {
//                isRunning = true;
//                handler.post(command_runner);
//                isRunning = false;
//            }
//        }
//    };

    private final long delay = 0;//no delay
    private final long period = 10;//every period ms
    //////////////////////

    private double value = 0;
    private void updateFeedback(){
        current_command = Command.getCommand(orientation);
        switch(current_command){
            case DISTANCE:
                value = indicateDistance();
                break;
            case ANGLE:
                value = indicateAngle();
                break;
            case TAG:
                current_command = setTagLocation();
            default:
                break;
        }
        displayToScreen(current_command, value);
    }

    private double indicateAngle(){
//        orientation;
        if(tagged_location != null && current_location != null && orientation != null){
            double azimuth = orientation.getAx();
            double bearingTo = current_location.bearingTo(tagged_location);

            double direction = azimuth - bearingTo;

            double threshold = 15;
            double distanceTo = current_location.distanceTo(tagged_location);
            if(distanceTo < 30)
                threshold = 40;

            double difference = Math.abs(azimuth - bearingTo);
//            display("Difference: "+difference);//debug

            if(difference < threshold)
                vibrator.vibrate(1000);
            else
                stopVibrate();
            return difference;
        }
        return 0;
    }

    private volatile boolean updateOrientation = false;
    private Command setTagLocation(){
        vibrator.vibrate(1000);
        waitMS(5000);
        updateOrientation = false;
        Command command = Command.EMPTY;
        while(!updateOrientation){
            command = Command.getCommand(orientation);
        }
        int pulses = 1;
        int time = 500;
        if(command == Command.TAG) {
            tagLocation();
            pulses = 3;
        }
        long[] pattern = getPattern(pulses, time);
        vibrator.vibrate(pattern, -1);
        waitMS(pulses * 2 * time + 2000);
        while(tag_location) {

        }
        vibrator.vibrate(pattern, -1);
        waitMS(pulses * 2 * time + 2000);
        return command;
    }

    private double indicateDistance(){
        double distanceTo = 0;
//        current_location;
        if(tagged_location != null && current_location != null) {
            distanceTo = current_location.distanceTo(tagged_location);
            double nearestTen = round(distanceTo, 10);
            int pulses = (int)(nearestTen/10);
            if(pulses == 0)
                pulses = 1;

            int time = 500;
            long[] pattern = getPattern(pulses, time);
            vibrator.vibrate(pattern, -1);
            long expectedElapsedTime = pulses * 2 * time + 2000;
            waitMS(expectedElapsedTime);

            stopVibrate();
        }
        return distanceTo;
    }

    private long[] getPattern(int pulses, int length){
        long[] pattern = new long[pulses * 2];
        for(int i = 0; i < pulses * 2; i++)
            pattern[i] = length;
        return pattern;
    }

    private void waitMS(long time){
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void displayToScreen(Command command, double value){
        String text = "Commands: "+command+" "+value+"\n";
               text +=orientation.toString()+"\n";
//        String text = "GPS: "+hasGps()+"\n";
//               text += "API: "+hasConnectedWearableAPI()+"\n";
               text += locationToString(current_location)+"\n";

            if(distance_beacon != null)
                text += distance_beacon.getRssi();
//        if(tagged_location != null) {
//            text += locationToString(tagged_location) + "\n";
//            text += "Distance: "+current_location.distanceTo(tagged_location)+"\n"+current_distance+"\n";
//        }
//        text += current_acceleration.toString();
//        text += current_compass.toString();
        display(text);
    }

    private String locationToString(Location location){
        if(location == null)
            return "No location set yet";
        return "Lat: "+location.getLatitude()+"\nLng: "+location.getLongitude()+"\nAcc: "+location.getAccuracy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wearable_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

//        drawView = new DrawView(this);
//        drawView.setBackgroundColor(Color.WHITE);
//        setContentView(drawView);

        tag_button = (Button) findViewById(R.id.geoButton);
        tag_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tagLocation();
            }
        });

        text = (EditText) findViewById(R.id.editText);
        text.setText("Connecting to GPS");

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
//                .addApi(AppIndex.API)
//                .addApi(Wearable.API)  // used for data layer API
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        beaconManager = BeaconManager.getInstanceForApplication(this);

        beaconManager.getBeaconParsers()
                .add(new BeaconParser().setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
                                                      //"m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"

        beaconManager.setForegroundScanPeriod(1000);
        beaconManager.setBackgroundScanPeriod(1000);

//        ComponentName myService = startService(new Intent(this, WearableMainActivity.class));
//        bindService(new Intent(this, WearableMainActivity.class), myServiceConn, BIND_AUTO_CREATE);

        beaconManager.bind(this);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        //start sensor manager and set accelerometer listener
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_NORMAL);
        //determine what sensors are on testing device, aka my cheap phone, for proof of concept
//        List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
//        for(Sensor sensor : deviceSensors)
//            display(sensor.toString());

//        current_location = new Location("");
//        current_location.setLatitude(-41.287502);
//        current_location.setLongitude(174.760777);

//        tagged_location = new Location("");
        //122
//        tagged_location.setLatitude(-41.287312);
//        tagged_location.setLongitude(174.760955);
        //121 Upland Road
//        tagged_location.setLatitude(-41.287644);
//        tagged_location.setLongitude(174.760831);
        //four square
//        tagged_location.setLatitude(-41.289011);
//        tagged_location.setLongitude(174.761843);

        current_location = new Location("");
        current_location.setLatitude(-36.876400);
        current_location.setLongitude(174.772313);

        tagged_location = new Location("");
        tagged_location.setLatitude(-36.876275);
        tagged_location.setLongitude(174.772201);

        scheduleMain();
    }

    public void settingsRequest() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true); //this is the key ingredient

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates state = result.getLocationSettingsStates();
                state.isGpsPresent();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location
                        // requests here.
                        startLocationUpdates();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(WearableMainActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        break;
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        startLocationUpdates();
                        break;
                    case Activity.RESULT_CANCELED:
                        settingsRequest();//keep asking if imp or do whatever
                        break;
                }
                break;
        }
    }

    public void startLocationUpdates(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            display("Permissions not set, Failed to connect\n");
            return;
        }

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(1000)
                .setFastestInterval(1000);

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onPause() {
        super.onPause();
//        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
//        settingsRequest();
        mGoogleApiClient.connect();
        startLocationUpdates();

//        settingsRequest();
    }

    @Override
    public void onConnectionSuspended(int i) {
        display("Suspended: "+i);
    }


    @Override
    public void onLocationChanged(Location location) {

        if(tag_location) {
            if(!setTagLocation(location))
                return;
        }
//        Location raw_location = new Location(location);
        kalmanFilter.process(location);
        current_location = kalmanFilter.returnLocation();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        display("Failed: "+connectionResult);
        if (connectionResult.getErrorCode() == ConnectionResult.API_UNAVAILABLE) {
            display("API not there");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopVibrate();
        mGoogleApiClient.disconnect();
        mSensorManager.unregisterListener(this);
        beaconManager.unbind(this);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        while(!command_thread.isInterrupted())
            command_thread.interrupt();
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
    }

    private float[] gravity = new float[3];
    private float[] geomag = new float[3];

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
            for(int i = 0; i < 3; i++)
                gravity[i] = event.values[i];
        }
        else if(event.sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD){
            for(int i = 0; i < 3; i++)
                geomag[i] = event.values[i];
        }

        float[] inR = new float[9];
        float[] I = new float[9];
        float[] orientVals = new float[3];

        double azimuth = 0;
        double pitch = 0;
        double roll = 0;

        if (gravity != null && geomag != null) {
            // checks that the rotation matrix is found
            boolean success = SensorManager.getRotationMatrix(inR, I, gravity, geomag);
            if (success) {
                SensorManager.getOrientation(inR, orientVals);
                azimuth = Math.toDegrees(orientVals[0]);
                pitch = Math.toDegrees(orientVals[1]);
                roll = Math.toDegrees(orientVals[2]);
                orientation.update(azimuth, pitch, roll);
                updateOrientation = true;
            }
        }
    }

    @Override
    public void onBeaconServiceConnect() {
        final String UUID = "2f234454-cf6d-4a0f-adf2-f4911ba9ffa6";
        final String ADDRESS = "0C:F3:EE:09:47:10";
//        final int major = 0;
//        final int minor = 1;
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(final Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    for(Beacon beacon : beacons) {
                        if(beacon.getBluetoothAddress().equals(ADDRESS))
                            distance_beacon = beacon;
//                        final String name = beacon.getBluetoothName();
//                        final double distance = beacon.getDistance();
//                        final String address = beacon.getBluetoothAddress();
//                        beacon.getRssi();
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                display("Beacon "+ name +" I see is about " + distance + " meters away. "+address);
//                            }
//                        });
                    }
                }
            }
        });

        try {
;            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", Identifier.parse(UUID), null, null));
        } catch (RemoteException e) {    }
    }

    private void stopVibrate(){
        vibrator.cancel();
    }

//    private void setVibrationPattern(double distance){
//        distance = round(distance);
//        if(distance == 0)
//            distance = 1;//min of 1m error
//
//        long timing = (long)(100*distance);
//
//        long[] pattern = new long[2];
//        for(int i = 0; i < pattern.length; i++) {
//            if (i % 2 == 0)
//                pattern[i] = timing;
//            else
//                pattern[i] = 100;
//        }
//        vibrator.vibrate(pattern, 0);
//    }

    private double round(double i){
        return round(i, 10);
    }

    private double round(double i, int v){
        return (Math.round(i/v) * v);
    }

    private boolean hasGps() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
    }

    private boolean hasConnectedWearableAPI(){
        return mGoogleApiClient.hasConnectedApi(Wearable.API);
    }

    private void display(String value){
        final String text_display = value;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text.setText(text_display);
            }
        });
    }

    //Geo Tagging code, potentially move to new class
    private final int SAMPLES_REQUIRED = 10;
    private int samples = 0;
    private boolean tag_location = false;

    private KalmanFilter tagLocationKalman = new KalmanFilter(ACCURACY_DECAYS_TIME);

    private void tagLocation(){
        tag_location = true;
        display("Tagging Location, please do not move");
    }

    private void resetTagLocation(){
        samples = 1;
        tagLocationKalman = new KalmanFilter(ACCURACY_DECAYS_TIME);
        tagged_location = null;
    }

    private Boolean setTagLocation(Location location){
        if(tagged_location != null)
            resetTagLocation();

        tagLocationKalman.process(location);

        if(samples++ >= SAMPLES_REQUIRED) {
            location.setLatitude(tagLocationKalman.get_lat());
            location.setLongitude(tagLocationKalman.get_lng());
            location.setAccuracy(tagLocationKalman.get_accuracy());
            location.setTime(tagLocationKalman.get_TimeStamp());
            tagged_location = new Location(location);
            tag_location = false;
            display("Tagging: "+samples+"/"+SAMPLES_REQUIRED);
            return true;
        }
        return false;
    }
}
