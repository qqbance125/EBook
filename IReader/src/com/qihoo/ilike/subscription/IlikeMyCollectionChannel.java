/**
 *
 */
package com.qihoo.ilike.subscription;

import java.util.List;

import android.content.ContentResolver;
import android.os.AsyncTask;

import com.qihoo.ilike.http.core.IHttpRequest.HttpRequestStatus;
import com.qihoo.ilike.json.MyCollectionGetter;
import com.qihoo.ilike.json.listener.OnCollectionResultListener;
import com.qihoo.ilike.util.Valuable;
import com.qihoo.ilike.vo.Bookmark;
import com.qihoo.ilike.vo.ErrorInfo;
import com.qihoo.ilike.vo.Favorit;
import com.qihoo360.reader.listener.OnGetArticlesResultListener;
import com.qihoo360.reader.subscription.Channel;

/**
 * @author Jiongxuan Zhang
 *
 */
public class IlikeMyCollectionChannel extends IlikeChannel {

    IlikeMyCollectionChannel() {
        super(IlikeChannel.ILIKE_MY_COLLECTION, "我的收藏",
                Channel.TYPE_ILIKE_MY_COLLECTION, LevelType.Special);
    }

    @Override
    protected void getArticles(final ContentResolver resolver,
            final OnGetArticlesResultListener listener, final boolean isGetNew,
            String newestId, String newestQid, String oldestId,
            String oldestQid, int rangeCount) {

        MyCollectionGetter getter = new MyCollectionGetter();
        setGetter(isGetNew, getter);

        getter.parse(new OnCollectionResultListener() {

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
            public void onComplete(Favorit list, int total) {
                if (list != null) {
                    if (list.bookmarks != null && list.bookmarks.length > 0) {
                        int startIndex = 0;
                        if (list.svc_grab_index > -1) {
                            startIndex = list.svc_grab_index + 1;
                        }

                        insertToDb(resolver, listener, isGetNew, list,
                                startIndex);
                    } else {
                        // 针对收藏为空的情况
                        if (isGetNew && list.cache_expire == 1) {
                            deleteArticles(resolver);
                            listener.onCompletion(0, 19, 0, true);
                        } else {
                            listener.onNotExists(list.cache_expire == 1);
                        }
                    }

                    setGetter(isGetNew, null);
                }
            }
        }, Valuable.getQid(), isGetNew ? 0 : 1, newestId, oldestId, rangeCount);
    }

    private void insertToDb(final ContentResolver resolver,
            final OnGetArticlesResultListener listener, final boolean isGetNew,
            final Favorit list, final int startIndex) {
        AsyncTask<Integer, Integer, Integer> task = new AsyncTask<Integer, Integer, Integer>() {
            private boolean mIsDeleted = false;

            @Override
            protected Integer doInBackground(Integer... params) {
                mIsDbBusy = true;

                mIsDeleted = ensureContinuous(resolver, isGetNew,
                        list.cache_expire);

                if (list.bookmarks != null) {

                    List<String> containBookmarkIds = getAllBookmarkIds(resolver);
                    int sort = calculateSort(resolver, isGetNew,
                            list.bookmarks.length);

                    for (int i = startIndex; i < list.bookmarks.length; i++) {
                        Bookmark bookmark = list.bookmarks[i];
                        if (!containBookmarkIds.contains(bookmark.id)) {
                            insertArticleToDb(resolver, bookmark, list.user,
                                    sort, true);
                        }

                        sort++;
                    }

                    if (!mIsDeleted) {
                        mIsDeleted = clearOldCache(isGetNew, resolver);
                    }
                }

                mIsDbBusy = false;
                return list.new_cnt;
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
