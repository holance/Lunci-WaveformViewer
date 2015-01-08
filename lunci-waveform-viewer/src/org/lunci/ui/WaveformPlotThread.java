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

package org.lunci.ui;

import java.util.concurrent.SynchronousQueue;

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
	private float mDeltaX = 4;// unit:pixel.
	private float mCurrentX = 0;// unit:pixel.
	private float mCurrentY = 0;// unit:pixel.
	private final SynchronousQueue<Integer[]> mDataQueue = new SynchronousQueue<Integer[]>();
	private final Rect mClearRect = new Rect();
	private float mClearRectWidthMultiplier = 4;
	private final Paint mLinePaint = new Paint();
	private final Paint mAxisPaint = new Paint();
	private final Paint mBackgroundPaint = new Paint();
	private WaveformView.Config mConfig = new WaveformView.Config();
	private float mScaling = 1;

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
	}

	@Override
	public void run() {
		Canvas c;
		while (!stop) {
			c = null;
			try {
				Integer[] y;
				try {
					y = mDataQueue.take();
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
							if (mCurrentX >= mViewWidth) {
								mCurrentX = 0;
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
			float lastY, Integer[] newY) {
		final float delta = deltaX / newY.length;
		for (Integer element : newY) {
			final float tempX = lastX + delta;
			float scaledY = 0;
			scaledY = mViewHeight - (element & mConfig.DataMaxValue) * mScaling;
			if (mConfig.ZoomRatio != 1) {
				scaledY = scaledY > mAxialXCoord.y ? (scaledY - mAxialXCoord.y)
						* mConfig.ZoomRatio + scaledY : scaledY
						- (mAxialXCoord.y - scaledY) * mConfig.ZoomRatio;
			}
			// if (BuildConfig.DEBUG) {
			// Log.i(TAG, "orgY=" + element + "scaledY=" + scaledY);
			// }
			canvas.drawLine(lastX, lastY, tempX, scaledY, mLinePaint);
			lastY = scaledY;
			lastX = tempX;
		}
		return lastY;
	}

	public SynchronousQueue<Integer[]> getDataQueue() {
		return mDataQueue;
	}

	/**
	 * @param delta
	 *            pixel per draw
	 */
	public void setDrawingDeltaX(int delta) {
		mDeltaX = delta;
	}

	private void updateConfig(WaveformView.Config config) {
		mConfig = config;
		updateScaling(mViewHeight, config.DataMaxValue, config.DataMinValue);
		if (BuildConfig.DEBUG) {
			Log.d(TAG, "updating config, dataMax=" + config.DataMaxValue
					+ "; dataMin=" + config.DataMinValue);
		}
		this.setPriority(config.PlotThreadPriority);
	}

	private void updateScaling(int height, int dataMax, int dataMin)
			throws ArithmeticException {
		mScaling = (float) height / (dataMax - dataMin);
		if (BuildConfig.DEBUG) {
			Log.d(TAG, "updating scaling:" + mScaling + "; height=" + height);
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
				clearWaveformSafe();
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
			Message.obtain(mHandler, MESSAGE_SET_CONFIG, 0, 0, config)
					.sendToTarget();
		} else {
			updateConfig(config);
		}
	}

	private void clearWaveformSafe() {
		if (holder == null)
			return;
		final Canvas c = holder.lockCanvas(null);
		try {
			synchronized (holder) {
				if (c != null) {
					c.drawColor(mConfig.BackgroundColor);
					c.drawRect(mClearRect, mBackgroundPaint);
				}
			}
		} catch (NullPointerException ex) {
			ex.printStackTrace();
		} finally {
			if (c != null) {
				holder.unlockCanvasAndPost(c);
			}
		}
	}

	public void clearWaveform() {
		if (this.isAlive()) {
			Message.obtain(mHandler, MESSAGE_CLEAR_WAVEFORM).sendToTarget();
		}
	}
}
