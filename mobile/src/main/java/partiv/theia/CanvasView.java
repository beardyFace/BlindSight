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

// CanvasView represents Theia's debugger used to show details of the functionality of Theia's system
public class CanvasView extends View {

	// the width and height of screen
    public int width;
    public int height;
	
	// current active mode
    private boolean outDoor = true;
	// variables for drawable objects
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Path mPath;
	private Paint mPaint;
	
	// distance from the tagged position
    private float distanceFromTag = 0;
	// offset used to initialise "walking stragiht" as "up" on the debugger 
    private double offset = 0;
	// current azimuth angle
    private double azimuth = 0;
	// bearing from previous tracked position to current position
    private float bearing = 0;
    private float bOffset = 0;
	// tagged position, and the current position
    private float tagX, tagY, currX = 0, currY = 0;
    Context context;
	// list of tracked positions (the path)
    private ArrayList<PointF> tracks = new ArrayList();

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

		// set the initial paint style, stroke and width
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeWidth(8f);

		// draw the tracks as yellow circles
        for(PointF f : tracks)
        {
            mPaint.setColor(Color.YELLOW);
            canvas.drawCircle(f.x, f.y, 15, mPaint);
        }

		// draw the current position as green circles
        mPaint.setColor(Color.GREEN);
        canvas.drawCircle(currX, currY, 15, mPaint);

		// draw the tagged position as a red circle
        mPaint.setColor(Color.RED);
        canvas.drawPath(mPath, mPaint);

		// draw the end position (before returning) as a black circle
        mPaint.setColor(Color.BLACK);
        mPaint.setTextSize(35);

		// draw the texts representing the information about the current state of the system
        canvas.drawText("Outdoor: " + Boolean.toString(outDoor), 10, 150, mPaint);
        canvas.drawText("DistanceFromTag: " + Float.toString(distanceFromTag) + " m", 10, 50, mPaint);
        canvas.drawText("ChangeInBearing: " + Float.toString(bearing) + " degrees", 10, 100, mPaint);
        canvas.drawText("Azimuth: " + Double.toString(azimuth) + " degrees", 10, 200, mPaint);
    }

	// set the mode on the debugger 
    public void setMode(boolean outDoor)
    {
        this.outDoor = outDoor;
    }

	// draw the tag position
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

	// draw the end position
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

	// update the current position on debugger using arguments from the system
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

	// add new tracked position into the debugger
    public void updateTrackLocation()
    {
        tracks.add(new PointF(currX, currY));
        invalidate();
    }

	// update the current position for indoor mode
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

	// constantly update the current position to monitor the outdoor mode results
    public void monitor(float x, float y, double azimuth)
    {
        this.bearing = bearing;
        this.azimuth = (azimuth + offset + 360) % 360;

        currX = tagX - (20 * x);
        currY = tagY - (20 * y);
        distanceFromTag = (float) Math.sqrt(Math.pow(currX - tagX, 2) + Math.pow(currY - tagY, 2) / 20);
        invalidate();
    }

	// remove track on the debugger once the user gets to it 
    public void removeTrack()
    {
        tracks.remove(tracks.size() - 1);
        invalidate();
    }

	// to correct when the user cut corner
    public void overStepCorrection(int x)
    {
        for(int i = x; i < tracks.size(); i++)
        {
            tracks.remove(i);
        }
    }

	// reset the debugger
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