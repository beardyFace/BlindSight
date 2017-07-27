package partiv.theia;


import android.content.Context;
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
    private float[] gravity = new float[3];
    private float[] magnetic = new float[3];
    private float[] linearAcc = new float[3];

    private static final double STEP_SIZE = 0.76;
    private int count;
    private double azimuth_temp, azimuth, pitch, roll;
    private Position position;
    private Object lockObj;

    Sensors(Context context, Object lockObj)
    {
        this.context = context;
        this.lockObj = lockObj;
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR), SensorManager.SENSOR_DELAY_NORMAL);
    }


    public void unRegister()
    {
        mSensorManager.unregisterListener(this);
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            for(int i = 0; i < 3; i++)
                gravity[i] = event.values[i];
        }
        else if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            for(int i = 0; i < 3; i++)
                magnetic[i] = event.values[i];
        }
        else if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION)
        {
            for(int i = 0; i < 3; i++)
                linearAcc[i] = event.values[i];
        }
        else if(event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR)
        {
            if (event.values[0] == 1.0f)
            {
                steps++;
                updatePosition();
                /*if(steps % 5 == 0)
                {*/
                    synchronized (lockObj)
                    {
                        lockObj.notify();
                    }
                //}
            }
        }

        float[] R = new float[9];
        float[] I = new float[9];
        float[] orientation = new float[3];

        if (gravity != null && magnetic != null) {
            if (SensorManager.getRotationMatrix(R, I, gravity, magnetic)) {
                SensorManager.getOrientation(R, orientation);
                azimuth_temp += orientation[0];
                count++;
                if(count == 10)
                {
                    azimuth = (azimuth_temp / count) + Math.PI;
                    azimuth_temp = 0.0;
                    count = 0;
                }
                pitch = orientation[1];
                roll = orientation[2];
                //Log.d("Orientation", Double.toString(azimuth) + " " + Double.toString(pitch) + " " + Double.toString(roll));
            }
        }
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
        if(position != null) {
            double x, y, angle;
            x = position.getX() + STEP_SIZE * Math.sin(azimuth);
            y = position.getY() + STEP_SIZE * Math.cos(azimuth);
            angle = Math.abs(position.getAngle() - azimuth);
            position.setPosition(x, y, angle);
        }
    }

    public double getDistance()
    {
        return STEP_SIZE * (steps * 0.9);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
