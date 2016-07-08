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

    private Location init_location;

    public static final int ACCURACY_DECAYS_TIME = 1; // Metres per second

    private KalmanFilter kalmanFilter = new KalmanFilter(ACCURACY_DECAYS_TIME);

    protected static final int REQUEST_CHECK_SETTINGS = 0x1;

    private boolean pushed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wearable_main);

        tag_loc = (Button) findViewById(R.id.geoButton);
        tag_loc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                text.setText(""+pushed);
                pushed = !pushed;
            }
        });

        text = (EditText) findViewById(R.id.editText);
        text.setText("Hello People");

        // ATTENTION: This "addApi(AppIndex.API)"was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
//                .addApi(Wearable.API)  // used for data layer API
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(AppIndex.API).build();

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }

    public void settingsRequest()
    {
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
            public void onResult(LocationSettingsResult result) {
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
            text.setText("Failed to connect\n"+values);
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
                .setInterval(1000)        // 10 seconds, in milliseconds
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
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        settingsRequest();
//        printLocation(mLastLocation);
    }

    private boolean hasGps() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
    }

    @Override
    public void onConnectionSuspended(int i) {
        text.setText("Suspended: "+i);
    }

    @Override
    public void onLocationChanged(Location location) {

        if(init_location == null) {
            init_location = setTagLocation(location);
            if(init_location == null)
                return;
        }

        Location raw_location = new Location(location);

        kalmanFilter.process(location.getLatitude(), location.getLongitude(), location.getAccuracy(), location.getTime());

        location.setLatitude(kalmanFilter.get_lat());
        location.setLongitude(kalmanFilter.get_lng());
        location.setAccuracy(kalmanFilter.get_accuracy());

        float dist = location.distanceTo(init_location);

        String value = "Initial:\n"+printLocation(init_location)+"\n";
               value+= "New:\n"+printLocation(location)+"\n";
               value+= "Distance: "+dist+"\n";
               value+= "Raw:\n"+printLocation(raw_location);
        display(value);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        text.setText("Failed: "+connectionResult);
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

    private final int SAMPLES_REQUIRED = 10;
    private int samples = 0;

    private KalmanFilter tagLocationKalman = new KalmanFilter(ACCURACY_DECAYS_TIME);

    private void resetTagLocation(){
        samples = 0;
        tagLocationKalman = new KalmanFilter(ACCURACY_DECAYS_TIME);
        init_location = null;
    }

    private Location setTagLocation(Location location){
        if(init_location != null)
            resetTagLocation();

        if(samples++ >= SAMPLES_REQUIRED) {
            location.setLatitude(tagLocationKalman.get_lat());
            location.setLongitude(tagLocationKalman.get_lng());
            location.setAccuracy(tagLocationKalman.get_accuracy());
            location.setTime(tagLocationKalman.get_TimeStamp());
            return location;
        }
        tagLocationKalman.process(location.getLatitude(), location.getLongitude(), location.getAccuracy(), location.getTime());
        return null;
    }
}
