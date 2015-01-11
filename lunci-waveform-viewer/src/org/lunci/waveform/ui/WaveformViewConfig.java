package org.lunci.waveform.ui;

public class WaveformViewConfig {
	public int DataMaxValue = Integer.MAX_VALUE;
	public int DataMinValue = Integer.MIN_VALUE;
	public int BackgroundColor = 0xFF000000;
	public int LineColor = 0xFF00FF00;
	public int AxialColor = 0xA02196F3;
	public int PlotThreadPriority = Thread.NORM_PRIORITY + 1;
	public float VerticalZoom = 1;
	public float HorizontalZoom = 1;
	public boolean AutoPositionAfterZoom = false;
	public int DefaultDataBufferSize = 1000;
	public int DrawingDeltaX = 8;
	public boolean ShowFPS = true;
	public int LeadingClearWidth = 16;
	public boolean ShowAxialX = true;
	public int PaddingLeft = 0;
	public int PaddingTop = 0;
	public int PaddingBottom = 0;
}
