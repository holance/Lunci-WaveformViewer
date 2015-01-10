package org.lunci.lunci_waveform_service;

import java.util.Timer;

import de.greenrobot.event.EventBus;

public class DataPublisher extends Thread {
	private static final String TAG = DataPublisher.class.getSimpleName();

	public static class DataPublisherConfig {
		public int DataRate = 25;
		public int CacheSize = 25;
	}

	public int mFPS = 25;

	private final EventBus mEventBus = EventBus.getDefault();

	private Timer mTimer;

}
