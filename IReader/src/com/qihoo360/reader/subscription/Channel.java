package com.qihoo360.reader.subscription;

import java.util.List;

import android.content.ContentResolver;
import android.database.Cursor;
import android.text.TextUtils;

import com.qihoo.ilike.subscription.IlikeChannel;
import com.qihoo360.reader.listener.OnGetArticlesResultListener;
import com.qihoo360.reader.offline.OfflineTask;
import com.qihoo360.reader.subscription.reader.RssChannel;

public abstract class Channel {

    /**
     * 当前频道类型为“文章”
     */
    public static final int TYPE_ARTICLE = 1;

    /**
     * 当前频道类型为“图集”
     */
    public static final int TYPE_PHOTO_ALBUM = 3;

    /**
     * 当前频道类型为“我喜欢——我的关注”
     */
    public static final int TYPE_ILIKE_MY_FOCUS = 100;

    /**
     * 当前频道类型为“我喜欢——大家喜欢”
     */
    public static final int TYPE_ILIKE_HOT_COLLECTION = 101;

    /**
     * 当前频道类型为“我喜欢——我的收藏”
     */
    public static final int TYPE_ILIKE_MY_COLLECTION = 102;

    /**
     * 频道标题
     */
    public String title;
    /**
     * 频道类型：是微博？还是RSS？还是……
     */
    public int type;
    /**
     * 频道概述
     */
    public String desc;
    /**
     * 用于搜索的拼音字段
     */
    public String pinyin;
    /**
     * 唯一标志频道的字段
     */
    public String channel;
    /**
     * 频道封面
     */
    public String image;
    /**
     * 频道封面的版本号
     */
    public String imageversion;
    /**
     * 图集的源地址（仅图集可用）
     */
    public String src;
    /**
     * 该频道是否不在“内容中心（添加订阅）”中显示
     */
    public boolean disabled;

    public static boolean sIsRunningTask = false;

    /**
     * 根据频道名，获取频道对象。
     *
     * @param name
     * @return
     */
    public static synchronized Channel get(String name) {
        if (!TextUtils.isEmpty(name) && name.startsWith("ilike:")) {
            return IlikeChannel.get(name);
        } else {
            return RssChannel.get(name);
        }
    }

    /**
     * 判断当前任务是否正在运行
     *
     * @return
     */
    public static boolean isRunningTask() {
        return sIsRunningTask;
    }

    public Channel() {
        super();
    }

    /**
     * 根据ContentId获取Article对象
     *
     * @param resolver
     * @param contentId
     * @return
     */
    public abstract Article getArticle(ContentResolver resolver, long contentId);

    /**
     * 获取最新的文章
     *
     * @param resolver
     * @return
     */
    public abstract Article getNewestArticle(ContentResolver resolver);

    /**
     * 获取最新的文章Id
     *
     * @param resolver
     * @return
     */
    public abstract long getNewestContentId(ContentResolver resolver);

    /**
     * 获取最旧的文章
     *
     * @param resolver
     * @return
     */
    public abstract Article getOldestArticle(ContentResolver resolver);

    /**
     * 获取最旧的文章Id
     *
     * @param resolver
     * @return
     */
    public abstract long getOldestContentId(ContentResolver resolver);

    /**
     * 获取可以搜索的项目
     *
     * @return
     */
    public abstract List<String> getSearchItems();

    /**
     * 获取订阅的频道对象
     *
     * @param resolver
     * @return
     */
    public abstract SubscribedChannel getSubscribedChannel(
            ContentResolver resolver);

    /**
     * 直接获取当前频道内，在本地中所有的消息
     *
     * @param contentResolver
     * @return
     */
    public abstract Cursor getFullCursor(ContentResolver resolver);

    /**
     * 直接获取当前频道内，只含标题和摘要的Cursor
     *
     * @param resolver
     * @return
     */
    public abstract Cursor getAbstractCursor(ContentResolver resolver);

    /**
     * 获取你所需要的字段
     *
     * @param resolver
     * @param projections
     * @return
     */
    public abstract Cursor getCursorOfSpecificFields(ContentResolver resolver,
            String[] projections);

    /**
     * 获取在指定ContentId（ilike为sort）范围内，包含的文章数
     *
     * @param resolver
     * @param from 注意，ilike中，该值为sort的最大值
     * @param to 注意，ilike中，该值为sort的最小值
     * @return
     */
    public abstract int getCount(ContentResolver resolver, long from, long to);

    /**
     * 同步获取最新的文章
     *
     * @param contentResolver
     * @param listener
     * @param isAbstract
     *            是只下载摘要？还是下载全部内容？
     */
    public int getNewArticlesSync(ContentResolver resolver, boolean isAbstract) {
        return getNewArticlesSync(resolver, 0, isAbstract);
    }

    /**
     * 同步获取最新的文章
     *
     * @param contentResolver
     * @param listener
     * @param isAbstract
     *            是只下载摘要？还是下载全部内容？
     */
    public int getNewArticlesSync(ContentResolver resolver,
            final OnGetArticlesResultListener listener, boolean isAbstract) {
        return getNewArticles(resolver, listener, true, 0, isAbstract);
    }

    /**
     * 同步获取最新的文章
     *
     * @param contentResolver
     * @param count
     *            为正表示会从最新的contentId开始，读取更新的内容。为负表示要多少就给下载多少，以前下载过的则直接覆盖。
     */
    public int getNewArticlesSync(ContentResolver resolver, int count,
            boolean isAbstract) {
        return getNewArticles(resolver, null, true, count, isAbstract);
    }

    /**
     * 同步获取最新的文章
     *
     * @param resolver
     * @param listener
     * @param count
     * @param isAbstract
     * @return
     */
    public int getNewArticlesSync(ContentResolver resolver,
            final OnGetArticlesResultListener listener, int count,
            boolean isAbstract) {
        return getNewArticles(resolver, listener, true, count, isAbstract);
    }

    /**
     * 异步获取最新的文章
     *
     * @param contentResolver
     * @param listener
     */
    public void getNewArticlesAsync(ContentResolver resolver,
            final OnGetArticlesResultListener listener, boolean isAbstract) {
        getNewArticlesAsync(resolver, listener, 0, isAbstract);
    }

    /**
     * 异步获取最新的文章
     *
     * @param contentResolver
     * @param listener
     * @param count
     *            为正表示会从最新的contentId开始，读取更新的内容。为负表示要多少就给下载多少，以前下载过的则直接覆盖。
     */
    public void getNewArticlesAsync(ContentResolver resolver,
            final OnGetArticlesResultListener listener, int count,
            boolean isAbstract) {
        getNewArticles(resolver, listener, false, count, isAbstract);
    }

    /**
     * 同步获取当前需要的文章
     *
     * @param contentResolver
     * @param listener
     */
    public int getOldArticlesSync(ContentResolver resolver, boolean isAbstract) {
        return getOldArticlesSync(resolver, 0, isAbstract);
    }

    /**
     * 同步获取当前需要的文章
     *
     * @param contentResolver
     * @param listener
     */
    public int getOldArticlesSync(ContentResolver resolver, int count,
            boolean isAbstract) {
        return getOldArticles(resolver, null, true, 0, -1, count, isAbstract);
    }

    /**
     * 异步获取当前需要的文章
     *
     * @param contentResolver
     * @param listener
     */
    public void getOldArticlesAsync(ContentResolver resolver,
            final OnGetArticlesResultListener listener, boolean isAbstract) {
        getOldArticlesAsync(resolver, listener, 0, isAbstract);
    }

    /**
     * 异步获取当前需要的文章
     *
     * @param contentResolver
     * @param listener
     */
    public void getOldArticlesAsync(ContentResolver resolver,
            final OnGetArticlesResultListener listener, int count,
            boolean isAbstract) {
        getOldArticles(resolver, listener, false, 0, -1, count, isAbstract);
    }

    /**
     * 同步获取当前文章的范围
     *
     * @param resolver
     * @param listener
     * @param contentId
     * @param endContentId
     * @param count
     * @param isAbstract
     */
    public int getArticlesRangeSync(ContentResolver resolver,
            final OnGetArticlesResultListener listener, long contentId,
            long endContentId, int count, boolean isAbstract) {
        return getOldArticles(resolver, listener, true, contentId,
                endContentId, count, isAbstract);
    }

    protected abstract int getNewArticles(ContentResolver resolver,
            final OnGetArticlesResultListener listener, boolean isSync,
            int count, boolean isAbstract);

    protected abstract int getOldArticles(ContentResolver resolver,
            final OnGetArticlesResultListener listener, boolean isSync,
            long fromId, long sinceId, int count, boolean isAbstract);

    /**
     * 获取最新带图片的文章Id
     *
     * @return
     */
    public abstract long getNewestImageOfContentId(ContentResolver resolver);

    /**
     * 查看是否已经订阅。
     *
     * @return
     */
    public boolean isSubscribed(ContentResolver resolver) {
        return getSubscribedChannel(resolver) != null;
    }

    /**
     * 订阅该频道。
     *
     * @param resolver
     * @return
     */
    public abstract SubscribedChannel subscribe(ContentResolver resolver);

    /**
     * 取消该频道的订阅
     *
     * @param resolver
     */
    public abstract void unsubscribe(ContentResolver resolver);

    /**
     * 如果当前任务在执行，则立即取消
     */
    public void stopGet() {
        stopGetNew();
        stopGetOld();
    }

    /**
     * 停止获取新的文章。
     */
    public abstract void stopGetNew();

    /**
     * 停止获取旧的文章。
     */
    public abstract void stopGetOld();

    protected boolean mIsDbBusy;

    /**
     * 是否正在做数据库的操作？
     * @return
     */
    public boolean isDbBusy() {
        return mIsDbBusy;
    }

    /**
     * 清空该频道里的所有文章
     *
     * @param resolver
     */
    public abstract int clearAllArticles(ContentResolver resolver);

    /**
     * 获取离线任务
     *
     * @return
     */
    public abstract OfflineTask offline();

    /**
     * 获取图片频道的图集封面在数据库中对应的字段名称
     *
     * @return
     */
    public abstract String getAlbumCoverDataFieldName();

    public static final int RESULT_OK = 0;
    public static final int RESULT_FAILURE = 1;
    public static final int RESULT_NOT_EXISTS = 2;
    public static final int RESULT_USER_CANCELLED = 3;
}