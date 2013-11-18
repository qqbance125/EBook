package com.qihoo360.reader.subscription;

import com.qihoo.ilike.data.Tables;
import com.qihoo.ilike.subscription.IlikeArticle;
import com.qihoo360.reader.data.DataEntryManager;
import com.qihoo360.reader.listener.OnFillArticleResultListener;
import com.qihoo360.reader.listener.OnMarkStarResultListener;
import com.qihoo360.reader.subscription.reader.RssArticle;

import android.content.ContentResolver;
import android.database.Cursor;

public abstract class Article {

    /**
     * 针对star字段，表示没有标星
     */
    public static final int STAR_NONE = 0;
    /**
     * 针对star字段，表示已经标星，并且应出现在频道详情里
     */
    public static final int STAR_APPEARANCE = 1;
    /**
     * 针对star字段，表示已经标星，但不出现在频道详情里
     */
    public static final int STAR_DISAPPEARANCE = 2;
    /**
     * 针对read字段，表示还没有读过
     */
    public static final int UN_READ = 0;
    /**
     * 针对read字段，表示已经读过
     */
    public static final int READ = 1;
    /**
     * [DB] 数据行的ID，注意它不是ContentId（文章Id），不要用它做别的操作
     */
    public int _id;
    /**
     * [DB] 频道名
     */
    public String channel;
    /**
     * [DB & Json] 文章ID
     */
    public long contentid;
    /**
     * [DB & Json] 文章标题
     */
    public String title;
    /**
     * [DB & Json] 文章内容
     */
    public String description;
    /**
     * [DB & Json] 压缩后的图片地址
     */
    public String images360;
    /**
     * [DB & Json] 发布日期，以秒数表示
     */
    public long pubdate;
    /**
     * [DB & Json] 原文链接
     */
    public String link;
    /**
     * [DB & Json] 文章作者
     */
    public String author;
    /**
     * [DB] 文章是否已经读过
     */
    public int read;
    /**
     * [DB] 文章是否已经标星，详见STAR_系列
     */
    public int star;
    /**
     * [DB] 加入星标的日期
     */
    public long stardate;
    /**
     * [DB] 是否已经是拥有详情的文章（已经下载过）
     */
    public boolean isDownloaded = false;
    /**
     * [DB] 是否已离线下载过
     */
    public boolean isOfflined;

    /**
     * 通过Cursor创建Article
     *
     * @param cursor
     * @return
     */
    public static Article inject(Cursor cursor) {
        if (cursor.getColumnIndex(Tables.Bookmarks.BOOKMARK_ID) != -1) {
            return IlikeArticle.inject(cursor);
        } else {
            return RssArticle.inject(cursor);
        }
    }

    /**
     * 通过Cursor创建精简过的Article
     *
     * @param cursor
     * @return
     */
    public static Article injectAbstract(Cursor cursor) {
        if (cursor.getColumnIndex(Tables.Bookmarks.BOOKMARK_ID) != -1) {
            return IlikeArticle.inject(cursor);
        } else {
            return DataEntryManager.ArticleHelper.injectAbstract(cursor);
        }
    }

    /**
     * 通过_id，从数据库中获得Article
     *
     * @param resolver
     * @param _id
     * @return
     */
    public static Article getById(ContentResolver resolver, int _id) {
        return DataEntryManager.ArticleHelper.get(resolver, _id);
    }

    public Article() {
        super();
    }

    public static final int RESULT_OK = 0;
    public static final int RESULT_FAILURE = 1;

    public static final int ERROR_NETWORK_ERROR = 1;
    public static final int ERROR_USER_CANCELLED = 2;

    /**
     * 异步填充这篇文章的详情
     *
     * @param resolver
     */
    public void fillAsync(ContentResolver resolver,
            OnFillArticleResultListener listener) {
        fill(resolver, listener, false);
    }

    /**
     * 同步填充这篇文章的详情
     *
     * @param resolver
     */
    public int fillSync(ContentResolver resolver) {
        return fill(resolver, null, true);
    }

    protected abstract int fill(ContentResolver resolver,
            OnFillArticleResultListener listener, boolean isSync);

    /**
     * 停止当前异步的下载请求
     */
    public abstract void stopFill();

    /**
     * 将该文章标记为星号
     *
     * @param resolver
     */
    public void markStar(ContentResolver resolver) {
        markStar(resolver, null);
    }

    /**
     * 将该文章标记为星号，并通过listener告知结果。
     *
     * @param resolver
     * @param listener
     */
    public abstract void markStar(ContentResolver resolver,
            OnMarkStarResultListener listener);

    /**
     * 取消星号标记
     *
     * @param resolver
     */
    public void unmarkStar(ContentResolver resolver) {
        unmarkStar(resolver, null);
    }

    /**
     * 取消星号标记，并通过listener告知结果。
     *
     * @param resolver
     * @param listener
     */
    public abstract void unmarkStar(ContentResolver resolver,
            OnMarkStarResultListener listener);

    /**
     * 将文章标记为已读
     *
     * @param resolver
     */
    public abstract void markRead(ContentResolver resolver);

    /**
     * 将文章标记为未读
     *
     * @param resolver
     */
    public abstract void markUnread(ContentResolver resolver);

}