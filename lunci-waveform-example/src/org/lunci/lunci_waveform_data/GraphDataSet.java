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

package org.lunci.lunci_waveform_data;

//Contains different graph model in one set
public class GraphDataSet {

	private final GraphDataModel[] mDataSet;
	private boolean mIsValid;

	public GraphDataSet(int size) {
		this(size, true, 0);
	}

	public GraphDataSet(int size, boolean isValid, int id) {
		mDataSet = new GraphDataModel[size];
		mIsValid = isValid;
	}

	public GraphDataModel[] getDataSet() {
		return mDataSet;
	}

	public boolean isValid() {
		return mIsValid;
	}

	public void setIsValid(boolean valid) {
		mIsValid = valid;
	}
}
