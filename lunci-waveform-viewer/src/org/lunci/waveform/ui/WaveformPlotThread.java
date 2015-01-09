/*
 * Copyright (C) 2014 Lunci Hua
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lunci.waveform.ui;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.lunci.waveform_viewer.BuildConfig;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;

public class WaveformPlotThread extends Thread {
	private static final String TAG = WaveformPlotThread.class.getSimpleName();
	public static final int MESSAGE_SET_CONFIG = 1;
	public static final int MESSAGE_SET_WIDTH_HEIGHT = 2;
	public static final int MESSAGE_CLEAR_WAVEFORM = 3;
	private final SurfaceHolder holder;
	private boolean stop = false;
	private int mViewHeight = 0;// unit:pixel.
	private int mViewWidth = 0;// unit:pixel.
	private final Point mAxialXCoord = new Point(0, 0);
	private float mAutoPositionNominalValue = 0;
	private float mDeltaX = 4;// unit:pixel.
	private float mCurrentX = 0;// unit:pixel.
	private float mCurrentY = 0;// unit:pixel.
	private float mMaxY, mMinY;
	private BlockingQueue<int[]> mDataQueue;
	private final Rect mClearRect = new Rect();
	private float mClearRectWidthMultiplier = 4;
	private final Paint mLinePaint = new Paint();
	private final Paint mAxisPaint = new Paint();
	private final Paint mBackgroundPaint = new Paint();
	private WaveformView.Config mConfig = new WaveformView.Config();
	private Float mScaling = 1f;
	private boolean mClearScreenFlag = false;

	public WaveformPlotThread(SurfaceHolder surfaceHolder, WaveformView view) {
		holder = surfaceHolder;
		mLinePaint.setColor(view.getLineColor());
		mAxisPaint.setColor(view.getAxisColor());
		mBackgroundPaint.setColor(view.getBackgroundColor());
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
		mClearRect.top = 0;
		mClearRect.bottom = height;
		updateScaling(height, mConfig.DataMaxValue, mConfig.DataMinValue);
		mAxialXCoord.y = height / 2;
		mAutoPositionNominalValue = height / 2;
		mMaxY = Float.MIN_VALUE;
		mMinY = Float.MAX_VALUE;
	}

	@Override
	public void run() {
		if (BuildConfig.DEBUG) {
			Log.i(TAG, "Plot Thread is running");
		}
		Canvas c;
		while (!stop) {
			c = null;
			try {
				int[] y;
				try {
					y = mDataQueue.take();
					if (mClearScreenFlag) {
						mCurrentX = 0;
						mCurrentY = mAxialXCoord.y;
						c = holder.lockCanvas(null);
						synchronized (holder) {
							c.drawColor(mConfig.BackgroundColor);
						}
						mClearScreenFlag = false;
					} else {
						mClearRect.left = (int) mCurrentX;
						mClearRect.right = (int) (mCurrentX + mDeltaX
								* mClearRectWidthMultiplier);
						c = holder.lockCanvas(mClearRect);
						synchronized (holder) {
							if (c != null) {
								c.drawRect(mClearRect, mBackgroundPaint);
								mCurrentY = PlotPoints(c, mCurrentX, mDeltaX,
										mCurrentY, y);
								mCurrentX += mDeltaX;
								if (mConfig.AutoPositionAfterZoom) {
									if (mMaxY < mCurrentY) {
										mMaxY = mCurrentY;
									} else if (mMinY > mCurrentY) {
										mMinY = mCurrentY;
									}
								}
								if (mCurrentX >= mViewWidth) {
									mCurrentX = 0;
									if (mConfig.AutoPositionAfterZoom
											&& mMaxY > mMinY) {
										final float tempNominal = (mMaxY - mMinY) / 2;
										mCurrentY = mCurrentY
												- mAutoPositionNominalValue
												+ tempNominal;
										mAutoPositionNominalValue = tempNominal;
									}
								}
							}
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} finally {
				if (c != null) {
					holder.unlockCanvasAndPost(c);
				}
			}
		}
	}

	@Override
	public void interrupt() {
		stop = true;
		super.interrupt();
	}

	public void setClearRectWidthMultiplier(float multiplier) {
		mClearRectWidthMultiplier = multiplier;
	}

	private float PlotPoints(Canvas canvas, float lastX, float deltaX,
			float lastY, int[] newY) {
		final float delta = deltaX / newY.length;
		final float scale = mScaling;
		for (int element : newY) {
			final float tempX = lastX + delta;
			float scaledY = 0;
			scaledY = mViewHeight - (element & mConfig.DataMaxValue) * scale;
			if (mConfig.ZoomRatio != 1) {
				float center = mAutoPositionNominalValue;
				scaledY = scaledY > center ? (scaledY - center)
						* mConfig.ZoomRatio + center : center
						- (center - scaledY) * mConfig.ZoomRatio;
			}
			canvas.drawLine(lastX, lastY, tempX, scaledY, mLinePaint);
			lastY = scaledY;
			lastX = tempX;
		}
		return lastY;
	}

	public BlockingQueue<int[]> getDataQueue() {
		return mDataQueue;
	}

	public synchronized void setDataQueue(BlockingQueue<int[]> dataQueue) {
		mDataQueue = dataQueue;
	}

	/**
	 * @param delta
	 *            pixel per draw
	 */
	public void setDrawingDeltaX(int delta) {
		mDeltaX = delta;
	}

	private void updateConfig(WaveformView.Config config) {
		updateScaling(mViewHeight, config.DataMaxValue, config.DataMinValue);
		if (BuildConfig.DEBUG) {
			Log.d(TAG, "updating config, dataMax=" + config.DataMaxValue
					+ "; dataMin=" + config.DataMinValue);
		}
		this.setPriority(config.PlotThreadPriority);
		if (mDataQueue == null
				|| mConfig == null
				|| mConfig.DefaultDataBufferSize != config.DefaultDataBufferSize)
			mDataQueue = new ArrayBlockingQueue<int[]>(
					config.DefaultDataBufferSize);
			mConfig = config;
	}

	private void updateScaling(int height, int dataMax, int dataMin)
			throws ArithmeticException {
		synchronized (mScaling) {
			mScaling = (float) height / (dataMax - dataMin);
			clearWaveform();
			if (BuildConfig.DEBUG) {
				Log.d(TAG, "updating scaling:" + mScaling + "; height="
						+ height);
			}
		}

	}

	private final Handler mHandler = new Handler(new Handler.Callback() {

		@Override
		public boolean handleMessage(Message msg) {
			boolean result = true;
			switch (msg.what) {
			case MESSAGE_SET_CONFIG:
				final WaveformView.Config config = (WaveformView.Config) msg.obj;
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
			default:
				result = false;
				break;
			}
			return result;
		}
	});

	public Handler getHandler() {
		return mHandler;
	}

	public void setConfig(WaveformView.Config config) {
		if (this.isAlive()) {
			if (BuildConfig.DEBUG) {
				Log.i(TAG, "setConfig async");
			}
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
}
