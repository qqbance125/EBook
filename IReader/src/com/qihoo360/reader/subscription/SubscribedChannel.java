package com.qihoo360.reader.subscription;

import android.content.ContentResolver;

public abstract class SubscribedChannel {

    /**
     * 对应数据行的ID
     */
    public int _id = 0;
    /**
     * 已订阅的频道名
     */
    public String channel = "";
    /**
     * 已订阅的频道标题
     */
    public String title = "";
    /**
     * 已订阅的频道图片
     */
    public String photo_url = "";
    /**
     * 已订阅的频道图片版本号
     */
    public int image_version = 0;
    /**
     * 订阅时间
     */
    public long sub_date = 0;
    /**
     * 访问次数
     */
    public long number_of_visited = 0;
    /**
     * 当前最新的图片的文章id
     */
    public long newest_image_content_id = 0;
    /**
     * 表示最后一篇文章的id
     */
    public long last_content_id = 0;
    /**
     * 表示最后一次刷新的时间
     */
    public long last_refresh_time = 0;
    /**
     * 表示频道的顺序
     */
    public double sort_float;
    /**
     * 表示是否支持离线
     */
    public boolean offline = true;
    public long offline_time;
    public int offline_count;

    public SubscribedChannel() {
        super();
    }

    /**
     * 获取已订阅的频道
     *
     * @return
     */
    public Channel getChannel() {
        return Channel.get(channel);
    }

    /**
     * 获取最新带图片的文章Id
     *
     * @return
     */
    public long getNewestImageOfContentId() {
        return this.newest_image_content_id;
    }

    /**
     * 获取七天内访问的次数。
     *
     * @return
     */
    public long getNumberOfVisited() {
        return this.number_of_visited;
    }

    /**
     * 计算七天内访问的次数，且将7天前的记录删除
     *
     * @param resolver
     */
    public abstract void calculateNumberOfVisited(ContentResolver resolver);

    /**
     * 取消该订阅。
     *
     * @return
     */
    public abstract int unsubscribe(ContentResolver resolver);

    /**
     * 设置最后一个文章的Id
     *
     * @param resolver
     * @param content_id
     */
    public abstract void setLastContentId(ContentResolver resolver, long content_id);

    /**
     * 获得最后一个文章的Id
     *
     * @return
     */
    public long getLastContentId() {
        return this.last_content_id;
    }

    /**
     * 设置最后一次刷新的日期
     *
     * @param resolver
     * @param content_id
     */
    public abstract void setLastRefreshTime(ContentResolver resolver, long refresh_time);

    /**
     * 获得最后一次刷新的日期
     *
     * @return
     */
    public long getLastRefreshTime() {
        return this.last_refresh_time;
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
    public abstract void setSortFloat(ContentResolver resolver, SortFloat sortFloat);

    /**
     * 根据sortFloat对象来计算出两者的中间值
     *
     * @param resolver
     * @param sortFloat
     * @return
     */
    public abstract double calculateSortFloat(ContentResolver resolver, SortFloat sortFloat);

    /**
     * 更新NewestImageOfContentId字段
     *
     * @param resolver
     * @param content_id
     */
    public abstract void updateNewestImageOfContentId(ContentResolver resolver, long content_id);

    /**
     * 获取频道的排序值
     */
    public double getSortFloat() {
        return sort_float;
    }

    /**
     * 设置是否允许离线下载
     *
     * @param isChecked
     */
    public abstract void setOffline(ContentResolver resolver, boolean isChecked);

    /**
     * 设置离线下载的时间
     *
     * @param resolver
     */
    public abstract void setOfflineTime(ContentResolver resolver, long offlineTime);

    /**
     * 获取离线下载的时间
     */
    public long getOfflineTime() {
        return this.offline_time;
    }

    /**
     * 设置离线下载的文章数
     *
     * @param resolver
     */
    public abstract void setOfflineCount(ContentResolver resolver, int count);

    /**
     * 获取离线下载的文章数
     */
    public long getOfflineCount() {
        return this.offline_count;
    }


    /**
     * 用来记录运算Sort值的左边和右边的量
     *
     * @author Jiongxuan Zhang
     */
    public static class SortFloat {
        public static final double MAX = 10000000.0;
        public static final double MIN = 0.0;

        public static final double MAX_ACTIVATE = MAX * 0.8;
        public static final double MIN_ACTIVATE = MAX * 0.2;

        public double left;
        public double right;
    }
}