package org.lunci.lunci_waveform_data;

public class GraphDataModel {
	public int id;
	public int[] dataValue;
	public int size = 0;
	public boolean isValid = true;

	public GraphDataModel() {

	}

	public GraphDataModel(int id, int[] dataValue) {
		this.id = id;
		this.dataValue = dataValue;
		if (dataValue != null)
			size = dataValue.length;
	}
}
