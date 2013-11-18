/**
 *
 */
package com.qihoo.ilike.json.listener;

import com.qihoo.ilike.vo.CategoryVo;

/**
 * @author Jiongxuan Zhang
 *
 */
public interface OnHotCollectionBookmarksResultListener extends IOnGetResultListener {
    void onComplete(CategoryVo categoryVo, int total);
}
