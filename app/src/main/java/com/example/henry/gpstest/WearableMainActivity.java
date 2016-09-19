package com.example.henry.gpstest;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.renderscript.ScriptGroup;
import android.speech.RecognizerIntent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.List;

public class WearableMainActivity extends Activity {

    //UI display
    private EditText text;
    private Button apeech_button;
//    private final Intent command_service = new Intent(WearableMainActivity.this, CommandService.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wearable_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        apeech_button = (Button) findViewById(R.id.geoButton);
        apeech_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displaySpeechRecognizer();
            }
        });

        text = (EditText) findViewById(R.id.editText);
        text.setText("Connecting to GPS");

//        command_service.setCommand(Command.EMPTY);
//        startService(command_service);
    }

    private static final int SPEECH_REQUEST_CODE = 0;

    // Create an intent that can start the Speech Recognizer activity
    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    // This callback is invoked when the Speech Recognizer returns.
    // This is where you process the intent and extract the speech text from the intent.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);

            display(results.get(0));
            Command command = Command.getCommand(results);
            sendCommand(command);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        bindService(new Intent(this, CommandService.class), connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mIsBound) {
            unbindService(connection);
            mIsBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //Inter-service communication
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private Messenger msgService;

    private boolean mIsBound;

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mIsBound = false;
            msgService = null;
            display("DisConnected: "+mIsBound);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mIsBound = true;
            msgService = new Messenger(service);
            display("Connected: "+mIsBound);
        }
    };

    public void sendCommand(Command command) {
        if (mIsBound) {
            try {
                Message message = Message.obtain(null, command.CM_ID, 0, 0);
                message.replyTo = replyMessenger;
//                Bundle bundle = new Bundle();
//                bundle.putString("rec", "Hi, you hear me");
//                message.setData(bundle);
                msgService.send(message); //sending message to service

            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    //setting reply messenger and handler
    private Messenger replyMessenger = new Messenger(new HandlerReplyMsg());

    private class HandlerReplyMsg extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String recdMessage = msg.obj.toString(); //msg received from service
            display(recdMessage);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //Binding service
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private void display(String value){
        final String text_display = value;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text.setText(text_display);
            }
        });
    }

//    private boolean hasGps() {
//        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
//    }

//    private boolean hasConnectedWearableAPI(){
//        return mGoogleApiClient.hasConnectedApi(Wearable.API);
//    }

}
