package partiv.theia;


import android.content.Context;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class Sensors implements SensorEventListener//, StepListener
{

    private SensorManager mSensorManager;
    private final Context context;
    private StepDetector simpleStepDetector;
    private int steps = 0;
    private boolean track = false;
    //private float[] gravity = new float[3];
    private float[] rotationMatrix = new float[16];
    //private float[] magnetic = new float[3];
    //private float[] linearAcc = new float[3];

    public static final double STEP_SIZE = 0.76;
    private double azimuth, pitch, roll;
    private Position position;
    private Object lockObj;

    Sensors(Context context, Object lockObj)
    {
        this.context = context;
        this.lockObj = lockObj;
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        //mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_NORMAL);
        //mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
        //mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
        //mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR), SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_FASTEST);
        /*simpleStepDetector = new StepDetector();
        simpleStepDetector.registerListener(this);*/
    }


    public boolean getTrack()
    {
        return this.track;
    }

    public void setTrack(boolean track)
    {
        this.track = track;
    }

    public void unRegister()
    {
        mSensorManager.unregisterListener(this);
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR)
        {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
        }
        /*else if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
        {
            simpleStepDetector.updateAccel(
                    event.timestamp, event.values[0], event.values[1], event.values[2]);
        }*/
        else if(event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR)
        {
            if (event.values[0] == 1.0f)
            {
                steps++;
                updatePosition();
                if(steps % 3 == 0)
                {
                    track = true;
                    steps = 0;
                    synchronized (lockObj)
                    {
                        lockObj.notify();
                    }
                }
            }
        }

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
            float x, y;
            double angle;
            x = (float) (position.getPosition().x + STEP_SIZE * Math.sin(Math.toRadians((azimuth + 360) % 360)));
            y = (float) (position.getPosition().y + STEP_SIZE * Math.cos(Math.toRadians((azimuth + 360) % 360)));
            PointF p = new PointF(x, y);
            angle = Math.abs(position.getAngle() - azimuth);
            position.setPosition(p, angle);
        }
    }

    public double getDistance()
    {
        return STEP_SIZE * (steps * 0.76);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /*@Override
    public void step(long timeNs) {
        steps++;
        updatePosition();
        if(steps % 3 == 0)
        {
            track = true;
            steps = 0;
            synchronized (lockObj)
            {
                lockObj.notify();
            }
        }
    }*/

}
