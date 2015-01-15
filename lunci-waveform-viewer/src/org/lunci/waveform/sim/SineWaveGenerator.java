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

package org.lunci.waveform.sim;

public final class SineWaveGenerator implements IWaveformGenerator {
	private int sineCounter = 0;
	private int amplitude = 1;
	private int frequency = 1;
	private int samplingRate = 100;
	private int nominal = 0;

	public SineWaveGenerator() {

	}

	public SineWaveGenerator(int amplitude, int frequency, int samplingRate,
			int nominal) {
		this.amplitude = amplitude;
		this.frequency = frequency;
		this.samplingRate = samplingRate;
		this.nominal = nominal;
	}

	@Override
	public double get() {
		final double y = (amplitude * Math.sin(sineCounter * (2 * Math.PI)
				* frequency / samplingRate))
				+ nominal;
		++sineCounter;
		if (sineCounter >= samplingRate / frequency) {
			sineCounter = 0;
		}
		// final double y = (amplitude * Math.sin(Math.toRadians(sineCounter)))
		// + nominal;
		// ++sineCounter;
		// if (sineCounter >= 360) {
		// sineCounter = 0;
		// }
		return y;
	}

	public int getSineCounter() {
		return sineCounter;
	}

	public void setSineCounter(int sineCounter) {
		this.sineCounter = sineCounter;
	}

	public int getAmplitude() {
		return amplitude;
	}

	public void setAmplitude(int amplitude) {
		this.amplitude = amplitude;
	}

	public int getFrequency() {
		return frequency;
	}

	public void setFrequency(int frequency) {
		this.frequency = frequency;
	}

	public int getSamplingRate() {
		return samplingRate;
	}

	public void setSamplingRate(int samplingRate) {
		this.samplingRate = samplingRate;
	}

	public int getNominal() {
		return nominal;
	}

	public void setNominal(int nominal) {
		this.nominal = nominal;
	}
}
