package com.sec.kbssm.happystream;

import android.content.Context;
        import android.graphics.Canvas;
        import android.graphics.Color;
        import android.graphics.Paint;
        import android.graphics.RectF;
import android.util.Log;
import android.view.View;

public class DrawGraph extends View {
    private Paint mPaints, mPaints2;
    private Paint mFramePaint;
    private boolean[] mUseCenters;
    private RectF[] mOvals;
    private RectF mBigOval;
    private RectF mBigOval2;
    private RectF mBigOval3;
    private RectF mBackBigOval;
    private RectF mBackBigOval2;
    //private RectF mBackBigOval3;
    private float mStart;
    private float mStart2;
    private float mSweep;
    private float mSweep2;
    private int mBigIndex;
    private static final float SWEEP_INC = 1;
    private static final float START_INC = 0;
    private int cache_dir;
    private int cache_total;


    public DrawGraph(Context context) {
        super(context);

        mPaints = new Paint();
        mPaints2 = new Paint();
        mUseCenters = new boolean[4];
        mOvals = new RectF[4];
        cache_dir = 0;
        cache_total = 0;

        mBigOval = new RectF(120, 150, 520, 550);//400
        mBackBigOval = new RectF(120, 150, 520, 550);//움직이는 그래프값
        mBigOval2 = new RectF(520, 570, 690, 740);//140크기
        mBackBigOval2 = new RectF(520, 570, 690, 740);//움직이는 그래프값
        mBigOval3 = new RectF(80, 110, 560, 590);


        mPaints = new Paint(mPaints);
        mPaints.setStyle(Paint.Style.STROKE);
        mPaints.setStrokeWidth(20);
        mPaints.setColor(Color.rgb(255, 255, 255));
        mPaints.setAntiAlias(true);
        mPaints.setStrokeCap(Paint.Cap.BUTT);
        mPaints2 = new Paint(mPaints2);

    }

    private void drawArcs(Canvas canvas, RectF oval, boolean useCenter,
                          Paint paint, float mSweep) {
        canvas.drawArc(oval, 130, mSweep, useCenter, paint);
    }//-90

    private void drawBackground(Canvas canvas){

        //큰 그래프
        mPaints.setStrokeWidth(28);
        mPaints.setColor(Color.argb(106 ,200, 200, 200));
        canvas.drawArc(mBackBigOval, 130, 280, mUseCenters[2], mPaints);

        //작은 그래프
        mPaints.setStrokeWidth(17);
        canvas.drawArc(mBackBigOval2, 130, 280, mUseCenters[2], mPaints);

        //큰 그래프 테두리
        mPaints.setStrokeWidth(2);
        mPaints.setColor(Color.rgb(255, 255, 255));
        canvas.drawArc(mBigOval3, 130, 280, mUseCenters[2], mPaints);

    }
    @Override protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.alpha(Color.GRAY));
        //그래프 바탕
        drawBackground(canvas);

        //첫번째 움직이는 그래프값
        mPaints.setColor(Color.rgb(255, 255, 255));
        mPaints.setStrokeWidth(28);
        drawArcs(canvas, mBackBigOval, mUseCenters[2] ,mPaints, mSweep);

        //두번째 움직이는 그래프값
        //drawArcs(canvas, mBigOval, mUseCenters[2],mPaints);
        mPaints.setStrokeWidth(17);

        drawArcs(canvas, mBackBigOval2, mUseCenters[2] ,mPaints, mSweep2);

        if(mSweep <= cache_dir)
            mSweep += SWEEP_INC;

        if(mSweep2 <= cache_total)
            mSweep2 += SWEEP_INC;
        if (mSweep2 > cache_total) {

            mStart2 += START_INC;
            mBigIndex = (mBigIndex) % mOvals.length;
        }
        invalidate();
    }
    void setmSweep(int dir_size){
        mSweep = 0;
        cache_dir = 280 * dir_size / 100;

    }
    void setmSweep2(int total_size){
        mSweep2 = 0;
        cache_total = 280 * total_size / 100;
    }
    public static int getSweepInc() {
        return Float.floatToIntBits(SWEEP_INC);
    }


}