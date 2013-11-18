/**
 *
 */
package com.qihoo.ilike.json.listener;

import com.qihoo.ilike.vo.Profile;

/**
 * @author zhangjiongxuan
 *
 */
public interface OnProfileResultListener extends IOnGetResultListener {
    void onComplete(Profile profile);
}
