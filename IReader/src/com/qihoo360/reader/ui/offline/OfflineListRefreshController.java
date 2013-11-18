/**
 *
 */

package com.qihoo360.reader.ui.offline;

import com.qihoo360.reader.listener.OnOfflineTaskListener;
import com.qihoo360.reader.offline.OfflineTask;
import com.qihoo360.reader.ui.offline.OfflineListAdapter.ViewHolder;

import android.view.View;

/**
 * 用来控制列表刷新的类
 *
 * @author Jiongxuan Zhang
 */
class OfflineListRefreshController {
	protected static final long REFRESH_TIME = 1000;

	private static OfflineListAdapter sAdapter = null;

	private static boolean sIsScrolling = false;

	static interface OnRefreshItemListener {
		void onRefreshItem(ViewHolder viewHolder);
	}

	private static OnRefreshItemListener mProgressListener = new OnRefreshItemListener() {

		@Override
		public void onRefreshItem(ViewHolder viewHolder) {
			String currentProgress = String.format("%d%%",
					viewHolder.offlineTask.getCurrentProgress());
			viewHolder.offlineStatus.setText(currentProgress);

			viewHolder.offlineProgress.setProgress(viewHolder.offlineTask
					.getCurrentProgress());
		}
	};

	private static OnRefreshItemListener mUpdateStatusListener = new OnRefreshItemListener() {

		@Override
		public void onRefreshItem(ViewHolder viewHolder) {
			sAdapter.refreshItem(viewHolder);
		}
	};

	private static final OnOfflineTaskListener sListener = new OnOfflineTaskListener() {

		@Override
		public void onProgress(OfflineTask task) {
			if (sAdapter == null || sIsScrolling) {
				return;
			}

			findViewAndRefresh(task, mProgressListener);
		}

		@Override
		public void OnUpdateStatus(OfflineTask task) {
			if (sAdapter == null) {
				return;
			}

			findViewAndRefresh(task, mUpdateStatusListener);
		}
	};

	private static void findViewAndRefresh(OfflineTask task,
			final OnRefreshItemListener listener) {
		OfflineActivity activity = (OfflineActivity) sAdapter.getContext();

		int count = activity.mListView.getChildCount();
		for (int i = 0; i < count; i++) {
			View itemView = activity.mListView.getChildAt(i);

			if (itemView != null) {
				ViewHolder viewHolder = (ViewHolder) itemView.getTag();

				if (viewHolder.offlineTask == task && listener != null) {
					listener.onRefreshItem(viewHolder);
					break;
				}
			}
		}
	}

	/**
	 * 获取Listener
	 *
	 * @return
	 */
	public static OnOfflineTaskListener getListener() {
		return sListener;
	}

	/**
	 * 设置Adapter，以控制是否刷新
	 *
	 * @param adapter
	 */
	public static void setAdapter(OfflineListAdapter adapter) {
		sAdapter = adapter;
	}

	static void setScrolling(boolean isScrolling) {
		sIsScrolling = isScrolling;
	}
}
