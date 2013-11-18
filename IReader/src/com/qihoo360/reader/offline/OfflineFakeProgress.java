/**
 *
 */

package com.qihoo360.reader.offline;

import com.qihoo360.reader.listener.OnProgressChangedListener;
import com.qihoo360.reader.support.FakeProgress;

import android.os.Handler;
import android.os.Looper;

/**
 * 用来管理离线下载的所有假进度
 *
 * @author Jiongxuan Zhang
 */
public class OfflineFakeProgress {
	private static Handler sMainHandler = new Handler(Looper.getMainLooper());
	private static final OnProgressChangedListener sProgressChangedListener = new OnProgressChangedListener() {

		@Override
		public void onProgressChanged(Object tag, double progress, double max) {
			if (tag != null) {
				OfflineTask task = (OfflineTask) tag;
				task.mCurrent = (int) progress;

				if (task.mTask != null) {
					task.mTask.publishProgress(false);
				}
			}
		}
	};

	/**
	 * 创建FakeProgress对象
	 *
	 * @param task
	 * @return
	 */
	public static FakeProgress create(OfflineTask task) {
		FakeProgress fakeProgress = new FakeProgress(sMainHandler,
				sProgressChangedListener);
		fakeProgress.setTag(task);

		return fakeProgress;
	}
}
