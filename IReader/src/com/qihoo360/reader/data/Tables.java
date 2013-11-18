
package com.qihoo360.reader.data;

import com.qihoo360.reader.BuildEnv;

import android.net.Uri;
import android.provider.BaseColumns;

public class Tables {
    public static final String AUTHORITY = BuildEnv.AUTHORITY_READER;

    /**
     * 已订阅频道table
     *
     * @author fanguofeng
     */

    public static final class Subscriptions implements BaseColumns {
        private Subscriptions() {
        }

        public static final String TABLE_NAME = "subscriptions";

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri
                .parse("content://" + AUTHORITY + "/subscriptions");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.qihoo.reader.data.subscriptions";

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.qihoo.reader.data.subscriptions";

        /**
         * The default sort order for this table
         * 此表的默认排序顺序
         */
        public static final String DEFAULT_SORT_ORDER = "sub_date ASC";

        public static final String CHANNEL = "channel";  // 频道

        public static final String TITLE = "title"; // 标题

        public static final String PHOTO_URL = "photo_url"; // 网址

        public static final String SUB_DATE = "sub_date"; //  日期

        public static final String NUMBER_OF_VISITED = "number_of_visited"; // 

        public static final String NEWEST_IMAGE_CONTENT_ID = "newest_image_content_id"; // 新闻图片内容id

        public static final String IMAGE_VERSION = "image_version"; // 图片的版本

        public static final String LAST_CONTENT_ID = "last_content_id"; // 最近的内容id

        public static final String LAST_REFRESH_TIME = "last_refresh_time"; // 刷新时间

        public static final String SORT_FLOAT = "sort_float"; // 分类的时间

        public static final String OFFLINE = "offline"; // 离线下载

        public static final String OFFLINE_TIME = "offline_time"; // 离线时间

        public static final String OFFLINE_COUNT = "offline_count"; //  下载的数目
    }

    /**
     * 频道访问记录
     *
     * @author fanguofeng
     */

    public static final class ChannelAccess implements BaseColumns {
        private ChannelAccess() {
        }

        public static final String TABLE_NAME = "channel_access";

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri
                .parse("content://" + AUTHORITY + "/channel_access");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.qihoo.reader.data.channel_access";

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.qihoo.reader.data.channel_access";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "_id ASC";

        public static final String CHANNEL = "channel";
        /** 日常数目 */
        public static final String DAILY_COUNT = "daily_count";

        public static final String DATE = "date";
    }

    /**
     * 已下载文章table
     *
     * @author fanguofeng
     */
    public static final class Articles implements BaseColumns {
        private Articles() {
        }

        public static final String TABLE_NAME = "articles";

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/articles");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.qihoo.reader.data.articles";

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.qihoo.reader.data.articles";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "_id ASC";

        public static final String CHANNEL = "channel";

        public static final String CONTENT_ID = "content_id";

        public static final String TITLE = "title";

        public static final String DESCRIPTION = "description";

        public static final String COMPRESSED_IMAGE_URL = "compressed_image_url";

        public static final String PUB_DATE = "pub_date";

        public static final String LINK = "link";

        public static final String AUTHOR = "author";

        public static final String READ = "read";

        public static final String STAR = "star";

        public static final String STARDATE = "stardate";

        public static final String ISDOWNLOADED = "isdownloaded";
        /** 脱机 */
        public static final String ISOFFLINED = "isofflined";
    }
}
