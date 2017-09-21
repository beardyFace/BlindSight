package partiv.theia;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * Created by Lewayne on 21/09/2017.
 */

public class saveLocation {

    private ArrayList<Tracking> savedPaths = new ArrayList<>();

    public saveLocation(){
    }

    public void addSavedLocation(Position destination, Tracking path)
    {
        savedPaths.add(path);
    }

    private Tracking getSavedLocation(int i){
        return savedPaths.get(i);
    }

}
