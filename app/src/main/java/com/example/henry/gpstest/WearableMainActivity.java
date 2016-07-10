package com.example.henry.gpstest;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.view.View;
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

public class WearableMainActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleApiClient mGoogleApiClient;
    private Vibrator vibrator;

    private EditText text;
    private Button tag_loc;

    private Location tagged_location;

    public static final int ACCURACY_DECAYS_TIME = 1; // Metres per second

    private KalmanFilter kalmanFilter = new KalmanFilter(ACCURACY_DECAYS_TIME);

    protected static final int REQUEST_CHECK_SETTINGS = 0x1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wearable_main);

        //TODO:
        // As the client is blind a GUI interface is not useful.
        // To be replaced by "accelerometer command" readings based on user testing with client
        tag_loc = (Button) findViewById(R.id.geoButton);
        tag_loc.setOnClickListener(new View.OnClickListener() {
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
        vibrator.vibrate(500);
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
        mGoogleApiClient.disconnect();
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
