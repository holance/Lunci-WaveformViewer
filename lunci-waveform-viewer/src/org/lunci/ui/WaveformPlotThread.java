package org.lunci.ui;

import java.util.concurrent.SynchronousQueue;

import org.lunci.waveform_viewer.BuildConfig;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;

public class WaveformPlotThread extends Thread {
	private static final String TAG = WaveformPlotThread.class.getSimpleName();
	public static final int MESSAGE_SET_CONFIG = 1;
	private final SurfaceHolder holder;
	private boolean stop = false;
	private int mViewHeight = 0;
	private int mViewWidth = 0;
	private float mDeltaX = 4;
	private float mCurrentX = 0;
	private float mCurrentY = 0;
	private final SynchronousQueue<Integer[]> mDataQueue = new SynchronousQueue<Integer[]>();
	private final Rect mClearRect = new Rect();
	private float mClearRectWidthMultiplier = 4;
	private final Paint mLinePaint = new Paint();
	private final Paint mAxisPaint = new Paint();
	private final Paint mBackgroundPaint = new Paint();
	private int mDrawingRate = 50; // Hz
	private boolean isYValid = true;
	private WaveformView.Config mConfig = new WaveformView.Config();
	private float mScaling = 1;

	public WaveformPlotThread(SurfaceHolder surfaceHolder, WaveformView view) {
		holder = surfaceHolder;
		mLinePaint.setColor(view.getLineColor());
		mAxisPaint.setColor(view.getAxisColor());
		mBackgroundPaint.setColor(view.getBackgroundColor());
		updateConfig(view.getConfig());
	}

	public synchronized void updateWidthHeight(int width, int height) {
		mViewHeight = height;
		mViewWidth = width;
		mClearRect.top = 0;
		mClearRect.bottom = height;
		updateScaling(height, mConfig.DataMax, mConfig.DataMin);
	}

	@Override
	public void run() {
		Canvas c;
		boolean valid = true;
		while (!stop) {
			c = null;
			try {
				Integer[] y;
				valid = true;
				try {
					y = mDataQueue.take();
					if (y != null && y.length > 0) {

					} else {
						valid = false;
						isYValid = false;
					}
					mClearRect.left = (int) mCurrentX;
					mClearRect.right = (int) (mCurrentX + mDeltaX
							* mClearRectWidthMultiplier);
					c = holder.lockCanvas(mClearRect);
					synchronized (holder) {
						if (c != null) {
							c.drawRect(mClearRect, mBackgroundPaint);
							if (!isYValid && valid) {
								mCurrentY = y[0];
								isYValid = true;
							}
							if (valid) {
								mCurrentY = PlotPoints(c, mCurrentX, mDeltaX,
										mCurrentY, y);
							}
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
		// final float delta = deltaX;
		final float delta = deltaX / newY.length;
		for (Integer element : newY) {
			final float tempX = lastX + delta;
			final float scaledY = mViewHeight - (element & mConfig.DataMax)
					* mScaling;
			if (BuildConfig.DEBUG) {
				Log.i(TAG, "orgY=" + element + "scaledY=" + scaledY);
			}
			canvas.drawLine(lastX, lastY, tempX, scaledY, mLinePaint);
			lastY = scaledY;
			lastX = tempX;
			// Log.i(TAG, "lastX=" + lastX + "; lastY=" + lastY);
		}
		return lastY;
	}

	public SynchronousQueue<Integer[]> getDataQueue() {
		return mDataQueue;
	}

	/**
	 * @param rate
	 *            drawing frame per second
	 */
	public void setDrawingRate(int rate) {
		mDrawingRate = rate;
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
		updateScaling(mViewHeight, config.DataMax, config.DataMin);
		if (BuildConfig.DEBUG) {
			Log.d(TAG, "updating config, dataMax=" + config.DataMax
					+ "; dataMin=" + config.DataMin);
		}
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
}
