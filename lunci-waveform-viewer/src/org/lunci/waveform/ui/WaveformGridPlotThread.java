package org.lunci.waveform.ui;

import android.view.SurfaceHolder;

public class WaveformGridPlotThread extends Thread {
	private final SurfaceHolder mSurfaceHolder;

	public WaveformGridPlotThread(SurfaceHolder surfaceHolder) {
		mSurfaceHolder = surfaceHolder;
	}
}
