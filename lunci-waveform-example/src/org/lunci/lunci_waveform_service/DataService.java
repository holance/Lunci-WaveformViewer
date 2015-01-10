package org.lunci.lunci_waveform_service;

import java.lang.ref.WeakReference;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.lunci.lunci_waveform_data.GraphDataSet;
import org.lunci.lunci_waveform_example.BuildConfig;

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
	private BlockingQueue<GraphDataSet> mWaveformDataTransferBuffer;
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
		mDataSendingThread = new DataSendingThread(mWaveformDataTransferBuffer);
		mDataSendingThread.start();
	}

	private void stopConnection() {
		if (mDataSendingThread != null && mDataSendingThread.isAlive()) {
			mDataSendingThread.interrupt();
		}
	}

	private void setConfig(DataServiceConfig config) {
		mConfig = config;
		mWaveformDataTransferBuffer = new ArrayBlockingQueue<GraphDataSet>(
				config.GraphDataTransferBufferSize);
	}

	public BlockingQueue<GraphDataSet> getGraphDataBuffer() {
		return mWaveformDataTransferBuffer;
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

	private final class DataSendingThread extends Thread {
		private final BlockingQueue<GraphDataSet> mDataTransferBuffer;
		private final EventBus mEventBus = EventBus.getDefault();;
		private boolean stop = false;

		public DataSendingThread(BlockingQueue<GraphDataSet> dataTransferBuffer) {
			super();
			mDataTransferBuffer = dataTransferBuffer;
			this.setPriority(Thread.NORM_PRIORITY);
		}

		@Override
		public void run() {
			while (!stop) {
				try {
					// Log.i(TAG, "sending data");
					final GraphDataSet set = mDataTransferBuffer.take();
					synchronized (mClients) {
						for (int i = 0; i < mClients.size(); ++i) {
							final GraphDataClient client = mClients
									.get(mClients.keyAt(i));
							if (client.getQueue().get() != null) {
								if (!client
										.getQueue()
										.get()
										.offer(set.getDataSet()[client
												.getDataSourceIndex()].dataValue)) {
									Log.w(TAG,
											"push data to client buffer failed");
								}
							} else {
								mClients.remove(client.getId());
							}
						}
					}
					// mEventBus.post(set);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public void interrupt() {
			stop = true;
			super.interrupt();
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
