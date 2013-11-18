/**
 *
 */

package com.qihoo360.reader.offline;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.qihoo360.reader.R;
import com.qihoo360.reader.support.Utils;
import com.qihoo360.reader.ui.ReaderPlugin;
import com.qihoo360.reader.ui.offline.OfflineActivity;

/**
 * 管理离线下载的通知栏
 *
 * @author Jiongxuan Zhang
 */
public class OfflineNotificationBar {
    private static OfflineNotificationBar sOfflineNotificationBar;

    private static final int NOTIFICATION_ID = 881007;
    private static final int NOTIFICATION_RESULT_ID = 198810;

    private Context mContext;
    private NotificationManager mNotificationManager;
    private Notification mNotification;
    private Notification mResultNotification;

    public enum Result {
        OKAY, FAILURE, WIFI_TO_3G
    }

    private boolean mIsProgressing;

    private OfflineNotificationBar(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context is null");
        }

        Utils.debug(getClass(), "starting OfflineNotificationBar method");

        mContext = context;
        mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        mNotification = new Notification(R.drawable.rd_icon, null,
                System.currentTimeMillis());
        mNotification.flags = Notification.FLAG_ONGOING_EVENT;
        mNotification.contentView = new RemoteViews(context.getPackageName(),
                R.layout.rd_offline_notification);
        mNotification.contentView.setProgressBar(R.id.offline_progress, 0, 0,
                true);

        Intent notificationIntent = new Intent(context, OfflineActivity.class);
        PendingIntent notificationPendingIntent = PendingIntent.getActivity(
                context, 0, notificationIntent, 0);
        mNotification.contentIntent = notificationPendingIntent;

        Utils.debug(getClass(), "end OfflineNotificationBar method");
    }

    /**
     * 获取实例
     *
     * @param context
     * @return
     */
    public static OfflineNotificationBar get(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context is null");
        }

        if (sOfflineNotificationBar == null) {
            sOfflineNotificationBar = new OfflineNotificationBar(context);
        }

        return sOfflineNotificationBar;
    }

    /**
     * 刷新通知栏上的进度条
     *
     * @param status
     */
    public void progress(int progress, int current, int total) {
        Utils.debug(getClass(), "starting progress method");

        String percentText = String.valueOf(progress);
        mNotification.contentView.setProgressBar(R.id.offline_progress, 100,
                progress, false);

        mNotification.contentView.setTextViewText(R.id.title,
                mContext.getString(R.string.rd_offline_downloading));
        mNotification.tickerText = mContext
                .getString(R.string.rd_offline_downloading);
        mNotification.contentView.setTextViewText(R.id.percent, percentText
                + "%");

        mNotificationManager.notify(NOTIFICATION_ID, mNotification);

        mIsProgressing = true;

        Utils.debug(getClass(), "end progress method");
    }

    /**
     * 取消通知栏上的通知
     */
    public void cancelComplete() {
        Utils.debug(getClass(), "starting cancelComplete method");

        mNotificationManager.cancel(NOTIFICATION_RESULT_ID);

        Utils.debug(getClass(), "end cancelComplete method");
    }

    /**
     * 取消进度条
     */
    public void cancelProgress() {
        Utils.debug(getClass(), "starting cancelProgress method");

        mNotificationManager.cancel(NOTIFICATION_ID);

        Utils.debug(getClass(), "end cancelProgress method");
    }

    /**
     * 提醒用户已下载完成
     *
     * @param statusType
     */
    public void complete(Result result, int articleCount, int imageCount) {
        Utils.debug(getClass(), "starting complete method");

        cancelComplete();

        if (mResultNotification == null) {
            mResultNotification = new Notification();
        }

        mResultNotification.when = 0;
        mResultNotification.defaults |= Notification.DEFAULT_SOUND;
        mResultNotification.flags |= Notification.FLAG_AUTO_CANCEL;
        mResultNotification.icon = R.drawable.rd_icon;

        if (result == Result.FAILURE) {
            Intent notificationIntent = new Intent(mContext,
                    OfflineActivity.class);
            PendingIntent notificationPendingIntent = PendingIntent
                    .getActivity(mContext, 0, notificationIntent, 0);

            mResultNotification.setLatestEventInfo(
                    mContext.getApplicationContext(),
                    mContext.getResources().getString(
                            R.string.rd_offline_download_error_title),
                    mContext.getResources().getString(
                            R.string.rd_offline_download_error),
                    notificationPendingIntent);
            mResultNotification.tickerText = mContext.getResources().getString(
                    R.string.rd_offline_download_error);

        } else if (result == Result.OKAY) {

            String completeText = null;
            if (articleCount > 0 && imageCount > 0) {
                completeText = mContext
                        .getString(
                                R.string.rd_offline_download_complete_with_articles_and_images,
                                articleCount, imageCount);
            } else if (articleCount > 0) {
                completeText = mContext.getString(
                        R.string.rd_offline_download_complete_with_articles,
                        articleCount);
            } else if (imageCount > 0) {
                completeText = mContext.getString(
                        R.string.rd_offline_download_complete_with_images,
                        imageCount);
            } else {
                completeText = mContext.getString(
                        R.string.rd_offline_download_complete_with_images, 0);
            }

            Intent notificationIntent = ReaderPlugin
                    .getBringBrowserForegroundIntent(mContext);
            PendingIntent notificationPendingIntent = PendingIntent
                    .getActivity(mContext, 0, notificationIntent, 0);

            mResultNotification.setLatestEventInfo(mContext
                    .getApplicationContext(), mContext.getResources()
                    .getString(R.string.rd_offline_download_complete_title),
                    completeText, notificationPendingIntent);
            mResultNotification.tickerText = completeText;
        } else if (result == Result.WIFI_TO_3G) {
            Intent notificationIntent = new Intent(mContext,
                    OfflineActivity.class);
            PendingIntent notificationPendingIntent = PendingIntent
                    .getActivity(mContext, 0, notificationIntent, 0);

            mResultNotification.setLatestEventInfo(
                    mContext.getApplicationContext(),
                    mContext.getResources().getString(
                            R.string.rd_offline_download_error_title),
                    mContext.getResources().getString(
                            R.string.rd_offline_3g_failure),
                    notificationPendingIntent);
            mResultNotification.tickerText = mContext.getResources().getString(
                    R.string.rd_offline_3g_failure);
        }

        mNotificationManager
                .notify(NOTIFICATION_RESULT_ID, mResultNotification);

        mIsProgressing = false;

        Utils.debug(getClass(), "end complete method");
    }

    /**
     * 是否正在走进度条？
     *
     * @return
     */
    public boolean isProgressing() {
        return mIsProgressing;
    }
}
