package partiv.theia;

import android.Manifest;
import android.app.Service;
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
    private Tagger tagger;
    private Haptic haptic;
    private KalmanFilter KF;
    private Sensors sensors;
    private ProcessLocation PL;
    private Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
            while(true) {
                if (current_task != Task.EMPTY) {
                    commandExe();
                }
                /*else
                {
                    locationSamples = 0;
                }*/
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
        PL = new ProcessLocation();
        KF = new KalmanFilter(1);
        haptic = new Haptic(this);
        sensors = new Sensors(this);
        thread.start();
    }

    @Override
    public void onDestroy() {
        googleApiClient.disconnect();
        sensors.unRegister();
        Log.d("Service", "Destroy");
        super.onDestroy();
    }

    @Override
    public void onLocationChanged(Location location)
    {
        KF.process(location);
        current_location = KF.returnLocation();
        if(current_task == Task.TAG || current_task == Task.RETURN)
        {
            PL.addLocation(location);
        }
        locationSamples++;
        Log.d("Lattitude", Double.toString(location.getLatitude()));
        Log.d("Longditude", Double.toString(location.getLongitude()));
        Log.d("Accuracy", Float.toString(location.getAccuracy()));
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
            current_task = Task.getTask(msg.what);
            Log.d("Command", Integer.toString(msg.what));
        }
    }

    private void commandExe (){
        switch (current_task){
            case TAG:
                tag();
                break;
            case SAVE:
                break;
            case RETURN:
                ret();
                break;
            case RESET:
                break;
            case EMPTY:
                break;
            default:
                break;
        }
    }

    private void tag()
    {
        sensors.resetSteps();
        current_task = Task.EMPTY;
        /*if(locationSamples >= Tagger.TAG_SAMPLE_SIZE) {
            tagger.setLocation(PL.average());
            current_task = Task.EMPTY;
            debugging("1", tagger.getLocation());
            PL.clear();
        }*/
    }
    private boolean firstTime = true;
    private double distance;

    private void ret(){
        if(firstTime)
        {
            distance = sensors.getDistance();
            sendMessage("2 / " + Double.toString(distance));
            sensors.resetSteps();
            firstTime = false;
            sleep(10);
        }
        else
        {
            sendMessage("3 / " + Double.toString(sensors.getDistance()));
            if(Math.abs(distance - sensors.getDistance()) <= 1)
            {
                haptic.vibrate();
                firstTime = true;
                current_task = Task.EMPTY;
            }
            sleep(10);
        }
        /*if(locationSamples >= 5)
        {
            if (tagger.getLocation() != null && current_location != null) {
                float distanceInMeters = PL.average().distanceTo(tagger.getLocation());
                debugging("3", PL.average());
                sendMessage("2 / " + Float.toString(distanceInMeters));
                current_task = Task.EMPTY;
                PL.clear();
            }
        }*/
    }

    private void debugging(String numDebug, Location location){
        String geoLoc = "";
        //Location location = tagger.getLocation();

        geoLoc += "Lat: ";
        geoLoc += Double.toString(location.getLatitude());
        geoLoc += "\nLon: ";
        geoLoc += Double.toString(location.getLongitude());

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
