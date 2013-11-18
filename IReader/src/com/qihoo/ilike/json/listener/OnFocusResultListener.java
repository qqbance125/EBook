/**
 *
 */
package com.qihoo.ilike.json.listener;

import com.qihoo.ilike.vo.CollectList;

/**
 * @author zhangjiongxuan
 *
 */
public interface OnFocusResultListener extends IOnGetResultListener {
    void onComplete(CollectList list, int total);
}
