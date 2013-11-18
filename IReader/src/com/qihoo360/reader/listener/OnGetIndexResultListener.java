/**
 *
 */
package com.qihoo360.reader.listener;

import com.qihoo360.reader.subscription.Index;

/**
 * 获取Index的结果
 *
 * @author Jiongxuan Zhang
 */
public interface OnGetIndexResultListener {

    /**
     * 当成功时
     * @param result
     */
    public void onCompletion(Index result);

    /**
     * 当失败时
     * @param result
     */
    public void onFailure();
}
