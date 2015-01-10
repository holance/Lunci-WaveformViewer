package org.lunci.lunci_waveform_fragments;

import org.lunci.lunci_waveform_example.R;
import org.lunci.waveform.ui.WaveformView;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ZoomControls;

public class WaveformFragment extends Fragment_ServiceManagerBase {
	private static final String TAG = WaveformFragment.class.getSimpleName();
	private org.lunci.waveform.ui.WaveformView mWaveformView;
	public static final String EXTRA_DATA_INDEX = "extra_data_index";
	private int mDataIndex = 0;
	private final ConditionVariable mWaveformViewReadyBlocker = new ConditionVariable();
	private final ConditionVariable mServiceReadyBlocker = new ConditionVariable();
	private int mClientId = -1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			final Bundle args = getArguments();
			if (args != null) {
				if (args.containsKey(EXTRA_DATA_INDEX)) {
					mDataIndex = args.getInt(EXTRA_DATA_INDEX);
				}
			}

		} else {
			mDataIndex = savedInstanceState.getInt(EXTRA_DATA_INDEX);
		}
		final BufferAttacher attacher = new BufferAttacher();
		attacher.start();
		Log.i(TAG, "onCreate");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.i(TAG, "onCreateView");
		final View rootView = inflater.inflate(R.layout.fragment_waveform,
				container, false);
		mWaveformView = (WaveformView) rootView
				.findViewById(R.id.waveformView_1);
		final WaveformView.Config config = new WaveformView.Config();
		config.DataMinValue = 0;
		config.DataMaxValue = 0x00FFFFFF;
		mWaveformView.setConfig(config);
		final ZoomControls zoomControls = (ZoomControls) rootView
				.findViewById(R.id.zoomControls_vertical_zoom);
		zoomControls.setOnZoomInClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mWaveformView.setZoomRatio(mWaveformView.getConfig().ZoomRatio * 2);
			}

		});
		zoomControls.setOnZoomOutClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mWaveformView.setZoomRatio(mWaveformView.getConfig().ZoomRatio / 2);
			}

		});
		return rootView;
	}

	@Override
	public void onStart() {
		super.onStart();
		// EventBus.getDefault().register(this);
		mWaveformViewReadyBlocker.open();
	}

	@Override
	public void onResume() {
		super.onResume();

	}

	@Override
	public void onPause() {

		super.onPause();
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}

	@Override
	public void onStop() {
		// EventBus.getDefault().unregister(this);
		super.onStop();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(EXTRA_DATA_INDEX, mDataIndex);
	}

	// public void onEvent(GraphDataSet set) {
	// if (mWaveformView != null && set.getDataSet().length > mDataIndex) {
	// mWaveformView.putData(set.getDataSet()[mDataIndex].dataValue);
	// }
	// }

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		super.onServiceConnected(name, service);
		mServiceReadyBlocker.open();
	}

	public void setDataIndex(int index) {
		mDataIndex = index;
	}

	private final class BufferAttacher extends Thread {
		@Override
		public void run() {
			mWaveformViewReadyBlocker.block();
			mServiceReadyBlocker.block();
			mClientId = getServiceFunction().getService().addGraphDataClient(
					mWaveformView.getDataQueue(), mDataIndex);
			mWaveformViewReadyBlocker.close();
			mServiceReadyBlocker.close();
		}
	}
}
