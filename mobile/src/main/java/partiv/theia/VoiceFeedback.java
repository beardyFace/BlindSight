package partiv.theia;
import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

// This class provides a voice feedback 
public class VoiceFeedback {

    private TextToSpeech t1;

    VoiceFeedback(Context context)
    {
        t1 = new TextToSpeech(context.getApplicationContext(), new TextToSpeech.OnInitListener()
        {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.UK);
                }
            }
        });
    }

    public void speak(String text)
    {
        t1.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

}
