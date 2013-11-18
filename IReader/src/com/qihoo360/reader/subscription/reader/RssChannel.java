/**
 *
 */

package com.qihoo360.reader.subscription.reader;

import com.qihoo360.reader.Constants;
import com.qihoo360.reader.ServerUris;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.data.DataEntryManager;
import com.qihoo360.reader.data.DataEntryManager.ArticleHelper;
import com.qihoo360.reader.data.Tables;
import com.qihoo360.reader.data.Tables.Articles;
import com.qihoo360.reader.json.JsonGetterBase;
import com.qihoo360.reader.json.JsonUtils;
import com.qihoo360.reader.listener.OnGetArticlesResultListener;
import com.qihoo360.reader.offline.OfflineTask;
import com.qihoo360.reader.push.PushManager;
import com.qihoo360.reader.subscription.Article;
import com.qihoo360.reader.subscription.Channel;
import com.qihoo360.reader.subscription.Content;
import com.qihoo360.reader.subscription.SubscribedChannel;
import com.qihoo360.reader.subscription.SubscribedChannel.SortFloat;
import com.qihoo360.reader.subscription.reader.RssArticle.RssArticleJson;
import com.qihoo360.reader.support.AsyncTask;
import com.qihoo360.reader.support.NetUtils;
import com.qihoo360.reader.support.Utils;
import com.qihoo360.reader.ui.ReaderPlugin;
import com.qihoo360.reader.ui.articles.ArticleUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.database.Cursor;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 表示“频道”，它在“主页”里面，包含了“内容”
 * 
 * @author Jiongxuan Zhang
 */
public class RssChannel extends Channel {
    /**
     * 在Wifi条件下，最大能获取的文章数
     */
    public static final int MAX_WIFI_SEE_ARTICLE_COUNT = 20;

    /**
     * 在蜂窝环境下，最大能获取的文章数
     */
    public static final int MAX_CELLULAR_SEE_ARTICLE_COUNT = 10;

    private static JsonGetterBase mJsonGetter;

    private long mGetNewArticlePrevContentId = -1;

    GetNewArticleTask mGetNewArticleTask;

    GetOldArticleTask mGetOldArticleTask;

    // 如果用户在进入频道后订阅了它，则下面的信息需要“补存”进Subscribe
    long newest_image_content_id = -1;

    long last_content_id = 0;

    OfflineTask mOfflineTask;

    static class RssChannelJson extends RssArticleJson {

        /**
         * 仅为填充文章而解析
         * 
         * @param jsonString
         * @return
         */
        public Object parseWithFull(String jsonString) {
            return super.parse(jsonString);
        }

        @Override
        public Object parse(String jsonString) {
            Content content = new Content();
            try {
                JSONObject contentJson = new JSONObject(jsonString);
                content.response = contentJson.getInt("response"); // 响应
                content.channel = JsonUtils.getJsonString(contentJson, "channel");
                content.number = JsonUtils.getJsonInt(contentJson, "number");
                /**
                 * jsonString =
                 * {"response":1,"number":1,"channel":"rednet","entry"
                 * :[{"contentid":761388805,"title":..."
                 */
                if (content.response == 1) {
                    // content.entry
                    JSONArray entryArray = contentJson.getJSONArray("entry");
                    List<RssArticle> articles = new ArrayList<RssArticle>();
                    for (int i = 0; i < entryArray.length(); i++) {
                        RssArticle article = new RssArticle();
                        JSONObject articleJson = (JSONObject) entryArray.opt(i);
                        article.contentid = JsonUtils.getJsonInt(articleJson, "contentid");
                        article.title = JsonUtils.getJsonString(articleJson, "title");
                        article.images360 = JsonUtils.getJsonString(articleJson, "images360");
                        article.pubdate = JsonUtils.getJsonInt(articleJson, "pubdate");
                        article.isDownloaded = false;
                        articles.add(article);
                    }
                    content.entry = articles;
                }
                // end
            } catch (Exception e) {
                Utils.error(getClass(), Utils.getStackTrace(e));
            }
            return content;
        }
    }

    public static RssChannel get(String name) {
        if (RssManager.mChannelMap == null) {
            RssManager.getIndex(); // 初始化这个Map
        }

        if (RssManager.mChannelMap != null) {
            return RssManager.mChannelMap.get(name);
        } else {
            return null;
        }
    }

    /**
     * 根据ContentId获取Article对象
     * 
     * @param resolver
     * @param contentId
     * @return
     */
    public RssArticle getArticle(ContentResolver resolver, long contentId) {
        return DataEntryManager.ArticleHelper.getByContentId(resolver, channel, contentId);
    }

    /**
     * 获取最新的文章
     * 
     * @param resolver
     * @return
     */
    public Article getNewestArticle(ContentResolver resolver) {
        Article result = null;
        Cursor cursor = null;
        try {
            cursor = resolver.query(Articles.CONTENT_URI, ArticleHelper.PROJECTIONS,
                    Tables.Articles.CHANNEL + "=? AND " + Articles.STAR + "<"
                            + Article.STAR_DISAPPEARANCE, new String[] {
                        channel
                    }, Articles.CONTENT_ID + " desc limit 1");

            if (cursor != null && cursor.getCount() == 1 && cursor.moveToNext()) {
                result = RssArticle.inject(cursor);
            }
        } catch (Exception e) {
            Utils.error(getClass(), Utils.getStackTrace(e));
        }

        if (cursor != null) {
            cursor.close();
        }

        return result;

    }

    /**
     * 获取最新的文章Id
     * 
     * @param resolver
     * @return
     */
    public long getNewestContentId(ContentResolver resolver) {

        long result = 0;
        Cursor cursor = null;
        try {
            cursor = resolver.query(Articles.CONTENT_URI, new String[] {
                Articles.CONTENT_ID
            }, Tables.Articles.CHANNEL + "=? AND " + Articles.STAR + "<"
                    + Article.STAR_DISAPPEARANCE, new String[] {
                channel
            }, Articles.CONTENT_ID + " desc limit 1");

            if (cursor != null && cursor.getCount() == 1 && cursor.moveToNext()) {
                result = cursor.getLong(0);
            }
        } catch (Exception e) {
            Utils.error(getClass(), Utils.getStackTrace(e));
        }

        if (cursor != null) {
            cursor.close();
        }

        return result;
    }

    @Override
    public Article getOldestArticle(ContentResolver resolver) {
        Article result = null;
        Cursor cursor = null;
        try {
            cursor = resolver.query(Articles.CONTENT_URI, ArticleHelper.PROJECTIONS,
                    Tables.Articles.CHANNEL + "=? AND " + Articles.STAR + "<"
                            + Article.STAR_DISAPPEARANCE, new String[] {
                        channel
                    }, Articles.CONTENT_ID + " limit 1");

            if (cursor != null && cursor.getCount() == 1 && cursor.moveToNext()) {
                result = RssArticle.inject(cursor);
            }
        } catch (Exception e) {
            Utils.error(getClass(), Utils.getStackTrace(e));
        }

        if (cursor != null) {
            cursor.close();
        }

        return result;

    }

    @Override
    public long getOldestContentId(ContentResolver resolver) {
        long result = 0;
        Cursor cursor = null;
        try {
            cursor = resolver.query(Articles.CONTENT_URI, new String[] {
                Articles.CONTENT_ID
            }, Tables.Articles.CHANNEL + "=? AND " + Articles.STAR + "<"
                    + Article.STAR_DISAPPEARANCE, new String[] {
                channel
            }, Articles.CONTENT_ID + " limit 1");

            if (cursor != null && cursor.getCount() == 1 && cursor.moveToNext()) {
                result = cursor.getLong(0);
                cursor.close();
            }
        } catch (Exception e) {
            Utils.error(getClass(), Utils.getStackTrace(e));
        }

        if (cursor != null) {
            cursor.close();
        }

        return result;

    }

    /**
     * 获取可以搜索的项目
     * 
     * @return
     */
    public List<String> getSearchItems() {
        String string[] = TextUtils.split(this.pinyin, "\\|");
        List<String> items = new ArrayList<String>();
        for (int i = 0; i < string.length; i++) {
            items.add(string[i]);
        }
        return items;
    }

    /**
     * 获取订阅的频道对象
     * 
     * @param resolver
     * @return
     */
    public SubscribedChannel getSubscribedChannel(ContentResolver resolver) {
        return RssSubscribedChannel.get(resolver, channel);
    }

    /**
     * 直接获取当前频道内，在本地中所有的消息
     * 
     * @param contentResolver
     * @return
     */
    public Cursor getFullCursor(ContentResolver resolver) {
        return DataEntryManager.ArticleHelper.Normal.getCursorByChannel(resolver, channel, false);
    }

    /**
     * 直接获取当前频道内，只含标题和摘要的Cursor
     * 
     * @param resolver
     * @return
     */
    public Cursor getAbstractCursor(ContentResolver resolver) {
        return DataEntryManager.ArticleHelper.Normal.getCursorByChannel(resolver, channel, true);
    }

    /**
     * 获取你所需要的字段
     * 
     * @param resolver
     * @param projections
     * @return
     */
    public Cursor getCursorOfSpecificFields(ContentResolver resolver, String[] projections) {
        return DataEntryManager.ArticleHelper.Normal.getCursorOfSpecificFieldsByChannel(resolver,
                channel, projections);
    }

    @Override
    public int getCount(ContentResolver resolver, long from, long to) {
        if (from == to) {
            return 0;
        }

        int result = 0;
        Cursor cursor = null;
        try {
            cursor = resolver.query(Articles.CONTENT_URI, new String[] {
                Articles._ID
            }, Articles.CONTENT_ID + ">? AND " + Articles.CONTENT_ID + "<=? AND "
                    + Articles.CHANNEL + "=?", new String[] {
                    String.valueOf(to), String.valueOf(from), channel
            }, null);

            if (cursor != null) {
                result = cursor.getCount();
                cursor.close();
            }
        } catch (Exception e) {
            Utils.error(getClass(), Utils.getStackTrace(e));
        }

        if (cursor != null) {
            cursor.close();
        }

        return result;

    }

    private static int getDownloadCount() {
        int count = 0;
        if (NetUtils.isWifiConnected()) {
            count = MAX_WIFI_SEE_ARTICLE_COUNT;
        } else {
            count = MAX_CELLULAR_SEE_ARTICLE_COUNT;
        }
        return count;
    }

    protected String getNewUrl(long sinceId, int count, boolean isAbstract) {
        return getUrl(0, count, sinceId, isAbstract);
    }

    protected String getOldUrl(long fromId, int count, boolean isAbstract) {
        return getUrl(fromId, count, 0, isAbstract);
    }

    protected String getUrl(long fromId, int count, long sinceId, boolean isAbstract) {
        if (isAbstract) { // 文章摘要
            return ServerUris.getTitleList(channel, fromId, count, sinceId);
        } else {
            return ServerUris.getContentList(channel, fromId, count, sinceId);
        }
    }

    @Override
    protected int getNewArticles(ContentResolver resolver,
            final OnGetArticlesResultListener listener, boolean isSync, int count,
            boolean isAbstract) {
        if (count == 0) {
            count = getDownloadCount();
        }

        mGetNewArticleTask = new GetNewArticleTask(resolver, this, listener, count, isAbstract);

        if (isSync) { // 同步
            return mGetNewArticleTask.doSync();
        } else {
            sIsRunningTask = true;
            mGetNewArticleTask.execute();
            return 0;
        }
    }

    @Override
    protected int getOldArticles(ContentResolver resolver,
            final OnGetArticlesResultListener listener, boolean isSync, long fromId, long sinceId,
            int count, boolean isAbstract) {
        if (count == 0) {
            count = getDownloadCount();
        }

        mGetOldArticleTask = new GetOldArticleTask(resolver, this, listener, fromId, sinceId,
                count, isAbstract);

        if (isSync) {
            return mGetOldArticleTask.doSync();
        } else {
            Channel.sIsRunningTask = true;
            mGetOldArticleTask.execute();
            return 0;
        }
    }

    /**
     * 获取最新带图片的文章Id
     * 
     * @return
     */
    public long getNewestImageOfContentId(ContentResolver resolver) {
        SubscribedChannel subscribedChannel = getSubscribedChannel(resolver);
        if (subscribedChannel != null) {
            return subscribedChannel.getNewestImageOfContentId();
        }

        // 如果是从随便看看进去的，则要单独计算
        return forceGetNewestImageOfContentId(resolver);
    }

    static abstract class GetArticleTaskBase extends AsyncTask<Integer, Integer, Integer> {

        protected ContentResolver mResolver;

        protected RssChannel mChannel;

        protected OnGetArticlesResultListener mListener;

        protected long mFrom = -1;

        protected long mTo = -1;

        protected int mCount = -1;

        protected int mCountForResult = -1;

        protected boolean mIsDeleted = false;

        protected boolean mIsAbstract = true;

        public GetArticleTaskBase(ContentResolver resolver, RssChannel channel,
                OnGetArticlesResultListener listener, int count, boolean isAbstract) {
            mResolver = resolver;
            mChannel = channel;
            mListener = listener;
            mCount = count;
            mIsAbstract = isAbstract;
            if (Constants.CLOSE_ABSTRACT_ARTICLES) {
                mIsAbstract = false;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Integer result) {
            sIsRunningTask = false;
            if (mListener != null && !isCancelled()) {
                switch (result) {
                    case RESULT_OK:
                        mListener.onCompletion(mFrom, mTo, mCountForResult, mIsDeleted);
                        break;

                    case RESULT_NOT_EXISTS:
                        mListener.onNotExists(mIsDeleted);
                        break;

                    case RESULT_FAILURE:
                        mListener.onFailure(0);
                        break;
                }
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onCancelled()
         */
        @Override
        protected void onCancelled() {
            super.onCancelled();
            sIsRunningTask = false;
        }

        /**
         * 取消下载过程
         */
        public void cancel() {
            getJsonGetter().stop();
            cancel(true);
        }

        protected Content download(String url) {
            if (TextUtils.isEmpty(url)) {
                throw new IllegalArgumentException("url is empty");
            }

            String jsonString = mChannel.getJsonString(url, getJsonGetter());
            if (TextUtils.isEmpty(jsonString) == false) {
                if (mIsAbstract) {
                    return (Content) getJsonGetter().parse(jsonString);
                } else {
                    return (Content) ((RssChannelJson) getJsonGetter()).parseWithFull(jsonString);
                }
            }
            return null;
        }

        private JsonGetterBase getJsonGetter() {
            if (mJsonGetter == null) {
                mJsonGetter = new RssChannelJson();
            }

            return mJsonGetter;
        }

        protected synchronized void insertContentToDb(Content content) {
            List<RssArticle> list = content.getItems();
            RssArticle firstArticle = list.get(0);
            RssArticle lastArticle = list.get(list.size() - 1);

            mFrom = firstArticle.contentid;
            mTo = lastArticle.contentid;
            mCountForResult = list.size();

            // 防止重复插入DB，并修改已有的星标
            Map<Long, Boolean> mapAll = DataEntryManager.ArticleHelper
                    .getContentIdAndDownloadedFlags(mResolver, mChannel.channel,
                            firstArticle.contentid, lastArticle.contentid);
            List<Long> listStared = DataEntryManager.ArticleHelper.Star.getContentIds(mResolver,
                    mChannel.channel, firstArticle.contentid, lastArticle.contentid,
                    RssArticle.STAR_DISAPPEARANCE);

            long higherPubDate = Long.MAX_VALUE; // 用来对比和排序
            for (RssArticle article : list) {
                if (listStared.contains(article.contentid)) {
                    // 只需要把star=2变成star=1即可
                    DataEntryManager.ArticleHelper.Star.updateStarByContentId(mResolver,
                            mChannel.channel, article.contentid, RssArticle.STAR_APPEARANCE);
                    article.star = RssArticle.STAR_APPEARANCE;
                }
                if (article.pubdate > higherPubDate) {
                    article.pubdate = higherPubDate - 60;
                }
                article.channel = mChannel.channel;

                // 去重处理
                Boolean isDownloaded = mapAll.get(article.contentid);
                if (isDownloaded == null) {
                    DataEntryManager.ArticleHelper.add(mResolver, article);
                } else if (!isDownloaded) {
                    DataEntryManager.ArticleHelper.update(mResolver, article);
                }
                higherPubDate = article.pubdate;
            }
        }
    }

    /**
     * 从服务器端获取Json字符串
     * 
     * @param url
     * @param getter
     * @return
     */
    protected String getJsonString(String url, JsonGetterBase getter) {
        if (getter == null) {
            throw new IllegalArgumentException("getter is null");
        }

        return getter.get(url);
    }

    private static final int MAX_RESTORE_ROW = 120;

    private byte[] GET_NEW_SYNC = new byte[0];

    static class GetNewArticleTask extends GetArticleTaskBase {

        public GetNewArticleTask(ContentResolver resolver, RssChannel channel,
                OnGetArticlesResultListener listener, int count, boolean isAbstract) {
            super(resolver, channel, listener, count, isAbstract);
        }

        @Override
        protected Integer doInBackground(Integer... params) {
            synchronized (mChannel.GET_NEW_SYNC) {
                // 给测试人员加Tag
                Utils.debug("prefmark", "Refresh-Begin %d", System.currentTimeMillis());

                mChannel.mGetNewArticlePrevContentId = mChannel.getNewestContentId(mResolver);

                // 看是否需要限制下载文章的个数，count为正表示需要限制，为负表示不需要
                long sinceId = 0;
                if (mCount > 0) {
                    sinceId = mChannel.mGetNewArticlePrevContentId;
                } else {
                    mCount = -mCount;
                }
                String url = mChannel.getNewUrl(sinceId, mCount, mIsAbstract);

                Utils.debug(getClass(), "doInBackground() >> sinceId = %d; url = %s", sinceId, url);

                Content content = download(url);
                if (isCancelled() == true) {
                    return RESULT_USER_CANCELLED;
                }
                if (content != null) {
                    return addContentToDb(sinceId, content);
                }

                return RESULT_FAILURE; // failure
            }
        }

        /**
         * @param sinceId
         * @param content
         */
        synchronized int addContentToDb(long sinceId, Content content) {
            if (content.isOk()) {
                mChannel.mIsDbBusy = true;

                List<RssArticle> list = content.getItems();
                RssArticle lastArticle = list.get(list.size() - 1);

                // 检查是否有“未下载的地方”
                if (sinceId > 0 && list.size() >= mCount) {
                    if (lastArticle != null && lastArticle.contentid - sinceId > 1) {
                        DataEntryManager.ArticleHelper.Normal
                                .deleteAll(mResolver, mChannel.channel);
                    }
                }

                insertContentToDb(content);
                clearOldCache();

                mChannel.newest_image_content_id = mChannel
                        .forceGetNewestImageOfContentId(mResolver);
                SubscribedChannel subscribedChannel = mChannel.getSubscribedChannel(mResolver);
                if (subscribedChannel != null) {
                    long newestImageId = mChannel.newest_image_content_id;
                    subscribedChannel.updateNewestImageOfContentId(mResolver, newestImageId);
                }

                mChannel.mIsDbBusy = false;

                return RESULT_OK;
            } else if (content.isNotExists()) {
                return RESULT_NOT_EXISTS;
            }

            return RESULT_FAILURE;
        }

        private void clearOldCache() {
            // 如果是Push或离线下载该频道，则不清除缓存
            if (PushManager.getPushingChannel() == mChannel || mChannel.offline().isRunning()) {
                return;
            }

            // 去掉旧的记录
            List<Long> contentIdList = DataEntryManager.ArticleHelper.Normal.getContentIds(
                    mResolver, mChannel.channel);
            if (contentIdList != null && contentIdList.size() > MAX_RESTORE_ROW) {
                long limitContentId = contentIdList.get(MAX_RESTORE_ROW - 1);
                String where = String.format("%s < %d", Articles.CONTENT_ID, limitContentId);
                DataEntryManager.ArticleHelper.Normal.delete(mResolver, mChannel.channel, where);
            }

            if (Constants.DEBUG) {
                contentIdList = DataEntryManager.ArticleHelper.Normal.getContentIds(mResolver,
                        mChannel.channel);
                String log = "";
                if (contentIdList != null) {
                    for (long contentId : contentIdList) {
                        log += contentId + " ";
                    }
                }
                Utils.debug(getClass(), "(%s)finally we have: %s", mChannel.channel, log);
            }
        }

        /*
         * (non-Javadoc)
         * @see
         * com.qihoo360.reader.subscription.Channel.GetArticleTaskBase#onPostExecute
         * (java.lang.Integer)
         */
        @Override
        protected void onPostExecute(Integer result) {
            mChannel.mGetNewArticleTask = null;
            super.onPostExecute(result);

            // 给测试人员加Tag
            Utils.debug("prefmark", "Refresh-End %d", System.currentTimeMillis());
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onCancelled()
         */
        @Override
        protected void onCancelled() {
            mChannel.mGetNewArticleTask = null;
            super.onCancelled();

            // 给测试人员加Tag
            Utils.debug("prefmark", "Refresh-End %d", System.currentTimeMillis());
        }
    }

    static class GetOldArticleTask extends GetArticleTaskBase {

        private long mSinceId = -1;

        private long mFromId = 0;

        public GetOldArticleTask(ContentResolver resolver, RssChannel channel,
                OnGetArticlesResultListener listener, long fromId, long sinceId, int count,
                boolean isAbstract) {
            super(resolver, channel, listener, count, isAbstract);
            mFromId = fromId;
            mSinceId = sinceId;
        }

        @Override
        protected Integer doInBackground(Integer... params) {
            // 给测试人员加Tag
            Utils.debug("prefmark", "LoadMore-Begin %d", System.currentTimeMillis());

            SubscribedChannel subChannel = mChannel.getSubscribedChannel(mResolver);

            if (mSinceId == -1) {
                // 如果不是取范围，则判断是否需要拿更旧的文章
                mFromId = mChannel.getOldestContentId(mResolver) - 1;

                if (subChannel != null) {
                    mChannel.last_content_id = subChannel.last_content_id;
                }
                if (mFromId <= 0 || (mFromId + 1 <= mChannel.last_content_id)) {
                    return RESULT_NOT_EXISTS;
                }
            }

            String url = mChannel.getUrl(mFromId, mCount, mSinceId, mIsAbstract);
            Utils.debug(getClass(), "doInBackground() >> from = %d; url = %s", mFromId, url);

            Content content = download(url);
            if (isCancelled() == true) {
                return RESULT_USER_CANCELLED;
            }
            if (content != null) {
                if (content.isOk()) {
                    insertContentToDb(content);
                    return RESULT_OK;
                } else if (content.isNotExists()) {

                    if (mSinceId == -1) {
                        // 如果不是取范围，而是拿最旧的文章，则需要记录最后一篇文章的id
                        mChannel.last_content_id = mFromId + 1;
                        if (subChannel != null) {
                            subChannel.setLastContentId(mResolver, mChannel.last_content_id);
                        }
                    }
                    return RESULT_NOT_EXISTS;
                }
            }

            return RESULT_FAILURE;
        }

        /*
         * (non-Javadoc)
         * @see
         * com.qihoo360.reader.subscription.Channel.GetArticleTaskBase#onPostExecute
         * (java.lang.Integer)
         */
        @Override
        protected void onPostExecute(Integer result) {
            mChannel.mGetOldArticleTask = null;
            super.onPostExecute(result);

            // 给测试人员加Tag
            Utils.debug("prefmark", "LoadMore-End %d", System.currentTimeMillis());
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onCancelled()
         */
        @Override
        protected void onCancelled() {
            mChannel.mGetOldArticleTask = null;
            super.onCancelled();

            // 给测试人员加Tag
            Utils.debug("prefmark", "LoadMore-End %d", System.currentTimeMillis());
        }
    }

    long forceGetNewestImageOfContentId(ContentResolver resolver) {
        Cursor cursor = getFullCursor(resolver);
        long result = -1;
        while (cursor != null && cursor.moveToNext()) {
            Article article = Article.inject(cursor);
            if (article.contentid <= mGetNewArticlePrevContentId) {
                break;
            }
            String url = ArticleUtils.getFirstValidImageUrlForHeaderView(article.images360);
            if (!TextUtils.isEmpty(url)) {
                result = article.contentid;
                break;
            }
        }

        if (cursor != null) {
            cursor.close();
        }
        Utils.debug(getClass(), "forceGetNewestImageOfContentId() -> newestId = %d", result);
        return result;
    }

    /**
     * 停止获取新的文章。
     */
    public void stopGetNew() {
        if (mGetNewArticleTask != null) {
            mGetNewArticleTask.cancel();
            mGetNewArticleTask = null;
        }
    }

    /**
     * 停止获取旧的文章。
     */
    public void stopGetOld() {
        if (mGetOldArticleTask != null) {
            mGetOldArticleTask.cancel();
            mGetOldArticleTask = null;
        }
    }

    /**
     * 订阅该频道。
     * 
     * @param resolver
     * @return
     */
    public SubscribedChannel subscribe(ContentResolver resolver) {
        ReaderPlugin.sIsSubscribedChanged = true;
        RssSubscribedChannel subscribedChannel = new RssSubscribedChannel();
        subscribedChannel.channel = channel;
        subscribedChannel.photo_url = image;
        subscribedChannel.title = title;
        subscribedChannel.sub_date = System.currentTimeMillis();
        subscribedChannel.offline = true;

        // 排序
        SortFloat sortFloat = new SortFloat();
        if (Settings.getAddChannelToTail()) {
            // 订阅频道，并放到末尾
            sortFloat.left = DataEntryManager.SubscriptionHelper.getLastSortFloat(resolver);
            sortFloat.right = SortFloat.MAX;
        } else {
            // 放到开头
            sortFloat.left = SortFloat.MIN;
            sortFloat.right = DataEntryManager.SubscriptionHelper.getFirstSortFloat(resolver);
        }
        subscribedChannel.sort_float = subscribedChannel.calculateSortFloat(resolver, sortFloat);

        // 补存下面几个信息
        subscribedChannel.newest_image_content_id = newest_image_content_id;
        subscribedChannel.last_content_id = last_content_id;
        try {
            subscribedChannel.image_version = Integer.valueOf(imageversion);
        } catch (Exception e) {
            Utils.error(getClass(), Utils.getStackTrace(e));
            subscribedChannel.image_version = 0;
        }
        if (subscribedChannel.insertToDb(resolver) == false) {
            return null;
        } else {
            return subscribedChannel;
        }
    }

    /**
     * 取消该频道的订阅
     * 
     * @param resolver
     */
    public void unsubscribe(ContentResolver resolver) {
        if (channel != RssSubscribedChannel.RANDOM_READ
                && channel != RssSubscribedChannel.ADD_SUBSCRIBE
                && channel != RssSubscribedChannel.MY_CONLLECTION
                && channel != RssSubscribedChannel.OFFLINE_DOWNLOAD) {
            RssSubscribedChannel.deleteByChannel(resolver, channel);
        }
    }

    /**
     * 清空该频道里的所有文章
     * 
     * @param resolver
     */
    public int clearAllArticles(ContentResolver resolver) {
        return DataEntryManager.ArticleHelper.Normal.deleteAll(resolver, channel);
    }

    /**
     * 获取离线任务
     * 
     * @return
     */
    public OfflineTask offline() {
        if (mOfflineTask == null) {
            mOfflineTask = new OfflineTask(this);
        }

        return mOfflineTask;
    }

    @Override
    public String getAlbumCoverDataFieldName() {
        return Articles.COMPRESSED_IMAGE_URL;
    }
}
