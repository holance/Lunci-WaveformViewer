/*
 *  Copyright (C) 2015 Lunci Hua
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 */

package org.lunci.waveform.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import org.lunci.waveform_viewer.BuildConfig;

import java.util.concurrent.BlockingQueue;

public class WaveformView extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = WaveformView.class.getSimpleName();
    private final GestureDetector mGestureDetector;
    private final OnTouchListener mTouchListener = new OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mGestureDetector.onTouchEvent(event);
            return true;
        }
    };
    private final GestureDetector.OnGestureListener mOnGestureListener = new GestureListener();
    private WaveformPlotThread mPlotThread;
    private WaveformViewConfig mConfig = new WaveformViewConfig();

    public WaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if(!this.isInEditMode()) {
            this.setZOrderOnTop(true);
        }
        getHolder().setFormat(PixelFormat.TRANSPARENT);
        getHolder().addCallback(this);
        mGestureDetector = new GestureDetector(this.getContext(),
                mOnGestureListener);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) throws NullPointerException {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "surfaceChanged");
        }
        mPlotThread.setWidthHeight(width, height);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "surfaceCreated");
        }
        this.setOnTouchListener(mTouchListener);
        mPlotThread = new WaveformPlotThread(holder, this);
        mPlotThread.setWidthHeight(this.getWidth(), this.getHeight());
        mPlotThread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        this.setOnTouchListener(null);
        mPlotThread.interrupt();
        try {
            mPlotThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    public int getLineColor() {
        return mConfig.LineColor;
    }

    public void setLineColor(int color) {
        mConfig.LineColor = color;
    }

    public int getAxisColor() {
        return mConfig.AxisColor;
    }

    public void setAxisColor(int color) {
        mConfig.AxisColor = color;
    }

    public void putData(int[] dataValue) {
        if (mPlotThread != null && mPlotThread.isAlive()) {
            try {
                mPlotThread.getDataQueue().put(dataValue);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public int getDataMax() {
        return mConfig.DataMaxValue;
    }

    public void setDataMax(int mDataMax) {
        mConfig.DataMaxValue = mDataMax;
    }

    public int getDataMin() {
        return mConfig.DataMinValue;
    }

    public void setDataMin(int mDataMin) {
        mConfig.DataMinValue = mDataMin;
    }

    public WaveformViewConfig getConfig() {
        return mConfig.clone();
    }

    public void setConfig(WaveformViewConfig config) {
        config.cloneParams(mConfig);
        if (mPlotThread != null)
            mPlotThread.setConfig(mConfig);
    }

    public void setVerticalZoomRatio(float ratio) {
        mConfig.VerticalZoom = ratio;
        if (mPlotThread != null) {
            mPlotThread.setConfig(mConfig);
        }
    }

    public void setHorizontalZoomRatio(float ratio) {
        mConfig.HorizontalZoom = ratio;
        if (mPlotThread != null) {
            mPlotThread.setConfig(mConfig);
        }
    }

    public void setDrawingDeltaX(int delta) {
        mConfig.DrawingDeltaX = delta;
        if (mPlotThread != null) {
            mPlotThread.setConfig(mConfig);
        }
    }

    public void setLeadingClearWidth(int width) {
        mConfig.LeadingClearWidth = width;
        if (mPlotThread != null) {
            mPlotThread.setConfig(mConfig);
        }
    }

    public synchronized BlockingQueue<int[]> getDataQueue() {
        if (mPlotThread != null)
            return mPlotThread.getDataQueue();
        else
            return null;
    }

    public void clearWaveform(){
        if(mPlotThread!=null){
            mPlotThread.clearWaveform();
        }
    }

    private class GestureListener implements GestureDetector.OnGestureListener{

        @Override
        public boolean onDown(MotionEvent e) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
            // TODO Auto-generated method stub

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
        float distanceX, float distanceY) {
            if (mConfig.EnableVerticalGestureMove
                    && e2.getAction() == MotionEvent.ACTION_MOVE) {
                if (mPlotThread != null) {
                    mPlotThread.moveVertical(distanceY
                            * mConfig.VerticalGestureMoveRatio/mConfig.VerticalZoom);
                    return true;
                }
            }
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            // TODO Auto-generated method stub

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
        float velocityY) {
            // TODO Auto-generated method stub
            return false;
        }

    }
}
