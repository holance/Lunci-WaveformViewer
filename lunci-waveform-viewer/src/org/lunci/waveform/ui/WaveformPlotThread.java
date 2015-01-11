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
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
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
	private float mCurrentY = Float.MAX_VALUE;// unit:pixel.
	private float mMaxY, mMinY;
	private BlockingQueue<int[]> mDataQueue;
	private final Rect mClearRect = new Rect();
	private float mClearRectWidthMultiplier = 4;
	private final Paint mLinePaint = new Paint();
	private final Paint mAxisPaint = new Paint();
	private final Paint mBackgroundPaint = new Paint();
	private final Paint mFPSPaint = new Paint();
	private WaveformViewConfig mConfig = new WaveformViewConfig();
	private float mScaling = 1f;
	private boolean mClearScreenFlag = false;
	// private final Path mPathSeg = new Path();
	private final Rect mTextClearRect = new Rect();
	private int mFPS = -1;
	private int mPerfCounter = 0;
	private long mPerfDrawingDelaySum = 0;
	private boolean mOnShowFPS = false;
	private final Rect mFPSBounds = new Rect();

	// private float[] mDrawingPoints;

	public WaveformPlotThread(SurfaceHolder surfaceHolder, WaveformView view) {
		super();
		this.setPriority(Thread.NORM_PRIORITY);
		holder = surfaceHolder;
		mLinePaint.setColor(view.getLineColor());
		mAxisPaint.setColor(view.getAxisColor());
		mBackgroundPaint.setColor(view.getBackgroundColor());
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
		mClearRect.top = 0;
		mClearRect.bottom = height;
		updateScaling(height, mConfig.DataMaxValue, mConfig.DataMinValue);
		mAxialXCoord.y = height / 2;
		mAutoPositionNominalValue = height / 2;
		mMaxY = Float.MIN_VALUE;
		mMinY = Float.MAX_VALUE;
	}

	private void resetAutoPositionParams() {
		mMaxY = Float.MIN_VALUE;
		mMinY = Float.MAX_VALUE;
		mAutoPositionNominalValue = mViewHeight / 2;
	}

	@Override
	public void run() {
		if (BuildConfig.DEBUG) {
			Log.i(TAG, "Plot Thread is running");
		}
		Canvas c;
		long startTime = 0;
		long endTime = 0;
		while (!stop) {
			c = null;
			final float deltaX = mDeltaX * mConfig.HorizontalZoom;
			try {
				if (mOnShowFPS) {
					final String fpsText = String.valueOf(mFPS);
					mFPSPaint.getTextBounds(fpsText, 0, fpsText.length(),
							mFPSBounds);
					mTextClearRect.left = 10;
					mTextClearRect.right = mFPSBounds.width() + 20;
					mTextClearRect.top = 10;
					mTextClearRect.bottom = mFPSBounds.height() + 10;
					c = holder.lockCanvas(mTextClearRect);
					if (c != null) {
						synchronized (holder) {
							c.drawColor(mConfig.BackgroundColor);
							c.drawText(String.valueOf(mFPS),
									mTextClearRect.left, mTextClearRect.bottom,
									mFPSPaint);
						}
					}
					mOnShowFPS = false;
				} else if (mClearScreenFlag) {
					mCurrentX = 0;
					mCurrentY = Float.MAX_VALUE;
					c = holder.lockCanvas(null);
					if (c != null) {
						synchronized (holder) {
							c.drawColor(mConfig.BackgroundColor);
						}
					}
					mClearScreenFlag = false;
					resetAutoPositionParams();
				} else {
					int[] y = mDataQueue.take();
					if (mConfig.ShowFPS) {
						startTime = System.currentTimeMillis();
					}
					mClearRect.left = (int) mCurrentX;
					mClearRect.right = (int) (mCurrentX + deltaX
							* mClearRectWidthMultiplier);
					c = holder.lockCanvas(mClearRect);
					if (c != null) {
						synchronized (holder) {
							c.drawColor(mConfig.BackgroundColor);
							// c.drawRect(mClearRect, mBackgroundPaint);
							mCurrentY = PlotPoints(c, mCurrentX, deltaX,
									mCurrentY, y);
							mCurrentX += deltaX;
							if (mCurrentX >= mViewWidth) {
								mCurrentX = 0;
								mCurrentY = Float.MAX_VALUE;
								if (mConfig.AutoPositionAfterZoom
										&& mMaxY > mMinY) {
									final float tempNominal = (mMaxY - mMinY) / 2;
									mAutoPositionNominalValue = mMaxY
											- tempNominal;
									if (BuildConfig.DEBUG) {
										Log.i(TAG, "autoPositionNominalValue="
												+ mAutoPositionNominalValue
												+ "; tempNominal="
												+ tempNominal + "; maxY="
												+ mMaxY + "; minY=" + mMinY);
									}
									mMaxY = Float.MIN_VALUE;
									mMinY = Float.MAX_VALUE;
								}
							}
						}
					}
					if (mConfig.ShowFPS) {
						endTime = System.currentTimeMillis();
						mPerfDrawingDelaySum += endTime - startTime;
						++mPerfCounter;
						if (mPerfCounter == 50 && mPerfDrawingDelaySum != 0) {
							final int fps = (int) (mPerfCounter / ((float) mPerfDrawingDelaySum / 1000));
							if (mFPS != fps) {
								mFPS = fps;
								mOnShowFPS = true;
							}
							mPerfCounter = 0;
							mPerfDrawingDelaySum = 0;
						}
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
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

	// private float PlotPointsByPath(Canvas canvas, float lastX, float deltaX,
	// float lastY, int[] newY) {
	// final float delta = deltaX / newY.length;
	// final float scale = mScaling;
	// mPathSeg.rewind();
	// mPathSeg.setLastPoint(lastX, lastY);
	// // mPathSeg.moveTo(lastX, lastY);
	// float tempX = lastX;
	// for (int element : newY) {
	// tempX += delta;
	// if (element > mConfig.DataMaxValue
	// || element < mConfig.DataMinValue) {
	// continue;
	// }
	// float scaledY = mViewHeight - element * scale;
	// if (mConfig.ZoomRatio != 1) {
	// float center = 0;
	// if (mConfig.AutoPositionAfterZoom) {
	// center = mAutoPositionNominalValue;
	// if (mMaxY < scaledY) {
	// mMaxY = scaledY;
	// } else if (mMinY > scaledY) {
	// mMinY = scaledY;
	// }
	// } else
	// center = mViewHeight / 2;
	// scaledY = scaledY > center ? (scaledY - center)
	// * mConfig.ZoomRatio + center : center
	// - (center - scaledY) * mConfig.ZoomRatio;
	// }
	// // Log.i(TAG, "orgY=" + element + "; scaledY=" + scaledY);
	// mPathSeg.lineTo(tempX, scaledY);
	// lastY = scaledY;
	// lastX = tempX;
	// }
	// canvas.drawPath(mPathSeg, mLinePaint);
	// return lastY;
	// }

	private float PlotPoints(final Canvas canvas, float lastX, float deltaX,
			float lastY, final int[] newY) {
		final float delta = deltaX / newY.length;
		final float scale = mScaling;
		float tempX = lastX;
		for (int element : newY) {
			tempX += delta;
			if (element > mConfig.DataMaxValue
					|| element < mConfig.DataMinValue) {
				continue;
			}
			float scaledY = mViewHeight - element * scale;
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
			canvas.drawLine(lastX, lastY, tempX, scaledY, mLinePaint);
			lastY = scaledY;
			lastX = tempX;
		}
		return lastY;
	}

	// private float PlotPointsByDrawLines(final Canvas canvas, float lastX,
	// float deltaX, float lastY, final int[] newY) {
	// final float delta = deltaX / newY.length;
	// final float scale = mScaling;
	// float tempX = lastX;
	// if (mDrawingPoints == null || mDrawingPoints.length != newY.length * 4) {
	// mDrawingPoints = new float[newY.length * 4];
	// }
	// int index = 0;
	// for (int element : newY) {
	// tempX += delta;
	// if (element > mConfig.DataMaxValue
	// || element < mConfig.DataMinValue) {
	// continue;
	// }
	// float scaledY = mViewHeight - element * scale;
	// if (mConfig.ZoomRatio != 1) {
	// float center = 0;
	// if (mConfig.AutoPositionAfterZoom) {
	// center = mAutoPositionNominalValue;
	// if (mMaxY < scaledY) {
	// mMaxY = scaledY;
	// } else if (mMinY > scaledY) {
	// mMinY = scaledY;
	// }
	// } else
	// center = mViewHeight / 2;
	// scaledY = scaledY > center ? (scaledY - center)
	// * mConfig.ZoomRatio + center : center
	// - (center - scaledY) * mConfig.ZoomRatio;
	// }
	// // canvas.drawLine(lastX, lastY, tempX, scaledY, mLinePaint);
	// mDrawingPoints[index++] = lastX;
	// mDrawingPoints[index++] = lastY;
	// lastY = scaledY;
	// lastX = tempX;
	// mDrawingPoints[index++] = lastX;
	// mDrawingPoints[index++] = lastY;
	// }
	// canvas.drawLines(mDrawingPoints, mLinePaint);
	// return lastY;
	// }

	public synchronized BlockingQueue<int[]> getDataQueue() {
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
		mConfig = config;
		mDeltaX = mConfig.DrawingDeltaX;
	}

	private void updateScaling(int height, int dataMax, int dataMin) {
		if (dataMax - dataMin == 0)
			return;
		mScaling = (float) height / (dataMax - dataMin);
		clearWaveform();
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

	public void setConfig(WaveformViewConfig config) {
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
