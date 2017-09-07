package partiv.theia;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

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

        mPaint.setColor(Color.RED);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeWidth(8f);
        canvas.drawPath(mPath, mPaint);

        mPaint.setColor(Color.GREEN);
        canvas.drawCircle(currX, currY, 15, mPaint);

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
        this.offset = 180 - (azimuth + 360) % 360;
        mPath.moveTo(tagX, tagY);
        mPath.addCircle(tagX, tagY, 15, Path.Direction.CW);
        invalidate();
    }

    public void drawRet(float distance, float bearing, double azimuth)
    {
        this.bearing = (bearing + 360) % 360;
        this.azimuth = (azimuth + 360 + offset) % 360;
        float dx = (float) Math.sin(Math.toRadians(this.azimuth)) * distance;
        float dy = (float) Math.cos(Math.toRadians(this.azimuth)) * distance;

        dx = tagX + (dx * 10);
        dy = tagY + (dy * 10);
        mPath.addCircle(dx, dy, 15, Path.Direction.CW);
        invalidate();
    }

    public void updatesLocation(float distance, float bearing, double azimuth) {
        this.bearing = (bearing + 360) % 360;
        this.azimuth = (azimuth + 360 + offset) % 360;
        float dx = (float) Math.sin(Math.toRadians(this.azimuth)) * distance;
        float dy = (float) Math.cos(Math.toRadians(this.azimuth)) * distance;

        currX += 10 * dx;
        currY += 10 * dy;

        distanceFromTag = (float) Math.sqrt(Math.pow(currX - tagX, 2) + Math.pow(currY - tagY, 2)) / 10;
        invalidate();
    }
    /*private void drawArrowHead(Canvas canvas, Point tip, Point tail)
    {
        double dy = tip.y - tail.y;
        double dx = tip.x - tail.x;
        double theta = Math.atan2(dy, dx);
        int tempX = tip.x ,tempY = tip.y;
        //make arrow touch the circle
        if(tip.x>tail.x && tip.y==tail.y)
        {
            tempX = (tip.x-10);
        }
        else if(tip.x<tail.x && tip.y==tail.y)
        {
            tempX = (tip.x+10);
        }
        else if(tip.y>tail.y && tip.x==tail.x)
        {
            tempY = (tip.y-10);
        }
        else if(tip.y<tail.y && tip.x==tail.x)
        {
            tempY = (tip.y+10);
        }
        else if(tip.x>tail.x || tip.x<tail.x)
        {
            int rCosTheta = (int) ((10)*Math.cos(theta)) ;
            int xx = tip.x - rCosTheta;
            int yy = (int) ((xx-tip.x)*(dy/dx) + tip.y);
            tempX = xx;
            tempY = yy;
        }


        double x, y, rho = theta + phi;
        for(int j = 0; j < 2; j++)
        {
            x = tempX - arrowLength * Math.cos(rho);
            y = tempY - arrowLength  * Math.sin(rho);

            canvas.drawLine(tempX,tempY,(int)x,(int)y,this.paint);
            rho = theta - phi;
        }
    }*/
}