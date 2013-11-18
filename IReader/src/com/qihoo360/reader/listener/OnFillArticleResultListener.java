/**
 *
 */
package com.qihoo360.reader.listener;


/**
 * 当下载完文章详情以后的Listener
 *
 * @author Jiongxuan Zhang
 *
 */
public interface OnFillArticleResultListener {

    static final int NOT_CONNECTED = 1;

    /**
     * 当成功时
     *
     * @param from
     * @param to
     */
    public void onCompletion();

    /**
     * 当失败时
     *
     * @param error
     */
    public void onFailure(int error);
}
