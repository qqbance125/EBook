/**
 *
 */
package com.qihoo.ilike.subscription;

import java.util.List;

import android.content.ContentResolver;
import android.os.AsyncTask;

import com.qihoo.ilike.http.core.IHttpRequest.HttpRequestStatus;
import com.qihoo.ilike.json.HotCollectionBookmarksGetter;
import com.qihoo.ilike.json.listener.OnHotCollectionBookmarksResultListener;
import com.qihoo.ilike.util.Utils;
import com.qihoo.ilike.vo.CategoryVo;
import com.qihoo.ilike.vo.Collect;
import com.qihoo.ilike.vo.ErrorInfo;
import com.qihoo360.reader.listener.OnGetArticlesResultListener;

/**
 * @author Jiongxuan Zhang
 *
 */
public class IlikeHotCollectionChannel extends IlikeChannel {

    IlikeHotCollectionChannel(String realName, String title, LevelType levelType) {
        super(ILIKE_PREFIX + realName, title, TYPE_ILIKE_HOT_COLLECTION,
                levelType);
    }

    @Override
    protected void getArticles(final ContentResolver resolver,
            final OnGetArticlesResultListener listener, final boolean isGetNew,
            String newestId, String newestQid, String oldestId,
            String oldestQid, int rangeCount) {

        HotCollectionBookmarksGetter getter = new HotCollectionBookmarksGetter();
        setGetter(isGetNew, getter);

        getter.get(new OnHotCollectionBookmarksResultListener() {

            @Override
            public void onResponseError(ErrorInfo errorInfo) {
                if (listener != null && errorInfo != null) {
                    listener.onFailure(RESULT_FAILURE);
                }
                setGetter(isGetNew, null);
            }

            @Override
            public void onRequestFailure(HttpRequestStatus errorStatus) {
                if (listener != null && errorStatus != null) {
                    listener.onFailure(RESULT_FAILURE);
                }
                setGetter(isGetNew, null);
            }

            @Override
            public void onComplete(CategoryVo categoryVo, int total) {
                if ((categoryVo.collectList != null)
                        && (categoryVo.collectList.length > 0)) {
                    insertToDb(resolver, listener, isGetNew, categoryVo);
                } else {
                    // 针对为空的情况
                    if (isGetNew && categoryVo.cache_expire == 1) {
                        deleteArticles(resolver);
                        listener.onCompletion(0, 19, 0, true);
                    } else {
                        listener.onNotExists(categoryVo.cache_expire == 1);
                    }
                }

                setGetter(isGetNew, null);
            }

        }, title, isGetNew ? 0 : 1, newestQid + "_" + newestId, oldestQid + "_"
                + oldestId, rangeCount);
    }

    private void insertToDb(final ContentResolver resolver,
            final OnGetArticlesResultListener listener, final boolean isGetNew,
            final CategoryVo categoryVo) {
        AsyncTask<Integer, Integer, Integer> task = new AsyncTask<Integer, Integer, Integer>() {
            private boolean mIsDeleted = false;

            @Override
            protected Integer doInBackground(Integer... params) {
                mIsDbBusy = true;

                mIsDeleted = ensureContinuous(resolver, isGetNew,
                        categoryVo.cache_expire);
                if (!mIsDeleted) {
                    mIsDeleted = clearOldCache(isGetNew, resolver);
                }

                List<String> containBookmarkIds = getAllBookmarkIds(resolver);
                int sort = calculateSort(resolver, isGetNew,
                        categoryVo.collectList.length);

                for (Collect collect : categoryVo.collectList) {
                    if ((collect != null)
                            && (collect.bookmark != null)
                            && (collect.bookmark.pics != null)
                            && (collect.bookmark.pics.length > 0)
                            && (collect.bookmark.pics[0] != null)
                            && (Utils
                                    .isStrValidable(collect.bookmark.pics[0].url))
                            && (collect.user != null)) {

                        if (!containBookmarkIds.contains(collect.bookmark.id)) {
                            insertArticleToDb(resolver, collect.bookmark,
                                    collect.user, sort, false);

                        }
                    }
                    sort++;
                }

                mIsDbBusy = false;

                return categoryVo.new_cnt;
            }

            @Override
            protected void onPostExecute(Integer result) {
                if (result > 0) {
                    listener.onCompletion(0, 19, result, mIsDeleted);
                } else {
                    listener.onNotExists(mIsDeleted);
                }
            }

        };

        setDbInserter(isGetNew, task);
        task.execute();
    }

}
