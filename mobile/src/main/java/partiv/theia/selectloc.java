package partiv.theia;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.RecognizerIntent;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

public class selectloc extends AppCompatActivity implements View.OnTouchListener {

    private CountDownTimer mCountDownTimer;
    private int i = 0;
    private boolean start = false;
    private int current_id;

    private VoiceFeedback Vf;
    private Button buttons[] = new Button[9];
    private ProgressBar pBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selectloc);

        initButtons();
        initTimer();
        Vf = new VoiceFeedback(this);
    }

    private void initButtons() {

        for(int i = 0; i < buttons.length; i++)
        {
            String buttonID = "button" + (i + 1);
            int resID = getResources().getIdentifier(buttonID, "id","partiv.theia");

            buttons[i] = (Button) this.findViewById(resID);
            buttons[i].setOnTouchListener(this);
            pBar = (ProgressBar)this.findViewById(R.id.progressBar1);
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
                        Vf.speak("Selected Location One");
                        MainActivity.selectedLoc = 1;
                        break;
                    case R.id.button2:
                        Vf.speak("Selected Location Two");
                        MainActivity.selectedLoc = 2;
                        break;
                    case R.id.button3:
                        Vf.speak("Selected Location Three");
                        MainActivity.selectedLoc = 3;
                        break;
                    case R.id.button4:
                        Vf.speak("Selected Location Four");
                        MainActivity.selectedLoc = 4;
                        break;
                    case R.id.button5:
                        Vf.speak("Selected Location Five");
                        MainActivity.selectedLoc = 5;
                        break;
                    case R.id.button6:
                        Vf.speak("Selected Location Six");
                        MainActivity.selectedLoc = 6;
                        break;
                    case R.id.button7:
                        Vf.speak("Speak name of selected location to save");
                        // change name spoken
                        break;
                    case R.id.button8:
                        Vf.speak("Help message");
                        // help message
                        break;
                    case R.id.button9:
                        //Vf.speak("Voice");
                        break;
                    default:
                        break;
                }
                mCountDownTimer.cancel();
                start = false;
            }
        };
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        current_id = view.getId();
        if (event.getAction() == android.view.MotionEvent.ACTION_DOWN)
        {
            Log.d("TouchTest", "Touch down");
            if(!start) {
                Vf.speak(((Button) this.findViewById(current_id)).getText().toString());
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
}
