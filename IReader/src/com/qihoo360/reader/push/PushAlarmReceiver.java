/**
 *
 */

package com.qihoo360.reader.push;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 当Push时间到达时执行
 *
 * @author Jiongxuan Zhang
 */
public class PushAlarmReceiver extends BroadcastReceiver {

	public static final String PUSH_NOW = "push_now";

	/*
	 * (non-Javadoc)
	 *
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
	 * android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		if (context == null) {
			throw new IllegalArgumentException("context is null");
		}
		if (intent == null) {
			return;
		}

		if (PUSH_NOW.equals(intent.getAction())) {
			PushManager.start(context);
		}
	}

}
