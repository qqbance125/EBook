/**
 *
 */
package com.qihoo360.reader.listener;

/**
 * 当下载完Index后出发
 *
 * @author Jiongxuan Zhang
 */
public interface OnDownloadIndexResultListener {

    /**
     * 当更新成功时
     */
    public void onUpdated();

    /**
     * 当已经是最新版本时
     */
    public void onAlreadyLastestVersion();

    /**
     * 当失败时
     */
    public void onFailure();

}
