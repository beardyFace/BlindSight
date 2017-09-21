package partiv.theia;

import android.Manifest;
import android.app.Service;
import android.graphics.PointF;
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

import java.util.ArrayList;

public class TheiaService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{
    private GoogleApiClient googleApiClient;
    private Task current_task = Task.EMPTY;
    private Position current_position;
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
    private ArrayList<Position> savedPath = new ArrayList<>();
    private saveLocation saved;
    private Position savedPosition;

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
                    synchronized (lockObj) {
                        try{
                            lockObj.wait();
                        } catch(InterruptedException e){
                            e.printStackTrace();
                        }
                        //locationSamples = 0;
                    }
                    if(tagger.status() && !outDoor && sensors.getTrack())
                    {
                        current_task = Task.TRACK;
                        sensors.setTrack(false);
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

                current_position = new Position(current_location, azimuth);

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
            if(current_task == Task.EMPTY || current_task == Task.TRACK || Task.getTask(msg.what) == Task.RESET)
            {
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
                save();
                break;
            case RETURN:
                ret();
                break;
            case RETURN2:
                saveRet();
                break;
            case HELP:
                help();
                break;
            case TRACK:
                track();
                break;
            case GUIDE:
                guide();
                break;
            case OUTDOOR:
                outDoor = true;
                reset();
                current_task = Task.EMPTY;
                break;
            case INDOOR:
                outDoor = false;
                reset();
                current_task = Task.EMPTY;
                break;
            case RESET:
                reset();
                vf.speak("reset completed");
                break;
            case EMPTY:
                break;
            default:
                break;
        }
    }

    private void reset()
    {
        tagger.setStatus(false);
        tracking = null;
        pathing = null;
        current_position = null;
        current_location = null;
        current_task = Task.EMPTY;
        sendMessage("RESET");
    }

    private void tag() {
        if (tracking == null) {
            tracking = new Tracking(sensors);
        }
        //if(locationSamples >= Tagger.TAG_SAMPLE_SIZE) {
        if (outDoor)
        {
            if (current_position != null) {
                vf.speak("Location is tagged");
                tagger.setPosition(current_position);
                prev_location = null;
                sendMessage("TAG," + Double.toString(azimuth));
                sleep(10);
            } else{
                vf.speak("Failed to tag location, please retry");
            }
        }
        else
        {
                vf.speak("Location is tagged");
                current_position = new Position(new PointF(0, 0), azimuth);
                tagger.setPosition(current_position);
                sendMessage("TAG," + Double.toString(azimuth));
                sensors.setPosition(current_position);
                sleep(10);
        }
        //}
        current_task = Task.EMPTY;
    }

    private void save(){
        vf.speak("save location");
        if(saved == null) {
            saved = new saveLocation();
        }

        saved.addSavedLocation(tagger.getPosition(), tracking);

        current_task = Task.EMPTY;
        sleep(10);
    }

    private void saveRet(){
        vf.speak("return to saved location");

        current_task = Task.EMPTY;
        sleep(10);
    }

    private void help(){
        vf.speak("The screen is split into 8 buttons with 4 columns and 2 rows. The top left corner is button 1 and the bottom right is button 8. From 1 through to 8 the buttons are as follows. 1 is to tag a location. 2 is to save a location. 3 is to return to a tagged location. 4 is to return to a saved location. 5 is the help button which can be pressed to hear this message. 6 is the reset button which resets tagged locations but not saved locations. 7 is a voice button which can be used to call out the names of the buttons instead of pressing them. 8 is the button to change from outdoor to indoor mode and vice versa. Tap a button to hear what the name of the button is and what it does. Hold down that button for 3 seconds and the button will be pressed.");
        current_task = Task.EMPTY;
        sleep(10);
    }

    private void track() {
        if (outDoor)
        {
            if (current_location != null) {
                tracking.addPosition(new Position(current_location, sensors.getAngle()));
                vf.speak("Track");
                current_task = Task.EMPTY;
            } else {
                sleep(10);
            }
        }
        else
        {
            azimuth = sensors.getAngle();
            tracking.addPosition(new Position(current_position.getPosition(), current_position.getAngle()));
            vf.speak("Track");
            sendCoordinates("UPDATEI", current_position.getPosition().x, current_position.getPosition().y, azimuth);
            current_task = Task.EMPTY;
            sleep(10);
        }
    }

    private void ret(){
        vf.speak("attempting to return");
        if (tagger.status()) {
                if (pathing == null && tracking.getSize() > 0) {
                    sendCoordinates("RETURN", current_position.distanceTo(outDoor, tagger.getPosition()), current_position.bearingTo(outDoor, tagger.getPosition()), azimuth);
                    pathing = new Pathing(tracking, current_position);
                    current_task = Task.GUIDE;
                    vf.speak("returning to tagged location");
                    tagger.setStatus(false);
                    sleep(10);
                } else {
                    vf.speak("No path found");
                    current_task = Task.EMPTY;
                }
        } else {
            vf.speak("there are no tagged locations to return to");
            current_task = Task.EMPTY;
        }
    }

    private void sendCoordinates(String type, float distance, float bearing, double azimuth)
    {
        sendMessage(type + "," + Float.toString(distance) + "," + Float.toString(bearing) + "," + Double.toString(azimuth));
    }

    private long startTime = 0;
    private void guide()
    {
        boolean timeOut = false;
        if(startTime == 0)
        {
            startTime = System.nanoTime();
        }

        if(System.nanoTime() - startTime >= 3000000000L)
        {
            timeOut = true;
            startTime = 0;
        }

        Position current_loc = current_position;//pathing.getCurrent().getLocation();
        Position target_loc = pathing.getTarget();

        if(current_loc.distanceTo(outDoor,target_loc) == 0)
        {
            pathing.next();
        }

        if(!outDoor)
        {
            this.azimuth = sensors.getAngle();
        }
        double bearing = (current_loc.bearingTo(outDoor, target_loc) + 360) % 360;
        double direction = Math.abs(azimuth - bearing);
        Log.d("Azimuth", Double.toString(azimuth));
        Log.d("Bearing", Double.toString(current_loc.bearingTo(outDoor, target_loc)));
        Log.d("Direction", Double.toString(direction));

        int index;
        if(current_loc.distanceTo(outDoor, target_loc) > 2)
        {
            if((direction >= 340 && direction <= 359) || (direction >= 0 && direction < 20))
            {
                if(timeOut)
                {
                    vf.speak("walk straight");
                }
            }
            else
            {
                if(timeOut) {
                    vf.speak(Integer.toString((int) direction) + " degrees");
                }
            }
        }
            /*else if((index = tracking.check(outDoor, current_position)) >= 0)
            {
                sendMessage("OVERSTEP," + Integer.toString(index));
            }*/
        else
        {
            if(!pathing.next())
            {
                vf.speak("Arrived at destination");
                tagger.setStatus(false);
                current_task = Task.EMPTY;
                sendMessage("TRACKBACK");
                sleep(10);
                return;
            }
            sendMessage("TRACKBACK");
        }
        sendCoordinates("MONITOR", current_position.getPosition().x, current_position.getPosition().y, azimuth);
        sleep(10);
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
