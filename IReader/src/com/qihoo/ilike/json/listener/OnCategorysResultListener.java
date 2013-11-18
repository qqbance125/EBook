/**
 *
 */
package com.qihoo.ilike.json.listener;

import com.qihoo.ilike.vo.CategoryList;

/**
 * @author zhangjiongxuan
 *
 */
public interface OnCategorysResultListener extends IOnGetResultListener {
    void onComplete(String jsonText, CategoryList list);
}
