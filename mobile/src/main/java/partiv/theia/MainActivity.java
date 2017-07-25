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
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener
{

    private CountDownTimer mCountDownTimer;
    private int i = 0;
    private boolean start = false;
    private boolean isBound = false;
    private VoiceFeedback Vf;
    private int current_id;
    private Button buttons[] = new Button[8];
    private ProgressBar pBar;
    public int[] locations;

    Messenger mService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        initButtons();
        initTimer();
        Vf = new VoiceFeedback(this);
        bindService(new Intent(this, TheiaService.class), connection, Context.BIND_AUTO_CREATE);
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
        for(int i = 0; i < buttons.length; i++)
        {
            String buttonID = "button" + (i + 1);
            int resID = getResources().getIdentifier(buttonID, "id","partiv.theia");

            buttons[i] = (Button) findViewById(resID);
            buttons[i].setOnTouchListener(this);
            pBar = (ProgressBar)findViewById(R.id.progressBar1);
        }
    }

    private void initTimer()
    {
        mCountDownTimer = new CountDownTimer(2000,200)
        {
            @Override
            public void onTick(long millisUntilFinished)
            {
                Log.v("Log_tag", "Tick of Progress " + i + " " + millisUntilFinished);
                i += 10;
                pBar.setProgress(i);

            }

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
                        break;
                    case R.id.button4:
                        sendTask(Task.RETURN);
                        break;
                    case R.id.button5:
                        sendTask(Task.RETURN);
                        break;
                    case R.id.button6:
                        break;
                    case R.id.button7:
                        displaySpeechRecognizer();
                        break;
                    case R.id.button8:
                        displaySpeechRecognizer();
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
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<String> voice_results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            Task task = Task.getTask(voice_results);
            sendTask(task);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

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
                Vf.speak(((Button) findViewById(current_id)).getText().toString());
                mCountDownTimer.start();
                start = true;
            }
        }
        else if (event.getAction() == android.view.MotionEvent.ACTION_UP)
        {
            Log.d("TouchTest", "Touch up");
            i = 0;
            if(start) {
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

    private class HandlerReplyMsg extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String recdMessage = msg.obj.toString(); //msg received from service
            if(recdMessage.charAt(0) == '1') {
                buttons[6].setText(recdMessage);
            }
            else if(recdMessage.charAt(0) == '3')
            {
                buttons[5].setText(recdMessage);
            }
            else if(recdMessage.charAt(0) == '4')
            {
                Vf.speak("Arrived at destination");
            }
            else
            {
                buttons[7].setText(recdMessage);
            }
        }
    }
}
