/**
 *
 */
package com.qihoo360.reader.listener;

import com.qihoo360.reader.offline.OfflineTask;

/**
 * 离线状态的Listener
 *
 * @author Jiongxuan Zhang
 *
 */
public interface OnOfflineTaskListener {
    static final int NOT_CONNECTED = 1;

    /**
     * 当用户正在进行时触发
     */
    public void onProgress(OfflineTask task);

    /**
     */
    public void OnUpdateStatus(OfflineTask task);
}
