package partiv.theia;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener
{

    private CountDownTimer mCountDownTimer;
    private int i = 0;
    private boolean start = false;
    private boolean isBound = false;
    private TextToSpeech t1;
    private int current_id;
    private Button buttons[] = new Button[6];
    private ProgressBar pBar;

    Messenger mService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        initButtons();
        initTimer();
        t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener()
        {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.UK);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onStart()
    {
        super.onStart();
        bindService(new Intent(this, TheiaService.class), connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
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
                        sayHello(null);
                        break;
                    case R.id.button2:
                        displaySpeechRecognizer();
                        break;
                    case R.id.button3:
                        break;
                    case R.id.button4:
                        break;
                    case R.id.button5:
                        break;
                    case R.id.button6:
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
    public boolean onTouch(View view, MotionEvent event) {
        current_id = view.getId();
        if (event.getAction() == android.view.MotionEvent.ACTION_DOWN)
        {
            Log.d("TouchTest", "Touch down");
            if(!start) {
                String toSpeak = ((Button) findViewById(current_id)).getText().toString();
                t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
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

    public void sayHello(View v) {
        if (!isBound) return;
        Message msg = Message.obtain(null, TheiaService.MSG_SAY_HELLO, 0, 0);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
