/**
 *
 */
package com.qihoo360.reader.offline;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

import com.qihoo360.reader.Settings;
import com.qihoo360.reader.support.NetUtils;

/**
 * 有关离线下载的监听器
 *
 * @author Jiongxuan Zhang
 *
 */
public class OfflineReceiver extends BroadcastReceiver {

	/* (non-Javadoc)
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent == null) {
			return;
		}

		String action = intent.getAction();
		if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
			if (OfflineQueue.isRunning() && NetUtils.isNetworkAvailable()
					&& !NetUtils.isWifiConnected()
					&& !Settings.isEnableOfflineWithoutWifi()) {
				// 如果当前正在离线下载，用户处在3G环境下，同时也没有允许3G环境下下载，则立即取消
				OfflineQueue.stopAll();
			}
		}
	}

}
