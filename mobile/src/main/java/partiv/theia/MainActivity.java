package partiv.theia;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.speech.RecognizerIntent;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener
{

    private CountDownTimer mCountDownTimer;
    private int i = 0;
    private boolean start = false;
    private boolean isBound = false;
    private boolean outDoor = true;
    private VoiceFeedback Vf;
    private int current_id;
    LayoutInflater inflater;
    ViewPager vp;
    private Button buttons[] = new Button[9];
    private ProgressBar pBar;
    public static int selectedLoc = 1;
	// the pages of the app
    private View pages[] = new View[2];

    Messenger mService = null;
    private CanvasView customCanvas;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		// use inflater to get the instances of the two pages
        inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        vp = (ViewPager)findViewById(R.id.viewPager);
        vp.setAdapter(new MyPagesAdapter());
        pages[0] = inflater.inflate(R.layout.swipe1, null);
        pages[1] = inflater.inflate(R.layout.swipe2, null);
		// initiate the buttons and timers
        initButtons();
        initTimer();
        Vf = new VoiceFeedback(this);
		// bind service with the main activity
        bindService(new Intent(this, TheiaService.class), connection, Context.BIND_AUTO_CREATE);
        customCanvas = pages[1].findViewById(R.id.signature_canvas);
    }

    class MyPagesAdapter extends PagerAdapter
    {

        @Override
        public int getCount() {
            return pages.length;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position)
        {
            ((ViewPager) container).addView(pages[position], 0);
            return pages[position];
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view==(View)object;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object)
        {
            ((ViewPager) container).removeView((View)object);
            object = null;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
    }

    @Override
    public void onStart()
    {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void initButtons()
    {
		// set touch listner for all buttons
        for(int i = 0; i < buttons.length; i++)
        {
            String buttonID = "button" + (i + 1);
            int resID = getResources().getIdentifier(buttonID, "id","partiv.theia");

            buttons[i] = (Button) pages[0].findViewById(resID);
            buttons[i].setOnTouchListener(this);
            pBar = (ProgressBar)pages[0].findViewById(R.id.progressBar1);
        }
    }

    private void initTimer()
    {
		// create countdown timer for button activation with 2 second period
        mCountDownTimer = new CountDownTimer(2000,200)
        {
            @Override
            public void onTick(long millisUntilFinished)
            {
                Log.v("Log_tag", "Tick of Progress " + i + " " + millisUntilFinished);
                i += 10;
                pBar.setProgress(i);

            }

			// when the timer times out send the task to the Theia service for processing
            @Override
            public void onFinish()
            {
                i += 10;
                pBar.setProgress(i);
                switch(current_id)
                {
                    case R.id.button1:
                        sendTask(Task.TAG);
                        break;
                    case R.id.button2:
                        sendTask(Task.SAVE);
                        break;
                    case R.id.button3:
                        sendTask(Task.RETURN);
                        break;
                    case R.id.button4:
                        sendTask(Task.RETURN2);
                        break;
                    case R.id.button5:
                        //select locations
                        Intent i = new Intent(MainActivity.this,selectloc.class);
                        startActivity(i);
                        break;
                    case R.id.button6:
                        sendTask(Task.RESET);
                        break;
                    case R.id.button7:
                        sendTask(Task.HELP);
                        break;
                    case R.id.button8:
                        if(outDoor) {
                            sendTask(Task.INDOOR);
                            buttons[7].setText("Change mode to Outdoor");
                            outDoor = false;
                            Vf.speak("indoor mode activated");
                        }
                        else
                        {
                            sendTask(Task.OUTDOOR);
                            buttons[7].setText("Change mode to Indoor");
                            outDoor = true;
                            Vf.speak("outdoor mode activated");
                        }
                        customCanvas.setMode(outDoor);
                        break;
                    case R.id.button9:
                        displaySpeechRecognizer();
                        break;
                    default:
                        break;
                }
                mCountDownTimer.cancel();
                start = false;
            }
        };
    }


    private static final int SPEECH_REQUEST_CODE = 0;

    // Create an intent that can start the Speech Recognizer activity
    private void displaySpeechRecognizer()
    {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// get the task based on the user's voice command and send it to the Theia service for execution
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<String> voice_results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            Task task = Task.getTask(voice_results);
            sendTask(task);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

	// This function sends a task to the theia service for execution
    private void sendTask(Task task)
    {
        if (!isBound) return;
        Message msg = Message.obtain(null, task.getId(), 0, 0);
        msg.replyTo = replyMessenger;
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        current_id = view.getId();
        if (event.getAction() == android.view.MotionEvent.ACTION_DOWN)
        {
            Log.d("TouchTest", "Touch down");
            if(!start) {
				// annouce button name
                Vf.speak(((Button) pages[0].findViewById(current_id)).getText().toString());
				// start timer
                mCountDownTimer.start();
                start = true;
            }
        }
        else if (event.getAction() == android.view.MotionEvent.ACTION_UP)
        {
            Log.d("TouchTest", "Touch up");
            i = 0;
            if(start) {
				// reset timer
                mCountDownTimer.cancel();
                start = false;
            }
            pBar.setProgress(i);
        }
        return true;
    }

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName TheiaService, IBinder service) {
            Log.d("Connection", "Connected");
            mService = new Messenger(service);
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName TheiaService) {
            Log.d("Connection", "Disconnected");
            mService = null;
            isBound = false;
        }
    };

    private Messenger replyMessenger = new Messenger(new HandlerReplyMsg());

	// this sub class handles the reply from the Theia service
    private class HandlerReplyMsg extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String recdMessage = msg.obj.toString(); //msg received from service
			// split the message by comma into list of strings to get the different data seperately
            List<String> data = Arrays.asList(recdMessage.split(","));
			// execute the debugger's command based on the type of data being send
            switch(data.get(0)) {
                case "TAG":
                    customCanvas.drawTag(Double.valueOf(data.get(1)));
                    break;
                case "RETURN":
                    customCanvas.drawRet(Float.valueOf(data.get(1)), Float.valueOf(data.get(2)), Double.valueOf(data.get(3)));
                    break;
                case "UPDATE":
                    customCanvas.updatesLocation(Float.valueOf(data.get(1)), Float.valueOf(data.get(2)), Double.valueOf(data.get(3)));
                    break;
                case "UPDATEI":
                    customCanvas.updateIndoor(Float.valueOf(data.get(1)), Float.valueOf(data.get(2)), Double.valueOf(data.get(3)));
                    break;
                case "MONITOR":
                    customCanvas.monitor(Float.valueOf(data.get(1)), Float.valueOf(data.get(2)), Double.valueOf(data.get(3)));
                    break;
                case "TRACKLOCATION":
                    customCanvas.updateTrackLocation();
                    break;
                case "TRACKBACK":
                    customCanvas.removeTrack();
                    break;
                case "OVERSTEP":
                    customCanvas.overStepCorrection(Integer.valueOf(data.get(1)));
                    break;
                case "RESET":
                    customCanvas.reset();
            }
        }
    }
}
