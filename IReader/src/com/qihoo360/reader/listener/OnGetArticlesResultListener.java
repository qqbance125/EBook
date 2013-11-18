/**
 *
 */

package com.qihoo360.reader.listener;

/**
 * 获取文章列表的结果。
 *
 * @author Jiongxuan Zhang
 */
public interface OnGetArticlesResultListener {

    static final int NOT_CONNECTED = 1;

    /**
     * 当成功时
     *
     * @param from
     * @param to
     */
    public void onCompletion(long getFrom, long getTo, int getCount, boolean isDeleted);

    /**
     * 当失败时
     *
     * @param error
     */
    public void onFailure(int error);

    /**
     * 当你要访问的数据不存在时
     */
    public void onNotExists(boolean isDeleted);
}
