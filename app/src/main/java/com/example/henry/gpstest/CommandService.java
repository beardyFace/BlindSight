package com.example.henry.gpstest;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Created by Henry on 17-Sep-16.
 */
public class CommandService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        SensorEventListener {

    //Android device control variables
    private GoogleApiClient mGoogleApiClient;
    private Vibrator vibrator;
    private SensorManager mSensorManager;
    //////////////////////

    //Program variables
    public static final int ACCURACY_DECAYS_TIME = 1; // Metres per second
    private KalmanFilter kalmanFilter = new KalmanFilter(ACCURACY_DECAYS_TIME);
    private Command current_command = Command.EMPTY;

    //Volatile data variables
    private LocationTagger location_tagger = new LocationTagger();
    private volatile Location current_location;

    private volatile SensorData orientation = new SensorData(0, 0, 0);
    //////////////////////

    private final Thread command_thread = new Thread(new Runnable() {
        @Override
        public void run() {
            while(!command_thread.isInterrupted()){
                long start_time = System.nanoTime()/1000000;
                runCommand();
                long process_time = System.nanoTime()/1000000 - start_time;
                long wait_time = (100 - process_time) > 0 ? (100 - process_time) : 0;
                waitMS(wait_time);
            }
        }
    });

    public CommandService(){

    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //Extend overrides
    ///////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onCreate(){
        super.onCreate();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
//                .addApi(AppIndex.API)
//                .addApi(Wearable.API)  // used for data layer API
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

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

        command_thread.start();
//        Toast.makeText(this, "Started", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        command_thread.interrupt();
        while(!command_thread.isInterrupted()){

        }
        mGoogleApiClient.disconnect();
        stopVibrate();
        mSensorManager.unregisterListener(this);

        super.onDestroy();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //Implementation overrides
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mGoogleApiClient.connect();
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        kalmanFilter.process(location);
        current_location = kalmanFilter.returnLocation();

        if(location_tagger.tagging()) {
            location_tagger.setTagLocation(current_location);
        }
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

        if (gravity != null && geomag != null) {
            // checks that the rotation matrix is found
            boolean success = SensorManager.getRotationMatrix(inR, I, gravity, geomag);
            if (success) {
                SensorManager.getOrientation(inR, orientVals);
                double azimuth = Math.toDegrees(orientVals[0]);
                double pitch = Math.toDegrees(orientVals[1]);
                double roll = Math.toDegrees(orientVals[2]);
                orientation.update(azimuth, pitch, roll);
//                updateOrientation = true;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //Local methods
    ///////////////////////////////////////////////////////////////////////////////////////////////

    public void startLocationUpdates(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            display("Permissions not set, Failed to connect\n");
            return;
        }

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(100)
                .setFastestInterval(100);

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
    }

    private void runCommand(){
        double value = 0;
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
        Location tagged_location = location_tagger.getTagLocation();
        if(tagged_location != null && current_location != null && orientation != null){
            double azimuth = orientation.getAx();
            double bearingTo = current_location.bearingTo(tagged_location);

            double threshold = 10;
            double distanceTo = current_location.distanceTo(tagged_location);
            if(distanceTo < 30)
                threshold = 30;

            double difference = Math.abs(azimuth - bearingTo);

            if(difference < threshold)
                vibrator.vibrate(1000);
//            else
//                stopVibrate();
            return difference;
        }
        return 0;
    }

//    private volatile boolean updateOrientation = false;
    private Command setTagLocation(){
//        vibrate(1, 1000, 0);
//        updateOrientation = false;
//        Command command = Command.EMPTY;
//        while(!updateOrientation){
//            command = Command.getCommand(orientation);
//        }
//        int pulses = 1;
//        int time = 500;
//        if(command == Command.TAG) {
//            location_tagger.tagLocation();
//            pulses = 3;
//        }
//        vibrate(pulses, time, 2000);
//        while(location_tagger.tagging()) {
//
//        }
//        vibrate(pulses, time, 2000);

        location_tagger.tagLocation();
        vibrate(3, 1000, 2000);
        while(location_tagger.tagging()){

        }
        vibrate(3, 1000, 2000);
        return Command.EMPTY;
    }

    private double indicateDistance(){
        double distanceTo = 0;
        Location tagged_location = location_tagger.getTagLocation();
        if(tagged_location != null && current_location != null) {
            distanceTo = current_location.distanceTo(tagged_location);
            double nearestTen = Helper.round(distanceTo, 10);
            int pulses = (int)(nearestTen/10);
            if(pulses == 0)
                pulses = 1;
            vibrate(pulses, 500, 2000);

            stopVibrate();
        }
        return distanceTo;
    }

    private void displayToScreen(Command command, double value){
        String text = "Commands: "+command+" "+value+"\n";
        text +=orientation.toString()+"\n";
        text += locationToString(current_location)+"\n";
        sendMessage(text);
    }

    private void vibrate(int pulses, int time, int delay){
        long[] pattern = getPattern(pulses, time);
        vibrator.vibrate(pattern, -1);
        waitMS(pulses * time + delay);
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

    private void stopVibrate(){
        vibrator.cancel();
    }

    private String locationToString(Location location){
        if(location == null)
            return "No location set yet";
        return "Lat: "+location.getLatitude()+"\nLng: "+location.getLongitude()+"\nAcc: "+location.getAccuracy();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //Inter-service communication
    ///////////////////////////////////////////////////////////////////////////////////////////////
    private volatile Messenger replyMessanger;

    private class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            replyMessanger = msg.replyTo; //init reply messenger
            current_command = Command.getCommand(msg.what);
        }
    }

    private void sendMessage(String msg) {
        // do stuff

        if (replyMessanger != null)
            try {
                Message message = new Message();
                message.obj = msg;
                replyMessanger.send(message);//replying / sending msg to activity
            } catch (RemoteException e) {
                e.printStackTrace();
            }
    }
    Messenger messenger = new Messenger(new IncomingHandler());

    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }
}
