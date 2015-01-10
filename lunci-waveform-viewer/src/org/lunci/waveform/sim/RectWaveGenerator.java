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

package org.lunci.waveform.sim;

public final class RectWaveGenerator implements IWaveformGenerator {
	private int mSineCounter = 0;
	private int mAmplitude = 1;
	private int mFrequency = 1;
	private int mSamplingRate = 100;
	private int mNominal = 0;
	private int mMiddlePoint;
	private int mCounter = 0;

	public RectWaveGenerator() {

	}

	public RectWaveGenerator(int amplitude, int frequency, int samplingRate,
			int nominal) {
		this.mAmplitude = amplitude;
		this.mFrequency = frequency;
		this.mSamplingRate = samplingRate;
		this.mNominal = nominal;
		mMiddlePoint = mSamplingRate / mFrequency / 2;
	}

	@Override
	public double get() {
		double value = 0;
		if (mCounter >= mMiddlePoint) {
			value = -mAmplitude + mNominal;
		} else {
			value = mAmplitude + mNominal;
		}
		++mCounter;
		if (mCounter == mSamplingRate / mFrequency) {
			mCounter = 0;
		}
		return value;
	}

	public int getmSineCounter() {
		return mSineCounter;
	}

	public void setmSineCounter(int mSineCounter) {
		this.mSineCounter = mSineCounter;
	}

	public int getmAmplitude() {
		return mAmplitude;
	}

	public void setmAmplitude(int mAmplitude) {
		this.mAmplitude = mAmplitude;
	}

	public int getmFrequency() {
		return mFrequency;
	}

	public void setmFrequency(int mFrequency) {
		this.mFrequency = mFrequency;
		mMiddlePoint = mSamplingRate / mFrequency / 2;
	}

	public int getmSamplingRate() {
		return mSamplingRate;
	}

	public void setmSamplingRate(int mSamplingRate) {
		this.mSamplingRate = mSamplingRate;
		mMiddlePoint = mSamplingRate / mFrequency / 2;
	}

	public int getmNominal() {
		return mNominal;
	}

	public void setmNominal(int mNominal) {
		this.mNominal = mNominal;
	}
}
