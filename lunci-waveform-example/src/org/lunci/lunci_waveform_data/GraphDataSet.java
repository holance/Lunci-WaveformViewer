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
