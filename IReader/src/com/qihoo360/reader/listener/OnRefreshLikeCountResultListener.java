/**
 *
 */
package com.qihoo360.reader.listener;

/**
 * @author Jiongxuan Zhang
 *
 */
public interface OnRefreshLikeCountResultListener {

    static final int NOT_CONNECTED = 1;

    /**
     * 当成功时
     *
     */
    public void onCompletion(int likeCount, boolean isChanged);

    /**
     * 当失败时
     *
     * @param error
     */
    public void onFailure(int error);
}
