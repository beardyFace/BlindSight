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
	// current task
    private Task current_task = Task.EMPTY;
	// positions and location
    private Position current_position;
    private Location current_location;
    private Position prev_position;
	// the tagger class
    private Tagger tagger;
	// haptic feedback
    private Haptic haptic;
    private Pathing pathing;
	// voice feedback
    private VoiceFeedback vf;
	// tracking the user
    private Tracking tracking;
	// sensors process
    private Sensors sensors;
	// kalman filtering process
    private KalmanFilter KF;
	// azimuth angle
    private double azimuth;
	// distance travelled
    private double distance = 0;
    private boolean newTask = true;
    private boolean outDoor = true;
    private ArrayList<Position> savedPath = new ArrayList<>();
    private saveLocation saved;
    private Position savedPosition;

	// threshold for accuracy of guidance algorithm
    private double threshold = 4;

    private volatile boolean running = true;
	// object used for thread resume and pausing to prevent unnecessary polling
    private final Object lockObj = new Object();
	// the Theia's system execution thread
    private Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
            while(running) {
				// if the current set task is not empty then execute that task
                if (current_task != Task.EMPTY) {
                    execute();
                }
                else
                {
					// pause the thread since no task is queued
                    synchronized (lockObj) {
                        try{
                            lockObj.wait();
                        } catch(InterruptedException e){
                            e.printStackTrace();
                        }
                        //locationSamples = 0;
                    }
					// once unpaused check if position is tagged, indoor is activated and tracking condition is met
                    if(tagger.status() && !outDoor && sensors.getTrack())
                    {
						// set task to track the user
                        current_task = Task.TRACK;
                        sensors.setTrack(false);
                    }
                }
            }
        }
    }
    );


    private int locationSamples = 0;

	// put the thread to sleep for x milliseconds
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
		// create new google api and adding location services to it
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        googleApiClient.connect();
		// initialise position tagger, kalman filtering, voice feedback, haptic feedback and sensors
        tagger = new Tagger();
        vf = new VoiceFeedback(this);
        sensors = new Sensors(this, lockObj);
		KF = new KalmanFilter(1, sensors);
        haptic = new Haptic(this);
		// start Theia thread
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

	// this function listen for GPS location changes
    @Override
    public void onLocationChanged(Location location)
    {
		// if outdoor mode
        if(outDoor)
        {
			// filter out all locations that have accuracy parameter > 15 (not precise enough)
            if (location.getAccuracy() <= 15) {
				// process the location using a kalman filter
                KF.process(location);
				// get the filtered location
                current_location = KF.returnLocation();

				// get the azimuth angle
                azimuth = sensors.getAngle();
				
				// use the geomagnetic field information of the location to compute the declination towards true north to improve accuracy of azimuth
                GeomagneticField geoField = new GeomagneticField(
                        (float) current_location.getLatitude(),
                        (float) current_location.getLongitude(),
                        (float) current_location.getAltitude(),
                        System.currentTimeMillis());
                azimuth += geoField.getDeclination();

                current_position = new Position(current_location, azimuth);

				// if previous position exists
                if (prev_position != null)
                {
					// update the debugger with current position and angles
                    sendCoordinates("UPDATE", prev_position.distanceTo(outDoor, current_position), prev_position.bearingTo(outDoor, current_position), azimuth);
					// if location is tagged then track the user every 3 metres
                    if(tagger.status()) {
                        distance += prev_position.distanceTo(outDoor, current_position);
                        if (distance >= 3) {
                            current_task = Task.TRACK;
                            distance = 0;
							// wake up the execution thread
                            synchronized (lockObj) {
                                lockObj.notify();
                            }
							// save track location on the debugger
                            sendMessage("TRACKLOCATION");
                        }
                    }
                }
				// make current position the previous
                prev_position = current_position;
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
		// set the permission required for the location services
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
		// set the location request settings to highest accuracy and interval to 100 ms
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(100)
                .setFastestInterval(100);
		// use fused api for location to get the best avaliable location service
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    private volatile Messenger replyMessanger;

	// this inner class handles the incoming messages from main activity
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            replyMessanger = msg.replyTo;
			// if the current task is empty or tracking or if the incoming task is a reset then
            if(current_task == Task.EMPTY || current_task == Task.TRACK || Task.getTask(msg.what) == Task.RESET)
            {
				// get the task
                current_task = Task.getTask(msg.what);
                newTask = true;
				// wake up the thread to process task
                synchronized (lockObj) {
                    lockObj.notify();
                }
                Log.d("Command", Integer.toString(msg.what));
            }
        }
    }

	// this function runs the task that was given by the user or system
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

	// this function reset Theia's system 
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

	// This function tags the user's current position 
    private void tag() {
        if (tracking == null) {
            tracking = new Tracking(sensors);
        }
        //if(locationSamples >= Tagger.TAG_SAMPLE_SIZE) {
        if (outDoor)
        {
            if (current_position != null) {
                vf.speak("Location is tagged");
				// set the tagged position
                tagger.setPosition(current_position);
                prev_position = null;
				// mark the tag location on the debugger
                sendMessage("TAG," + Double.toString(azimuth));
				// sleep the thread for UI to update smoothly
                sleep(10);
            } else{
                vf.speak("Failed to tag location, please retry");
            }
        }
        else
        {
                vf.speak("Location is tagged");
                current_position = new Position(new PointF(0, 0), azimuth);
				// set the tagged position
                tagger.setPosition(current_position);
				// mark the tag location on the debugger
                sendMessage("TAG," + Double.toString(azimuth));
                sensors.setPosition(current_position);
				// sleep the thread for UI to update smoothly
                sleep(10);
        }
        //}
        current_task = Task.EMPTY;
    }

	// This function saves the user's tagged location and path taken
    private void save(){
        vf.speak("save location");
        if(saved == null) {
            saved = new saveLocation();
        }
		// save the tagged location and tracking information
        saved.addSavedLocation(tagger.getPosition(), tracking);

        current_task = Task.EMPTY;
        sleep(10);
    }

	// This function returns the user to their selected saved location
    private void saveRet(){
        vf.speak("return to saved location");

        current_task = Task.EMPTY;
        sleep(10);
    }

	// provide the user with guidance on how to use the application verbally
    private void help(){
        vf.speak("The screen is split into 8 buttons with 4 columns and 2 rows. The top left corner is button 1 and the bottom right is button 8. From 1 through to 8 the buttons are as follows. 1 is to tag a location. 2 is to save a location. 3 is to return to a tagged location. 4 is to return to a saved location. 5 is the help button which can be pressed to hear this message. 6 is the reset button which resets tagged locations but not saved locations. 7 is a voice button which can be used to call out the names of the buttons instead of pressing them. 8 is the button to change from outdoor to indoor mode and vice versa. Tap a button to hear what the name of the button is and what it does. Hold down that button for 3 seconds and the button will be pressed.");
        current_task = Task.EMPTY;
        sleep(10);
    }

	// track the user's path by marking their position when certain event occurs (3 metres / 3 steps)
    private void track() {
        if (outDoor)
        {
            if (current_location != null) {
				// add the current position as a tracked position
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
			// add the current position as a tracked position
            tracking.addPosition(new Position(current_position.getPosition(), current_position.getAngle()));
            vf.speak("Track");
			// update indoor mode current position in the debugger
            sendCoordinates("UPDATEI", current_position.getPosition().x, current_position.getPosition().y, azimuth);
            current_task = Task.EMPTY;
            sleep(10);
        }
    }

    private void ret(){
        vf.speak("attempting to return");
		// if a position was tagged
        if (tagger.status()) {
				// if tracked positions exists and a path is not yet found
                if (pathing == null && tracking.getSize() > 0) {
					// mark the current position on the debugger
                    sendCoordinates("RETURN", current_position.distanceTo(outDoor, tagger.getPosition()), current_position.bearingTo(outDoor, tagger.getPosition()), azimuth);
					// create a new pathing object to guide the user to each point
                    pathing = new Pathing(tracking, current_position);
					// switch to guidance task
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

	// send coordinates and angles to the main activity
    private void sendCoordinates(String type, float distance, float bearing, double azimuth)
    {
        sendMessage(type + "," + Float.toString(distance) + "," + Float.toString(bearing) + "," + Double.toString(azimuth));
    }

    private long startTime = 0;
	
	// This function guides the user to the tagged location through the tracked points
    private void guide()
    {
		// boolean varaible indicating when a time out has occured
        boolean timeOut = false;
		// if start time is not initialise then initialise it
        if(startTime == 0)
        {
            startTime = System.nanoTime();
        }

		// if the time has passed by 5 seconds then time out is true
        if(System.nanoTime() - startTime >= 5000000000L)
        {
            timeOut = true;
            startTime = 0;
        }
		
		// get the current position and the target position (last known tracked point)
        Position current_loc = current_position;//pathing.getCurrent().getLocation();
        Position target_loc = pathing.getTarget();

		// if the current position is the same as the target then move to the next target
        if(current_loc.distanceTo(outDoor,target_loc) == 0)
        {
            pathing.next();
        }

		// if indoor mode then get the angle update
        if(!outDoor)
        {
            this.azimuth = sensors.getAngle();
        }
		// get the bearing between the current position and the target and compute the directional angle the user is required to take
        double bearing = (current_loc.bearingTo(outDoor, target_loc) + 360) % 360;
        double direction = Math.abs(azimuth - bearing);
        Log.d("Azimuth", Double.toString(azimuth));
        Log.d("Bearing", Double.toString(current_loc.bearingTo(outDoor, target_loc)));
        Log.d("Direction", Double.toString(direction));

        int index;
		// get the distance between the two positions
        double distance = current_loc.distanceTo(outDoor, target_loc);
		// if the distance is greater than the radius threshold value
        if(distance > threshold)
        {
			// if the target is within the user front 45 degrees then notify them to move towards north (straight)
            if((direction >= 337.5 && direction <= 359) || (direction >= 0 && direction < 22.5))
            {
                if(timeOut)
                {
                    vf.speak("head north for " + Double.toString(Math.round(distance)));
                }
            }
            else
            {
                if(timeOut)
                {
                    //vf.speak(Integer.toString((int) direction) + " degrees");
					
					// if the target is slightly to the left then notify them to move towards north east (straight towards the left side)
                    if(direction >= 22.5 && direction < 67.5)
                    {
                        vf.speak("head north east for " + Double.toString(Math.round(distance)) + " metres");
                    }
					// if the target is on their left then notify them to move towards east (turn left and walk straight)
                    else if(direction >= 67.5 && direction < 112.5)
                    {
                        vf.speak("head east for " + Double.toString(Math.round(distance)) + " metres");
                    }
					// if the target is behind them and slightly to the left then notify them to move towards north east (turn around and walk towards the left)
                    else if(direction >= 112.5 && direction < 157.5)
                    {
                        vf.speak("head south east for " + Double.toString(Math.round(distance)) + " metres");
                    }
					// if the target is behind them then notify them to move towards south (turn around)
                    else if(direction >= 157.5 && direction < 202.5)
                    {
                        vf.speak("head south for " + Double.toString(Math.round(distance)) + " metres");
                    }
					// if the target is behind them and slightly to the right then notify them to move towards south west (turn around and walk towards the right)
                    else if(direction >= 202.5 && direction < 247.5)
                    {
                        vf.speak("head south west for " + Double.toString(Math.round(distance)) + " metres");
                    }
					// if the target is on their right then notify them to move towards east (turn right and walk straight)
                    else if(direction >= 247.5 && direction < 292.5)
                    {
                        vf.speak("head west for " + Double.toString(Math.round(distance)) + " metres");
                    }
					// if the target is slightly to the right them then notify them to move towards north west (straight towards the right side)
                    else if(direction >= 292.5 && direction < 337.5)
                    {
                        vf.speak("head north west for " + Double.toString(Math.round(distance)) + " metres");
                    }

                }
            }
        }
            /*else if((index = tracking.check(outDoor, current_position)) >= 0)
            {
                sendMessage("OVERSTEP," + Integer.toString(index));
            }*/
        else
        {
			// if target has been reached and no more targets exist then the user has arrived at their desire location
            if(!pathing.next())
            {
                vf.speak("Arrived at destination");
                tagger.setStatus(false);
                current_task = Task.EMPTY;
                sendMessage("TRACKBACK");
                sleep(10);
                return;
            }
			// otherwise continue moving through the targets
            sendMessage("TRACKBACK");
        }

		// if indoor mode (outdoor is monitored by location changes already), monitor their position while guiding the user
        if(!outDoor)
        {
            sendCoordinates("MONITOR", current_position.getPosition().x, current_position.getPosition().y, azimuth);
        }
        sleep(10);
    }

	// to send messages to the main acitivity (no longer in used after debugger introduction)
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

	// send a message back to the main activity for UI updates
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
