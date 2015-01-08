package org.lunci.ui;


import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class WaveformView extends SurfaceView implements SurfaceHolder.Callback {

	private WaveformPlotThread mPlotThread;

	// plot area size
	private int mWidth = 320;
	private int mHeight = 240;
	private Config mConfig = new Config();

	public WaveformView(Context context, AttributeSet attrs) {
		super(context, attrs);
		getHolder().addCallback(this);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		mPlotThread.interrupt();
		try {
			mPlotThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		mPlotThread = new WaveformPlotThread(getHolder(), this);
		mPlotThread.updateWidthHeight(width, height);
		mWidth = width;
		mHeight = height;
		mPlotThread.start();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mPlotThread = new WaveformPlotThread(getHolder(), this);
		mPlotThread.updateWidthHeight(this.getWidth(), this.getHeight());
		mPlotThread.start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mPlotThread.interrupt();
		try {
			mPlotThread.join();
		} catch (InterruptedException e) {

		}
	}

	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
	}

	public void setLineColor(int color) {
		mConfig.LineColor = color;
	}

	public int getLineColor() {
		return mConfig.LineColor;
	}

	public void setAxisColor(int color) {
		mConfig.AxisColor = color;
	}

	public int getAxisColor() {
		return mConfig.AxisColor;
	}

	@Override
	public void setBackgroundColor(int color) {
		mConfig.BackgroundColor = color;
	}

	public int getBackgroundColor() {
		return mConfig.BackgroundColor;
	}

	public void putData(Integer[] dataValue) {
		mPlotThread.getDataQueue().offer(dataValue);
	}

	public int getmDataMax() {
		return mConfig.DataMax;
	}

	public void setmDataMax(int mDataMax) {
		mConfig.DataMax = mDataMax;
	}

	public int getmDataMin() {
		return mConfig.DataMin;
	}

	public void setmDataMin(int mDataMin) {
		mConfig.DataMin = mDataMin;
	}

	public void setConfig(Config config) {
		mConfig = config;
		if (mPlotThread != null)
			mPlotThread.setConfig(mConfig);
	}

	public Config getConfig() {
		return mConfig;
	}

	public static class Config {
		public int DataMax = 0;
		public int DataMin = 0;
		public int BackgroundColor = 0xFF000000;
		public int LineColor = 0xFF00FF00;
		public int AxisColor = 0xFFFFFFFF;
	}
}
