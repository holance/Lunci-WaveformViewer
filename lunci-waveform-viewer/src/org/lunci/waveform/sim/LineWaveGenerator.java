package org.lunci.waveform.sim;

public final class LineWaveGenerator implements IWaveformGenerator {
	private int amplitude = 1;

	public int getAmplitude() {
		return amplitude;
	}

	public void setAmplitude(int value) {
		amplitude = value;
	}

	@Override
	public double get() {
		// TODO Auto-generated method stub
		return amplitude;
	}

}
