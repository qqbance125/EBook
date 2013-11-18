package com.qihoo.ilike.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import com.qihoo.ilike.data.Tables.Bookmarks;
import com.qihoo.ilike.data.Tables.LikedUrls;
import com.qihoo.ilike.data.Tables.Subscriptions;
import com.qihoo.ilike.subscription.IlikeArticle;
import com.qihoo.ilike.subscription.IlikeSubscribedChannel;
import com.qihoo360.reader.subscription.Article;
import com.qihoo360.reader.subscription.SubscribedChannel.SortFloat;
import com.qihoo360.reader.support.Utils;

/**
 * 直接与数据库操作的类。
 *
 * @author Jiongxuan Zhang & Guofeng Fan
 */
public class DataEntryManager {
    final static String TAG = "DataEntryManager";

    private DataEntryManager() {
    };

    static public class SubscriptionHelper {
        static public final String[] PROJECTIONS = { Subscriptions._ID,
                Subscriptions.CATEGORY_SRV_ID, Subscriptions.TITLE,
                Subscriptions.TYPE, Subscriptions.SUB_DATE,
                Subscriptions.NUMBER_OF_VISITED,
                Subscriptions.LAST_BOOKMARK_ID,
                Subscriptions.LAST_REFRESH_TIME, Subscriptions.SORT_FLOAT };

        public static IlikeSubscribedChannel inject(Cursor cursor) {
            if (cursor == null) {
                return null;
            }

            IlikeSubscribedChannel isc = new IlikeSubscribedChannel();
            int idx;
            idx = cursor.getColumnIndex(Subscriptions._ID);
            if (idx >= 0)
                isc._id = cursor.getInt(idx);

            idx = cursor.getColumnIndex(Subscriptions.CATEGORY_SRV_ID);
            if (idx >= 0)
                isc.channel = cursor.getString(idx);

            idx = cursor.getColumnIndex(Subscriptions.TITLE);
            if (idx >= 0)
                isc.title = cursor.getString(idx);

            idx = cursor.getColumnIndex(Subscriptions.TYPE);
            if (idx >= 0)
                isc.type = cursor.getInt(idx);

            idx = cursor.getColumnIndex(Subscriptions.SUB_DATE);
            if (idx >= 0)
                isc.sub_date = cursor.getLong(idx);

            idx = cursor.getColumnIndex(Subscriptions.NUMBER_OF_VISITED);
            if (idx >= 0)
                isc.number_of_visited = cursor.getLong(idx);

            idx = cursor.getColumnIndex(Subscriptions.LAST_BOOKMARK_ID);
            if (idx >= 0)
                isc.last_content_id = cursor.getLong(idx);

            idx = cursor.getColumnIndex(Subscriptions.LAST_REFRESH_TIME);
            if (idx >= 0)
                isc.last_refresh_time = cursor.getLong(idx);

            idx = cursor.getColumnIndex(Subscriptions.SORT_FLOAT);
            if (idx >= 0)
                isc.sort_float = cursor.getDouble(idx);

            return isc;
        }

        public static boolean contains(ContentResolver resolver, String channel) {
            Cursor cursor = null;
            try {
                cursor = resolver.query(Subscriptions.CONTENT_URI,
                        new String[] { Subscriptions._ID },
                        Subscriptions.CATEGORY_SRV_ID + "='" + channel + "'",
                        null, null);

                if (cursor != null && cursor.moveToNext()) {
                    cursor.close();
                    return true;
                }
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, e.getStackTrace()
                        .toString());
            }

            if (cursor != null) {
                cursor.close();
            }
            return false;
        }

        public static boolean add(ContentResolver resolver,
                IlikeSubscribedChannel subscription) {
            if (resolver == null || subscription == null) {
                return false;
            }

            if (contains(resolver, subscription.channel)) {
                return true;
            }

            ContentValues values = getContentValues(subscription);
            try {
                resolver.insert(Subscriptions.CONTENT_URI, values);
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, e.getStackTrace()
                        .toString());
                return false;
            }

            return true;
        }

        public static Cursor getCursor(Activity activity,
                ContentResolver resolver) {
            if (resolver == null) {
                return null;
            }

            try {
                Cursor cursor = activity.managedQuery(
                        Subscriptions.CONTENT_URI,
                        SubscriptionHelper.PROJECTIONS, null, null,
                        Subscriptions.SORT_FLOAT);
                return cursor;
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, e.getStackTrace()
                        .toString());
                return null;
            }
        }

        public static Cursor getCursor(ContentResolver resolver) {
            try {
                Cursor cursor = resolver.query(Subscriptions.CONTENT_URI,
                        SubscriptionHelper.PROJECTIONS, null, null,
                        Subscriptions.SORT_FLOAT);
                return cursor;
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, e.getStackTrace()
                        .toString());
                return null;
            }
        }

        public static IlikeSubscribedChannel get(ContentResolver resolver,
                String channel) {
            if (resolver == null) {
                return null;
            }

            Cursor cursor = null;
            IlikeSubscribedChannel subscribedChannel = null;
            try {
                cursor = resolver.query(Subscriptions.CONTENT_URI,
                        SubscriptionHelper.PROJECTIONS,
                        Subscriptions.CATEGORY_SRV_ID + "='" + channel + "'",
                        null, null);
                if (cursor != null && cursor.moveToNext()) {
                    subscribedChannel = inject(cursor);
                }
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, e.getStackTrace()
                        .toString());
            }

            if (cursor != null) {
                cursor.close();
            }

            return subscribedChannel;
        }

        public static double getFirstSortFloat(ContentResolver resolver) {
            if (resolver == null) {
                return 0;
            }
            Cursor cursor = null;
            double firstSortFloat = 0;
            try {
                cursor = resolver.query(Subscriptions.CONTENT_URI,
                        new String[] { Subscriptions.SORT_FLOAT },
                        Subscriptions.SORT_FLOAT + ">0 AND "
                                + Subscriptions.SORT_FLOAT + "<"
                                + SortFloat.MAX, null, Subscriptions.SORT_FLOAT
                                + " limit " + 1);
                if (cursor != null && cursor.moveToNext()) {
                    firstSortFloat = cursor.getDouble(0);
                }
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, e.getStackTrace()
                        .toString());
            }

            if (cursor != null) {
                cursor.close();
            }

            return firstSortFloat;
        }

        public static double getLastSortFloat(ContentResolver resolver) {
            if (resolver == null) {
                return 0;
            }
            Cursor cursor = null;
            double lastSortFloat = 0;
            try {
                cursor = resolver.query(Subscriptions.CONTENT_URI,
                        new String[] { Subscriptions.SORT_FLOAT },
                        Subscriptions.SORT_FLOAT + ">0 AND "
                                + Subscriptions.SORT_FLOAT + "<"
                                + SortFloat.MAX, null, Subscriptions.SORT_FLOAT
                                + " desc limit " + 1);
                if (cursor != null && cursor.moveToNext()) {
                    lastSortFloat = cursor.getDouble(0);
                }
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, e.getStackTrace()
                        .toString());
            }

            if (cursor != null) {
                cursor.close();
            }
            return lastSortFloat;
        }

        public static int getCount(ContentResolver resolver, String where) {
            if (resolver == null) {
                return 0;
            }

            Cursor cursor = null;
            int count = 0;
            try {
                cursor = resolver.query(Subscriptions.CONTENT_URI,
                        new String[] { "count(_id)" }, where, null, null);
                if (cursor != null && cursor.moveToNext()) {
                    count = cursor.getInt(0);
                }
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, e.getStackTrace()
                        .toString());
            }

            if (cursor != null) {
                cursor.close();
            }

            return count;
        }

        public static int update(ContentResolver resolver,
                IlikeSubscribedChannel subscribedChannel) {
            try {
                return resolver.update(Subscriptions.CONTENT_URI,
                        getContentValues(subscribedChannel),
                        getWhereString(subscribedChannel.channel), null);
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, e.getStackTrace()
                        .toString());
            }

            return -1;
        }

        public static int update(ContentResolver resolver, String where,
                ContentValues values) {
            try {
                return resolver.update(Subscriptions.CONTENT_URI, values, null,
                        null);
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, e.getStackTrace()
                        .toString());
            }

            return -1;
        }

        public static int update(ContentResolver resolver, int subscription_id,
                ContentValues values) {
            try {
                return resolver.update(ContentUris.withAppendedId(
                        Subscriptions.CONTENT_URI, subscription_id), values,
                        null, null);
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, e.getStackTrace()
                        .toString());
            }

            return -1;
        }

        public static int delete(ContentResolver resolver, int subscription_id) {
            if (resolver == null || subscription_id <= 0) {
                return 0;
            }
            try {
                return resolver
                        .delete(ContentUris.withAppendedId(
                                Subscriptions.CONTENT_URI, subscription_id),
                                null, null);
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, e.getStackTrace()
                        .toString());
                return 0;
            }
        }

        public static int delete(ContentResolver resolver, String channel) {
            if (resolver == null || channel == null || channel.equals("")) {
                return 0;
            }
            try {
                return resolver.delete(Subscriptions.CONTENT_URI,
                        Subscriptions.CATEGORY_SRV_ID + "=?",
                        new String[] { channel });
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, e.getStackTrace()
                        .toString());
                return 0;
            }
        }

        private static String getWhereString(String channelName) {
            String whereString = String.format("%s='%s'",
                    Subscriptions.CATEGORY_SRV_ID, channelName);

            return whereString;
        }

        private static ContentValues getContentValues(
                IlikeSubscribedChannel subscription) {
            ContentValues values = new ContentValues();
            values.put(Subscriptions.CATEGORY_SRV_ID, subscription.channel);
            values.put(Subscriptions.TITLE, subscription.title);
            values.put(Subscriptions.TYPE, subscription.type);
            values.put(Subscriptions.SUB_DATE, subscription.sub_date);
            values.put(Subscriptions.NUMBER_OF_VISITED,
                    subscription.number_of_visited);
            values.put(Subscriptions.LAST_BOOKMARK_ID,
                    subscription.last_content_id);
            values.put(Subscriptions.LAST_REFRESH_TIME,
                    subscription.last_refresh_time);
            values.put(Subscriptions.SORT_FLOAT, subscription.sort_float);
            return values;
        }
    }

    static public class BookmarkHelper {
        static public final String[] PROJECTIONS = { Bookmarks._ID,
                Bookmarks.CATEGORY_SRV_ID, Bookmarks.BOOKMARK_ID,
                Bookmarks.TITLE, Bookmarks.SNAPSHOT, Bookmarks.DESCRIPTION,
                Bookmarks.ALBUM_ID, Bookmarks.ALBUM_TITLE, Bookmarks.IMAGES,
                Bookmarks.AUTHOR, Bookmarks.AUTHOR_IMAGE_URL,
                Bookmarks.AUTHOR_QID, Bookmarks.I_LIKE, Bookmarks.ISDOWNLOADED,
                Bookmarks.LIKE_COUNT, Bookmarks.PUB_DATE, Bookmarks.READ,
                Bookmarks.SORT };

        static public final String[] PROJECTIONS_ABSTRACT = { Bookmarks._ID,
                Bookmarks.CATEGORY_SRV_ID, Bookmarks.BOOKMARK_ID,
                Bookmarks.TITLE, Bookmarks.SNAPSHOT };

        public static boolean add(ContentResolver resolver, IlikeArticle article) {
            if (resolver == null || article == null) {
                return false;
            }

            ContentValues values = getContentValues(article);
            try {
                resolver.insert(Bookmarks.CONTENT_URI, values);
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, e.getStackTrace()
                        .toString());
                return false;
            }

            return true;
        }

        public static Map<Long, Boolean> getContentIdAndDownloadedFlags(
                ContentResolver resolver, String channel, long from, long to) {
            Map<Long, Boolean> map = new HashMap<Long, Boolean>();
            Cursor cursor = null;
            try {
                cursor = resolver.query(Bookmarks.CONTENT_URI, new String[] {
                        Bookmarks.BOOKMARK_ID, Bookmarks.ISDOWNLOADED },
                        getWhereString(channel, from, to), null, null);
                while (cursor != null && cursor.moveToNext()) {
                    map.put(cursor.getLong(0), cursor.getInt(1) == 1);
                }
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, e.getStackTrace()
                        .toString());
            }

            if (cursor != null) {
                cursor.close();
            }

            return map;
        }

        public static Map<Long, Boolean> getContentIdAndDownloadedFlags(
                ContentResolver resolver, String channel) {
            Map<Long, Boolean> map = new HashMap<Long, Boolean>();
            Cursor cursor = null;
            try {
                cursor = resolver.query(Bookmarks.CONTENT_URI, new String[] {
                        Bookmarks.BOOKMARK_ID, Bookmarks.ISDOWNLOADED },
                        getWhereString(channel), null, null);
                while (cursor != null && cursor.moveToNext()) {
                    map.put(cursor.getLong(0), cursor.getInt(1) == 1);
                }
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, e.getStackTrace()
                        .toString());
            }

            if (cursor != null) {
                cursor.close();
            }

            return map;
        }

        public static class Normal {

            public static List<Long> getContentIds(ContentResolver resolver,
                    String channel) {
                List<Long> list = new ArrayList<Long>();
                Cursor cursor = null;
                try {
                    cursor = resolver.query(Bookmarks.CONTENT_URI,
                            new String[] { Bookmarks.BOOKMARK_ID },
                            getWhereString(channel), null,
                            Bookmarks.BOOKMARK_ID + " desc");
                    while (cursor != null && cursor.moveToNext()) {
                        list.add(cursor.getLong(0));
                    }
                } catch (Exception e) {
                    Utils.error(DataEntryManager.class, e.getStackTrace()
                            .toString());
                }

                if (cursor != null) {
                    cursor.close();
                }
                return list;
            }

            public static Cursor getCursorByChannel(ContentResolver resolver,
                    String channel, boolean isAbstract) {
                String[] projections = BookmarkHelper.PROJECTIONS;
                try {
                    Cursor cursor = resolver.query(Bookmarks.CONTENT_URI,
                            projections, getWhereString(channel), null,
                            Bookmarks.BOOKMARK_ID + " desc");

                    return cursor;
                } catch (Exception e) {
                    Utils.error(DataEntryManager.class, e.getStackTrace()
                            .toString());
                    return null;
                }
            }

            public static Cursor getCursorOfSpecificFieldsByChannel(
                    ContentResolver resolver, String channel,
                    String[] projections) {
                try {
                    Cursor cursor = resolver.query(Bookmarks.CONTENT_URI,
                            projections, getWhereString(channel), null,
                            Bookmarks.BOOKMARK_ID + " desc");

                    return cursor;
                } catch (Exception e) {
                    Utils.error(DataEntryManager.class, e.getStackTrace()
                            .toString());
                    return null;
                }
            }

            public static Article getNewest(ContentResolver resolver,
                    String channel) {
                Article result = null;
                Cursor cursor = null;
                try {
                    cursor = resolver.query(Bookmarks.CONTENT_URI,
                            BookmarkHelper.PROJECTIONS,
                            getWhereString(channel), null,
                            Bookmarks.BOOKMARK_ID + " desc limit " + 1);

                    if (cursor != null && cursor.getCount() == 1
                            && cursor.moveToNext()) {
                        result = IlikeArticle.inject(cursor);
                    }
                } catch (Exception e) {
                    Utils.error(DataEntryManager.class, e.getStackTrace()
                            .toString());
                }

                if (cursor != null) {
                    cursor.close();
                }

                return result;
            }

            public static long getNewestContentId(ContentResolver resolver,
                    String channel) {
                long result = 0;
                Cursor cursor = null;
                try {
                    cursor = resolver.query(Bookmarks.CONTENT_URI,
                            new String[] { Bookmarks.BOOKMARK_ID },
                            getWhereString(channel), null,
                            Bookmarks.BOOKMARK_ID + " desc limit " + 1);

                    if (cursor != null && cursor.getCount() == 1
                            && cursor.moveToNext()) {
                        result = cursor.getLong(0);
                    }
                } catch (Exception e) {
                    Utils.error(DataEntryManager.class, e.getStackTrace()
                            .toString());
                }

                if (cursor != null) {
                    cursor.close();
                }

                return result;
            }

            public static long getOldestContentId(ContentResolver resolver,
                    String channel) {
                long result = 0;
                Cursor cursor = null;
                try {
                    cursor = resolver.query(Bookmarks.CONTENT_URI,
                            new String[] { Bookmarks.BOOKMARK_ID },
                            getWhereString(channel), null,
                            Bookmarks.BOOKMARK_ID + " limit " + 1);

                    if (cursor != null && cursor.getCount() == 1
                            && cursor.moveToNext()) {
                        result = cursor.getLong(0);
                        cursor.close();
                    }
                } catch (Exception e) {
                    Utils.error(DataEntryManager.class, e.getStackTrace()
                            .toString());
                }

                if (cursor != null) {
                    cursor.close();
                }

                return result;
            }

            public static int getCount(ContentResolver resolver,
                    String channel, String where) {
                int result = 0;
                Cursor cursor = null;
                try {
                    cursor = resolver.query(Bookmarks.CONTENT_URI,
                            new String[] { Bookmarks._ID },
                            getWhereString(channel, where), null, null);

                    if (cursor != null) {
                        result = cursor.getCount();
                        cursor.close();
                    }
                } catch (Exception e) {
                    Utils.error(DataEntryManager.class, e.getStackTrace()
                            .toString());
                }

                if (cursor != null) {
                    cursor.close();
                }

                return result;
            }

            public static int delete(ContentResolver resolver, String channel,
                    String where) {
                try {
                    return resolver.delete(Bookmarks.CONTENT_URI,
                            getWhereString(channel, where), null);
                } catch (Exception e) {
                    Utils.error(DataEntryManager.class, e.getStackTrace()
                            .toString());
                }
                return -1;
            }

            public static int deleteAll(ContentResolver resolver) {
                try {
                    return resolver.delete(Bookmarks.CONTENT_URI,
                            getWhereString(), null);
                } catch (Exception e) {
                    Utils.error(DataEntryManager.class, e.getStackTrace()
                            .toString());
                }
                return -1;
            }

            public static int deleteAll(ContentResolver resolver, String channel) {
                try {
                    return resolver.delete(Bookmarks.CONTENT_URI,
                            getWhereString(channel), null);
                } catch (Exception e) {
                    Utils.error(DataEntryManager.class, e.getStackTrace()
                            .toString());
                }
                return -1;
            }

            private static String getWhereString() {
                String where = String.format("%s<%d", Bookmarks.I_LIKE,
                        IlikeArticle.STAR_DISAPPEARANCE);
                return where;
            }

            private static String getWhereString(String channel) {
                String where = String.format("%s='%s' AND %s<%d",
                        Bookmarks.CATEGORY_SRV_ID, channel, Bookmarks.I_LIKE,
                        IlikeArticle.STAR_DISAPPEARANCE);
                return where;
            }

            private static String getWhereString(String channel, String cause) {
                String where = String.format("%s='%s' AND %s<%d AND %s",
                        Bookmarks.CATEGORY_SRV_ID, channel, Bookmarks.I_LIKE,
                        IlikeArticle.STAR_DISAPPEARANCE, cause);
                return where;
            }
        } // END [NORMAL]

        public static int update(ContentResolver resolver, IlikeArticle article) {
            try {
                return resolver.update(Bookmarks.CONTENT_URI,
                        getContentValues(article),
                        getWhereString(article.channel, article.contentid),
                        null);
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, e.getStackTrace()
                        .toString());
            }

            return -1;
        }

        public static int update(ContentResolver resolver, String channel,
                long contentId, ContentValues values) {
            return update(resolver, channel,
                    getWhereString(channel, contentId), values);
        }

        public static int update(ContentResolver resolver, String channel,
                String where, ContentValues values) {
            try {
                return resolver.update(Bookmarks.CONTENT_URI, values, where,
                        null);
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, e.getStackTrace()
                        .toString());
            }

            return -1;
        }

        public static int update(ContentResolver resolver, String channel,
                long from, long to, ContentValues values) {
            try {
                return resolver.update(Bookmarks.CONTENT_URI, values,
                        getWhereString(channel, from, to), null);
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, e.getStackTrace()
                        .toString());
            }

            return -1;
        }

        public static int delete(ContentResolver resolver, int _id) {
            try {
                return resolver.delete(Bookmarks.CONTENT_URI, Bookmarks._ID
                        + " = " + _id, null);
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, e.getStackTrace()
                        .toString());
            }
            return -1;
        }

        private static String getWhereString(String channel) {
            String where = String.format("%s='%s'", Bookmarks.CATEGORY_SRV_ID,
                    channel);
            return where;
        }

        private static String getWhereString(String channel, long from, long to) {
            String where = String.format("%s='%s' AND  %s<=%d AND %s>=%d",
                    Bookmarks.CATEGORY_SRV_ID, channel, Bookmarks.BOOKMARK_ID,
                    from, Bookmarks.BOOKMARK_ID, to);
            return where;
        }

        private static String getWhereString(String channel, long contentId) {
            String where = String.format("%s='%s' AND %s=%d",
                    Bookmarks.CATEGORY_SRV_ID, channel, Bookmarks.BOOKMARK_ID,
                    contentId);
            return where;
        }
    }

    private static ContentValues getContentValues(IlikeArticle article) {
        ContentValues values = new ContentValues();
        values.put(Bookmarks.CATEGORY_SRV_ID, article.channel);
        values.put(Bookmarks.BOOKMARK_ID, article.contentid);
        values.put(Bookmarks.TITLE, article.title);
        values.put(Bookmarks.SNAPSHOT, article.snapshot);
        values.put(Bookmarks.DESCRIPTION, article.description);
        values.put(Bookmarks.ALBUM_ID, article.album_id);
        values.put(Bookmarks.ALBUM_TITLE, article.album_title);
        values.put(Bookmarks.IMAGES, article.images360);
        values.put(Bookmarks.AUTHOR, article.author);
        values.put(Bookmarks.AUTHOR_IMAGE_URL, article.author_image_url);
        values.put(Bookmarks.AUTHOR_QID, article.author_qid);
        values.put(Bookmarks.I_LIKE, article.star);
        values.put(Bookmarks.ISDOWNLOADED, article.isDownloaded);
        values.put(Bookmarks.LIKE_COUNT, article.like_count);
        values.put(Bookmarks.PUB_DATE, article.pubdate);
        values.put(Bookmarks.READ, article.read);
        values.put(Bookmarks.SORT, article.sort);
        return values;
    }

    public static boolean addLikedUrl(ContentResolver resolver, String url) {
        if (resolver == null || TextUtils.isEmpty(url)
                || urlLiked(resolver, url)) {
            return false;
        }

        ContentValues values = new ContentValues();
        values.put(LikedUrls.LIKED_URLS, url);
        try {
            resolver.insert(LikedUrls.CONTENT_URI, values);
        } catch (Exception e) {
            Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
            return false;
        }

        return true;
    }

    public static boolean removeLikeUrl(ContentResolver resolver, String url) {
        if (resolver == null || TextUtils.isEmpty(url)) {
            return false;
        }

        try {
            resolver.delete(LikedUrls.CONTENT_URI, LikedUrls.LIKED_URLS + "=?",
                    new String[] { url });
        } catch (Exception e) {
            Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
            return false;
        }

        return true;
    }

    public static boolean urlLiked(ContentResolver resolver, String url) {
        try {
            Cursor cursor = resolver.query(LikedUrls.CONTENT_URI,
                    new String[0], LikedUrls.LIKED_URLS + "='" + url + "'",
                    null, null);
            if (cursor != null) {
                boolean ret = cursor.getCount() > 0;
                cursor.close();
                return ret;
            }
        } catch (Exception e) {
            Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
        }
        return false;
    }
}
