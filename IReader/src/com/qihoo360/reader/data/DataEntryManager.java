package com.qihoo360.reader.data;

import com.qihoo360.reader.Settings;
import com.qihoo360.reader.data.Tables.Articles;
import com.qihoo360.reader.data.Tables.ChannelAccess;
import com.qihoo360.reader.data.Tables.Subscriptions;
import com.qihoo360.reader.subscription.Article;
import com.qihoo360.reader.subscription.reader.RssArticle;
import com.qihoo360.reader.subscription.reader.RssSubscribedChannel;
import com.qihoo360.reader.support.Utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                Subscriptions.CHANNEL, Subscriptions.TITLE,
                Subscriptions.PHOTO_URL, Subscriptions.SUB_DATE,
                Subscriptions.NUMBER_OF_VISITED,
                Subscriptions.NEWEST_IMAGE_CONTENT_ID,
                Subscriptions.IMAGE_VERSION, Subscriptions.LAST_CONTENT_ID,
                Subscriptions.LAST_REFRESH_TIME, Subscriptions.SORT_FLOAT,
                Subscriptions.OFFLINE, Subscriptions.OFFLINE_TIME,
                Subscriptions.OFFLINE_COUNT };

        public static Cursor getCursor(ContentResolver resolver) {
            try {
                Cursor cursor = resolver.query(Subscriptions.CONTENT_URI,
                        SubscriptionHelper.PROJECTIONS, null, null,
                        Subscriptions.SORT_FLOAT);
                return cursor;
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
                return null;
            }
        }

        public static Cursor getCursorWithoutSpecial(ContentResolver resolver,
                String where) {
            try {
                Cursor cursor = resolver.query(Subscriptions.CONTENT_URI,
                        SubscriptionHelper.PROJECTIONS,
                        getWhereStringWithoutSpecial(where), null,
                        Subscriptions.SORT_FLOAT);
                return cursor;
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
                return null;
            }
        }

        public static RssSubscribedChannel get(ContentResolver resolver,
                String channel) {
            if (resolver == null) {
                return null;
            }

            Cursor cursor = null;
            RssSubscribedChannel subscribedChannel = null;
            try {
                cursor = resolver.query(Subscriptions.CONTENT_URI,
                        SubscriptionHelper.PROJECTIONS, Subscriptions.CHANNEL
                                + "='" + channel + "'", null, null);
                if (cursor != null && cursor.moveToNext()) {
                    subscribedChannel = RssSubscribedChannel.inject(cursor);
                }
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
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
                                + RssSubscribedChannel.SortFloat.MAX, null,
                        Subscriptions.SORT_FLOAT + " limit " + 1);
                if (cursor != null && cursor.moveToNext()) {
                    firstSortFloat = cursor.getDouble(0);
                }
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
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
                                + RssSubscribedChannel.SortFloat.MAX, null,
                        Subscriptions.SORT_FLOAT + " desc limit " + 1);
                if (cursor != null && cursor.moveToNext()) {
                    lastSortFloat = cursor.getDouble(0);
                }
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
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
                Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
            }

            if (cursor != null) {
                cursor.close();
            }

            return count;
        }

        public static int getFixedCount(ContentResolver resolver) {
            return getCount(resolver, getWhereStringWithFixed(null));
        }

        public static int getSpecialCount(ContentResolver resolver) {
            return getCount(resolver, getWhereStringWithSpecial(null));
        }

        public static int update(ContentResolver resolver, String where,
                ContentValues values) {
            try {
                return resolver.update(Subscriptions.CONTENT_URI, values, null,
                        null);
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
            }

            return -1;
        }
        /**
         * update
         */
        public static int update(ContentResolver resolver, int subscription_id,
                ContentValues values) {
            try {
                return resolver.update(ContentUris.withAppendedId(
                        Subscriptions.CONTENT_URI, subscription_id), values,
                        null, null);
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
            }

            return -1;
        }
        /**
         * delete
         */
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
                Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
                return 0;
            }
        }
        /**
         * delete
         *  @param resolver
         *  @param channel 
         *  @return    设定文件 
         */
        public static int delete(ContentResolver resolver, String channel) {
            if (resolver == null || channel == null || channel.equals("")) {
                return 0;
            }
            try {
                return resolver.delete(Subscriptions.CONTENT_URI,
                        Subscriptions.CHANNEL + "=?", new String[] { channel });
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
                return 0;
            }
        }
        /**
         *  获取没有特殊频道
         *  @param where
         *  @return    设定文件 
         */
        public static String getWhereStringWithoutSpecial(String where) {
            String whereResult = String.format("%s<>'%s'",
                    Subscriptions.NUMBER_OF_VISITED,
                    RssSubscribedChannel.SPECIAL_NUMBER_OF_VISITED);

            if (!TextUtils.isEmpty(where)) {
                whereResult += " AND " + where;
            }

            return whereResult;
        }
        /**
         * 获得特殊频道
         *  @param where
         *  @return    设定文件 
         */
        public static String getWhereStringWithSpecial(String where) {
            String whereResult = String.format("%s='%s'",
                    Subscriptions.NUMBER_OF_VISITED,
                    RssSubscribedChannel.SPECIAL_NUMBER_OF_VISITED);

            if (!TextUtils.isEmpty(where)) {
                whereResult += " AND " + where;
            }

            return whereResult;
        }
        /**
         * 获得固定频道
         *  @param where
         *  @return    设定文件 
         * @throws
         */
        public static String getWhereStringWithFixed(String where) {
            String whereResult = String
                    .format("%s='%s'", Subscriptions.SUB_DATE,
                            RssSubscribedChannel.FIXED_SUB_DATE);

            if (!TextUtils.isEmpty(where)) {
                whereResult += " AND " + where;
            }

            return whereResult;
        }

    }

    static public class ChannelAccessHelper {
        static public final String[] PROJECTIONS = { ChannelAccess._ID,
                ChannelAccess.CHANNEL, ChannelAccess.DAILY_COUNT,
                ChannelAccess.DATE };

        public static void increaseAccess(ContentResolver resolver,
                String channel) {
            if (TextUtils.isEmpty(channel)) {
                return;
            }

            long time = System.currentTimeMillis();
            int date = (int) (time / (1000 * 3600 * 24));

            Cursor cursor = null;
            try {
                boolean exists = false;
                ContentValues values = new ContentValues();

                String[] projections = { ChannelAccess._ID,
                        ChannelAccess.DAILY_COUNT, };
                String sel = ChannelAccess.CHANNEL + "='" + channel + "' and "
                        + ChannelAccess.DATE + "=" + date;
                cursor = resolver.query(ChannelAccess.CONTENT_URI, projections,
                        sel, null, null);
                if (cursor != null) {
                    if (cursor.getCount() > 0) {
                        exists = true;
                        cursor.moveToFirst();
                        int id = cursor.getInt(0);
                        int daily_count = cursor.getInt(1);

                        values.put(ChannelAccess.DAILY_COUNT, daily_count + 1);
                        resolver.update(ChannelAccess.CONTENT_URI, values,
                                ChannelAccess._ID + "=" + id, null);
                    }
                }

                if (!exists) {
                    values.put(ChannelAccess.CHANNEL, channel);
                    values.put(ChannelAccess.DAILY_COUNT, 1);
                    values.put(ChannelAccess.DATE, date);
                    resolver.insert(ChannelAccess.CONTENT_URI, values);
                }

                resort(resolver, date);
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
            }

            if (cursor != null) {
                cursor.close();
            }
        }

        public static class ChannelAccessInfo {
            String channel;
            int accessDayCount = 0;
            int totalAccessCount = 0;
        }

        private static final int RESORT_DAY_LIMIT = 7;

        public static void resort(ContentResolver resolver, int date) {
            if (date <= Settings.getLastResortDate()) {
                return;
            }

            HashMap<String, ChannelAccessInfo> mAccessInfoMap = null;
            String[] projections = { ChannelAccess.CHANNEL,
                    ChannelAccess.DAILY_COUNT, };
            Cursor cursor = null;
            try {
                cursor = resolver.query(ChannelAccess.CONTENT_URI, projections,
                        ChannelAccess.DATE + ">" + (date - RESORT_DAY_LIMIT),
                        null, null);
                if (cursor != null) {
                    if (cursor.getCount() > 0) {
                        mAccessInfoMap = new HashMap<String, ChannelAccessInfo>();

                        while (cursor.moveToNext()) {
                            String channel = cursor.getString(0);
                            int access_count = cursor.getInt(1);

                            ChannelAccessInfo cai = mAccessInfoMap.get(channel);
                            if (cai == null) {
                                cai = new ChannelAccessInfo();
                                cai.channel = channel;
                                mAccessInfoMap.put(channel, cai);
                            }
                            cai.accessDayCount++;
                            cai.totalAccessCount += access_count;
                        }

                        if (mAccessInfoMap != null && mAccessInfoMap.size() > 0) {
                            for (ChannelAccessInfo cai : mAccessInfoMap
                                    .values()) {
                                if (cai.accessDayCount > RESORT_DAY_LIMIT
                                        || cai.accessDayCount > 0x00FFFFFF) {
                                    Utils.debug(TAG,
                                            "Overflow when resorting channels!!!!");
                                }
                                int weightedCount = ((cai.accessDayCount & 0x000000FF) << 48)
                                        | (cai.totalAccessCount & 0x00FFFFFF);

                                ContentValues values = new ContentValues();
                                values.put(Subscriptions.NUMBER_OF_VISITED,
                                        weightedCount);
                                resolver.update(Subscriptions.CONTENT_URI,
                                        values, Subscriptions.CHANNEL + "=?",
                                        new String[] { cai.channel });
                            }

                            mAccessInfoMap.clear();
                            Settings.setLastResortDate(date);
                        }

                    }
                }

                // delete old data
                resolver.delete(ChannelAccess.CONTENT_URI, ChannelAccess.DATE
                        + "<=" + (date - RESORT_DAY_LIMIT), null);
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
            }
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    static public class ArticleHelper {
        static public final String[] PROJECTIONS = { Articles._ID,
                Articles.CHANNEL, Articles.CONTENT_ID, Articles.TITLE,
                Articles.DESCRIPTION, Articles.COMPRESSED_IMAGE_URL,
                Articles.PUB_DATE, Articles.LINK, Articles.AUTHOR,
                Articles.READ, Articles.STAR, Articles.STARDATE,
                Articles.ISDOWNLOADED, Articles.ISOFFLINED };

        static public final String[] PROJECTIONS_ABSTRACT = { Articles._ID,
                Articles.CONTENT_ID, Articles.TITLE,
                Articles.COMPRESSED_IMAGE_URL, Articles.PUB_DATE,
                Articles.READ, Articles.ISOFFLINED };

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
        /**
         *  注入摘要
         *  @param cursor
         *  @return    设定文件 
         */
        public static RssArticle injectAbstract(Cursor cursor) {
            if (cursor == null) {
                return null;
            }

            RssArticle article = new RssArticle();
            int idx;
            idx = cursor.getColumnIndex(Articles._ID);
            if (idx >= 0)
                article._id = cursor.getInt(idx);

            idx = cursor.getColumnIndex(Articles.CONTENT_ID);
            if (idx >= 0)
                article.contentid = cursor.getLong(idx);

            idx = cursor.getColumnIndex(Articles.TITLE);
            if (idx >= 0)
                article.title = cursor.getString(idx);

            idx = cursor.getColumnIndex(Articles.COMPRESSED_IMAGE_URL);
            if (idx >= 0)
                article.images360 = cursor.getString(idx);

            idx = cursor.getColumnIndex(Articles.PUB_DATE);
            if (idx >= 0)
                article.pubdate = cursor.getLong(idx);

            idx = cursor.getColumnIndex(Articles.READ);
            if (idx >= 0)
                article.read = cursor.getInt(idx);

            idx = cursor.getColumnIndex(Articles.ISOFFLINED);
            if (idx >= 0)
                article.isOfflined = cursor.getInt(idx) == 1;

            return article;
        }

        public static boolean add(ContentResolver resolver, RssArticle article) {
            if (resolver == null || article == null) {
                return false;
            }

            ContentValues values = getContentValues(article);
            try {
                resolver.insert(Articles.CONTENT_URI, values);
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
                return false;
            }

            return true;
        }
        /**
         * 获取数据文件
         *  @param resolver
         *  @param _id
         *  @return    设定文件 
         */
        public static Article get(ContentResolver resolver, int _id) {
            Cursor cursor = null;
            Article article = null;
            try {
                cursor = resolver.query(Articles.CONTENT_URI,
                        ArticleHelper.PROJECTIONS, Articles._ID + "=" + _id,
                        null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        article = ArticleHelper.inject(cursor);
                    }
                }
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
            }

            if (cursor != null) {
                cursor.close();
            }

            return article;
        }

        public static RssArticle getByContentId(ContentResolver resolver,
                String channel, long contentId) {
            RssArticle article = null;
            Cursor cursor = null;
            try {
                cursor = resolver.query(Articles.CONTENT_URI,
                        ArticleHelper.PROJECTIONS,
                        getWhereString(channel, contentId), null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        article = ArticleHelper.inject(cursor);
                    }
                    cursor.close();
                }
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
            }

            if (cursor != null) {
                cursor.close();
            }

            return article;
        }

        public static Map<Long, Boolean> getContentIdAndDownloadedFlags(
                ContentResolver resolver, String channel, long from, long to) {
            Map<Long, Boolean> map = new HashMap<Long, Boolean>();
            Cursor cursor = null;
            try {
                cursor = resolver.query(Articles.CONTENT_URI, new String[] {
                        Articles.CONTENT_ID, Articles.ISDOWNLOADED },
                        getWhereString(channel, from, to), null, null);
                while (cursor != null && cursor.moveToNext()) {
                    map.put(cursor.getLong(0), cursor.getInt(1) == 1);
                }
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
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
                cursor = resolver.query(Articles.CONTENT_URI, new String[] {
                        Articles.CONTENT_ID, Articles.ISDOWNLOADED },
                        getWhereString(channel), null, null);
                while (cursor != null && cursor.moveToNext()) {
                    map.put(cursor.getLong(0), cursor.getInt(1) == 1);
                }
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
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
                    cursor = resolver.query(Articles.CONTENT_URI,
                            new String[] { Articles.CONTENT_ID },
                            getWhereString(channel), null, Articles.CONTENT_ID
                                    + " desc");
                    while (cursor != null && cursor.moveToNext()) {
                        list.add(cursor.getLong(0));
                    }
                } catch (Exception e) {
                    Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
                }

                if (cursor != null) {
                    cursor.close();
                }
                return list;
            }

            public static Cursor getCursorByChannel(ContentResolver resolver,
                    String channel, boolean isAbstract) {
                String[] projections = null;
                if (isAbstract) {
                    projections = ArticleHelper.PROJECTIONS_ABSTRACT;
                } else {
                    projections = ArticleHelper.PROJECTIONS;
                }
                try {
                    Cursor cursor = resolver.query(Articles.CONTENT_URI,
                            projections, getWhereString(channel), null,
                            Articles.CONTENT_ID + " desc");

                    return cursor;
                } catch (Exception e) {
                    Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
                    return null;
                }
            }

            public static Cursor getCursorOfSpecificFieldsByChannel(
                    ContentResolver resolver, String channel,
                    String[] projections) {
                try {
                    Cursor cursor = resolver.query(Articles.CONTENT_URI,
                            projections, getWhereString(channel), null,
                            Articles.CONTENT_ID + " desc");

                    return cursor;
                } catch (Exception e) {
                    Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
                    return null;
                }
            }

            public static int delete(ContentResolver resolver, String channel,
                    String where) {
                try {
                    // 将所有star=1的变成star=2，以“逃过”删除所有文章的“命运”
                    if (DataEntryManager.ArticleHelper.Star.updateAllStar(
                            resolver, RssArticle.STAR_APPEARANCE,
                            RssArticle.STAR_DISAPPEARANCE) > -1) {
                        return resolver.delete(Articles.CONTENT_URI,
                                getWhereString(channel, where), null);
                    }
                } catch (Exception e) {
                    Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
                }
                return -1;
            }

            public static int deleteAll(ContentResolver resolver) {
                try {
                    // 将所有star=1的变成star=2，以“逃过”删除所有文章的“命运”
                    if (DataEntryManager.ArticleHelper.Star.updateAllStar(
                            resolver, RssArticle.STAR_APPEARANCE,
                            RssArticle.STAR_DISAPPEARANCE) > -1) {
                        return resolver.delete(Articles.CONTENT_URI,
                                getWhereString(), null);
                    }
                } catch (Exception e) {
                    Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
                }
                return -1;
            }

            public static int deleteAll(ContentResolver resolver, String channel) {
                try {
                    // 将所有star=1的变成star=2，以“逃过”删除所有文章的“命运”
                    if (DataEntryManager.ArticleHelper.Star.updateStar(
                            resolver, channel, RssArticle.STAR_APPEARANCE,
                            RssArticle.STAR_DISAPPEARANCE) > -1) {
                        return resolver.delete(Articles.CONTENT_URI,
                                getWhereString(channel), null);
                    }
                } catch (Exception e) {
                    Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
                }
                return -1;
            }

            private static String getWhereString() {
                String where = String.format("%s<%d", Articles.STAR,
                        RssArticle.STAR_DISAPPEARANCE);
                return where;
            }

            private static String getWhereString(String channel) {
                String where = String.format("%s='%s' AND %s<%d",
                        Articles.CHANNEL, channel, Articles.STAR,
                        RssArticle.STAR_DISAPPEARANCE);
                return where;
            }

            private static String getWhereString(String channel, String cause) {
                String where = String.format("%s='%s' AND %s<%d AND %s",
                        Articles.CHANNEL, channel, Articles.STAR,
                        RssArticle.STAR_DISAPPEARANCE, cause);
                return where;
            }
        } // END [NORMAL]

        public static class Star {
            public static Cursor getCursor(ContentResolver resolver) {
                try {
                    String where = String.format("%s=%d OR %s=%d",
                            Articles.STAR, RssArticle.STAR_APPEARANCE,
                            Articles.STAR, RssArticle.STAR_DISAPPEARANCE);
                    Cursor cursor = resolver.query(Articles.CONTENT_URI,
                            ArticleHelper.PROJECTIONS, where, null,
                            Articles.STARDATE + " desc");

                    return cursor;
                } catch (Exception e) {
                    Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
                    return null;
                }
            }

            public static List<Long> getContentIds(ContentResolver resolver,
                    String channel, long from, long to, int star) {
                if (from < to) {
                    long temp = from;
                    from = to;
                    to = temp;
                }

                List<Long> list = new ArrayList<Long>();
                Cursor cursor = null;
                try {
                    String where = String.format(
                            "%s='%s' AND %s=%d AND %s<=%d AND %s>=%d",
                            Articles.CHANNEL, channel, Articles.STAR, star,
                            Articles.CONTENT_ID, from, Articles.CONTENT_ID, to);
                    cursor = resolver.query(Articles.CONTENT_URI,
                            new String[] { Articles.CONTENT_ID }, where, null,
                            Articles.CONTENT_ID + " desc");
                    while (cursor != null && cursor.moveToNext()) {
                        list.add(cursor.getLong(0));
                    }
                } catch (Exception e) {
                    Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
                }

                if (cursor != null) {
                    cursor.close();
                }

                return list;
            }

            public static int updateStar(ContentResolver resolver,
                    String channel, int whereStar, int toStar) {
                String where = String.format("%s='%s' AND %s=%d",
                        Articles.CHANNEL, channel, Articles.STAR, whereStar);
                ContentValues values = new ContentValues();
                values.put(Articles.STAR, toStar);
                try {
                    return resolver.update(Articles.CONTENT_URI, values, where,
                            null);
                } catch (Exception e) {
                    Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
                }

                return -1;
            }

            public static int updateAllStar(ContentResolver resolver,
                    int whereStar, int toStar) {
                String where = String.format("%s=%d", Articles.STAR, whereStar);
                ContentValues values = new ContentValues();
                values.put(Articles.STAR, toStar);
                try {
                    return resolver.update(Articles.CONTENT_URI, values, where,
                            null);
                } catch (Exception e) {
                    Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
                }

                return -1;
            }

            public static int updateStarByContentId(ContentResolver resolver,
                    String channel, long contentId, int star) {
                String where = String.format("%s='%s' AND %s=%d",
                        Articles.CHANNEL, channel, Articles.CONTENT_ID,
                        contentId);
                ContentValues values = new ContentValues();
                values.put(Articles.STAR, star);
                try {
                    return resolver.update(Articles.CONTENT_URI, values, where,
                            null);
                } catch (Exception e) {
                    Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
                }

                return -1;
            }

            public static int deleteAll(ContentResolver resolver) {
                try {
                    String where = String.format("%s=%d", Articles.STAR,
                            RssArticle.STAR_DISAPPEARANCE);
                    // 将所有star=1的变成star=0，我们只删除star=2的“顽固”的收藏
                    if (DataEntryManager.ArticleHelper.Star.updateAllStar(
                            resolver, RssArticle.STAR_APPEARANCE,
                            RssArticle.STAR_NONE) > -1) {
                        return resolver.delete(Articles.CONTENT_URI, where,
                                null);
                    }
                } catch (Exception e) {
                    Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
                }
                return -1;
            }
        } // END [Star]

        public static int update(ContentResolver resolver, RssArticle article) {
            try {
                return resolver.update(Articles.CONTENT_URI,
                        getContentValues(article),
                        getWhereString(article.channel, article.contentid),
                        null);
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
            }

            return -1;
        }

        public static int updateContent(ContentResolver resolver,
                RssArticle article) {
            try {
                ContentValues values = new ContentValues();
                values.put(Articles.DESCRIPTION, article.description);
                values.put(Articles.COMPRESSED_IMAGE_URL, article.images360);
                values.put(Articles.LINK, article.link);
                values.put(Articles.AUTHOR, article.author);
                values.put(Articles.ISDOWNLOADED, article.isDownloaded);
                return resolver.update(Articles.CONTENT_URI, values,
                        getWhereString(article.channel, article.contentid),
                        null);
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
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
                return resolver.update(Articles.CONTENT_URI, values, where,
                        null);
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
            }

            return -1;
        }

        public static int update(ContentResolver resolver, String channel,
                long from, long to, ContentValues values) {
            try {
                return resolver.update(Articles.CONTENT_URI, values,
                        getWhereString(channel, from, to), null);
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
            }

            return -1;
        }

        public static int delete(ContentResolver resolver, int _id) {
            try {
                return resolver.delete(Articles.CONTENT_URI, Articles._ID
                        + " = " + _id, null);
            } catch (Exception e) {
                Utils.error(DataEntryManager.class, Utils.getStackTrace(e));
            }
            return -1;
        }

        private static String getWhereString(String channel) {
            String where = String.format("%s='%s'", Articles.CHANNEL, channel);
            return where;
        }

        private static String getWhereString(String channel, long from, long to) {
            String where = String.format("%s='%s' AND  %s<=%d AND %s>=%d",
                    Articles.CHANNEL, channel, Articles.CONTENT_ID, from,
                    Articles.CONTENT_ID, to);
            return where;
        }

        private static String getWhereString(String channel, long contentId) {
            String where = String.format("%s='%s' AND %s=%d", Articles.CHANNEL,
                    channel, Articles.CONTENT_ID, contentId);
            return where;
        }
    }

    private static ContentValues getContentValues(RssArticle article) {
        ContentValues values = new ContentValues();
        values.put(Articles.CHANNEL, article.channel);
        values.put(Articles.CONTENT_ID, article.contentid);
        values.put(Articles.TITLE, article.title);
        values.put(Articles.DESCRIPTION, article.description);
        values.put(Articles.COMPRESSED_IMAGE_URL, article.images360);
        values.put(Articles.PUB_DATE, article.pubdate);
        values.put(Articles.LINK, article.link);
        values.put(Articles.AUTHOR, article.author);
        values.put(Articles.READ, article.read);
        values.put(Articles.STAR, article.star);
        values.put(Articles.STARDATE, article.stardate);
        values.put(Articles.ISDOWNLOADED, article.isDownloaded);
        values.put(Articles.ISOFFLINED, article.isOfflined);
        return values;
    }
}
