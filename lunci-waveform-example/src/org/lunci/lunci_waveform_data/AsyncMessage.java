package org.lunci.lunci_waveform_data;

import android.os.Message;

public final class AsyncMessage {
	private final Message mMsg;

	public AsyncMessage(Message msg) {
		mMsg = msg;
	}

	public Message getMsg() {
		return mMsg;
	}
}
