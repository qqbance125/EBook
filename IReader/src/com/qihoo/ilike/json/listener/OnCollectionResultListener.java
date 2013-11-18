/**
 *
 */
package com.qihoo.ilike.json.listener;

import com.qihoo.ilike.vo.Favorit;

/**
 * @author zhangjiongxuan
 *
 */
public interface OnCollectionResultListener extends IOnGetResultListener {
    void onComplete(Favorit list, int total);
}
