/**
 *
 */

package com.qihoo.ilike.subscription;

import com.qihoo.ilike.data.Tables;
import com.qihoo.ilike.data.Tables.Bookmarks;
import com.qihoo.ilike.http.core.IHttpRequest.HttpRequestStatus;
import com.qihoo.ilike.json.DetailGetter;
import com.qihoo.ilike.json.ILikeItPoster;
import com.qihoo.ilike.json.listener.OnDetailResultListener;
import com.qihoo.ilike.json.listener.OnILikeItPostResultListener;
import com.qihoo.ilike.vo.Detail;
import com.qihoo.ilike.vo.ErrorInfo;
import com.qihoo.ilike.vo.Note;
import com.qihoo360.reader.listener.OnFillArticleResultListener;
import com.qihoo360.reader.listener.OnMarkStarResultListener;
import com.qihoo360.reader.listener.OnRefreshLikeCountResultListener;
import com.qihoo360.reader.subscription.Article;
import com.qihoo360.reader.support.Utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;

import java.text.ParseException;

/**
 * 表示一个“文章”——在一个频道内。
 *
 * @author Jiongxuan Zhang
 */
public class IlikeArticle extends Article {
    public String snapshot = "";
    public String author_qid = "";
    public String author_image_url = "";
    public int like_count = 0;
    public String album_id;
    public String album_title;

    private DetailGetter mDetailGetter;
    public int sort;

    /**
     * 从cursor中创建IlikeArticle对象
     *
     * @param cursor
     * @return
     */
    public static IlikeArticle inject(Cursor cursor) {

        if (cursor == null) {
            return null;
        }

        IlikeArticle article = new IlikeArticle();
        int idx;
        idx = cursor.getColumnIndex(Bookmarks._ID);
        if (idx >= 0)
            article._id = cursor.getInt(idx);

        idx = cursor.getColumnIndex(Bookmarks.CATEGORY_SRV_ID);
        if (idx >= 0)
            article.channel = cursor.getString(idx);

        idx = cursor.getColumnIndex(Bookmarks.BOOKMARK_ID);
        if (idx >= 0)
            article.contentid = cursor.getLong(idx);

        idx = cursor.getColumnIndex(Bookmarks.TITLE);
        if (idx >= 0)
            article.title = cursor.getString(idx);

        idx = cursor.getColumnIndex(Bookmarks.SNAPSHOT);
        if (idx >= 0)
            article.snapshot = cursor.getString(idx);

        idx = cursor.getColumnIndex(Bookmarks.DESCRIPTION);
        if (idx >= 0)
            article.description = cursor.getString(idx);

        idx = cursor.getColumnIndex(Bookmarks.ALBUM_ID);
        if (idx >= 0)
            article.album_id = cursor.getString(idx);

        idx = cursor.getColumnIndex(Bookmarks.ALBUM_TITLE);
        if (idx >= 0)
            article.album_title = cursor.getString(idx);

        idx = cursor.getColumnIndex(Bookmarks.IMAGES);
        if (idx >= 0)
            article.images360 = cursor.getString(idx);

        idx = cursor.getColumnIndex(Bookmarks.AUTHOR);
        if (idx >= 0)
            article.author = cursor.getString(idx);

        idx = cursor.getColumnIndex(Bookmarks.AUTHOR_IMAGE_URL);
        if (idx >= 0)
            article.author_image_url = cursor.getString(idx);

        idx = cursor.getColumnIndex(Bookmarks.AUTHOR_QID);
        if (idx >= 0)
            article.author_qid = cursor.getString(idx);

        idx = cursor.getColumnIndex(Bookmarks.I_LIKE);
        if (idx >= 0)
            article.star = cursor.getInt(idx);

        idx = cursor.getColumnIndex(Bookmarks.ISDOWNLOADED);
        if (idx >= 0)
            article.isDownloaded = cursor.getInt(idx) == 1;

        idx = cursor.getColumnIndex(Bookmarks.LIKE_COUNT);
        if (idx >= 0)
            article.like_count = cursor.getInt(idx);

        idx = cursor.getColumnIndex(Bookmarks.PUB_DATE);
        if (idx >= 0)
            article.pubdate = cursor.getLong(idx);

        idx = cursor.getColumnIndex(Bookmarks.READ);
        if (idx >= 0)
            article.read = cursor.getInt(idx);

        idx = cursor.getColumnIndex(Bookmarks.SORT);
        if (idx >= 0)
            article.sort = cursor.getInt(idx);

        return article;

    }

    @Override
    protected int fill(final ContentResolver resolver,
            final OnFillArticleResultListener listener, boolean isSync) {
        if (isDownloaded) {
            return RESULT_FAILURE;
        }
        mDetailGetter = new DetailGetter();
        mDetailGetter.get(new OnDetailResultListener() {

            @Override
            public void onResponseError(ErrorInfo errorInfo) {
                if (errorInfo != null) {
                    listener.onFailure(OnFillArticleResultListener.NOT_CONNECTED);
                }
                mDetailGetter = null;
            }

            @Override
            public void onRequestFailure(HttpRequestStatus errorStatus) {
                if (errorStatus != null) {
                    listener.onFailure(OnFillArticleResultListener.NOT_CONNECTED);
                }
                mDetailGetter = null;
            }

            @Override
            public void onComplete(Detail detail) {
                if (detail != null) {
                    ContentValues values = new ContentValues();
                    injectBookmarkContentValues(values, detail);

                    resolver.update(Tables.Bookmarks.CONTENT_URI, values,
                            Tables.Bookmarks.BOOKMARK_ID + "=?",
                            new String[] { detail.detailBookmark.id });
                }

                listener.onCompletion();
                mDetailGetter = null;
            }
        }, author_qid, String.valueOf(contentid), "0");

        // 暂不支持同步处理功能
        return RESULT_OK;
    }

    void injectBookmarkContentValues(ContentValues values, Detail detail) {
        title = detail.detailBookmark.title;
        description = buildUpDescription(detail.detailBookmark.notes);
        images360 = buildUpImageUrl(detail.detailBookmark.notes);
        like_count = detail.detailBookmark.like_num;
        try {
            pubdate = com.qihoo.ilike.util.DateUtils.convStringToDate(
                    detail.detailBookmark.create_time).getTime() / 1000;

        } catch (ParseException e) {
            Utils.error(getClass(), Utils.getStackTrace(e));
            pubdate = 0;
        }
        if (detail.volume != null) {
            album_id = detail.volume.id;
            album_title = detail.volume.name;
        }
        if (detail.user != null) {
            author = detail.user.name;
            if (detail.user.icon != null) {
                author_image_url = detail.user.icon.url;
            }
        }
        star = detail.has_pin == 1 ? Article.STAR_APPEARANCE
                : Article.STAR_NONE;
        if (IlikeChannel.ILIKE_MY_COLLECTION.equals(channel)) {
            star = STAR_APPEARANCE;
        }
        isDownloaded = true;

        values.put(Tables.Bookmarks.TITLE, title);
        values.put(Tables.Bookmarks.DESCRIPTION, description);
        values.put(Tables.Bookmarks.IMAGES, images360);
        values.put(Tables.Bookmarks.LIKE_COUNT, like_count);
        if (pubdate != 0) {
            values.put(Tables.Bookmarks.PUB_DATE, pubdate);
        }
        if (detail.volume != null) {
            values.put(Tables.Bookmarks.ALBUM_ID, album_id);
            values.put(Tables.Bookmarks.ALBUM_TITLE, album_title);
        }
        if (detail.user != null) {
            values.put(Tables.Bookmarks.AUTHOR, author);
            if (detail.user.icon != null) {
                values.put(Tables.Bookmarks.AUTHOR_IMAGE_URL, author_image_url);
            }
        }
        values.put(Tables.Bookmarks.I_LIKE, star);
        values.put(Tables.Bookmarks.ISDOWNLOADED, isDownloaded);
    }

    static String buildUpDescription(Note[] notes) {
        StringBuilder descriptionStringBuilder = new StringBuilder();
        if (notes != null && notes.length > 0) {
            for (Note note : notes) {
                if (Note.TYPE_TEXT.equals(note.type)) {
                    descriptionStringBuilder.append(note.src);
                    descriptionStringBuilder.append('\n');
                } else if (Note.TYPE_PIC.equals(note.type)) {
                    descriptionStringBuilder.append("<img>");
                }
            }
        }

        return descriptionStringBuilder.toString();
    }

    static String buildUpImageUrl(Note[] notes) {
        StringBuilder imageUrlStringBuilder = new StringBuilder();
        if (notes != null && notes.length > 0) {
            for (Note note : notes) {
                if (Note.TYPE_PIC.equals(note.type)) {
                    String url = note.src.replace("/dr/450__", "");
                    imageUrlStringBuilder.append(url);
                    imageUrlStringBuilder.append("||size:");
                    imageUrlStringBuilder.append(note.w);
                    imageUrlStringBuilder.append('*');
                    imageUrlStringBuilder.append(note.h);
                    imageUrlStringBuilder.append(';');
                }
            }
            if (imageUrlStringBuilder.length() > 0) {
                imageUrlStringBuilder.deleteCharAt(imageUrlStringBuilder
                        .length() - 1);
            }
        }

        return imageUrlStringBuilder.toString();
    }

    @Override
    public void stopFill() {
        if (mDetailGetter != null) {
            mDetailGetter.stop();
            mDetailGetter = null;
        }
    }

    /**
     * 刷新喜欢数
     *
     * @param listener
     */
    public void refreshLikeCount(final ContentResolver resolver,
            final OnRefreshLikeCountResultListener listener) {
        DetailGetter detailGetter = new DetailGetter();
        detailGetter.get(new OnDetailResultListener() {

            @Override
            public void onResponseError(ErrorInfo errorInfo) {
                if (errorInfo != null) {
                    listener.onFailure(OnFillArticleResultListener.NOT_CONNECTED);
                }
            }

            @Override
            public void onRequestFailure(HttpRequestStatus errorStatus) {
                if (errorStatus != null) {
                    listener.onFailure(OnFillArticleResultListener.NOT_CONNECTED);
                }
            }

            @Override
            public void onComplete(Detail detail) {
                if (detail != null && detail.detailBookmark != null) {
                    int new_like_num = detail.detailBookmark.like_num;

                    boolean isChanged = like_count != new_like_num;
                    if (isChanged) {
                        like_count = detail.detailBookmark.like_num;

                        ContentValues values = new ContentValues();
                        values.put(Tables.Bookmarks.LIKE_COUNT,
                                detail.detailBookmark.like_num);

                        resolver.update(Tables.Bookmarks.CONTENT_URI, values,
                                Tables.Bookmarks.BOOKMARK_ID + "=?",
                                new String[] { detail.detailBookmark.id });
                    }

                    listener.onCompletion(new_like_num, isChanged);
                }

            }
        }, author_qid, String.valueOf(contentid), "0");
    }

    @Override
    public void markStar(final ContentResolver resolver,
            final OnMarkStarResultListener listener) {
        new ILikeItPoster().post(new OnILikeItPostResultListener() {

            @Override
            public void onResponseError(ErrorInfo errorInfo) {
                if (errorInfo != null) {
                    unmarkStar(resolver);

                    if (listener != null) {
                        listener.onFailure();
                    }
                }
            }

            @Override
            public void onRequestFailure(HttpRequestStatus errorStatus) {
                if (errorStatus != null) {
                    unmarkStar(resolver);

                    if (listener != null) {
                        listener.onFailure();
                    }
                }
            }

            @Override
            public void onComplete() {
                if (listener != null) {
                    listener.onComplete();
                }
            }
        }, author_qid, String.valueOf(contentid), "0", title);

        setStar(resolver, Article.STAR_APPEARANCE);

    }

    @Override
    public void unmarkStar(ContentResolver resolver,
            OnMarkStarResultListener listener) {
        setStar(resolver, Article.STAR_NONE);
    }

    private void setStar(final ContentResolver resolver, int star) {
        this.star = star;
        ContentValues values = new ContentValues();
        values.put(Tables.Bookmarks.I_LIKE, String.valueOf(star));
        resolver.update(Tables.Bookmarks.CONTENT_URI, values,
                Tables.Bookmarks.BOOKMARK_ID + "=?",
                new String[] { String.valueOf(contentid) });
    }

    @Override
    public void markRead(ContentResolver resolver) {
        // TODO
    }

    @Override
    public void markUnread(ContentResolver resolver) {
        // TODO
    }

}
