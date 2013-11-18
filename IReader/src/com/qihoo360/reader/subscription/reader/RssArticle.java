/**
 *
 */

package com.qihoo360.reader.subscription.reader;

import com.qihoo360.reader.ServerUris;
import com.qihoo360.reader.data.DataEntryManager;
import com.qihoo360.reader.data.Tables.Articles;
import com.qihoo360.reader.json.JsonGetterBase;
import com.qihoo360.reader.json.JsonUtils;
import com.qihoo360.reader.listener.OnFillArticleResultListener;
import com.qihoo360.reader.listener.OnMarkStarResultListener;
import com.qihoo360.reader.subscription.Article;
import com.qihoo360.reader.subscription.Content;
import com.qihoo360.reader.support.AsyncTask;
import com.qihoo360.reader.support.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 表示一个“文章”——在一个频道内。
 *
 * @author Jiongxuan Zhang
 */
public class RssArticle extends Article {

    /**
     * 将Cursor转换成RssArticle对象
     *
     * @param cursor
     * @return
     */
    public static RssArticle inject(Cursor cursor) {

        if (cursor == null) {
            return null;
        }

        RssArticle article = new RssArticle();
        int idx;
        idx = cursor.getColumnIndex(Articles._ID);
        if (idx >= 0)
            article._id = cursor.getInt(idx);

        idx = cursor.getColumnIndex(Articles.CHANNEL);
        if (idx >= 0)
            article.channel = cursor.getString(idx);

        idx = cursor.getColumnIndex(Articles.CONTENT_ID);
        if (idx >= 0)
            article.contentid = cursor.getLong(idx);

        idx = cursor.getColumnIndex(Articles.TITLE);
        if (idx >= 0)
            article.title = cursor.getString(idx);

        idx = cursor.getColumnIndex(Articles.DESCRIPTION);
        if (idx >= 0)
            article.description = cursor.getString(idx);

        idx = cursor.getColumnIndex(Articles.COMPRESSED_IMAGE_URL);
        if (idx >= 0)
            article.images360 = cursor.getString(idx);

        idx = cursor.getColumnIndex(Articles.PUB_DATE);
        if (idx >= 0)
            article.pubdate = cursor.getLong(idx);

        idx = cursor.getColumnIndex(Articles.LINK);
        if (idx >= 0)
            article.link = cursor.getString(idx);

        idx = cursor.getColumnIndex(Articles.AUTHOR);
        if (idx >= 0)
            article.author = cursor.getString(idx);

        idx = cursor.getColumnIndex(Articles.READ);
        if (idx >= 0)
            article.read = cursor.getInt(idx);

        idx = cursor.getColumnIndex(Articles.STAR);
        if (idx >= 0)
            article.star = cursor.getInt(idx);

        idx = cursor.getColumnIndex(Articles.STARDATE);
        if (idx >= 0)
            article.stardate = cursor.getLong(idx);

        idx = cursor.getColumnIndex(Articles.ISDOWNLOADED);
        if (idx >= 0)
            article.isDownloaded = cursor.getInt(idx) == 1;

        idx = cursor.getColumnIndex(Articles.ISOFFLINED);
        if (idx >= 0)
            article.isOfflined = cursor.getInt(idx) == 1;

        return article;
    }

    @Override
    protected int fill(ContentResolver resolver,
            OnFillArticleResultListener listener, boolean isSync) {
        if (isDownloaded) {
            return RESULT_FAILURE;
        }

        mFillTask = new FillTask(resolver, this, listener);

        if (isSync) {
            return mFillTask.doSync();
        } else {
            mFillTask.execute();
            return 0;
        }
    }

    /**
     * 停止当前异步的下载请求
     */
    public void stopFill() {
        if (mFillTask != null
                && mFillTask.getStatus() == android.os.AsyncTask.Status.RUNNING) {
            mFillTask.cancel();
            mFillTask = null;
        }
    }

    FillTask mFillTask = null;
    private JsonGetterBase mJsonGetter = null;

    static class RssArticleJson extends JsonGetterBase {

        @Override
        public Object parse(String jsonString) {
            Content content = new Content();
            try {
                JSONObject contentJson = new JSONObject(jsonString);
                content.response = contentJson.getInt("response");
                content.channel = JsonUtils.getJsonString(contentJson,
                        "channel");
                content.number = JsonUtils.getJsonInt(contentJson, "number");
                // content.entry
                JSONArray entryArray = contentJson.getJSONArray("entry");
                List<RssArticle> articles = new ArrayList<RssArticle>();
                for (int i = 0; i < entryArray.length(); i++) {
                    RssArticle article = new RssArticle();
                    JSONObject articleJson = (JSONObject) entryArray.opt(i);
                    article.contentid = JsonUtils.getJsonInt(articleJson,
                            "contentid");
                    article.title = JsonUtils.getJsonString(articleJson,
                            "title");
                    article.description = JsonUtils.getJsonString(articleJson,
                            "description");
                    article.images360 = JsonUtils.getJsonString(articleJson,
                            "images360");
                    article.pubdate = JsonUtils.getJsonInt(articleJson,
                            "pubdate");
                    article.link = JsonUtils.getJsonString(articleJson, "link");
                    article.author = JsonUtils.getJsonString(articleJson,
                            "author");
                    article.isDownloaded = true;
                    articles.add(article);
                }
                content.entry = articles;
                // end
            } catch (Exception e) {
                Utils.error(getClass(), Utils.getStackTrace(e));
            }
            return content;
        }

    }

    void markStar(ContentResolver resolver, int star, long stardate) {
        this.star = star;
        this.stardate = stardate;
        ContentValues values = new ContentValues();
        values.put(Articles.STAR, star);
        values.put(Articles.STARDATE, stardate);
        DataEntryManager.ArticleHelper.update(resolver, channel, contentid,
                values);
    }

    void markRead(ContentResolver resolver, int read) {
        if (this.read != read) {
            this.read = read;
            ContentValues values = new ContentValues();
            values.put(Articles.READ, read);
            DataEntryManager.ArticleHelper.update(resolver, channel, contentid,
                    values);
        }
    }

    /**
     * 将该文章标记为已经离线下载过的
     */
    public void markOfflined(ContentResolver resolver) {
        if (!isOfflined) {
            this.isOfflined = true;
            ContentValues values = new ContentValues();
            values.put(Articles.ISOFFLINED, isOfflined);
            DataEntryManager.ArticleHelper.update(resolver, channel, contentid,
                    values);
        }
    }

    static int clear(ContentResolver resolver) {
        return DataEntryManager.ArticleHelper.Normal.deleteAll(resolver);
    }

    static class FillTask extends AsyncTask<Integer, Integer, Integer> {

        protected ContentResolver mResolver;
        protected RssArticle mArticle;
        protected OnFillArticleResultListener mListener;
        protected int mError = 0;

        public FillTask(ContentResolver resolver, RssArticle article,
                OnFillArticleResultListener listener) {
            mResolver = resolver;
            mArticle = article;
            mListener = listener;
        }

        /**
         * 取消下载过程
         */
        public void cancel() {
            mArticle.getJsonGetter().stop();
            cancel(true);
        }

        @Override
        protected Integer doInBackground(Integer... params) {
            String url = getUrl();

            Utils.debug(getClass(),
                    "doInBackground() >> sinceId = %d; url = %s",
                    mArticle.contentid, url);

            Content content = download(url);
            if (isCancelled() == true) {
                mError = ERROR_USER_CANCELLED;
                return RESULT_FAILURE;
            }
            if (content != null && content.isOk()) {
                List<RssArticle> list = content.getItems();
                if (list != null && list.size() >= 1) {
                    RssArticle filledArticle = list.get(0);
                    if (filledArticle != null) {
                        mArticle.description = filledArticle.description;
                        mArticle.images360 = filledArticle.images360;
                        mArticle.link = filledArticle.link;
                        mArticle.author = filledArticle.author;
                        mArticle.isDownloaded = filledArticle.isDownloaded;
                        DataEntryManager.ArticleHelper.updateContent(mResolver,
                                mArticle);
                        return RESULT_OK;
                    }
                }
            }

            mError = ERROR_NETWORK_ERROR;
            return RESULT_FAILURE;
        }

        /*
         * (non-Javadoc)
         *
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Integer result) {
            if (mListener != null && !isCancelled()) {
                switch (result) {
                case RESULT_OK:
                    mListener.onCompletion();
                    break;

                case RESULT_FAILURE:
                    mListener.onFailure(mError);
                    break;
                }
            }
            mArticle.mFillTask = null;
        }

        /*
         * (non-Javadoc)
         *
         * @see android.os.AsyncTask#onCancelled()
         */
        @Override
        protected void onCancelled() {
            mArticle.mFillTask = null;
        }

        private String getUrl() {
            return ServerUris.getContentList(mArticle.channel,
                    mArticle.contentid, 1, 0);
        }

        private Content download(String url) {
            if (!TextUtils.isEmpty(url)) {
                String jsonString = mArticle.getJsonGetter().get(url);
                if (!TextUtils.isEmpty(jsonString)) {
                    return (Content) mArticle.getJsonGetter().parse(jsonString);
                }
            }
            return null;
        }
    }

    private JsonGetterBase getJsonGetter() {
        if (mJsonGetter == null) {
            mJsonGetter = new RssArticleJson();
        }

        return mJsonGetter;
    }

    @Override
    public void markStar(ContentResolver resolver,
            OnMarkStarResultListener listener) {
        markStar(resolver, STAR_APPEARANCE, System.currentTimeMillis());
    }

    @Override
    public void unmarkStar(ContentResolver resolver,
            OnMarkStarResultListener listener) {
        if (star == RssArticle.STAR_DISAPPEARANCE) {
            DataEntryManager.ArticleHelper.delete(resolver, _id);
        } else {
            markStar(resolver, STAR_NONE, 0);
        }
    }

    /**
     * 将文章标记为已读
     *
     * @param resolver
     */
    public void markRead(ContentResolver resolver) {
        markRead(resolver, READ);
    }

    /**
     * 将文章标记为未读
     *
     * @param resolver
     */
    public void markUnread(ContentResolver resolver) {
        markRead(resolver, UN_READ);
    }
}
