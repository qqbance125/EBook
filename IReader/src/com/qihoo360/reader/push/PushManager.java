/**
 *
 */

package com.qihoo360.reader.push;

import java.util.Calendar;

import android.content.Context;

import com.qihoo360.reader.Settings;
import com.qihoo360.reader.subscription.Channel;
import com.qihoo360.reader.subscription.reader.RssChannel;
import com.qihoo360.reader.support.ArithmeticUtils;
import com.qihoo360.reader.support.NetUtils;

/**
 * 有关操作Push的类
 *
 * @author Jiongxuan Zhang
 */
public class PushManager {
	static PushTask sPushTask;

	private static final int START_PUSH_HOUR_OF_DAY = 7;
	private static final int END_PUSH_HOUR_OF_DAY = 20;

	/**
	 * 开始向服务器请求Push，并在两小时后继续
	 *
	 * @param context
	 */
	public static void start(Context context) {
		if (context == null) {
			throw new IllegalArgumentException("context is null");
		}

		/*
		 * 如果：
		 *
		 * *当前没有Push
		 *
		 * *用户接受了许可协议
		 *
		 * *不处于“两小时定时期”
		 *
		 * *没有过应该Push的时间
		 *
		 * *今天也没Push过
		 *
		 * *网络状态良好
		 *
		 * 则开始Push
		 */
		if (sPushTask == null && Settings.isAllowStatement(context)
				&& !PushAlarm.isScheduleInHours() && !isPushedToday()
				&& !isPastPushSchedule(Calendar.getInstance())
				&& NetUtils.isNetworkAvailable()) {
			sPushTask = new PushTask(context);
			sPushTask.execute();
		}

		schedule(context);
	}
	/**
	 * 时间表
	 *  @param context    设定文件 
	 * @throws
	 */
	private static void schedule(Context context) {
		if (context == null) {
			throw new IllegalArgumentException("context is null");
		}

		if (PushAlarm.isScheduleInHours()) {
			return;
		}

		Calendar scheduleCalendar = Calendar.getInstance();
		scheduleCalendar.add(Calendar.HOUR_OF_DAY, 2);

		if (isPastPushSchedule(scheduleCalendar)) {
			// 2小时以后，是否是晚上21点-早上7点？那就定时到明天早上7点以后？

			if (scheduleCalendar.get(Calendar.HOUR_OF_DAY) > END_PUSH_HOUR_OF_DAY) {
				// 如果在21:00到23:59之间，订到明天
				// 否则已经超过0点，就订“今天”
				scheduleCalendar.add(Calendar.DAY_OF_YEAR, 1);
			}

			scheduleCalendar.set(Calendar.HOUR_OF_DAY, START_PUSH_HOUR_OF_DAY);
			scheduleCalendar.set(
					Calendar.MINUTE,
					(int) ArithmeticUtils.getRandom(
							scheduleCalendar.get(Calendar.MINUTE), 59)); // minute 

		} else {
			// 新的一天，该做什么呢？

			if (isPushedToday()) {
				// 今天已经Push过了？那就放到明天早上7点开始查
				scheduleCalendar.add(Calendar.DAY_OF_YEAR, 1);
				scheduleCalendar.set(Calendar.HOUR_OF_DAY,
						START_PUSH_HOUR_OF_DAY);
				scheduleCalendar.set(
						Calendar.MINUTE,
						(int) ArithmeticUtils.getRandom(
								scheduleCalendar.get(Calendar.MINUTE), 59));
			}
		}

		PushAlarm.schedule(context, scheduleCalendar);
	}

	/**
	 * 停止Push服务
	 *
	 * @param context
	 */
	public static void stop(Context context) {
		stop(context, null);
	}

	/**
	 * 停止指定频道的Push操作
	 *
	 * @param context
	 * @param channel
	 */
	public static void stop(Context context, Channel channel) {
		if (context == null) {
			throw new IllegalArgumentException("context is null");
		}

		if (channel == null
				|| (channel != null && getPushingChannel() == channel)) {
			if (sPushTask != null) {
				sPushTask.cancel();
			}
		}

		PushAlarm.cancel(context);
	}

	/**
	 * 是否正在获取Push？
	 *
	 * @return
	 */
	public static boolean isPushing() {
		return sPushTask != null;
	}

	/**
	 * 获取当前正在Push的频道。为Null表示还没有开始进行。
	 *
	 * @return
	 */
	public static Channel getPushingChannel() {
		if (sPushTask != null) {
			return sPushTask.getCurrentChannel();
		}

		return null;
	}

	/**
	 * 删除通知栏上的频道
	 *
	 * @param context
	 * @param channel
	 */
	public static void removeNotification(Context context, Channel channel) {
		if (channel == null) {
			throw new IllegalArgumentException("channel is null");
		}

		if (RssChannel.get(Settings.getPushedChannel()) != null) {
			PushNotificationBar.get(context).cancel();
		}
	}

	/**
	 * 今天是否已经Push过
	 *
	 * @return
	 */
	public static boolean isPushedToday() {
		Calendar pushedCalendar = Calendar.getInstance();
		pushedCalendar.setTimeInMillis(Settings.getPushedTime());

		return isToday(pushedCalendar);
	}

	private static boolean isToday(Calendar calendar) {
		if (calendar == null) {
			throw new IllegalArgumentException("calendar is null");
		}

		Calendar nowCalendar = Calendar.getInstance();

		return calendar.get(Calendar.YEAR) == nowCalendar.get(Calendar.YEAR)
				&& calendar.get(Calendar.DAY_OF_YEAR) == nowCalendar
						.get(Calendar.DAY_OF_YEAR);
	}

	/**
	 * 是否已经错过了应该Push的时间？
	 *
	 * @return
	 */
	public static boolean isPastPushSchedule(Calendar calendar) {
		return calendar.get(Calendar.HOUR_OF_DAY) > END_PUSH_HOUR_OF_DAY
				|| calendar.get(Calendar.HOUR_OF_DAY) < START_PUSH_HOUR_OF_DAY;
	}
}
