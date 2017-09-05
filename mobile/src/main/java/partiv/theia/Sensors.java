package partiv.theia;


import android.content.Context;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class Sensors implements SensorEventListener
{

    private SensorManager mSensorManager;
    private final Context context;
    private int steps = 0;
    //private float[] gravity = new float[3];
    private float[] rotationMatrix = new float[16];
    //private float[] magnetic = new float[3];
    //private float[] linearAcc = new float[3];

    private static final double STEP_SIZE = 0.76;
    private double azimuth, pitch, roll;
    private Position position;
    private Object lockObj;

    Sensors(Context context, Object lockObj)
    {
        this.context = context;
        this.lockObj = lockObj;
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        //mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_NORMAL);
        //mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_NORMAL);
        //mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
        //mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR), SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_NORMAL);
    }


    public void unRegister()
    {
        mSensorManager.unregisterListener(this);
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        /*if (event.sensor.getType() == Sensor.TYPE_GRAVITY){
            for(int i = 0; i < 3; i++)
                gravity[i] = event.values[i]; //lowPass(event.values.clone(), gravity);
        }*/
        if(event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR)
        {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
        }
        /*else if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            for(int i = 0; i < 3; i++)
                magnetic[i] = event.values[i];
        }*/
        /*else if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION)
        {
            for(int i = 0; i < 3; i++)
                linearAcc[i] = event.values[i];
        }*/
        else if(event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR)
        {
            if (event.values[0] == 1.0f)
            {
                steps++;
                //updatePosition();
                if(steps % 3 == 0)
                {
                    steps = 0;
                    synchronized (lockObj)
                    {
                        lockObj.notify();
                    }
                }
            }
        }

        //float[] I = new float[9];
        float[] orientation = new float[3];
        if(rotationMatrix != null)
        {
            SensorManager.getOrientation(rotationMatrix, orientation);
            azimuth = (float) Math.toDegrees(orientation[0]);
            //azimuth = (azimuth + 360) % 360;
            pitch = orientation[1] + Math.PI;
            roll = orientation[2] + Math.PI;
            //Log.d("Orientation", Double.toString(azimuth) + " " + Double.toString(Math.toDegrees(pitch)) + " " + Double.toString(Math.toDegrees(roll)));
        }
        /*if (gravity != null && magnetic != null) {
            if (SensorManager.getRotationMatrix(R, I, gravity, magnetic)) {
                SensorManager.getOrientation(R, orientation);
                azimuth = orientation[0] + Math.PI;
                pitch = orientation[1] + Math.PI;
                roll = orientation[2] + Math.PI;
                Log.d("Orientation", Double.toString(Math.toDegrees(azimuth)) + " " + Double.toString(Math.toDegrees(pitch)) + " " + Double.toString(Math.toDegrees(roll)));
            }
        }*/
        //Log.d("Accerometer", Float.toString(x) + " " + Float.toString(y) + " " + Float.toString(z));
    }

    public void setSteps(int steps)
    {
        this.steps = steps;
    }
    public int getSteps()
    {
        return this.steps;
    }
    public double getAngle()
    {
        return this.azimuth;
    }

    public void setPosition(Position position)
    {
        this.position = position;
    }

    private void updatePosition()
    {
        /*if(position != null) {
            double x, y, angle;
            x = position.getX() + STEP_SIZE * Math.sin(Math.toRadians(azimuth));
            y = position.getY() + STEP_SIZE * Math.cos(Math.toRadians(azimuth));
            angle = Math.abs(position.getAngle() - azimuth);
            position.setPosition(x, y, angle);
        }*/
    }

    public double getDistance()
    {
        return STEP_SIZE * (steps * 0.9);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /*static final float ALPHA = 0.15f;

    private float[] lowPass( float[] input, float[] output ) {
        if ( output == null ) return input;

        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }*/

}
