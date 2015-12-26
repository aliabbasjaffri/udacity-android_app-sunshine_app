package com.sunshine;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by aliabbasjaffri on 26/12/15.
 */
public class WindMill extends View
{
    private final String LOG_TAG = getClass().getSimpleName();

    private Paint mWindmillPaint;
    private Paint mArrowPaint;
    private float mSpeed;
    private float mDegrees;
    private float mRotation = 359f;
    private Bitmap mRotor;
    private Bitmap mStand;

    public WindMill(Context context) {
        super(context);
    }

    public WindMill(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WindMill(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void init()
    {
        mWindmillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mWindmillPaint.setStyle(Paint.Style.FILL);
        mArrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mArrowPaint.setStyle(Paint.Style.FILL);
        mArrowPaint.setColor(Color.GRAY);
        mArrowPaint.setStrokeWidth(1f);
        mRotor = BitmapFactory.decodeResource(getResources(), R.drawable.rotor);
        mStand = BitmapFactory.decodeResource(getResources(), R.drawable.windmill);
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int h = 0;
        int w = 0;
        canvas.drawBitmap(mRotor, rotate(mRotor, h, w), mWindmillPaint);
        canvas.drawBitmap(mStand, 0, 30, mWindmillPaint);
        invalidate();
    }

    public Matrix rotate(Bitmap bm, int x, int y){
        Matrix matrix = new Matrix();
        matrix.postRotate(mRotation, bm.getWidth() / 2, bm.getHeight() / 2);
        matrix.postTranslate(x, y);    //The coordinates where we want to put our bitmap
        mRotation -= mSpeed;        //degree of rotation
        return matrix;
    }

    public void setSpeed(float speed) {
        mSpeed = speed;
    }

    public void setDegrees(float degrees) {
        mDegrees = degrees;
    }
}
