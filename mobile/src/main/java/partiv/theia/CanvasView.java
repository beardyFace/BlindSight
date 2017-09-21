package partiv.theia;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

public class CanvasView extends View {

    public int width;
    public int height;
    private boolean outDoor = true;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Path mPath;
    private float distanceFromTag = 0;
    private double offset = 0;
    private double azimuth = 0;
    private float bearing = 0;
    private float tagX, tagY, currX = 0, currY = 0;
    Context context;
    private ArrayList<PointF> tracks = new ArrayList();
    private Paint mPaint;

    public CanvasView(Context c, AttributeSet attrs) {
        super(c, attrs);
        context = c;
        mPaint = new Paint();
        mPath = new Path();
        mPaint.setAntiAlias(true);
    }

    // override onDraw
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeWidth(8f);

        for(PointF f : tracks)
        {
            mPaint.setColor(Color.YELLOW);
            canvas.drawCircle(f.x, f.y, 15, mPaint);
        }

        mPaint.setColor(Color.GREEN);
        canvas.drawCircle(currX, currY, 15, mPaint);

        mPaint.setColor(Color.RED);
        canvas.drawPath(mPath, mPaint);

        mPaint.setColor(Color.BLACK);
        mPaint.setTextSize(35);

        canvas.drawText("Outdoor: " + Boolean.toString(outDoor), 10, 150, mPaint);
        canvas.drawText("DistanceFromTag: " + Float.toString(distanceFromTag) + " m", 10, 50, mPaint);
        canvas.drawText("ChangeInBearing: " + Float.toString(bearing) + " degrees", 10, 100, mPaint);
        canvas.drawText("Azimuth: " + Double.toString(azimuth) + " degrees", 10, 200, mPaint);
    }

    public void setMode(boolean outDoor)
    {
        this.outDoor = outDoor;
    }

    public void drawTag(double azimuth)
    {
        mPath.reset();
        mPath = new Path();
        width = this.getWidth();
        height = this.getHeight();
        mPaint.setColor(Color.RED);
        tagX = width/2;
        currX = tagX;
        tagY = height/2;
        currY = tagY;
        this.azimuth = 180;
        this.offset = (180 - azimuth + 360) % 360;
        mPath.moveTo(tagX, tagY);
        mPath.addCircle(tagX, tagY, 15, Path.Direction.CW);
        invalidate();
    }

    public void drawRet(float distance, float bearing, double azimuth)
    {
        this.bearing = bearing;
        this.azimuth = (azimuth + offset + 360) % 360;
        float dx = (float) Math.sin(Math.toRadians(this.azimuth)) * distance;
        float dy = (float) Math.cos(Math.toRadians(this.azimuth)) * distance;

        dx = tagX + (dx * 20);
        dy = tagY + (dy * 20);
        mPath.addCircle(dx, dy, 15, Path.Direction.CW);
        invalidate();
    }

    public void updatesLocation(float distance, float bearing, double azimuth) {
        this.bearing = bearing;
        this.azimuth = (azimuth + offset + 360) % 360;
        float dx = (float) Math.sin(Math.toRadians(this.azimuth)) * distance;
        float dy = (float) Math.cos(Math.toRadians(this.azimuth)) * distance;

        currX += 20 * dx;
        currY += 20 * dy;

        distanceFromTag = (float) Math.sqrt(Math.pow(currX - tagX, 2) + Math.pow(currY - tagY, 2)) / 20;

        invalidate();
    }

    public void updateTrackLocation()
    {
        tracks.add(new PointF(currX, currY));
        invalidate();
    }

    public void updateIndoor(float x, float y, double azimuth)
    {
        this.bearing = bearing;
        this.azimuth = (azimuth + offset + 360) % 360;

        currX = tagX - (20 * x);
        currY = tagY - (20 * y);
        tracks.add(new PointF(currX, currY));
        distanceFromTag = (float) Math.sqrt(Math.pow(currX - tagX, 2) + Math.pow(currY - tagY, 2) / 20);
        invalidate();
    }

    public void monitor(float x, float y, double azimuth)
    {
        this.bearing = bearing;
        this.azimuth = (azimuth + offset + 360) % 360;

        currX = tagX - (20 * x);
        currY = tagY - (20 * y);
        distanceFromTag = (float) Math.sqrt(Math.pow(currX - tagX, 2) + Math.pow(currY - tagY, 2) / 20);
        invalidate();
    }

    public void removeTrack()
    {
        tracks.remove(tracks.size() - 1);
        invalidate();
    }

    public void overStepCorrection(int x)
    {
        for(int i = x; i < tracks.size(); i++)
        {
            tracks.remove(i);
        }
    }

    public void reset()
    {
        azimuth = 0;
        bearing = 0;
        currX = 0;
        currY = 0;
        distanceFromTag = 0;
        mPath.reset();
        tracks.clear();
        invalidate();
    }
}