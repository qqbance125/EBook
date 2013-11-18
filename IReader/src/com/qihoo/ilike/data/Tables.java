package com.qihoo.ilike.data;

import com.qihoo360.reader.BuildEnv;

import android.net.Uri;
import android.provider.BaseColumns;

public class Tables {
    public static final String AUTHORITY = BuildEnv.AUTHORITY_ILIKE;

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
        public static final Uri CONTENT_URI = Uri.parse("content://"
                + AUTHORITY + "/subscriptions");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.qihoo.ilike.data.subscriptions";

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.qihoo.ilike.data.subscriptions";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "sub_date ASC";

        public static final String CATEGORY_SRV_ID = "category_srv_id";

        public static final String TITLE = "title";

        public static final String TYPE = "type";

        public static final String SUB_DATE = "sub_date";

        public static final String NUMBER_OF_VISITED = "number_of_visited";

        public static final String LAST_BOOKMARK_ID = "last_bookmark_id";

        public static final String LAST_REFRESH_TIME = "last_refresh_time";

        public static final String SORT_FLOAT = "sort_float";
    }

    /**
     * 已下载文章table
     *
     * @author fanguofeng
     */
    public static final class Bookmarks implements BaseColumns {
        private Bookmarks() {
        }

        public static final String TABLE_NAME = "bookmarks";

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://"
                + AUTHORITY + "/bookmarks");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.qihoo.ilike.data.bookmarks";

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.qihoo.ilike.data.bookmarks";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "_id ASC";

        public static final String CATEGORY_SRV_ID = "category_srv_id";

        public static final String BOOKMARK_ID = "bookmark_id";

        public static final String TITLE = "title";

        public static final String SNAPSHOT = "snapshot";

        public static final String DESCRIPTION = "description";

        public static final String ALBUM_ID = "album_id";

        public static final String ALBUM_TITLE = "album_title";

        public static final String IMAGES = "images";

        public static final String AUTHOR = "author";

        public static final String AUTHOR_QID = "author_qid";

        public static final String AUTHOR_IMAGE_URL = "author_image_url";

        public static final String I_LIKE = "i_like";

        public static final String ISDOWNLOADED = "isdownloaded";

        public static final String LIKE_COUNT = "like_count";

        public static final String PUB_DATE = "pub_date";

        public static final String READ = "read";

        public static final String SORT = "sort";
    }

    /**
     * 收集过的Url，包括网页，阅读文章，图片等
     *
     * @author fanguofeng
     */
    public static final class LikedUrls implements BaseColumns {
        private LikedUrls() {
        }

        public static final String TABLE_NAME = "likedurls";

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://"
                + AUTHORITY + "/likedurls");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.qihoo.ilike.data.likedurls";

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.qihoo.ilike.data.likedurls";

        public static final String LIKED_URLS = "liked_urls";
    }
}
