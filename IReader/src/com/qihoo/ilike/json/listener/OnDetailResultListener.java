/**
 *
 */
package com.qihoo.ilike.json.listener;

import com.qihoo.ilike.vo.Detail;

/**
 * @author zhangjiongxuan
 *
 */
public interface OnDetailResultListener extends IOnGetResultListener {
    void onComplete(Detail detail);
}
