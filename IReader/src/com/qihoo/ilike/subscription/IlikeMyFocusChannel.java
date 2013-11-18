/**
 *
 */
package com.qihoo.ilike.subscription;

import com.qihoo.ilike.http.core.IHttpRequest.HttpRequestStatus;
import com.qihoo.ilike.json.FocusGetter;
import com.qihoo.ilike.json.listener.OnFocusResultListener;
import com.qihoo.ilike.vo.Collect;
import com.qihoo.ilike.vo.CollectList;
import com.qihoo.ilike.vo.ErrorInfo;
import com.qihoo360.reader.listener.OnGetArticlesResultListener;
import com.qihoo360.reader.subscription.Channel;

import android.content.ContentResolver;
import android.os.AsyncTask;

import java.util.List;

/**
 * @author Jiongxuan Zhang
 *
 */
public class IlikeMyFocusChannel extends IlikeChannel {

    IlikeMyFocusChannel() {
        super(IlikeChannel.ILIKE_MY_FOCUS, "我的关注", Channel.TYPE_ILIKE_MY_FOCUS,
                LevelType.Special);
    }

    @Override
    protected void getArticles(final ContentResolver resolver,
            final OnGetArticlesResultListener listener, final boolean isGetNew,
            String newestId, String newestQid, String oldestId,
            String oldestQid, int rangeCount) {

        FocusGetter getter = new FocusGetter();
        setGetter(isGetNew, getter);

        getter.parse(new OnFocusResultListener() {

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
            public void onComplete(CollectList list, int total) {
                if (list != null) {

                    if (list.collectList != null && list.collectList.length > 0) {
                        insertToDb(resolver, listener, isGetNew, list);
                    } else {
                        listener.onNotExists(false);
                    }
                }
                setGetter(isGetNew, null);
            }
        }, isGetNew ? 0 : 1, newestId, oldestId, rangeCount);
    }

    private void insertToDb(final ContentResolver resolver,
            final OnGetArticlesResultListener listener, final boolean isGetNew,
            final CollectList list) {
        AsyncTask<Integer, Integer, Integer> task = new AsyncTask<Integer, Integer, Integer>() {

            private boolean mIsDeleted;

            @Override
            protected Integer doInBackground(Integer... params) {
                mIsDbBusy = true;

                mIsDeleted = ensureContinuous(resolver, isGetNew, 0);
                if (!mIsDeleted) {
                    mIsDeleted = clearOldCache(isGetNew, resolver);
                }

                int addedCount = 0;

                if (list.collectList != null) {

                    List<String> containBookmarkIds = getAllBookmarkIds(resolver);
                    int sort = calculateSort(resolver, isGetNew,
                            list.collectList.length);

                    for (Collect collect : list.collectList) {
                        if (!containBookmarkIds.contains(collect.bookmark.id)) {
                            insertArticleToDb(resolver, collect.bookmark,
                                    collect.user, sort, false);

                            addedCount++;
                        }

                        sort++;
                    }
                }

                mIsDbBusy = false;
                return addedCount;
            }

            @Override
            protected void onPostExecute(Integer result) {
                if (result > 0) {
                    listener.onCompletion(0, 19, result, mIsDeleted);
                } else {
                    listener.onNotExists(mIsDeleted);
                }
                super.onPostExecute(result);
            }

        };

        setDbInserter(isGetNew, task);
        task.execute();
    }
}
