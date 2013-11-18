/**
 *
 */
package com.qihoo.ilike.subscription;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.AsyncTask;

import com.qihoo.ilike.data.DataEntryManager;
import com.qihoo.ilike.data.DataEntryManager.BookmarkHelper;
import com.qihoo.ilike.data.DataEntryManager.SubscriptionHelper;
import com.qihoo.ilike.data.Tables;
import com.qihoo.ilike.data.Tables.Bookmarks;
import com.qihoo.ilike.data.Tables.Subscriptions;
import com.qihoo.ilike.http.core.IHttpRequest.HttpRequestStatus;
import com.qihoo.ilike.json.CategorysGetter;
import com.qihoo.ilike.json.IlikeJsonGetter;
import com.qihoo.ilike.json.listener.OnCategorysResultListener;
import com.qihoo.ilike.vo.Bookmark;
import com.qihoo.ilike.vo.Category;
import com.qihoo.ilike.vo.CategoryList;
import com.qihoo.ilike.vo.ErrorInfo;
import com.qihoo.ilike.vo.Pic;
import com.qihoo.ilike.vo.User;
import com.qihoo360.reader.R;
import com.qihoo360.reader.ReaderApplication;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.data.DataEntryManager.ArticleHelper;
import com.qihoo360.reader.data.Tables.Articles;
import com.qihoo360.reader.listener.OnGetArticlesResultListener;
import com.qihoo360.reader.offline.IlikeOfflineTask;
import com.qihoo360.reader.offline.OfflineTask;
import com.qihoo360.reader.push.PushManager;
import com.qihoo360.reader.subscription.Article;
import com.qihoo360.reader.subscription.Channel;
import com.qihoo360.reader.subscription.SubscribedChannel;
import com.qihoo360.reader.subscription.reader.RssArticle;
import com.qihoo360.reader.support.FileUtils;
import com.qihoo360.reader.support.NetUtils;
import com.qihoo360.reader.support.Utils;
import com.qihoo360.reader.ui.ReaderPlugin;

/**
 * @author Jiongxuan Zhang
 *
 */
public abstract class IlikeChannel extends Channel {

    private static final String CATEGORY_FILE_NAME = "i5.dat";

    /**
     * 表示“我喜欢”
     */
    public static final String ILIKE_PREFIX = "ilike:";

    /**
     * 表示“我喜欢——我的收藏”
     */
    public static final String ILIKE_MY_COLLECTION = ILIKE_PREFIX
            + "my_collection";

    /**
     * 表示“我喜欢——热门收藏（全部）”
     */
    public static final String ILIKE_HOT_COLLECTION = ILIKE_PREFIX + "-1";

    /**
     * 表示“我喜欢——最近收藏”
     */
    public static final String ILIKE_LASTEST_COLLECTION = ILIKE_PREFIX + "-2";

    /**
     * 表示“我喜欢——我的关注”
     */
    public static final String ILIKE_MY_FOCUS = ILIKE_PREFIX + "my_focus";

    private static final int MAX_RESTORE_ROW = 180;

    static Map<String, IlikeChannel> mChannelMap;

    private static List<IlikeChannel> mHotCollectionList;

    private static String mVersion;

    public enum LevelType {
        Special, HotCollectionItem, TopLevel
    }

    public LevelType levelType = LevelType.HotCollectionItem;

    protected IlikeChannel(String channel, String title, int type,
            LevelType levelType) {
        this.channel = channel;
        this.title = title;
        this.type = type;
        this.levelType = levelType;
    }

    /**
     * 获取某个Channel频道
     *
     * @param name
     * @return
     */
    public static IlikeChannel get(String name) {
        initMap();

        return mChannelMap.get(name);
    }

    /**
     * 获取所有的ILikeChannel
     *
     * @return
     */
    public static Collection<IlikeChannel> getChannels() {
        initMap();

        return mChannelMap.values();
    }

    /**
     * 查看Map是否被初始化以供使用。
     *
     * @return
     */
    public static boolean isInitMap() {
        return mChannelMap != null;
    }

    private static boolean initMap() {
        if (mChannelMap == null) {
            mChannelMap = new HashMap<String, IlikeChannel>();
        } else {
            mChannelMap.clear();
        }

        if (mHotCollectionList == null) {
            mHotCollectionList = new ArrayList<IlikeChannel>();
        } else {
            mHotCollectionList.clear();
        }

        String json = FileUtils.getTextFromReaderFile(R.raw.i5,
                CATEGORY_FILE_NAME);
        CategoryList list = new CategoryList();
        try {
            list.builder(new JSONObject(json));
        } catch (JSONException e) {
            Utils.error(IlikeChannel.class, Utils.getStackTrace(e));
            return false;
        }
        mVersion = list.ver;

        mChannelMap.put(ILIKE_MY_COLLECTION, new IlikeMyCollectionChannel());
        mChannelMap.put(ILIKE_MY_FOCUS, new IlikeMyFocusChannel());

        for (Category category : list.categoryList) {
            LevelType levelType = category.id.charAt(0) == '-' ? LevelType.TopLevel
                    : LevelType.HotCollectionItem;
            IlikeChannel categoryChannel = new IlikeHotCollectionChannel(
                    category.id, category.name, levelType);
            mChannelMap.put(categoryChannel.channel, categoryChannel);

            if (categoryChannel.type == TYPE_ILIKE_HOT_COLLECTION
                    && categoryChannel.levelType == LevelType.HotCollectionItem) {
                mHotCollectionList.add(categoryChannel);
            }
        }

        return true;
    }

    /**
     * 获取所有位于“热门收藏”的项
     *
     * @return
     */
    public static List<IlikeChannel> getHotCollectionChannels() {
        return mHotCollectionList;
    }

    private static CategorysGetter sEveryoneLikeGetter;

    /**
     * 根据版本号判断该不该下载全部的频道列表，并作出选择
     *
     * @param listener
     * @return 有没有必要更新，True表示开始下载并更新
     */
    public static boolean checkIfNeedUpdateAsync(
            final OnDownloadCategoryListResultListener listener) {

        /*
         * 何时下载？ 1. 前提：再网，且超过1天 1.1 在蜂窝网络环境下，且确认设置中勾选了允许下载选项 （或者） 1.2 在Wifi环境下
         */
        long lastUpdated = Settings.getLastIlikeCategoryListUpdatedDate();
        if (NetUtils.isNetworkAvailable()
                && System.currentTimeMillis() - lastUpdated > 24 * 60 * 60 * 1000
                && (NetUtils.isWifiConnected() || Settings.is3GModeAutoUpdate())) {
            return forceUpdateAsync(listener);
        }

        return false;
    }

    /**
     * 强制下载全部的频道
     *
     * @param listener
     * @return
     */
    public static boolean forceUpdateAsync(
            final OnDownloadCategoryListResultListener listener) {
        if (isInitMap() && !initMap() && NetUtils.isNetworkAvailable()) {
            return false;
        }

        update(listener);

        return false;
    }

    /**
     * 立即开始更新最新的分类
     */
    public static boolean update(
            final OnDownloadCategoryListResultListener listener) {
        if (!NetUtils.isNetworkAvailable()) {
            return false;
        }

        sEveryoneLikeGetter = new CategorysGetter();
        sEveryoneLikeGetter.get(new OnCategorysResultListener() {

            @Override
            public void onResponseError(ErrorInfo errorInfo) {
                sEveryoneLikeGetter = null;
                if (listener != null) {
                    listener.onFailure();
                }
            }

            @Override
            public void onRequestFailure(HttpRequestStatus errorStatus) {
                sEveryoneLikeGetter = null;
                if (listener != null) {
                    listener.onFailure();
                }
            }

            @Override
            public void onComplete(String jsonText, CategoryList list) {
                if (list.ver.equals(mVersion) && listener != null) {
                    listener.onAlreadyLastestVersion();
                    return;
                }

                FileWriter writer;
                try {
                    writer = new FileWriter(
                            FileUtils.getFilePathToSave(
                                    ReaderApplication.getContext(),
                                    CATEGORY_FILE_NAME), false);
                    writer.write(jsonText);
                    writer.flush();
                    writer.close();

                    initMap();

                    if (listener != null) {
                        listener.onUpdated();
                    }

                    Settings.setLastIlikeCategoryListUpdatedDate(System
                            .currentTimeMillis());
                    sEveryoneLikeGetter = null;
                } catch (IOException e) {
                    Utils.error(IlikeChannel.class, Utils.getStackTrace(e));
                }
            }
        }, mVersion);

        return true;
    }

    /**
     * 停止更新
     */
    public static void stopUpdate() {
        if (sEveryoneLikeGetter != null) {
            sEveryoneLikeGetter.stop();
            sEveryoneLikeGetter = null;
        }
    }

    private OfflineTask mOfflineTask;

    /*
     * (non-Javadoc)
     *
     * @see com.qihoo360.reader.subscription.Channel#getArticle(android.content.
     * ContentResolver, long)
     */
    @Override
    public Article getArticle(ContentResolver resolver, long contentId) {
        RssArticle article = null;
        Cursor cursor = null;
        try {
            cursor = resolver.query(Articles.CONTENT_URI,
                    ArticleHelper.PROJECTIONS, Tables.Bookmarks.CATEGORY_SRV_ID
                            + "=? AND " + Tables.Bookmarks.BOOKMARK_ID + "=?",
                    new String[] { channel, String.valueOf(contentId) }, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    article = ArticleHelper.inject(cursor);
                }
                cursor.close();
            }
        } catch (Exception e) {
            Utils.error(IlikeChannel.class, Utils.getStackTrace(e));
        }

        if (cursor != null) {
            cursor.close();
        }

        return article;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.qihoo360.reader.subscription.Channel#getNewestArticle(android.content
     * .ContentResolver)
     */
    @Override
    public Article getNewestArticle(ContentResolver resolver) {
        Article result = null;
        Cursor cursor = null;
        try {
            cursor = resolver.query(Bookmarks.CONTENT_URI,
                    BookmarkHelper.PROJECTIONS, Bookmarks.CATEGORY_SRV_ID
                            + "=?", new String[] { channel }, Bookmarks.SORT
                            + " limit 1");

            if (cursor != null && cursor.getCount() == 1 && cursor.moveToNext()) {
                result = IlikeArticle.inject(cursor);
            }
        } catch (Exception e) {
            Utils.error(IlikeChannel.class, Utils.getStackTrace(e));
        }

        if (cursor != null) {
            cursor.close();
        }

        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.qihoo360.reader.subscription.Channel#getNewestContentId(android.content
     * .ContentResolver)
     */
    @Override
    public long getNewestContentId(ContentResolver resolver) {
        long result = 0;
        Cursor cursor = null;
        try {
            cursor = resolver.query(Bookmarks.CONTENT_URI,
                    new String[] { Bookmarks.BOOKMARK_ID },
                    Bookmarks.CATEGORY_SRV_ID + "=?", new String[] { channel },
                    Bookmarks.SORT + " limit 1");

            if (cursor != null && cursor.getCount() == 1 && cursor.moveToNext()) {
                result = cursor.getLong(0);
            }
        } catch (Exception e) {
            Utils.error(IlikeChannel.class, Utils.getStackTrace(e));
        }

        if (cursor != null) {
            cursor.close();
        }

        return result;

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.qihoo360.reader.subscription.Channel#getNewestArticle(android.content
     * .ContentResolver)
     */
    @Override
    public Article getOldestArticle(ContentResolver resolver) {
        Article result = null;
        Cursor cursor = null;
        try {
            cursor = resolver.query(Bookmarks.CONTENT_URI,
                    BookmarkHelper.PROJECTIONS, Bookmarks.CATEGORY_SRV_ID
                            + "=?", new String[] { channel }, Bookmarks.SORT
                            + " desc limit 1");

            if (cursor != null && cursor.getCount() == 1 && cursor.moveToNext()) {
                result = IlikeArticle.inject(cursor);
            }
        } catch (Exception e) {
            Utils.error(IlikeChannel.class, Utils.getStackTrace(e));
        }

        if (cursor != null) {
            cursor.close();
        }

        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.qihoo360.reader.subscription.Channel#getOldestContentId(android.content
     * .ContentResolver)
     */
    @Override
    public long getOldestContentId(ContentResolver resolver) {
        long result = 0;
        Cursor cursor = null;
        try {
            cursor = resolver.query(Bookmarks.CONTENT_URI,
                    new String[] { Bookmarks.BOOKMARK_ID },
                    Bookmarks.CATEGORY_SRV_ID + "=?", new String[] { channel },
                    Bookmarks.SORT + " desc limit 1");

            if (cursor != null && cursor.getCount() == 1 && cursor.moveToNext()) {
                result = cursor.getLong(0);
            }
        } catch (Exception e) {
            Utils.error(IlikeChannel.class, Utils.getStackTrace(e));
        }

        if (cursor != null) {
            cursor.close();
        }

        return result;

    }

    /*
     * (non-Javadoc)
     *
     * @see com.qihoo360.reader.subscription.Channel#getSearchItems()
     */
    @Override
    public List<String> getSearchItems() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.qihoo360.reader.subscription.Channel#getSubscribedChannel(android
     * .content.ContentResolver)
     */
    @Override
    public SubscribedChannel getSubscribedChannel(ContentResolver resolver) {
        Cursor cursor = null;
        SubscribedChannel subscribedChannel = null;
        try {
            cursor = resolver.query(Subscriptions.CONTENT_URI,
                    SubscriptionHelper.PROJECTIONS,
                    Subscriptions.CATEGORY_SRV_ID + "=?",
                    new String[] { channel }, null);
            if (cursor != null && cursor.moveToNext()) {
                subscribedChannel = DataEntryManager.SubscriptionHelper
                        .inject(cursor);
            }
        } catch (Exception e) {
            Utils.error(IlikeChannel.class, Utils.getStackTrace(e));
        }

        if (cursor != null) {
            cursor.close();
        }

        return subscribedChannel;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.qihoo360.reader.subscription.Channel#getFullCursor(android.content
     * .ContentResolver)
     */
    @Override
    public Cursor getFullCursor(ContentResolver resolver) {
        return getCursorOfSpecificFields(resolver,
                DataEntryManager.BookmarkHelper.PROJECTIONS);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.qihoo360.reader.subscription.Channel#getAbstractCursor(android.content
     * .ContentResolver)
     */
    @Override
    public Cursor getAbstractCursor(ContentResolver resolver) {
        return getCursorOfSpecificFields(resolver,
                DataEntryManager.BookmarkHelper.PROJECTIONS_ABSTRACT);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.qihoo360.reader.subscription.Channel#getCursorOfSpecificFields(android
     * .content.ContentResolver, java.lang.String[])
     */
    @Override
    public Cursor getCursorOfSpecificFields(ContentResolver resolver,
            String[] projections) {
        try {
            Cursor cursor = resolver.query(Bookmarks.CONTENT_URI, projections,
                    Tables.Bookmarks.CATEGORY_SRV_ID + "=?",
                    new String[] { channel }, Bookmarks.SORT);

            return cursor;
        } catch (Exception e) {
            Utils.error(IlikeChannel.class, Utils.getStackTrace(e));
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.qihoo360.reader.subscription.Channel#getCount(android.content.
     * ContentResolver, long, long)
     */
    @Override
    public int getCount(ContentResolver resolver, long sortFrom, long sortTo) {
        int result = 0;
        Cursor cursor = null;
        try {
            cursor = resolver.query(Bookmarks.CONTENT_URI,
                    new String[] { Bookmarks._ID }, Bookmarks.SORT + ">=? AND "
                            + Bookmarks.SORT + "<=? AND "
                            + Bookmarks.CATEGORY_SRV_ID + "=?", new String[] {
                            String.valueOf(sortTo), String.valueOf(sortFrom),
                            channel }, null);

            if (cursor != null) {
                result = cursor.getCount();
                cursor.close();
            }
        } catch (Exception e) {
            Utils.error(IlikeChannel.class, Utils.getStackTrace(e));
        }

        if (cursor != null) {
            cursor.close();
        }

        return result;

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.qihoo360.reader.subscription.Channel#getNewArticles(android.content
     * .ContentResolver,
     * com.qihoo360.reader.listener.OnGetArticlesResultListener, boolean, int,
     * boolean)
     */
    @Override
    protected int getNewArticles(final ContentResolver resolver,
            final OnGetArticlesResultListener listener, boolean isSync,
            int count, boolean isAbstract) {
        IlikeArticle newestArticle = (IlikeArticle) getNewestArticle(resolver);
        IlikeArticle oldestArticle = (IlikeArticle) getOldestArticle(resolver);

        String newestId;
        String newestQid;
        if (newestArticle != null) {
            newestId = String.valueOf(newestArticle.contentid);
            newestQid = String.valueOf(newestArticle.author_qid);
        } else {
            newestId = newestQid = "0";
        }

        String oldestId;
        String oldestQid;
        if (oldestArticle != null) {
            oldestId = String.valueOf(oldestArticle.contentid);
            oldestQid = String.valueOf(oldestArticle.author_qid);
        } else {
            oldestId = oldestQid = "0";
        }

        int rangeCount = 0;
        if (newestArticle != null && oldestArticle != null) {
            rangeCount = getCount(resolver, oldestArticle.sort,
                    newestArticle.sort);
        }

        getArticles(resolver, listener, true, newestId, newestQid, oldestId,
                oldestQid, rangeCount);

        return 0;
    }

    protected abstract void getArticles(ContentResolver resolver,
            OnGetArticlesResultListener listener, boolean isGetNew,
            String newestId, String newestQid, String oldestId,
            String oldestQid, int rangeCount);

    protected IlikeJsonGetter mNewJsonGetter;
    protected IlikeJsonGetter mOldJsonGetter;

    protected void setGetter(final boolean isGetNew, IlikeJsonGetter getter) {
        if (isGetNew) {
            mNewJsonGetter = getter;
        } else {
            mOldJsonGetter = getter;
        }
    }

    protected AsyncTask<Integer, Integer, Integer> mNewDbInserter;
    protected AsyncTask<Integer, Integer, Integer> mOldDbInserter;

    protected void setDbInserter(final boolean isGetNew,
            AsyncTask<Integer, Integer, Integer> inserter) {
        if (isGetNew) {
            mNewDbInserter = inserter;
        } else {
            mOldDbInserter = inserter;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.qihoo360.reader.subscription.Channel#stopGetNew()
     */
    @Override
    public void stopGetNew() {
        if (mNewJsonGetter != null) {
            mNewJsonGetter.stop();
            mNewJsonGetter = null;
        }
        if (mNewDbInserter != null) {
            mNewDbInserter.cancel(true);
            mNewDbInserter = null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.qihoo360.reader.subscription.Channel#stopGetOld()
     */
    @Override
    public void stopGetOld() {
        if (mOldJsonGetter != null) {
            mOldJsonGetter.stop();
            mOldJsonGetter = null;
        }
        if (mOldDbInserter != null) {
            mOldDbInserter.cancel(true);
            mOldDbInserter = null;
        }
    }

    protected boolean ensureContinuous(ContentResolver resolver,
            boolean isGetNew, int cache_expire) {
        if (isGetNew && (cache_expire == 1)) {
            // 可能有断层，立即删除所有文章
            deleteArticles(resolver);
            return true;
        }

        return false;
    }

    protected void deleteArticles(ContentResolver resolver) {
        resolver.delete(Tables.Bookmarks.CONTENT_URI,
                Tables.Bookmarks.CATEGORY_SRV_ID + "=?",
                new String[] { channel });
    }

    protected int calculateSort(final ContentResolver resolver,
            final boolean isGetNew, int count) {
        int sort = 0;
        IlikeArticle article = null;
        if (isGetNew) {
            article = (IlikeArticle) getNewestArticle(resolver);
            if (article != null) {
                sort = article.sort - count;
            }
        } else {
            article = (IlikeArticle) getOldestArticle(resolver);
            if (article != null) {
                sort = article.sort + 1;
            }
        }
        return sort;
    }

    protected List<String> getAllBookmarkIds(ContentResolver resolver) {
        Cursor cursor = resolver.query(Tables.Bookmarks.CONTENT_URI,
                new String[] { Tables.Bookmarks.BOOKMARK_ID },
                Tables.Bookmarks.CATEGORY_SRV_ID + "=?",
                new String[] { channel }, null);
        List<String> result = new ArrayList<String>();
        while (cursor != null && cursor.moveToNext()) {
            result.add(cursor.getString(0));
        }

        if (cursor != null) {
            cursor.close();
        }

        return result;
    }

    void insertArticleToDb(ContentResolver resolver, Bookmark bookmark,
            User user, int sort, boolean focusStar) {
        ContentValues values = new ContentValues();

        Pic[] pics = null;
        if (bookmark.pics != null && bookmark.pics.length > 0) {
            pics = new Pic[] { bookmark.pics[0] };
        }

        values.put(Tables.Bookmarks.CATEGORY_SRV_ID, channel);
        values.put(Tables.Bookmarks.BOOKMARK_ID, bookmark.id);
        values.put(Tables.Bookmarks.TITLE, bookmark.title);
        if (user != null) {
            values.put(Tables.Bookmarks.AUTHOR_QID, user.id);
        }

        values.put(Tables.Bookmarks.SNAPSHOT, buildUpImageUrl(pics));
        values.put(Tables.Bookmarks.SORT, sort);
        if (focusStar) {
            values.put(Tables.Bookmarks.I_LIKE, Article.STAR_APPEARANCE);
        }

        resolver.insert(Tables.Bookmarks.CONTENT_URI, values);
    }

    static String buildUpImageUrl(Pic[] pics) {
        StringBuilder urlStringBuilder = new StringBuilder();

        if (pics != null && pics.length > 0) {
            for (Pic pic : pics) {
                String url = pic.url.replace("/dr/450__", "");
                urlStringBuilder.append(url);
                urlStringBuilder.append("||size:");
                urlStringBuilder.append(pic.originalWidth);
                urlStringBuilder.append('*');
                urlStringBuilder.append(pic.originalHeight);
                urlStringBuilder.append(';');
            }
            urlStringBuilder.deleteCharAt(urlStringBuilder.length() - 1);
        }

        return urlStringBuilder.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.qihoo360.reader.subscription.Channel#getOldArticles(android.content
     * .ContentResolver,
     * com.qihoo360.reader.listener.OnGetArticlesResultListener, boolean, long,
     * long, int, boolean)
     */
    @Override
    protected int getOldArticles(ContentResolver resolver,
            OnGetArticlesResultListener listener, boolean isSync, long fromId,
            long sinceId, int count, boolean isAbstract) {
        IlikeArticle oldestArticle = (IlikeArticle) getOldestArticle(resolver);

        String oldestId;
        String oldestQid;
        if (oldestArticle != null) {
            oldestId = String.valueOf(oldestArticle.contentid);
            oldestQid = String.valueOf(oldestArticle.author_qid);
        } else {
            oldestId = oldestQid = "0";
        }

        getArticles(resolver, listener, false, "0", "0", oldestId, oldestQid, 0);

        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.qihoo360.reader.subscription.Channel#getNewestImageOfContentId(android
     * .content.ContentResolver)
     */
    @Override
    public long getNewestImageOfContentId(ContentResolver resolver) {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.qihoo360.reader.subscription.Channel#subscribe(android.content.
     * ContentResolver)
     */
    @Override
    public SubscribedChannel subscribe(ContentResolver resolver) {
        ReaderPlugin.sIsSubscribedChanged = true;

        IlikeSubscribedChannel subscribedChannel = new IlikeSubscribedChannel();
        subscribedChannel.channel = channel;
        subscribedChannel.title = title;
        subscribedChannel.sub_date = System.currentTimeMillis();
        subscribedChannel.type = TYPE_ILIKE_HOT_COLLECTION;

        // // 排序
        // SortFloat sortFloat = new SortFloat();
        // if (Settings.getAddChannelToTail()) {
        // // 订阅频道，并放到末尾
        // sortFloat.left = DataEntryManager.SubscriptionHelper
        // .getLastSortFloat(resolver);
        // sortFloat.right = SortFloat.MAX;
        // } else {
        // // 放到开头
        // sortFloat.left = SortFloat.MIN;
        // sortFloat.right = DataEntryManager.SubscriptionHelper
        // .getFirstSortFloat(resolver);
        // }
        // subscribedChannel.sort_float = subscribedChannel.calculateSortFloat(
        // resolver, sortFloat);

        // 补存下面几个信息
        // subscribedChannel.newest_image_content_id = newest_image_content_id;
        // subscribedChannel.last_content_id = last_content_id;
        // try {
        // subscribedChannel.image_version = Integer.valueOf(imageversion);
        // } catch (Exception e) {
        // Utils.error(IlikeChannel.class, Utils.getStackTrace(e));
        // subscribedChannel.image_version = 0;
        // }
        if (subscribedChannel.insertToDb(resolver) == false) {
            return null;
        } else {
            return subscribedChannel;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.qihoo360.reader.subscription.Channel#unsubscribe(android.content.
     * ContentResolver)
     */
    @Override
    public void unsubscribe(ContentResolver resolver) {
        if (type == TYPE_ILIKE_HOT_COLLECTION) {
            IlikeSubscribedChannel.deleteByChannel(resolver, channel);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.qihoo360.reader.subscription.Channel#clearAllArticles(android.content
     * .ContentResolver)
     */
    @Override
    public int clearAllArticles(ContentResolver resolver) {
        return IlikeSubscribedChannel.deleteByChannel(resolver, channel);
    }

    protected boolean clearOldCache(boolean isGetNew, ContentResolver resolver) {
        // 如果是Push或离线下载该频道，则不清除缓存
        if (!isGetNew || PushManager.getPushingChannel() == this
                || offline().isRunning()) {
            return false;
        }

        int newestSort = -1;
        IlikeArticle newestArticle = (IlikeArticle) getNewestArticle(resolver);
        if (newestArticle != null) {
            newestSort = newestArticle.sort;
        }

        int oldestSort = -1;
        IlikeArticle oldestArticle = (IlikeArticle) getOldestArticle(resolver);
        if (oldestArticle != null) {
            oldestSort = oldestArticle.sort;
        }

        int wantToDeleteSort = oldestSort - newestSort;
        if (wantToDeleteSort > MAX_RESTORE_ROW) {
            wantToDeleteSort = newestSort + MAX_RESTORE_ROW;

            resolver.delete(Tables.Bookmarks.CONTENT_URI, Tables.Bookmarks.SORT
                    + ">?", new String[] { String.valueOf(wantToDeleteSort) });
            return true;
        }

        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.qihoo360.reader.subscription.Channel#offline()
     */
    @Override
    public OfflineTask offline() {
        if (mOfflineTask == null) {
            mOfflineTask = new IlikeOfflineTask(this);
        }

        return mOfflineTask;
    }

    @Override
    public String getAlbumCoverDataFieldName() {
        return Bookmarks.SNAPSHOT;
    }

}
