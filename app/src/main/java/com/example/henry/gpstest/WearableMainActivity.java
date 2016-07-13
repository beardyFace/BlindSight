package com.example.henry.gpstest;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.app.Activity;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import android.os.Vibrator;

import android.widget.Button;

import com.google.android.gms.appindexing.AppIndex;
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

import java.util.List;

public class WearableMainActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleApiClient mGoogleApiClient;
    private Vibrator vibrator;
//    private PowerManager powerManager;
    //To be used later for "wrist" control, proof of concept
//    private SensorManager mSensorManager;

    private EditText text;
    private Button tag_button;

    private Location tagged_location;

    public static final int ACCURACY_DECAYS_TIME = 1; // Metres per second

    private KalmanFilter kalmanFilter = new KalmanFilter(ACCURACY_DECAYS_TIME);

    protected static final int REQUEST_CHECK_SETTINGS = 0x1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wearable_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        //TODO:
        // As the client is blind a GUI interface is not useful.
        // To be replaced by "accelerometer command" readings based on user testing with client
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
//                .addApi(Wearable.API)  // used for data layer API
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(AppIndex.API).build();

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        //Proof of concept PWM vibrator control
        new CountDownTimer(600000, 60000) {

            public void onTick(long millisUntilFinished) {
                double minToFinish = Math.floor(millisUntilFinished/60000);
                double secToFinish = Math.floor((millisUntilFinished - minToFinish * 60000)/1000);
                double milToFinish = millisUntilFinished - secToFinish * 1000;
                text.setText("seconds remaining: " + minToFinish+"\n"+secToFinish+"\n"+milToFinish+"\n"+millisUntilFinished);
                setVibrationPattern(minToFinish * 10);
            }

            public void onFinish() {
                display("done!");
            }
        }.start();

        //determine what sensors are on testing device, aka my cheap phone, for proof of concept
//        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//        List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
//        for(Sensor sensor : deviceSensors)
//            display(sensor.toString());
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

            String values = "";
            values += ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
            values += " "+PackageManager.PERMISSION_GRANTED;
            values += " "+ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
            values += " "+PackageManager.PERMISSION_GRANTED;
            values += " "+(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED);

            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            display("Failed to connect\n"+values);
            return;
        }

//        LocationManager locationManager = (LocationManager) getSystemService(Activity.LOCATION_SERVICE);
//        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
//        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

//        if (!hasGps()) {
////            text.setText("No GPS: "+isGPSEnabled+" "+isNetworkEnabled);
//            text.setText("No GPS");
//            return;
//        }

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(1000)        // 1 seconds, in milliseconds
                .setFastestInterval(1000); // 1 second, in milliseconds

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
        settingsRequest();
//        printLocation(mLastLocation);
    }

    //Outdated mode of GPS, best practise is to use GPSFused via Google
//    private boolean hasGps() {
//        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
//    }

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

        Location raw_location = new Location(location);

        kalmanFilter.process(location);
        location = kalmanFilter.returnLocation();

        //Print data to screen for debugging
        String value = "Data:\n";
        if(tagged_location != null) {
            value += "Tagged:\n" + printLocation(tagged_location) + "\n";
            float dist = location.distanceTo(tagged_location);
            value+= "Distance from: "+dist+"\n";

//            if(vibrate_count++ > 10) {
//                long timing = (long) (10000 / dist);
//                pwmVibrate(timing);
//                vibrate_count = 0;
//            }
        }
        value+= "New:\n"+printLocation(location)+"\n";
        value+= "Raw:\n"+printLocation(raw_location);
        display(value);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        display("Failed: "+connectionResult);
    }

    private String printLocation(Location location){
        String values = location.getLatitude()+"\n"+location.getLongitude()+"\n"+location.getAccuracy();
//        vibrator.vibrate(500);
        return values;
    }

    private void display(String value){
        text.setText(value);
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
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    private void setVibrationPattern(double distance){
        //TODO
        //Tweak this equation to allow for communicating distance from location.
        //Needs to be noticeably different at a range of ~200m? maybe
        //To add: Compass/Bluetooth to calculate distance better
        //Compass will help get bearing at least. current problem.
        long timing = (long)(distance * 1000)/2;
        pwmVibrate(timing);
    }

    private void pwmVibrate(long timing){
        long[] pattern = new long[2];
        for(int i = 0; i < pattern.length; i++) {
            if (i % 2 == 0)
                pattern[i] = timing;
            else
                pattern[i] = 500;
        }
        //vibrate at pattern forever, until stopped
        vibrator.vibrate(pattern, 0);
//        vibrator.vibrate(timing);
    }

    private void stopVibrate(){
        vibrator.cancel();
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
            return true;
        }
        return false;
    }
}
