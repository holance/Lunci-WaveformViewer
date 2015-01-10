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

package org.lunci.lunci_waveform_example;

import org.lunci.lunci_waveform_service.DataService;
import org.lunci.lunci_waveform_service.DataService.IDataService;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

public abstract class ActivityServiceManagerBase extends FragmentActivity
		implements IActivityCommon, ServiceConnection {
	private static final String TAG = ActivityServiceManagerBase.class
			.getSimpleName();

	private IDataService mServiceFunction;
	// private Messenger mMessenger;
	private int mServiceKey;
	private Messenger mServiceMessenger;
	private boolean mBound = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!mBound) {
			Intent intent = new Intent(this, DataService.class);
			bindService(intent, this, Context.BIND_AUTO_CREATE);
		}
	}

	@Override
	protected void onDestroy() {
		if (mBound) {
			unbindService(this);
			mBound = false;
		}
		super.onDestroy();
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		// mMessenger = new Messenger(getHandler());
		mBound = true;
		if (BuildConfig.DEBUG) {
			Log.i(TAG, "onServiceConnected, id=" + mServiceKey);
		}
		mServiceFunction = (IDataService) service;
		mServiceMessenger = new Messenger(mServiceFunction.getMessengerBinder());
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		mServiceFunction = null;
		mServiceMessenger = null;
	}

	public IDataService getServiceFunction() {
		return mServiceFunction;
	}

	protected boolean sendMessageToService(Message msg) {
		if (mServiceMessenger == null) {
			Log.w(TAG, "service messenger is null");
			return false;
		}
		msg.replyTo = mServiceMessenger;
		if (msg.arg1 != 0)
			Log.e(TAG,
					"Message argument 1 is reserved for service assigned id. Do not use argument 1.");
		msg.arg1 = mServiceKey;
		try {
			mServiceMessenger.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
