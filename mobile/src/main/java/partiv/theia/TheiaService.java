package partiv.theia;

import android.Manifest;
import android.app.Service;
import android.hardware.GeomagneticField;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public class TheiaService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{
    private GoogleApiClient googleApiClient;
    private Task current_task = Task.EMPTY;
    private Location current_location;
    private Location prev_location;
    private Tagger tagger;
    private Haptic haptic;
    private Pathing pathing;
    private VoiceFeedback vf;
    private Tracking tracking;
    private Sensors sensors;
    private KalmanFilter KF;
    private double azimuth;
    private double distance = 0;
    private boolean newTask = true;
    private boolean outDoor = true;

    private volatile boolean running = true;
    private final Object lockObj = new Object();
    private Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
            while(running) {
                if (current_task != Task.EMPTY) {
                    execute();
                }
                else
                {
                    /*if(tagger.status())
                    {
                        current_task = Task.TRACK;
                    }*/
                    synchronized (lockObj) {
                        try{
                            lockObj.wait();
                        } catch(InterruptedException e){
                            e.printStackTrace();
                        }
                        //locationSamples = 0;
                    }
                }
            }
        }
    }
    );


    private int locationSamples = 0;

    private void sleep(long time)
    {
        try {
            thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate(){
        super.onCreate();

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        googleApiClient.connect();
        tagger = new Tagger();
        KF = new KalmanFilter(1);
        vf = new VoiceFeedback(this);
        sensors = new Sensors(this, lockObj);
        haptic = new Haptic(this);
        thread.start();
    }

    @Override
    public void onDestroy() {
        googleApiClient.disconnect();
        sensors.unRegister();
        super.onDestroy();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(Location location)
    {
        if(outDoor)
        {
            if (location.getAccuracy() <= 15) {
                KF.process(location);
                current_location = KF.returnLocation();

                azimuth = sensors.getAngle();
                GeomagneticField geoField = new GeomagneticField(
                        (float) current_location.getLatitude(),
                        (float) current_location.getLongitude(),
                        (float) current_location.getAltitude(),
                        System.currentTimeMillis());
                azimuth += geoField.getDeclination();

                if (prev_location != null)
                {
                    if(tagger.status()) {
                        distance += prev_location.distanceTo(current_location);
                        if (distance >= 3) {
                            current_task = Task.TRACK;
                            distance = 0;
                            synchronized (lockObj) {
                                lockObj.notify();
                            }
                        }
                    }
                    sendCoordinates("UPDATE", prev_location.distanceTo(current_location), prev_location.bearingTo(current_location), azimuth);
                }
                prev_location = current_location;
                //locationSamples++;
                Log.d("Lattitude", Double.toString(location.getLatitude()));
                Log.d("Longditude", Double.toString(location.getLongitude()));
                Log.d("Accuracy", Float.toString(location.getAccuracy()));
                Log.d("Orientation", Double.toString(azimuth));
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle)
    {
        locationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public void locationUpdates(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(100)
                .setFastestInterval(100);

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    private volatile Messenger replyMessanger;

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            replyMessanger = msg.replyTo;
            if(current_task == Task.EMPTY || current_task == Task.TRACK) {
                current_task = Task.getTask(msg.what);
                newTask = true;
                synchronized (lockObj) {
                    lockObj.notify();
                }
                Log.d("Command", Integer.toString(msg.what));
            }
        }
    }

    private void execute (){
        switch (current_task){
            case TAG:
                tag();
                break;
            case SAVE:
                break;
            case RETURN:
                ret();
                break;
            case TRACK:
                track();
                break;
            case GUIDE:
                guide();
                break;
            case OUTDOOR:
                outDoor = true;
                current_task = Task.EMPTY;
                break;
            case INDOOR:
                outDoor = false;
                current_task = Task.EMPTY;
            case RESET:
                break;
            case EMPTY:
                break;
            default:
                break;
        }
    }

    private Position current;
    private void tag() {
        if (tracking == null) {
            tracking = new Tracking(sensors);
        }
        //if(locationSamples >= Tagger.TAG_SAMPLE_SIZE) {
        if (outDoor)
        {
            if (current_location != null) {
                tagger.setLocation(current_location/*PL.average()*/);
                current_task = Task.EMPTY;
                prev_location = null;
                sendMessage("TAG," + Double.toString(azimuth));
                sleep(10);
            }
        }
        else
        {
            current_task = Task.EMPTY;
            tagger.setStatus(true);
            sendMessage("TAG," + Double.toString(azimuth));
        }
        //}
    }

    private void track() {
        float distance;
        distance = (float) 0.76;
        if (outDoor)
        {
            if (current_location != null) {
                tracking.addPosition(new Position(current_location, sensors.getAngle()));
                vf.speak("TRACK");
                current_task = Task.EMPTY;
            } else {
                sleep(10);
            }
        }
        else
        {
            azimuth = sensors.getAngle();
            current_task = Task.EMPTY;
            sendCoordinates("UPDATE", distance, 0, azimuth);
            sleep(10);
        }
    }

    private void ret(){
        if(pathing == null && tracking.getSize() > 0) {
            sendCoordinates("RETURN", current_location.distanceTo(tagger.getLocation()), current_location.bearingTo(tagger.getLocation()), azimuth);
            pathing = new Pathing(tracking, new Position(current_location, sensors.getAngle()));
            current_task = Task.GUIDE;
            vf.speak("TONY");
            tagger.setStatus(false);
            sleep(10);
        }
        else
        {
            vf.speak("No path found");
            current_task = Task.EMPTY;
        }
        // }
    }

    private void sendCoordinates(String type, float distance, float bearing, double azimuth)
    {
        sendMessage(type + "," + Float.toString(distance) + "," + Float.toString(bearing) + "," + Double.toString(azimuth));
    }

    private void guide()
    {
        Location current_loc = current_location;//pathing.getCurrent().getLocation();
        Location target_loc = pathing.getTarget().getLocation();

        double bearing = (current_loc.bearingTo(target_loc) + 360) % 360;
        double azimuth = (this.azimuth + 360) % 360;
        double direction = Math.abs(azimuth - bearing);
        Log.d("Bearing", Double.toString(current_loc.bearingTo(target_loc)));
        Log.d("Direction", Double.toString(direction));
        if((direction >= 340 && direction <= 359) || (direction >= 0 && direction < 20))
        {
            if(current_loc.distanceTo(target_loc) > 8)
            {
                vf.speak("walk straight");
                sleep(3000);
            }
            else
            {
                if(!pathing.next())
                {
                    vf.speak("Arrived at destination");
                    tagger.setStatus(false);
                    current_task = Task.EMPTY;
                }
            }
        }
        else
        {
            vf.speak(Integer.toString((int) direction) + " degrees");
            sleep(3000);
        }
    }

    private void debugging(String numDebug, Location location){
        String geoLoc = "";
        //Location location = tagger.getLocation();

        geoLoc += "Lat: ";
        geoLoc += Double.toString(location.getLatitude());
        geoLoc += "\nLon: ";
        geoLoc += Double.toString(location.getLongitude());
        geoLoc += "\nAcc: ";
        geoLoc += Double.toString(location.getAccuracy());

        sendMessage(numDebug + " / " + geoLoc);
    }

    private void sendMessage(String msg) {
        // do stuff
        if (replyMessanger != null)
            try {
                Message message = new Message();
                message.obj = msg;
                replyMessanger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
    }

    final Messenger mMessenger = new Messenger(new IncomingHandler());

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }
}
