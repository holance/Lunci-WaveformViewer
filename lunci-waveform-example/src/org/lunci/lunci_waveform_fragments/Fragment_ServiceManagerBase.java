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

package org.lunci.lunci_waveform_fragments;

import org.lunci.lunci_waveform_example.BuildConfig;
import org.lunci.lunci_waveform_service.DataService;
import org.lunci.lunci_waveform_service.DataService.IDataService;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.MenuItem;

public abstract class Fragment_ServiceManagerBase extends Fragment implements
ServiceConnection {
	private static final String TAG = Fragment_ServiceManagerBase.class
			.getSimpleName();

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		mServiceFunction = (IDataService) service;
		mServiceMessenger = new Messenger(mServiceFunction.getMessengerBinder());
		mBound = true;
		if (BuildConfig.DEBUG) {
			Log.i(TAG, "onServiceConnected, id=" + mServiceKey);
		}
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		mServiceFunction = null;
		mServiceMessenger = null;
	}

	private Messenger mServiceMessenger;
	private IDataService mServiceFunction;
	private int mServiceKey;
	private boolean mBound = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	protected boolean sendMessageToService(Message msg) {
		if (mServiceMessenger == null)
			return false;
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

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (!mBound) {
			Intent intent = new Intent(activity, DataService.class);
			activity.getApplicationContext().bindService(intent, this,
					Context.BIND_AUTO_CREATE);
		}
	}

	@Override
	public void onDetach() {
		if (mBound) {
			getActivity().getApplicationContext().unbindService(this);
			mBound = false;
		}
		super.onDetach();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (!super.onOptionsItemSelected(item)) {
			return getActivity().onOptionsItemSelected(item);
		} else
			return true;
	}

	public IDataService getServiceFunction() {
		return mServiceFunction;
	}
}
