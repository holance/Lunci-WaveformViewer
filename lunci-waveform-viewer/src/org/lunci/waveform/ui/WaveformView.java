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

import java.util.concurrent.BlockingQueue;

import org.lunci.waveform_viewer.BuildConfig;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class WaveformView extends SurfaceView implements SurfaceHolder.Callback {
	private static final String TAG = WaveformView.class.getSimpleName();
	private WaveformPlotThread mPlotThread;
	private Config mConfig = new Config();

	public WaveformView(Context context, AttributeSet attrs) {
		super(context, attrs);
		getHolder().addCallback(this);
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
		mPlotThread = new WaveformPlotThread(getHolder(), this);
		mPlotThread.setWidthHeight(this.getWidth(), this.getHeight());
		mPlotThread.start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
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

	public void setConfig(Config config) {
		mConfig = config;
		if (mPlotThread != null)
			mPlotThread.setConfig(mConfig);
	}

	public Config getConfig() {
		return mConfig;
	}

	public void setZoomRatio(float ratio) {
		mConfig.ZoomRatio = ratio;
		if (mPlotThread != null) {
			mPlotThread.setConfig(mConfig);
		}
	}

	public static class Config {
		public int DataMaxValue = Integer.MAX_VALUE;
		public int DataMinValue = Integer.MIN_VALUE;
		public int BackgroundColor = 0xFF000000;
		public int LineColor = 0xFF00FF00;
		public int AxisColor = 0xFFFFFFFF;
		public int PlotThreadPriority = Thread.NORM_PRIORITY;
		public float ZoomRatio = 1;
		public boolean AutoPositionAfterZoom = false;
		public int DefaultDataBufferSize = 1000;
		public int DrawingDeltaX = 8;
	}

	public synchronized BlockingQueue<int[]> getDataQueue() {
		if (mPlotThread != null)
			return mPlotThread.getDataQueue();
		else
			return null;
	}
}
