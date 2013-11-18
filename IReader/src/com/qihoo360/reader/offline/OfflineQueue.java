/**
 *
 */

package com.qihoo360.reader.offline;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

import com.qihoo360.reader.ReaderApplication;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.listener.OnOfflineQueueListener;
import com.qihoo360.reader.listener.OnOfflineTaskListener;
import com.qihoo360.reader.offline.OfflineNotificationBar.Result;
import com.qihoo360.reader.offline.OfflineTask.OfflineTaskStatus;
import com.qihoo360.reader.subscription.Channel;
import com.qihoo360.reader.support.NetUtils;
import com.qihoo360.reader.support.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 离线下载的队列
 *
 * @author Jiongxuan Zhang
 */
public class OfflineQueue {
	private static final int MAX_OFFLINING_COUNT = 3;

	private static List<OfflineTask> sOffliningList = new ArrayList<OfflineTask>(
			MAX_OFFLINING_COUNT);
	private static Queue<OfflineTask> sWaitingQueue = new LinkedBlockingQueue<OfflineTask>();

	private static OfflineNotificationBar sNotificationBar = OfflineNotificationBar
			.get(ReaderApplication.getContext());

	private static Result sResult = Result.OKAY;

	private static int sArticles = 0;
	private static int sImages = 0;
	private static boolean sIsFirstEnsure = true;
	private static int sPassed = 0;
	private static boolean sHasCompleted = false;
	private static boolean sHasFailure = false;

	private static OnOfflineQueueListener sOfflineQueueListener;

	/**
	 * 设置有关总进度的Listener
	 *
	 * @param listener
	 */
	public static void setQueueListener(OnOfflineQueueListener listener) {
		sOfflineQueueListener = listener;
	}

	/**
	 * 把任务添加进来
	 *
	 * @param task
	 */
	public static void add(OfflineTask task) {
		if (task == null) {
			throw new IllegalArgumentException("task is null");
		}

		// 保证只能有一个
		if (sOffliningList.contains(task) || sWaitingQueue.contains(task)) {
			return;
		}

		Utils.debug(OfflineQueue.class, "starting add method");
		// 是不是第一次
		sIsFirstEnsure = (sOffliningList.size() == 0 && sWaitingQueue.size() == 0);

		if (sOffliningList.size() < MAX_OFFLINING_COUNT) {
			// 加入“立即下载”队列
			Utils.debug(OfflineQueue.class,
					"add -> adding to offlining list...");
			sOffliningList.add(task);
		} else {
			// 放入等待队列
			Utils.debug(OfflineQueue.class, "add -> adding to waiting queue...");
			task.updateStatus(OfflineTaskStatus.WAITING);

			sWaitingQueue.add(task);
		}

		ensure();

		Utils.debug(OfflineQueue.class, "end add method");
	}

	/**
	 * 将其从列表中移除，如果任务正在运行，则在移除过后立即停止任务
	 *
	 * @param task
	 */
	public static void remove(OfflineTask task) {
		if (task == null) {
			throw new IllegalArgumentException("task is null");
		}

		Utils.debug(OfflineQueue.class, "starting remove method");

		if (sOffliningList.contains(task)) {
			removeOfflining(task);
		} else if (sWaitingQueue.contains(task)) {
			removeWaiting(task);
		}

		ensure();

		Utils.debug(OfflineQueue.class, "end remove method");
	}

	private static void removeOfflining(OfflineTask task) {
		if (task.isRunning()) {
			Utils.debug(OfflineQueue.class,
					"removeOfflining -> stoping task: %s",
					task.getChannel().channel);
		}

		task.stopTask();

		Utils.debug(OfflineQueue.class,
				"removeOfflining -> removing from offling list: %s",
				task.getChannel().channel);
		sOffliningList.remove(task);
	}

	private static void removeWaiting(OfflineTask task) {
		Utils.debug(OfflineQueue.class,
				"removeWaiting -> removing from waiting queue: %s",
				task.getChannel().channel);
		sWaitingQueue.remove(task);

		// 告诉用户已从排队中取消
		task.updateStatus(OfflineTaskStatus.USER_CANCEL);
	}

	private static boolean sIsScrolling = false;

	/**
	 * 如果当前用户正在离线下载页面中滚动列表，则我们没必要为这时候刷新通知栏上的进度。
	 *
	 * @param isScrolling
	 */
	public static void setIsScrollingOnUi(boolean isScrolling) {
		sIsScrolling = isScrolling;
	}

	private static final OnOfflineTaskListener sOfflineListener = new OnOfflineTaskListener() {

		private static final int REFRESH = 2000;
		private long mLastRefreshTime;

		@Override
		public void onProgress(OfflineTask task) {
			long now = System.currentTimeMillis();
			if (now - mLastRefreshTime > REFRESH && !sIsScrolling) {
				// 计算通知栏的进度
				// 这次是一取消则都取消，所以不存在进度条会乱的问题
				int processing = 0;
				final int total = sOffliningList.size() + sWaitingQueue.size()
						+ sPassed;
				if (total <= 0) {
					// 表示已经完成，不需要再开始
					return;
				}

				for (OfflineTask entryTask : sOffliningList) {
					processing += entryTask.getCurrentProgress();
				}

				final int process = (sPassed * 100 + processing) / total;

				sNotificationBar.progress(process, sPassed, total);

				mLastRefreshTime = now;
			}
		}

		@Override
		public void OnUpdateStatus(OfflineTask task) {
			if (task.isCompleted()) {
				sHasCompleted = true;
				sPassed++;
			} else if (task.isFailure()) {
				sHasFailure = true;
				sPassed++;
			}
		}
	};

	/**
	 * 确保正在离线的列表中的任务都在进行。
	 */
	private static void ensure() {
		Utils.debug(OfflineQueue.class, "starting ensure method");

		// 第一次离线下载？

		// 把等待中的队列往“正在离线”的队列中调整，并立即开启它
		int addCount = MAX_OFFLINING_COUNT - sOffliningList.size();
		Utils.debug(OfflineQueue.class,
				"ensure -> allow to convert the waiting to the offling: %d",
				addCount);
		Utils.debug(OfflineQueue.class,
				"ensure -> the waiting queue has %d elements.",
				sWaitingQueue.size());

		if (addCount > 0 && sWaitingQueue.size() != 0) {
			for (int i = 0; i < addCount; i++) {
				OfflineTask task = sWaitingQueue.poll();

				if (task != null) {
					sOffliningList.add(task);
					Utils.debug(OfflineQueue.class,
							"ensure -> convert task: channel=%s",
							task.getChannel().channel);
				}
			}
		}

		// 把直接从add加入的，和刚刚从等待队列中转过来的，都做个轮询，并启用
		for (int i = 0; i < sOffliningList.size(); i++) {
			OfflineTask task = sOffliningList.get(i);

			// 如果任务已经完成、出现问题、没有运行过，都可以重新开启
			if (!task.isRunning()) {
				task.setQueueListener(sOfflineListener);
				task.startTask();
				Utils.debug(OfflineQueue.class,
						"ensure -> starting task: %s, status: %s",
						task.getChannel().channel, task.getStatus().toString());
			}
		}

		// 如果已经完成，则在通知栏上弹出消息，并重置状态
		if (sOffliningList.size() == 0) {
			sNotificationBar.cancelProgress();
			if (NetUtils.isNetworkAvailable() && !NetUtils.isWifiConnected()
					&& !Settings.isEnableOfflineWithoutWifi()) {
				// 如果用户从Wifi突然切换到2G/3G模式下，则退出的时候要给予用户提示
				sResult = Result.WIFI_TO_3G;
				sNotificationBar.complete(sResult, 0, 0);
			} else if (sHasFailure) {
				sResult = Result.FAILURE;
				sNotificationBar.complete(sResult, 0, 0);
			} else if (sHasCompleted) {
				sResult = Result.OKAY;
				sNotificationBar.complete(sResult, sArticles, sImages);
			}

			if (sOfflineQueueListener != null) {
				sOfflineQueueListener.onQueueComplete();
			}

			unregisterReceiver();
			sIsFirstEnsure = true;
			sResult = Result.OKAY;
			sPassed = 0;
			sArticles = 0;
			sImages = 0;
			sHasFailure = false;
			sHasCompleted = false;
		} else if (sIsFirstEnsure && sOfflineQueueListener != null) {
			registerReceiver();
			sOfflineQueueListener.onQueueStart();
		}

		Utils.debug(OfflineQueue.class, "end ensure method");
	}

	private static BroadcastReceiver sOfflineReceiver = null;
	private static void registerReceiver() {
		if (sOfflineReceiver != null) {
			return;
		}

		sOfflineReceiver = new OfflineReceiver();
		IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		ReaderApplication.getContext().registerReceiver(sOfflineReceiver, filter);
	}

	private static void unregisterReceiver() {
		if (sOfflineReceiver == null) {
			return;
		}

		ReaderApplication.getContext().unregisterReceiver(sOfflineReceiver);
		sOfflineReceiver = null;
	}

	/**
	 * 追加文章的个数，方便在通知栏中显示
	 *
	 * @param count
	 */
	public static void addArticleCount(int count) {
		sArticles += count;
	}

	/**
	 * 追加图集中图片的个数，方便在通知栏中显示
	 *
	 * @param count
	 */
	public static void addImageCount(int count) {
		sImages += count;
	}

	/**
	 * 是否已经在排队？
	 *
	 * @param offlineTask
	 * @return
	 */
	public static boolean isWaiting(OfflineTask offlineTask) {
		return sWaitingQueue.contains(offlineTask);
	}

	/**
	 * 当前离线任务是否已开始进行
	 *
	 * @return
	 */
	public static boolean isRunning() {
		return sOffliningList.size() != 0;
	}

	/**
	 * 当前频道是否正在离线下载中
	 *
	 * @param channel
	 * @return
	 */
	public static boolean isRunning(Channel channel) {
		return sOffliningList.contains(channel);
	}

	/**
	 * 立即停止全部的下载
	 */
	public static void stopAll() {
		Utils.debug(OfflineQueue.class, "starting stopAll method");

		// 先停止所有排队着的队列
		OfflineTask waitingTask = null;

		while ((waitingTask = sWaitingQueue.poll()) != null) {
			removeWaiting(waitingTask);
		}

		// 再逐个停止当前正在进行的任务
		int size = sOffliningList.size();
		for (int i = 0; i < size; i++) {
			OfflineTask offliningTask = sOffliningList.get(0);
			removeOfflining(offliningTask);
		}

		ensure();

		Utils.debug(OfflineQueue.class, "end stopAll method");
	}
}
