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

package org.lunci.lunci_waveform_example;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.lunci.lunci_waveform_data.AsyncMessage;
import org.lunci.lunci_waveform_data.GlobalEventIds;
import org.lunci.lunci_waveform_fragments.WaveformFragment;
import org.lunci.lunci_waveform_service.DataServiceCommands;

import java.lang.ref.WeakReference;

import de.greenrobot.event.EventBus;

public class WaveformActivity extends ActivityServiceManagerBase {
	private static final String TAG = WaveformActivity.class.getSimpleName();
	private static Handler mHandler;
	private TextView mTextView_PacketPerf;
	private final EventBus mEventBus = EventBus.getDefault();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_waveform);
		mTextView_PacketPerf = (TextView) findViewById(R.id.textView_packet_sec);
		if (savedInstanceState == null) {
			final WaveformFragment fragment1 = new WaveformFragment();
			fragment1.setDataIndex(0);
			final WaveformFragment fragment2 = new WaveformFragment();
			fragment2.setDataIndex(1);
			final WaveformFragment fragment3 = new WaveformFragment();
			fragment3.setDataIndex(2);
			final WaveformFragment fragment4 = new WaveformFragment();
			fragment4.setDataIndex(3);
			final WaveformFragment fragment5 = new WaveformFragment();
			fragment5.setDataIndex(4);
			final WaveformFragment fragment6 = new WaveformFragment();
			fragment6.setDataIndex(1);
			getSupportFragmentManager().beginTransaction()
					.add(R.id.frameLayout_wave_1, fragment1).commit();
			getSupportFragmentManager().beginTransaction()
					.add(R.id.frameLayout_wave_2, fragment2).commit();
			getSupportFragmentManager().beginTransaction()
					.add(R.id.frameLayout_wave_3, fragment3).commit();
			getSupportFragmentManager().beginTransaction()
					.add(R.id.frameLayout_wave_4, fragment4).commit();
			getSupportFragmentManager().beginTransaction()
					.add(R.id.frameLayout_wave_5, fragment5).commit();
			getSupportFragmentManager().beginTransaction()
					.add(R.id.frameLayout_wave_6, fragment6).commit();
		} else {

		}
		if (savedInstanceState == null) {

		} else {

		}
		mHandler = new MainHandler(this);
	}

	@Override
	public void onStart() {
		super.onStart();
		mEventBus.register(this);
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
	public void onStop() {
		mEventBus.unregister(this);
		super.onStop();
	}

	public void onEventMainThread(Message msg) {
		mHandler.dispatchMessage(msg);
	}

	public void onEventAsync(AsyncMessage msg) {
		mEventBus.post(msg.getMsg());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		super.onServiceConnected(name, service);
		this.sendMessageToService(Message.obtain(null,
				DataServiceCommands.MESSAGE_START_SENDING_DATA));
	}

	@Override
	public void onDestroy() {
		this.sendMessageToService(Message.obtain(null,
				DataServiceCommands.MESSAGE_STOP_SENDING_DATA));
		super.onDestroy();
	}

	private final static class MainHandler extends Handler {
		private final WeakReference<WaveformActivity> mWaveformActivity;

		public MainHandler(WaveformActivity waveformActivity) {
			super(waveformActivity.getMainLooper());
			mWaveformActivity = new WeakReference<WaveformActivity>(
					waveformActivity);
		}

		@Override
		public void handleMessage(Message msg) {
			final WaveformActivity act = mWaveformActivity.get();
			if (act == null)
				return;
			switch (msg.what) {
			case GlobalEventIds.MESSAGE_PACKET_PERFORMANCE:
				act.mTextView_PacketPerf.setText(String.valueOf(msg.arg1));
				break;
			}
		}
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */

	@Override
	public Handler getHandler() {
		// TODO Auto-generated method stub
		return mHandler;
	}

}
