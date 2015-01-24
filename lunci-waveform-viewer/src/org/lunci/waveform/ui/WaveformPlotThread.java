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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;

import org.lunci.waveform_viewer.BuildConfig;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class WaveformPlotThread extends Thread {
    public static final int MESSAGE_SET_CONFIG = 1;
    public static final int MESSAGE_SET_WIDTH_HEIGHT = 2;
    public static final int MESSAGE_CLEAR_WAVEFORM = 3;
    public static final int MESSAGE_MOVE_VERTICAL = 4;
    public static final int MESSAGE_CLEAR_AREA = 5;
    private static final String TAG = WaveformPlotThread.class.getSimpleName();
    private final SurfaceHolder holder;
    private final Point mAxisCoordinate = new Point(0, 0);
    private final Rect mOverheadClearRect = new Rect();
    // Paints
    private final Paint mLinePaint = new Paint();
    private final Paint mAxisPaint = new Paint();
    private final Paint mFPSPaint = new Paint();
    // FPS drawing
    private final Rect mFPSTextClearRect = new Rect();
    private final Rect mFPSBounds = new Rect();
    private boolean stop = false;
    private int mViewHeight = 0;// unit:pixel.
    private int mViewWidth = 0;// unit:pixel.
    private float mAutoPositionNominalValue = 0;
    private float mVerticalMoveOffset = 0;
    private float mDeltaX = 4;// unit:pixel.
    private float mCurrentX = 0;// unit:pixel.
    private float mCurrentY = Float.MAX_VALUE;// unit:pixel.
    private float mMaxY, mMinY;
    private BlockingQueue<int[]> mDataQueue;
    private WaveformViewConfig mConfig = new WaveformViewConfig();
    private float mScaling = 1f;
    private boolean mClearScreenFlag = false;
    private boolean mClearAreaFlag = false;
    private final Rect mClearAreaRect = new Rect();
    private final Handler mHandler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            boolean result = true;
            switch (msg.what) {
                case MESSAGE_SET_CONFIG:
                    final WaveformViewConfig config = (WaveformViewConfig) msg.obj;
                    if (config != null) {
                        updateConfig(config);
                    }
                    break;
                case MESSAGE_SET_WIDTH_HEIGHT:
                    updateWidthHeightSafe(msg.arg1, msg.arg2);
                    break;
                case MESSAGE_CLEAR_WAVEFORM:
                    mClearScreenFlag = true;
                    break;
                case MESSAGE_MOVE_VERTICAL:
                    if (msg.obj != null) {
                        final float offset = (float) msg.obj;
                        mVerticalMoveOffset -= offset;
                        if (BuildConfig.DEBUG) {
                            Log.i(TAG, "move vertical, offset=" + offset
                                    + "; mVerticalMoveOffset="
                                    + mVerticalMoveOffset);
                        }
                    }
                    break;
                case MESSAGE_CLEAR_AREA:
                    if (msg.obj != null) {
                        final Rect rect = (Rect) msg.obj;
                        if (rect != null) {
                            mClearAreaRect.left = rect.left;
                            mClearAreaRect.right = rect.right;
                            mClearAreaRect.top = rect.top;
                            mClearAreaRect.bottom = rect.bottom;
                        }
                    }
                    mClearAreaFlag = true;
                    break;
                default:
                    result = false;
                    break;
            }
            return result;
        }
    });
    private int mFPS = -1;
    private int mPerformanceCounter = 0;
    private long mPerformanceDrawingDelaySum = 0;

    // private float[] mDrawingPoints;
    private boolean mOnShowFPSFlag = false;

    public WaveformPlotThread(SurfaceHolder surfaceHolder, WaveformView view) {
        super();
        this.setPriority(Thread.NORM_PRIORITY);
        holder = surfaceHolder;
        mAxisPaint.setColor(view.getAxisColor());
        mAxisPaint.setStyle(Paint.Style.STROKE);
        mAxisPaint.setAntiAlias(true);
        mAxisPaint.setStrokeWidth(1);
        mLinePaint.setColor(view.getLineColor());
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setAntiAlias(true);
        mLinePaint.setStrokeWidth(1);
        mFPSPaint.setTextSize(20);
        mFPSPaint.setColor(Color.YELLOW);
        mFPSPaint.setTypeface(Typeface.MONOSPACE);
        mFPSPaint.setStyle(Style.FILL);
        updateConfig(view.getConfig());
    }

    public void setWidthHeight(int width, int height) {
        if (this.isAlive()) {
            Message.obtain(mHandler, MESSAGE_SET_WIDTH_HEIGHT, width, height)
                    .sendToTarget();
        } else {
            updateWidthHeightSafe(width, height);
        }
    }

    private void updateWidthHeightSafe(int width, int height) {
        mViewHeight = height;
        mViewWidth = width;
        mOverheadClearRect.top = 0;
        mOverheadClearRect.bottom = height;
        updateScaling(height, mConfig.DataMaxValue, mConfig.DataMinValue);
        mAxisCoordinate.y = height / 2;
        mAutoPositionNominalValue = height / 2;
        mMaxY = Float.MIN_VALUE;
        mMinY = Float.MAX_VALUE;
    }

    private void resetAutoPositionParams() {
        mMaxY = Float.MIN_VALUE;
        mMinY = Float.MAX_VALUE;
        // mAutoPositionNominalValue = mViewHeight / 2;
    }

    @Override
    public void run() {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "Plot Thread is running");
        }
        Canvas canvas;
        long startTime = 0;
        boolean cycleCompleted = true;
        int remainIndex = 0;
        int[] y = null;
        while (!stop) {
            canvas = null;
            final float deltaX = mDeltaX * mConfig.HorizontalZoom;
            try {
                if (mOnShowFPSFlag) {
                    final String fpsText = String.valueOf(mFPS);
                    mFPSPaint.getTextBounds(fpsText, 0, fpsText.length(),
                            mFPSBounds);
                    mFPSTextClearRect.left = 10;
                    mFPSTextClearRect.right = mFPSBounds.width() + 20;
                    mFPSTextClearRect.top = 10;
                    mFPSTextClearRect.bottom = mFPSBounds.height() + 10;
                    canvas = holder.lockCanvas(mFPSTextClearRect);
                    if (canvas != null) {
                        synchronized (holder) {
                            canvas.drawColor(Color.TRANSPARENT,
                                    PorterDuff.Mode.CLEAR);
                            canvas.drawText(String.valueOf(mFPS),
                                    mFPSTextClearRect.left,
                                    mFPSTextClearRect.bottom, mFPSPaint);
                        }
                    }
                    mOnShowFPSFlag = false;
                } else if (mClearScreenFlag) {
                    mCurrentX = mConfig.PaddingLeft;
                    mCurrentY = Float.MAX_VALUE;
                    canvas = holder.lockCanvas(null);
                    if (canvas != null) {
                        synchronized (holder) {
                            canvas.drawColor(Color.TRANSPARENT,
                                    PorterDuff.Mode.CLEAR);
                            if (mConfig.ShowCenterLineY) {
                                canvas.drawLine(mConfig.PaddingLeft,
                                        mAxisCoordinate.y, mViewWidth,
                                        mAxisCoordinate.y, mAxisPaint);
                            }
                        }
                    }
                    mClearScreenFlag = false;
                    resetAutoPositionParams();
                } else if (mClearAreaFlag) {
                    if (BuildConfig.DEBUG) {
                        Log.i(TAG, "clear area:" + mClearAreaRect);
                    }
                    canvas = holder.lockCanvas(mClearAreaRect);
                    if (canvas != null) {
                        synchronized (holder) {
                            canvas.drawColor(Color.TRANSPARENT,
                                    PorterDuff.Mode.CLEAR);
                        }
                    }
                    mClearAreaFlag = false;
                } else {
                    if (cycleCompleted) {// take new y set if cycle is completed
                        y = mDataQueue.take();
                        if (y.length == 0) continue;
                    }
                    if (mConfig.ShowFPS && mCurrentX <= mFPSTextClearRect.right) {
                        mOnShowFPSFlag = true;
                    }
                    if (mConfig.ShowFPS) {
                        startTime = System.currentTimeMillis();
                    }
                    mOverheadClearRect.left = (int) mCurrentX;
                    mOverheadClearRect.right = (int) (mCurrentX + deltaX + mConfig.LeadingClearWidth);
                    canvas = holder.lockCanvas(mOverheadClearRect);
                    if (canvas != null) {
                        synchronized (holder) {
                            canvas.drawColor(Color.TRANSPARENT,
                                    PorterDuff.Mode.CLEAR);
                            if (mConfig.ShowCenterLineY) {
                                canvas.drawLine(mOverheadClearRect.left,
                                        mAxisCoordinate.y
                                                + mVerticalMoveOffset,
                                        mOverheadClearRect.right,
                                        mAxisCoordinate.y
                                                + mVerticalMoveOffset,
                                        mAxisPaint);
                            }
                            remainIndex = PlotPoints(canvas, deltaX, y,
                                    remainIndex);
                            cycleCompleted = remainIndex == 0 ? true : false;// reach
                            // end
                            // of
                            // view,
                            // cycle
                            // is
                            // not
                            // completed
                            // yet.
                        }
                    }
                    if (mConfig.ShowFPS) {
                        mPerformanceDrawingDelaySum += System
                                .currentTimeMillis() - startTime;
                        ++mPerformanceCounter;
                        if (mPerformanceCounter == 50
                                && mPerformanceDrawingDelaySum != 0) {
                            final int fps = (int) (mPerformanceCounter / ((float) mPerformanceDrawingDelaySum / 1000));
                            if (mFPS != fps) {
                                mFPS = fps;
                                mOnShowFPSFlag = true;
                            }
                            mPerformanceCounter = 0;
                            mPerformanceDrawingDelaySum = 0;
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    @Override
    public void interrupt() {
        stop = true;
        super.interrupt();
    }

    private int PlotPoints(final Canvas canvas, float deltaX, final int[] newY,
                           final int startIndex) {
        final float delta = deltaX / newY.length;
        final float scale = mScaling;
        float tempX = mCurrentX;
        int index = startIndex;
        int element = 0;
        for (; index < newY.length; ++index) {
            tempX += delta;
            element = newY[index];
            // if (element > mConfig.DataMaxValue
            // || element < mConfig.DataMinValue) {
            // continue;
            // }
            float scaledY = mViewHeight - element * scale;
            scaledY += mVerticalMoveOffset;
            if (mConfig.VerticalZoom != 1) {
                float center = 0;
                if (mConfig.AutoPositionAfterZoom) {
                    center = mAutoPositionNominalValue;
                    if (mMaxY < scaledY) {
                        mMaxY = scaledY;
                    } else if (mMinY > scaledY) {
                        mMinY = scaledY;
                    }
                } else
                    center = mViewHeight / 2;
                scaledY = scaledY > center ? (scaledY - center)
                        * mConfig.VerticalZoom + center : center
                        - (center - scaledY) * mConfig.VerticalZoom;
            }

            if (scaledY >= 0 && scaledY <= mViewHeight) {
                canvas.drawLine(mCurrentX, mCurrentY, tempX, scaledY, mLinePaint);
            }
            mCurrentY = scaledY;
            mCurrentX = tempX;
            if (mCurrentX >= mViewWidth) {
                mCurrentX = mConfig.PaddingLeft;
                mCurrentY = Float.MAX_VALUE;
                if (mConfig.AutoPositionAfterZoom && mMaxY >= mMinY) {
                    final float tempNominal = (mMaxY - mMinY) / 2;
                    mAutoPositionNominalValue = mMaxY - tempNominal;
                    if (BuildConfig.DEBUG) {
                        Log.i(TAG, "autoPositionNominalValue="
                                + mAutoPositionNominalValue + "; tempNominal="
                                + tempNominal + "; maxY=" + mMaxY + "; minY="
                                + mMinY);
                    }
                    mMaxY = Float.MIN_VALUE;
                    mMinY = Float.MAX_VALUE;
                }
                return index;
            }
        }
        return 0;
    }

    public synchronized BlockingQueue<int[]> getDataQueue() {
        return mDataQueue;
    }

    public synchronized void setDataQueue(BlockingQueue<int[]> dataQueue) {
        mDataQueue = dataQueue;
    }

    /**
     * @param delta pixel per draw
     */
    public void setDrawingDeltaX(int delta) {
        mDeltaX = delta;
    }

    private void updateConfig(WaveformViewConfig config) {
        updateScaling(mViewHeight, config.DataMaxValue, config.DataMinValue);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "updating config, dataMax=" + config.DataMaxValue
                    + "; dataMin=" + config.DataMinValue);
        }
        this.setPriority(config.PlotThreadPriority);
        if (mDataQueue == null
                || mConfig == null
                || mConfig.DefaultDataBufferSize != config.DefaultDataBufferSize) {
            mDataQueue = new ArrayBlockingQueue<int[]>(
                    config.DefaultDataBufferSize);
        }
        if (mConfig.PaddingLeft < config.PaddingLeft) {
            mClearAreaRect.left = 0;
            mClearAreaRect.right = config.PaddingLeft;
            mClearAreaRect.top = 0;
            mClearAreaRect.bottom = mViewHeight;
            mClearAreaFlag = true;
            if (mCurrentX < config.PaddingLeft)
                mCurrentX = config.PaddingLeft;
        }
        mDeltaX = config.DrawingDeltaX;
        mAxisPaint.setColor(config.AxisColor);
        mLinePaint.setColor(config.LineColor);
        config.cloneParams(mConfig);
    }

    private void updateScaling(int height, int dataMax, int dataMin) {
        if (dataMax - dataMin == 0)
            return;
        mScaling = (float) height / (dataMax - dataMin);
        // clearWaveform();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "updating scaling:" + mScaling + "; height=" + height);
        }
    }

    public Handler getHandler() {
        return mHandler;
    }

    public void setConfig(WaveformViewConfig config) {
        if (this.isAlive()) {
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "setConfig async");
            }
            mHandler.removeMessages(MESSAGE_SET_CONFIG);
            Message.obtain(mHandler, MESSAGE_SET_CONFIG, 0, 0, config)
                    .sendToTarget();
        } else {
            updateConfig(config);
        }
    }

    public void clearWaveform() {
        if (this.isAlive()) {
            Message.obtain(mHandler, MESSAGE_CLEAR_WAVEFORM).sendToTarget();
        }
    }

    public void moveVertical(float y) {
        if (this.isAlive()) {
            Message.obtain(mHandler, MESSAGE_MOVE_VERTICAL, y).sendToTarget();
        }
    }
}
