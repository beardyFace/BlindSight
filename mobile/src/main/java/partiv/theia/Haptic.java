package partiv.theia;

import android.content.Context;
import android.os.Build;
import android.os.Vibrator;
import android.os.VibrationEffect;

public class Haptic {

    private Context context;
    private Vibrator v;

    Haptic(Context context)
    {
        this.context = context;
        v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public void vibrate(int time)
    {
        v.vibrate(time);
    }


}
