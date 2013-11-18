/**
 *
 */

package com.qihoo360.reader.subscription.reader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import com.qihoo360.reader.Constants;
import com.qihoo360.reader.ReaderApplication;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.data.DataEntryManager;
import com.qihoo360.reader.data.DataEntryManager.SubscriptionHelper;
import com.qihoo360.reader.data.Tables.Subscriptions;
import com.qihoo360.reader.subscription.Channel;
import com.qihoo360.reader.subscription.SubscribedChannel;
import com.qihoo360.reader.support.Utils;

/**
 * 表示在主页上显示的“已订阅频道”
 *
 * @author Jiongxuan Zhang
 */
public class RssSubscribedChannel extends SubscribedChannel {

    /**
     * 网址导航
     */
    public static final String SITE_NAVIGATION = "my_conllection";

    /**
     * 离线下载
     */
    public static final String OFFLINE_DOWNLOAD = "offline_download";

    /**
     * 添加订阅
     */
    public static final String ADD_SUBSCRIBE = "add_subscribe";

    /**
     * 随便看看
     */
    public static final String RANDOM_READ = "random_read";

    /**
     * 我的收藏
     */
    public static final String MY_CONLLECTION = "my_collection";

    /**
     * 意见反馈
     */
    public static final String FEEDBACK = "feedback";

    /**
     * 看是否需要重新刷新排序
     */
    public static boolean sNeedRefreshSortFloat = false;

    /**
     * 表示为固定的频道
     */
    public static long FIXED_SUB_DATE = Long.MIN_VALUE;

    /**
     * 表示为特殊的频道，和可订阅的频道区分开
     */
    public static long SPECIAL_NUMBER_OF_VISITED = Long.MAX_VALUE;

    /**
     * 初始化频道
     *
     * @param resolver
     */
    public static void initDefault(ContentResolver resolver) {
        Map<String, RssSubscribedChannel> defaultMap = getDefaultSubscribedMap();
        // 用户过去就有的
        double sort = SortFloat.MAX_ACTIVATE + 20; // 加到后面
        double step = 0.0001;
        List<String> subedList = RssSubscribedChannel
                .getChannelsForNameOnly(resolver);
        for (String channelName : subedList) {
            if (!defaultMap.containsKey(channelName)) { // 用户自己的
                RssSubscribedChannel subscribedChannel = RssSubscribedChannel
                        .get(resolver, channelName);
                if (subscribedChannel != null) {
                    subscribedChannel.sort_float = sort;
                    sort += step;
                    defaultMap
                            .put(subscribedChannel.channel, subscribedChannel);
                }
            }
        }

        // 现在将频道插入数据库
        for (RssSubscribedChannel subscribedChannel : defaultMap.values()) {
            if (subedList.contains(subscribedChannel.channel)) {
                subscribedChannel.updateToDb(resolver);
            } else {
                subscribedChannel.insertToDb(resolver);
            }
        }

        update(resolver);
        calculateSortFloat(resolver);

        Settings.setDatabaseInit(true);
    }

    int updateToDb(ContentResolver resolver) {
        try {
            return resolver.update(Subscriptions.CONTENT_URI,
                    getContentValues(this),
                    String.format("%s='%s'", Subscriptions.CHANNEL, channel),
                    null);
        } catch (Exception e) {
            Utils.error(RssSubscribedChannel.class, Utils.getStackTrace(e));
        }

        return -1;
    }

    boolean insertToDb(ContentResolver resolver) {
        if (resolver == null) {
            return false;
        }

        if (contains(resolver)) {
            return true;
        }

        ContentValues values = getContentValues(this);
        try {
            resolver.insert(Subscriptions.CONTENT_URI, values);
        } catch (Exception e) {
            Utils.error(RssSubscribedChannel.class, Utils.getStackTrace(e));
            return false;
        }

        return true;
    }

    boolean contains(ContentResolver resolver) {
        Cursor cursor = null;
        try {
            cursor = resolver.query(Subscriptions.CONTENT_URI,
                    new String[] { Subscriptions._ID }, "channel=" + "'"
                            + channel + "'", null, null);

            if (cursor != null && cursor.moveToNext()) {
                cursor.close();
                return true;
            }
        } catch (Exception e) {
            Utils.error(RssSubscribedChannel.class, Utils.getStackTrace(e));
        }

        if (cursor != null) {
            cursor.close();
        }
        return false;
    }

    private static ContentValues getContentValues(SubscribedChannel subscription) {
        ContentValues values = new ContentValues();
        values.put(Subscriptions.CHANNEL, subscription.channel);
        values.put(Subscriptions.TITLE, subscription.title);
        values.put(Subscriptions.PHOTO_URL, subscription.photo_url);
        values.put(Subscriptions.SUB_DATE, subscription.sub_date);
        values.put(Subscriptions.NUMBER_OF_VISITED,
                subscription.number_of_visited);
        values.put(Subscriptions.NEWEST_IMAGE_CONTENT_ID,
                subscription.newest_image_content_id);
        values.put(Subscriptions.IMAGE_VERSION, subscription.image_version);
        values.put(Subscriptions.LAST_CONTENT_ID, subscription.last_content_id);
        values.put(Subscriptions.LAST_REFRESH_TIME,
                subscription.last_refresh_time);
        values.put(Subscriptions.SORT_FLOAT, subscription.sort_float);
        values.put(Subscriptions.OFFLINE, subscription.offline);
        values.put(Subscriptions.OFFLINE_TIME, subscription.offline_time);
        values.put(Subscriptions.OFFLINE_COUNT, subscription.offline_count);
        return values;
    }

    /**
     * 立即更新标题和对应的ImageVersion
     *
     * @param resolver
     */
    public static void update(ContentResolver resolver) {
        List<RssSubscribedChannel> list = getWithoutFixed(resolver);
        // 需要刷新数据
        RssManager.getIndex();
        if (list != null) {
            for (RssSubscribedChannel subscribedChannel : list) {
                Channel channel = subscribedChannel.getChannel();
                if (channel != null) {
                    // title
                    boolean changed = false;
                    ContentValues values = new ContentValues();
                    if (!TextUtils.isEmpty(channel.title)
                            && !channel.title.equals(subscribedChannel.title)) {
                        changed = true;
                        values.put(Subscriptions.TITLE, channel.title);
                    }
                    // imageVersion
                    int imageVersion = TextUtils.isEmpty(channel.imageversion) == false ? Integer
                            .parseInt(channel.imageversion) : 0;
                    if (imageVersion > subscribedChannel.image_version) {
                        changed = true;
                        values.put(Subscriptions.IMAGE_VERSION, imageVersion);
                    }
                    // image(url)
                    if (!TextUtils.isEmpty(channel.image)
                            && !channel.title
                                    .equals(subscribedChannel.photo_url)) {
                        changed = true;
                        values.put(Subscriptions.PHOTO_URL, channel.image);
                    }

                    if (changed) {
                        DataEntryManager.SubscriptionHelper.update(resolver,
                                subscribedChannel._id, values);
                    }
                } else {
                    // 针对服务器端已经没有的频道，则直接干掉。
                    subscribedChannel.unsubscribe(resolver);
                }
            }
        }
    }

    /**
     * 将所有订阅过的频道都设为允许或者不允许离线下载
     *
     * @param resolver
     */
    public static void setAllOffline(ContentResolver resolver, boolean isChecked) {
        int checked = isChecked ? 1 : 0;

        ContentValues values = new ContentValues();
        values.put(Subscriptions.OFFLINE, isChecked);
        DataEntryManager.SubscriptionHelper.update(resolver,
                Subscriptions.OFFLINE + "=" + Math.abs(checked - 1), values); // |1-1|
                                                                                // =
                                                                                // 0;
                                                                                // |0-1|
                                                                                // =
                                                                                // 1

        List<RssSubscribedChannel> list = getForOffline(resolver, checked);
        for (RssSubscribedChannel subscribedChannel : list) {
            subscribedChannel.offline = isChecked;
        }
    }

    /**
     * 重新计算所有的频道排序
     *
     * @param resolver
     */
    public static void calculateSortFloat(ContentResolver resolver) {
        List<RssSubscribedChannel> list = get(resolver);

        if (list != null && list.size() != 0) {
            int size = list.size() - 3;
            double current = SortFloat.MIN_ACTIVATE;
            double step = (SortFloat.MAX_ACTIVATE - SortFloat.MIN_ACTIVATE)
                    / size;
            for (RssSubscribedChannel subscribedChannel : list) {
                // 在手动调整顺序以前，先把固定的几个频道的sort值设的夸张一些，如负数或大的正数
                if (RssSubscribedChannel.RANDOM_READ
                        .equalsIgnoreCase(subscribedChannel.channel)) {
                    subscribedChannel
                            .setSortFloat(resolver, -SortFloat.MAX / 2);
                    continue;
                } else if (RssSubscribedChannel.OFFLINE_DOWNLOAD
                        .equalsIgnoreCase(subscribedChannel.channel)) {
                    subscribedChannel
                            .setSortFloat(resolver, -SortFloat.MAX / 4);
                    continue;
                } else if (RssSubscribedChannel.MY_CONLLECTION
                        .equalsIgnoreCase(subscribedChannel.channel)) {
                    subscribedChannel
                            .setSortFloat(resolver, -SortFloat.MAX / 8);
                    continue;
                } else if (RssSubscribedChannel.ADD_SUBSCRIBE
                        .equalsIgnoreCase(subscribedChannel.channel)) {
                    subscribedChannel.setSortFloat(resolver, SortFloat.MAX * 2);
                    continue;
                }
                subscribedChannel.setSortFloat(resolver, current);
                current += step;
            }
        }

        RssSubscribedChannel.sNeedRefreshSortFloat = false;
        Settings.setLastResortDate((int) (System.currentTimeMillis() / (1000 * 3600 * 24)));
    }

    public static boolean isNeedRefreshSortFloat() {
        return RssSubscribedChannel.sNeedRefreshSortFloat;
    }

    /**
     * 通过Cursor填充Subscription对象
     *
     * @param cursor
     * @return
     */
    public static RssSubscribedChannel inject(Cursor cursor) {

        if (cursor == null) {
            return null;
        }

        RssSubscribedChannel se = new RssSubscribedChannel();
        int idx;
        idx = cursor.getColumnIndex(Subscriptions._ID);
        if (idx >= 0)
            se._id = cursor.getInt(idx);

        idx = cursor.getColumnIndex(Subscriptions.CHANNEL);
        if (idx >= 0)
            se.channel = cursor.getString(idx);

        idx = cursor.getColumnIndex(Subscriptions.TITLE);
        if (idx >= 0)
            se.title = cursor.getString(idx);

        idx = cursor.getColumnIndex(Subscriptions.PHOTO_URL);
        if (idx >= 0)
            se.photo_url = cursor.getString(idx);

        idx = cursor.getColumnIndex(Subscriptions.SUB_DATE);
        if (idx >= 0)
            se.sub_date = cursor.getLong(idx);

        idx = cursor.getColumnIndex(Subscriptions.NUMBER_OF_VISITED);
        if (idx >= 0)
            se.number_of_visited = cursor.getLong(idx);

        idx = cursor.getColumnIndex(Subscriptions.NEWEST_IMAGE_CONTENT_ID);
        if (idx >= 0)
            se.newest_image_content_id = cursor.getLong(idx);

        idx = cursor.getColumnIndex(Subscriptions.IMAGE_VERSION);
        if (idx >= 0)
            se.image_version = cursor.getInt(idx);

        idx = cursor.getColumnIndex(Subscriptions.LAST_CONTENT_ID);
        if (idx >= 0)
            se.last_content_id = cursor.getLong(idx);

        idx = cursor.getColumnIndex(Subscriptions.LAST_REFRESH_TIME);
        if (idx >= 0)
            se.last_refresh_time = cursor.getLong(idx);

        idx = cursor.getColumnIndex(Subscriptions.SORT_FLOAT);
        if (idx >= 0)
            se.sort_float = cursor.getDouble(idx);

        idx = cursor.getColumnIndex(Subscriptions.OFFLINE);
        if (idx >= 0)
            se.offline = cursor.getInt(idx) != 0;

        idx = cursor.getColumnIndex(Subscriptions.OFFLINE_TIME);
        if (idx >= 0)
            se.offline_time = cursor.getLong(idx);

        idx = cursor.getColumnIndex(Subscriptions.OFFLINE_COUNT);
        if (idx >= 0)
            se.offline_count = cursor.getInt(idx);

        return se;

    }

    /**
     * 获取所有可供显示的频道列表，用Cursor表示
     *
     * @param resolver
     * @return
     */
    public static Cursor getCursor(Activity activity, ContentResolver resolver) {
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
            Utils.error(RssSubscribedChannel.class, Utils.getStackTrace(e));
            return null;
        }

    }

    /**
     * 获取除了三个固定频道外，已经订阅过的频道
     *
     * @param resolver
     * @return
     */
    public static Cursor getCursorWithoutFixed(ContentResolver resolver) {
        return DataEntryManager.SubscriptionHelper.getCursorWithoutSpecial(
                resolver, null);
    }

    /**
     * 获取所有包含固定频道和已订阅频道的List
     *
     * @param resolver
     * @return
     */
    public static List<RssSubscribedChannel> get(ContentResolver resolver) {
        List<RssSubscribedChannel> list = new ArrayList<RssSubscribedChannel>();
        Cursor cursor = null;
        try {
            cursor = DataEntryManager.SubscriptionHelper.getCursor(resolver);
            while (cursor != null && cursor.moveToNext()) {
                list.add(RssSubscribedChannel.inject(cursor));
            }
        } catch (Exception e) {
            Utils.error(RssSubscribedChannel.class, Utils.getStackTrace(e));
        }

        if (cursor != null) {
            cursor.close();
        }
        return list;
    }

    /**
     * 获取所有包含固定频道和已订阅频道的List
     *
     * @param resolver
     * @return
     */
    public static List<RssSubscribedChannel> getWithoutFixed(
            ContentResolver resolver) {
        List<RssSubscribedChannel> list = new ArrayList<RssSubscribedChannel>();
        Cursor cursor = null;
        try {
            cursor = DataEntryManager.SubscriptionHelper
                    .getCursorWithoutSpecial(resolver, null);
            while (cursor != null && cursor.moveToNext()) {
                list.add(RssSubscribedChannel.inject(cursor));
            }
        } catch (Exception e) {
            Utils.error(RssSubscribedChannel.class, Utils.getStackTrace(e));
        }

        if (cursor != null) {
            cursor.close();
        }
        return list;
    }

    /**
     * 获取固定频道的数量
     *
     * @return
     */
    public static int getFixedCount() {
        return DataEntryManager.SubscriptionHelper
                .getFixedCount(ReaderApplication.getContext()
                        .getContentResolver());
    }

    /**
     * 获取特殊频道的数量
     *
     * @return
     */
    public static int getSpecialCount() {
        return DataEntryManager.SubscriptionHelper
                .getFixedCount(ReaderApplication.getContext()
                        .getContentResolver());
    }

    /**
     * 获取用户订阅的频道数量
     *
     * @return
     */
    public static int getSubscribedCount() {
        return DataEntryManager.SubscriptionHelper.getCount(ReaderApplication
                .getContext().getContentResolver(), String.format("%s<>'%s'",
                Subscriptions.NUMBER_OF_VISITED,
                RssSubscribedChannel.SPECIAL_NUMBER_OF_VISITED));
    }

    /**
     * 根据频道名获取频道
     *
     * @param resolver
     * @param channelName
     * @return
     */
    public static RssSubscribedChannel get(ContentResolver resolver,
            String channelName) {
        return DataEntryManager.SubscriptionHelper.get(resolver, channelName);
    }

    /**
     * 获取所有拒绝离线下载的List的个数
     *
     * @param resolver
     * @return
     */
    public static int getRefusedOfflineCount(ContentResolver resolver) {
        return DataEntryManager.SubscriptionHelper.getCount(resolver,
                DataEntryManager.SubscriptionHelper
                        .getWhereStringWithoutSpecial(Subscriptions.OFFLINE
                                + "=0"));
    }

    /**
     * 获取所有已订阅频道的List
     *
     * @param resolver
     * @return
     */
    public static List<String> getChannelsForNameOnly(ContentResolver resolver) {
        List<String> list = new ArrayList<String>();
        Cursor cursor = null;
        try {
            cursor = DataEntryManager.SubscriptionHelper.getCursor(resolver);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int idx = cursor.getColumnIndex(Subscriptions.CHANNEL);
                    if (idx >= 0) {
                        list.add(cursor.getString(idx));
                    }
                }
            }
        } catch (Exception e) {
            Utils.error(RssSubscribedChannel.class, Utils.getStackTrace(e));
        }

        if (cursor != null) {
            cursor.close();
        }
        return list;
    }

    /**
     * 是否需要插入到数据库中
     *
     * @param resolver
     * @return
     */
    public static boolean isAllowToInsert(ContentResolver resolver) {
        int count = RssSubscribedChannel.getSubscribedCount();
        return count < Constants.MAX_SUBSCRIBE_NUM;
    }

    /**
     * 是否需要插入到数据库中
     *
     * @param wantToNum
     * @return
     */
    public static boolean isAllowToInsert(int wantToNum) {
        return wantToNum <= Constants.MAX_SUBSCRIBE_NUM
                + RssSubscribedChannel.getSpecialCount();
    }

    /**
     * 计算七天内访问的次数，且将7天前的记录删除
     *
     * @param resolver
     */
    public void calculateNumberOfVisited(ContentResolver resolver) {
        DataEntryManager.ChannelAccessHelper.increaseAccess(resolver, channel);
    }

    /**
     * 取消该订阅。
     *
     * @return
     */
    public int unsubscribe(ContentResolver resolver) {
        if (DataEntryManager.ArticleHelper.Normal.deleteAll(resolver, channel) > -1) {
            return DataEntryManager.SubscriptionHelper.delete(resolver, _id);
        }

        return -1;
    }

    /**
     * 设置最后一个文章的Id
     *
     * @param resolver
     * @param content_id
     */
    public void setLastContentId(ContentResolver resolver, long content_id) {
        this.last_content_id = content_id;
        ContentValues values = new ContentValues();
        values.put(Subscriptions.LAST_CONTENT_ID, content_id);
        DataEntryManager.SubscriptionHelper.update(resolver, this._id, values);
    }

    /**
     * 设置最后一次刷新的日期
     *
     * @param resolver
     * @param content_id
     */
    public void setLastRefreshTime(ContentResolver resolver, long refresh_time) {
        this.last_refresh_time = refresh_time;
        ContentValues values = new ContentValues();
        values.put(Subscriptions.LAST_REFRESH_TIME, refresh_time);
        DataEntryManager.SubscriptionHelper.update(resolver, this._id, values);
    }

    static void clearCache(ContentResolver resolver) {
        ContentValues values = new ContentValues();
        values.put(Subscriptions.LAST_CONTENT_ID, 0);
        values.put(Subscriptions.NEWEST_IMAGE_CONTENT_ID, 0);
        values.put(Subscriptions.LAST_REFRESH_TIME, 0);
        values.put(Subscriptions.OFFLINE_TIME, 0);
        values.put(Subscriptions.OFFLINE_COUNT, 0);
        DataEntryManager.SubscriptionHelper.update(resolver, null, values);
    }

    /**
     * 设置频道的排序，需要提供左和右面的sort数据
     *
     * @param resolver
     * @param leftSortFloat
     *            0 表示在最顶端
     * @param rightSortFloat
     *            0 表示在最低端
     */
    public void setSortFloat(ContentResolver resolver, SortFloat sortFloat) {
        setSortFloat(resolver, calculateSortFloat(resolver, sortFloat));
    }

    void setSortFloat(ContentResolver resolver, double sortFloat) {
        this.sort_float = sortFloat;
        ContentValues values = new ContentValues();
        values.put(Subscriptions.SORT_FLOAT, sortFloat);
        DataEntryManager.SubscriptionHelper.update(resolver, this._id, values);
    }

    /**
     * 根据sortFloat对象来计算出两者的中间值
     *
     * @param resolver
     * @param sortFloat
     * @return
     */
    public double calculateSortFloat(ContentResolver resolver,
            SortFloat sortFloat) {
        return (sortFloat.right + sortFloat.left) / 2;
    }

    /**
     * 更新NewestImageOfContentId字段
     *
     * @param resolver
     * @param content_id
     */
    public void updateNewestImageOfContentId(ContentResolver resolver,
            long content_id) {
        if (this.newest_image_content_id == content_id) {
            return;
        }
        this.newest_image_content_id = content_id;
        ContentValues values = new ContentValues();
        values.put(Subscriptions.NEWEST_IMAGE_CONTENT_ID, content_id);
        DataEntryManager.SubscriptionHelper.update(resolver, this._id, values);
    }

    /**
     * 设置是否允许离线下载
     *
     * @param isChecked
     */
    public void setOffline(ContentResolver resolver, boolean isChecked) {
        if (this.offline == isChecked) {
            return;
        }
        this.offline = isChecked;
        ContentValues values = new ContentValues();
        values.put(Subscriptions.OFFLINE, isChecked);
        DataEntryManager.SubscriptionHelper.update(resolver, this._id, values);
    }

    /**
     * 设置离线下载的时间
     *
     * @param resolver
     */
    public void setOfflineTime(ContentResolver resolver, long offlineTime) {
        this.offline_time = offlineTime;
        ContentValues values = new ContentValues();
        values.put(Subscriptions.OFFLINE_TIME, offlineTime);
        DataEntryManager.SubscriptionHelper.update(resolver, this._id, values);
    }

    /**
     * 设置离线下载的文章数
     *
     * @param resolver
     */
    public void setOfflineCount(ContentResolver resolver, int count) {
        this.offline_count = count;
        ContentValues values = new ContentValues();
        values.put(Subscriptions.OFFLINE_COUNT, count);
        DataEntryManager.SubscriptionHelper.update(resolver, this._id, values);
    }

    private static List<RssSubscribedChannel> getForOffline(
            ContentResolver resolver, int offline) {
        List<RssSubscribedChannel> list = new ArrayList<RssSubscribedChannel>();
        Cursor cursor = null;
        try {
            cursor = DataEntryManager.SubscriptionHelper
                    .getCursorWithoutSpecial(resolver, Subscriptions.OFFLINE
                            + "=" + offline);
            while (cursor != null && cursor.moveToNext()) {
                list.add(RssSubscribedChannel.inject(cursor));
            }

            cursor.close();
        } catch (Exception e) {
            Utils.error(RssSubscribedChannel.class, Utils.getStackTrace(e));
        }

        if (cursor != null) {
            cursor.close();
        }
        return list;
    }

    private static Map<String, RssSubscribedChannel> getDefaultSubscribedMap() {
        final double step = 0.0001;
        double sort = SortFloat.MIN_ACTIVATE;
        Map<String, RssSubscribedChannel> list = new HashMap<String, RssSubscribedChannel>();
        RssSubscribedChannel sub = null;

        long time = System.currentTimeMillis();

        sort += step;
        sub = new RssSubscribedChannel();
        sub.channel = RssSubscribedChannel.RANDOM_READ;
        sub.title = "摇一摇";
        sub.sub_date = FIXED_SUB_DATE; // 不可删除的频道
        sub.number_of_visited = SPECIAL_NUMBER_OF_VISITED; // 特殊的频道
        sub.sort_float = sort;
        list.put(sub.channel, sub);

        sort += step;
        sub = new RssSubscribedChannel();
        sub.channel = RssSubscribedChannel.OFFLINE_DOWNLOAD;
        sub.title = "离线下载";
        sub.sub_date = FIXED_SUB_DATE;
        sub.number_of_visited = SPECIAL_NUMBER_OF_VISITED; // 特殊的频道
        sub.sort_float = sort;
        list.put(sub.channel, sub);

        sort += step;
        sub = new RssSubscribedChannel();
        sub.channel = "sinanewschina";
        sub.title = "时事新闻";
        sub.sub_date = time;
        sub.sort_float = sort;
        list.put(sub.channel, sub);

        sort += step;
        sub = new RssSubscribedChannel();
        sub.channel = "caijing";
        sub.title = "财经频道";
        sub.sub_date = time;
        sub.sort_float = sort;
        list.put(sub.channel, sub);

        sort += step;
        sub = new RssSubscribedChannel();
        sub.channel = "rednet";
        sub.title = "娱乐新闻";
        sub.sub_date = time;
        sub.sort_float = sort;
        list.put(sub.channel, sub);

        sort += step;
        sub = new RssSubscribedChannel();
        sub.channel = "163sportsgj";
        sub.title = "2012欧洲杯";
        sub.sub_date = time;
        sub.sort_float = sort;
        list.put(sub.channel, sub);

        sort += step;
        sub = new RssSubscribedChannel();
        sub.channel = "sinamarry";
        sub.title = "情爱频道";
        sub.sub_date = time;
        sub.sort_float = sort;
        list.put(sub.channel, sub);

        sort += step;
        sub = new RssSubscribedChannel();
        sub.channel = "pic_beauty";
        sub.title = "美女";
        sub.sub_date = time;
        sub.sort_float = sort;
        list.put(sub.channel, sub);

        sort += step;
        sub = new RssSubscribedChannel();
        sub.channel = "moko";
        sub.title = "美空";
        sub.sub_date = time;
        sub.sort_float = sort;
        list.put(sub.channel, sub);

        sort += step;
        sub = new RssSubscribedChannel();
        sub.channel = "ibabe";
        sub.title = "爱正妹";
        sub.sub_date = time;
        sub.sort_float = sort;
        list.put(sub.channel, sub);

        sort += step;
        sub = new RssSubscribedChannel();
        sub.channel = "sinagossip";
        sub.title = "明星八卦";
        sub.sub_date = time;
        sub.sort_float = sort;
        list.put(sub.channel, sub);

        sort += step;
        sub = new RssSubscribedChannel();
        sub.channel = "sinaladies";
        sub.title = "女性频道";
        sub.sub_date = time;
        sub.sort_float = sort;
        list.put(sub.channel, sub);

        sort += step;
        sub = new RssSubscribedChannel();
        sub.channel = "chinanewssports";
        sub.title = "体育新闻";
        sub.sub_date = time;
        sub.sort_float = sort;
        list.put(sub.channel, sub);

        sort += step;
        sub = new RssSubscribedChannel();
        sub.channel = "sinanewsworld";
        sub.title = "国际新闻";
        sub.sub_date = time;
        sub.sort_float = sort;
        list.put(sub.channel, sub);

        sort += step;
        sub = new RssSubscribedChannel();
        sub.channel = "sohuit";
        sub.title = "科技新闻";
        sub.sub_date = time;
        sub.sort_float = sort;
        list.put(sub.channel, sub);

        sort += step;
        sub = new RssSubscribedChannel();
        sub.channel = "meizitu";
        sub.title = "妹子图";
        sub.sub_date = time;
        sub.sort_float = sort;
        list.put(sub.channel, sub);

        sort += step;
        sub = new RssSubscribedChannel();
        sub.channel = "ifengnews";
        sub.title = "凤凰资讯";
        sub.sub_date = time;
        sub.sort_float = sort;
        list.put(sub.channel, sub);

        sort += step;
        sub = new RssSubscribedChannel();
        sub.channel = "163tech";
        sub.title = "互联网新闻";
        sub.sub_date = time;
        sub.sort_float = sort;
        list.put(sub.channel, sub);

        sort += step;
        sub = new RssSubscribedChannel();
        sub.channel = RssSubscribedChannel.ADD_SUBSCRIBE;
        sub.title = "分类阅读";
        sub.sub_date = FIXED_SUB_DATE; // 不可删除的频道
        sub.number_of_visited = SPECIAL_NUMBER_OF_VISITED; // 特殊的频道
        sub.sort_float = sort;
        list.put(sub.channel, sub);

        return list;
    }

    /**
     * 取消指定频道的订阅
     *
     * @param resolver
     * @param channel
     * @return
     */
    public static int deleteByChannel(ContentResolver resolver, String channel) {
        if (DataEntryManager.ArticleHelper.Normal.deleteAll(resolver, channel) > -1) {
            return DataEntryManager.SubscriptionHelper
                    .delete(resolver, channel);
        }

        return -1;
    }
}
