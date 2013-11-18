/**
 *
 */
package com.qihoo360.reader.listener;

/**
 * 离线下载总进度的Listener
 *
 * @author Jiongxuan Zhang
 *
 */
public interface OnOfflineQueueListener {

    /**
     * 当离线下载开始时执行。
     */
    public void onQueueStart();

    /**
     * 当整个离线下载过程都结束时执行。
     */
    public void onQueueComplete();
}
