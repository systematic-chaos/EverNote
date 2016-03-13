package corp.katet.evernote.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class FingerPaintView extends View {

    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Path mPath;
    private Paint mDrawPaint, mErasePaint;
    private boolean mIsDrawModeActive;

    public FingerPaintView(Context c) {
        super(c);

        initialize();
    }

    public FingerPaintView(Context context, AttributeSet attrs) {
        super(context, attrs);

        initialize();
    }

    private void initialize() {
        mPath = new Path();

        mDrawPaint = new Paint();
        mDrawPaint.setAntiAlias(true);
        mDrawPaint.setDither(true);
        mDrawPaint.setColor(0x16A600);
        mDrawPaint.setStyle(Paint.Style.STROKE);
        mDrawPaint.setStrokeJoin(Paint.Join.ROUND);
        mDrawPaint.setStrokeCap(Paint.Cap.ROUND);
        mDrawPaint.setStrokeWidth(12);
        mDrawPaint.setMaskFilter(new BlurMaskFilter(8, BlurMaskFilter.Blur.NORMAL));
        mDrawPaint.setMaskFilter(new EmbossMaskFilter(new float[]{1, 1, 1}, 0.4f, 6, 3.5f));
        mDrawPaint.setXfermode(null);
        mDrawPaint.setAlpha(0xFF);

        mErasePaint = new Paint();
        mErasePaint.setAntiAlias(true);
        mErasePaint.setDither(true);
        mErasePaint.setColor(Color.WHITE);
        mErasePaint.setStyle(Paint.Style.STROKE);
        mErasePaint.setStrokeWidth(48);
        mErasePaint.setMaskFilter(null);
        mErasePaint.setXfermode(null);
        mErasePaint.setAlpha(0xFF);

        setDrawModeActive(true);
    }

    public boolean isDrawModeActive() {
        return mIsDrawModeActive;
    }

    public void setDrawModeActive(boolean drawModeActive) {
        mIsDrawModeActive = drawModeActive;
    }

    public boolean toggleDrawModeActive() {
        setDrawModeActive(!mIsDrawModeActive);
        return isDrawModeActive();
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mCanvas.drawColor(Color.WHITE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(0xFF);
        canvas.drawBitmap(mBitmap, 0, 0, mIsDrawModeActive ? mDrawPaint : mErasePaint);
        canvas.drawPath(mPath, mIsDrawModeActive ? mDrawPaint : mErasePaint);
    }

    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 4;

    private void touchStart(float x, float y) {
        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
    }

    private void touchMove(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;
        }
    }

    private void touchUp() {
        mPath.lineTo(mX, mY);

        // Commit the path to our offscreen
        mCanvas.drawPath(mPath, mIsDrawModeActive ? mDrawPaint : mErasePaint);

        // Kill this so we don't double draw
        mPath.reset();
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStart(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touchMove(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touchUp();
                invalidate();
                break;
        }
        return true;
    }
}
