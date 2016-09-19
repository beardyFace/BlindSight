package com.example.henry.gpstest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by Henry on 14-Jul-16.
 */
public enum Command {
    EMPTY(0),
    DISTANCE(1, "distance"),
    ANGLE(2, "angle", "orientation"),
    TAG(3, "tag", "save");

    private final ArrayList<String> string_commands = new ArrayList<String>();
    public final int CM_ID;

    private Command(int ID, String...voice_commands){
        CM_ID = ID;
        for(String cmd : voice_commands)
            string_commands.add(cmd);
    }


    public static Command getCommand(List<String> voice_command){
        for(Command cmd : Command.values())
            if(!Collections.disjoint(cmd.string_commands, voice_command))
                return cmd;
        return EMPTY;
    }

    public static Command getCommand(int cmd_id){
        for(Command cmd : Command.values())
            if(cmd.CM_ID == cmd_id)
                return cmd;
        return EMPTY;
    }

    public static Command getCommand(SensorData orientation){
        double pitch = orientation.getAy();
        double roll = orientation.getAz();

        if(Math.abs(pitch) < 45 && Math.abs(roll) < 20)
            return ANGLE;
        else if(pitch > 70 && pitch < 90)
            return DISTANCE;
        else if(pitch < -70 && pitch > -90)
            return TAG;

        return EMPTY;
    }



}
