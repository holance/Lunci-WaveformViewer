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

import java.util.ArrayList;
import java.util.List;

import org.lunci.waveform_viewer.BuildConfig;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class WaveformGridView extends View {
    private static final String TAG = WaveformGridView.class.getSimpleName();
    public static final int MESSAGE_SET_CONFIG = 1;
    public static final int MESSAGE_SET_WIDTH_HEIGHT = 2;
    public static final int MESSAGE_CLEAR_GRID = 3;
    public static final int MESSAGE_MOVE_VERTICAL = 4;
    public static final int MESSAGE_MOVE_HORIZONTAL = 5;
    public static final int MESSAGE_DRAW_GRID = 6;
    private Bitmap mBitmap;
    private final Canvas mCanvas = new Canvas();
    private final Handler mHandler;
    private WaveformGridViewConfig mConfig = new WaveformGridViewConfig();
    private final List<Float> mAxisXSet = new ArrayList<Float>();
    private final List<Float> mAxisYSet = new ArrayList<Float>();
    private final Paint mAxisXPaint = new Paint();
    private final Paint mAxisYPaint = new Paint();
    private int mWidth = 0;
    private int mHeight = 0;

    public WaveformGridView(Context context) {
        super(context);
        mWidth = this.getWidth();
        mHeight = this.getHeight();
        initPaints(mConfig);
        initAxises(mConfig);
        mHandler = new Handler(new MainHandlerCallbacks());
        Message.obtain(mHandler, MESSAGE_DRAW_GRID).sendToTarget();
    }

    public WaveformGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mWidth = this.getWidth();
        mHeight = this.getHeight();
        initPaints(mConfig);
        initAxises(mConfig);
        mHandler = new Handler(new MainHandlerCallbacks());
        Message.obtain(mHandler, MESSAGE_DRAW_GRID).sendToTarget();
    }

    public WaveformGridView(Context context, AttributeSet attrs,
                            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mWidth = this.getWidth();
        mHeight = this.getHeight();
        initPaints(mConfig);
        initAxises(mConfig);
        mHandler = new Handler(new MainHandlerCallbacks());

    }

    private void initPaints(WaveformGridViewConfig config) {
        mAxisXPaint.setColor(config.AxisXColor);
        mAxisXPaint.setStyle(Paint.Style.STROKE);
        mAxisXPaint.setAntiAlias(true);
        mAxisXPaint.setStrokeWidth(1);
        mAxisYPaint.setColor(config.AxisYColor);
        mAxisYPaint.setStyle(Paint.Style.STROKE);
        mAxisYPaint.setAntiAlias(true);
        mAxisYPaint.setStrokeWidth(1);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if(!isInEditMode()) {
            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
            mCanvas.setBitmap(mBitmap);
            mCanvas.drawColor(mConfig.BackgroundColor);
            mWidth = w;
            mHeight = h;
            initAxises(mConfig);
        }
        super.onSizeChanged(w, h, oldw, oldh);
        if (mHandler != null)
            mHandler.sendEmptyMessage(MESSAGE_DRAW_GRID);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Message.obtain(mHandler, MESSAGE_DRAW_GRID).sendToTarget();
    }

    @Override
    public void onDetachedFromWindow() {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "surfaceDestroyed");
        }
        mHandler.removeMessages(MESSAGE_DRAW_GRID);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawGrid(canvas);
    }

    private final class MainHandlerCallbacks implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            boolean succ = true;
            switch (msg.what) {
                case MESSAGE_SET_CONFIG:
                    final WaveformGridViewConfig config = (WaveformGridViewConfig) msg.obj;
                    if (config != null)
                        setConfigSafe(config);
                    else
                        succ = false;
                    break;
                case MESSAGE_DRAW_GRID:
                    invalidate();
                    break;
                default:
                    succ = false;
                    break;
            }
            return succ;
        }
    }

    private boolean drawGrid(Canvas canvas) {
        synchronized (this) {
            if (mBitmap != null) {
                try {
                    mCanvas.drawColor(mConfig.BackgroundColor);
                    for (Float f : mAxisXSet) {
                        mCanvas.drawLine(0, f, mWidth, f, mAxisXPaint);
                    }
                    for (Float f : mAxisYSet) {
                        mCanvas.drawLine(f, 0, f, mHeight, mAxisYPaint);
                    }
                } catch (NullPointerException ex) {
                    ex.printStackTrace();
                } finally {
                    if (canvas != null)
                        canvas.drawBitmap(mBitmap, 0, 0, null);
                }
                return true;
            } else
                return false;
        }
    }

    public void setConfig(WaveformGridViewConfig config) {
        Message.obtain(mHandler, MESSAGE_SET_CONFIG).sendToTarget();
    }

    private void setConfigSafe(WaveformGridViewConfig config) {
        initPaints(config);
        initAxises(config);
        config.cloneParams(mConfig);
        Message.obtain(mHandler, MESSAGE_DRAW_GRID).sendToTarget();
    }

    private void initAxises(WaveformGridViewConfig config) {
        mAxisYSet.clear();
        final float delta = mWidth / (config.NumColumns - 1);
        for (int i = 0; i < config.NumColumns; ++i) {
            if (i == 0 && !config.ShowGridBorderLeft)
                continue;
            else if (i == config.NumColumns - 1 && !config.ShowGridBorderRight)
                continue;
            else
                mAxisYSet.add(i * delta);
        }
        mAxisXSet.clear();
        final float delta1 = mHeight / (config.NumRows - 1);
        for (int i = 0; i < config.NumRows; ++i) {
            if (i == 0 && !config.ShowGridBorderTop)
                continue;
            else if (i == config.NumRows - 1 && !config.ShowGridBorderBottom)
                continue;
            else
                mAxisXSet.add(i * delta1);
        }
    }

    public WaveformGridViewConfig getConfig(){
        return mConfig.clone();
    }
}
