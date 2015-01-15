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

package org.lunci.lunci_waveform_service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.util.Log;
import android.util.SparseArray;

import org.lunci.lunci_waveform_data.AsyncMessage;
import org.lunci.lunci_waveform_data.GlobalEventIds;
import org.lunci.lunci_waveform_example.BuildConfig;
import org.lunci.waveform.sim.IWaveformGenerator;
import org.lunci.waveform.sim.RectWaveGenerator;
import org.lunci.waveform.sim.SineWaveGenerator;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;

import de.greenrobot.event.EventBus;

/**
 * @author z0034hwj
 *
 */
public class DataService extends Service {

	private static final String TAG = DataService.class.getSimpleName();

	public static interface IDataService {
		IBinder getMessengerBinder();

		DataService getService();
	}

	protected final class DataServiceBinder extends Binder implements
			IDataService {

		public DataServiceBinder() {
			super();
		}

		@Override
		public DataService getService() {
			return DataService.this;
		}

		@Override
		public IBinder getMessengerBinder() {
			return mMessenger.getBinder();
		}
	}

	private static final class MainHandler extends Handler {
		private final WeakReference<DataService> mService;

		public MainHandler(DataService service, Looper looper) {
			super(looper);
			mService = new WeakReference<DataService>(service);
		}

		@Override
		public void handleMessage(Message msg) {
			final DataService service = mService.get();
			if (service == null)
				return;
			switch (msg.what) {
			case DataServiceCommands.MESSAGE_CONFIG:
				break;
			case DataServiceCommands.MESSAGE_START_SENDING_DATA:
				service.startConnection();
				// service.startPublishing();
				break;
			case DataServiceCommands.MESSAGE_STOP_SENDING_DATA:
				service.stopConnection();
				// service.stopPublishing();
				break;
			}
		}
	}

	private Messenger mMessenger;
	private Looper mServiceLooper;
	private HandlerThread mHandlerThread;
	private Handler mHandler;
	private DataSendingThread mDataSendingThread;
	private DataServiceConfig mConfig = new DataServiceConfig();
	private final SparseArray<GraphDataClient> mClients = new SparseArray<GraphDataClient>();

	@Override
	public void onCreate() {
		super.onCreate();
		if (BuildConfig.DEBUG) {
			Log.d(TAG, "onCreate");
		}
		this.setConfig(mConfig);
		mHandlerThread = new HandlerThread("ServiceStartArguments",
				Process.THREAD_PRIORITY_BACKGROUND);
		mHandlerThread.start();
		mServiceLooper = mHandlerThread.getLooper();
		mHandler = new MainHandler(this, mServiceLooper);
		mMessenger = new Messenger(mHandler);
	}

	@Override
	public final IBinder onBind(final Intent arg0) {
		if (BuildConfig.DEBUG) {
			Log.d(TAG, "onBind");
		}
		return new DataServiceBinder();
	}

	@Override
	public boolean onUnbind(Intent intent) {
		if (BuildConfig.DEBUG) {
			Log.d(TAG, "onUnbind");
		}
		boolean result = super.onUnbind(intent);
		return result;
	}

	@Override
	public void onDestroy() {
		if (BuildConfig.DEBUG) {
			Log.d(TAG, "onDestroy");
		}
		stopConnection();
		// stopPublishing();
		mHandlerThread.quit();
		super.onDestroy();
	}

	private void startConnection() {
		stopConnection();
		mDataSendingThread = new DataSendingThread(mConfig);
		mDataSendingThread.start();
	}

	private void stopConnection() {
		if (mDataSendingThread != null) {
			mDataSendingThread.interrupt();
		}
	}

	private void setConfig(DataServiceConfig config) {
		mConfig = config;
	}

	public int addGraphDataClient(BlockingQueue<int[]> queue, int sourceIndex) {
		final int key = queue.hashCode();
		synchronized (mClients) {
			mClients.append(key, new GraphDataClient(key, queue, sourceIndex));
		}
		return key;
	}

	public void removeGraphDataClient(int id) {
		synchronized (mClients) {
			mClients.remove(id);
		}
	}

	public void removeAllGraphDataClients() {
		synchronized (mClients) {
			mClients.clear();
		}
	}

	private final class DataSendingThread {
		private final DataServiceConfig mConfig;
		private final IWaveformGenerator[] mDataGenSet;
		private long mDataCounterStartTime;
		private long mDataCounterEndTime;
		private Timer mTimer;

		public DataSendingThread(DataServiceConfig config) {
			super();
			// this.setPriority(Thread.NORM_PRIORITY + 1);
			mConfig = config;
			mDataGenSet = new IWaveformGenerator[] {
					new SineWaveGenerator(1000, 10, mConfig.FPS
							* mConfig.DATA_SIZE, 5000),
					new RectWaveGenerator(1500, 2, mConfig.FPS
							* mConfig.DATA_SIZE, 5000) };
		}

		public void start() {
			interrupt();
			mTimer = new Timer();
			mTimer.scheduleAtFixedRate(mDataTask, 0, 1000 / mConfig.FPS);
		}

		private final TimerTask mDataTask = new TimerTask() {

			private int mDataCounter = 0;
			private int mDataPerSec = 0;
			private final EventBus mEventBus = EventBus.getDefault();

			@Override
			public void run() {
				if (mDataCounter == 0) {
					mDataCounterStartTime = System.currentTimeMillis();
				}
				final int[][] data = new int[mDataGenSet.length + 3][mConfig.DATA_SIZE];
				synchronized (mClients) {
					for (int i = 0; i < mDataGenSet.length; ++i) {
						for (int j = 0; j < data[i].length; ++j) {
							final double value = mDataGenSet[i].get();
							data[i][j] = (int) Math.round(value);
						}
					}
					for (int i = 0; i < data[2].length; ++i) {
						data[2][i] = data[1][i] - 5000 > 0 ? data[0][i] : 5000;
						data[3][i] = data[1][i] - 5000 < 0 ? data[0][i] : 5000;
						data[4][i] = 1;
					}
					for (int i = 0; i < mClients.size(); ++i) {
						final GraphDataClient client = mClients.get(mClients
								.keyAt(i));
						if (client.getQueue().get() != null) {
							if (!client.getQueue().get()
									.offer(data[client.getDataSourceIndex()])) {
								Log.w(TAG, "push data to client buffer failed");
							}
						} else {
							mClients.remove(client.getId());
						}
					}
				}
				++mDataCounter;
				if (mDataCounter >= mConfig.DATA_COUNTER_LIMIT) {
					mDataCounterEndTime = System.currentTimeMillis();
					final int packedPerSec = (int) (mConfig.DATA_COUNTER_LIMIT / ((mDataCounterEndTime - mDataCounterStartTime) / 1000));
					if (mDataPerSec != packedPerSec) {
						mDataPerSec = packedPerSec;
						mEventBus.post(new AsyncMessage(Message.obtain(null,
								GlobalEventIds.MESSAGE_PACKET_PERFORMANCE,
								packedPerSec, -1)));
					}
					mDataCounter = 0;
				}
			}

		};

		public void interrupt() {
			if (mTimer != null) {
				mTimer.cancel();
				mTimer.purge();
				mTimer = null;
			}
		}
	}

	private final class GraphDataClient {
		private final WeakReference<BlockingQueue<int[]>> mQueue;
		private final int mId;
		private final int mDataSourceIndex;

		public GraphDataClient(int id, BlockingQueue<int[]> queue,
				int dataSourceIndex) {
			mId = id;
			mQueue = new WeakReference<BlockingQueue<int[]>>(queue);
			mDataSourceIndex = dataSourceIndex;
		}

		public WeakReference<BlockingQueue<int[]>> getQueue() {
			return mQueue;
		}

		public int getId() {
			return mId;
		}

		public int getDataSourceIndex() {
			return mDataSourceIndex;
		}
	}
}
