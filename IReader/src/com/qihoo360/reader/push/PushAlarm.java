/**
 *
 */

package com.qihoo360.reader.push;

import com.qihoo360.reader.Constants;
import com.qihoo360.reader.Settings;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

/**
 * 有关Push服务的定时类
 *
 * @author Jiongxuan Zhang
 */
class PushAlarm {
	private static final int DAY_IN_MILESECONDS = 24 * 60 * 60 * 1000;

	private static AlarmManager sAlarmManager;
	private static PendingIntent sPendingIntent;

	/**
	 * 现在就开始定时
	 *
	 * @param scheduleCalendar
	 */
	public static void schedule(Context context, Calendar scheduleCalendar) {
		if (context == null) {
			throw new IllegalArgumentException("context is null");
		}
		if (scheduleCalendar == null) {
			throw new IllegalArgumentException("scheduleCalendar is null");
		}
		// 闹钟管理器的设置
		getAlarmManager(context).setRepeating(AlarmManager.RTC_WAKEUP,
				scheduleCalendar.getTimeInMillis(), DAY_IN_MILESECONDS,
				getPendingIntent(context));

		Settings.setSchedulePushTime(scheduleCalendar.getTimeInMillis());
	}

	private static AlarmManager getAlarmManager(Context context) {
		if (sAlarmManager == null) {
			sAlarmManager = (AlarmManager) context
					.getSystemService(Context.ALARM_SERVICE);
		}

		return sAlarmManager;
	}

	private static PendingIntent getPendingIntent(Context context) {
		if (context == null) {
			throw new IllegalArgumentException("context is null");
		}

		if (sPendingIntent == null) {
			Intent intent = new Intent(context, PushAlarmReceiver.class);
			intent.setAction(PushAlarmReceiver.PUSH_NOW);
			sPendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
		}

		return sPendingIntent;
	}

	/**
	 * 两小时内是否已经给push定时过？
	 *
	 * @return
	 */
	public static boolean isScheduleInHours() {
		return System.currentTimeMillis() - Settings.getSchedulePushTime() <= Constants.TWO_HOURS;
	}

	/**
	 * 取消定时
	 *
	 * @param context
	 */
	public static void cancel(Context context) {
		if (context == null) {
			throw new IllegalArgumentException("context is null");
		}

		getAlarmManager(context).cancel(getPendingIntent(context));
	}
}
