/**
 *
 */

package com.qihoo360.reader.push;

import com.qihoo360.reader.R;
import com.qihoo360.reader.ui.articles.ArticleDetailActivity;
import com.qihoo360.reader.ui.articles.ArticleUtils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/**
 * 专门针对Push的消息栏的处理
 *
 * @author Jiongxuan Zhang
 */
class PushNotificationBar {
	private static final int NOTIFICATION_ID = 546649826;

	private static PushNotificationBar sPushNotificationBar;

	private Context mContext;

	private NotificationManager mNotificationManager;
	private Notification mNotification;

	private PushNotificationBar(Context context) {
		if (context == null) {
			throw new IllegalArgumentException("context is null");
		}

		mContext = context;
		mNotificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotification = new Notification();
		mNotification.icon = R.drawable.rd_icon;
		mNotification.defaults |= Notification.DEFAULT_SOUND;
	}

	/**
	 * 获得PushNotification的实例
	 *
	 * @param context
	 * @return
	 */
	public static PushNotificationBar get(Context context) {
		if (context == null) {
			throw new IllegalArgumentException("context is null");
		}

		if (sPushNotificationBar == null) {
			sPushNotificationBar = new PushNotificationBar(context);
		}

		return sPushNotificationBar;
	}

	/**
	 * 推送消息到任务栏
	 *
	 * @param title
	 * @param message
	 * @param channel
	 * @param positionInCursor
	 */
	public void push(String title, String message, String channel,
			int positionInCursor) {
		if (title == null) {
			throw new IllegalArgumentException("title is null");
		}
		if (message == null) {
			throw new IllegalArgumentException("message is null");
		}
		if (channel == null) {
			throw new IllegalArgumentException("channel is null");
		}
		if (positionInCursor < 0) {
			throw new IllegalArgumentException("positionInCursor less 0");
		}

		Intent intent = new Intent(mContext, ArticleDetailActivity.class);
		intent.putExtra("channel", channel);
		intent.putExtra("from_push", true);
		intent.putExtra(ArticleUtils.LIST_POSITION, positionInCursor);
		PendingIntent notificationPendingIntent = PendingIntent.getActivity(
				mContext, 0, intent, 0);

		mNotification.when = System.currentTimeMillis();
		mNotification.setLatestEventInfo(mContext, title, message,
				notificationPendingIntent);
		mNotification.flags |= Notification.FLAG_AUTO_CANCEL;
		mNotification.tickerText = title + "\n" + message;
		mNotificationManager.notify(NOTIFICATION_ID, mNotification);
	}

	/**
	 * 取消先前Push的内容
	 */
	public void cancel() {
		mNotificationManager.cancel(NOTIFICATION_ID);
	}
}
