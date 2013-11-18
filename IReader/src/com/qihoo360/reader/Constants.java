/**
 *
 */

package com.qihoo360.reader;

/**
 * 有关360阅读的常量
 *
 * @author Jiongxuan Zhang
 */
public class Constants {
    /**
     * 是调试模式吗？
     */
    public static final boolean DEBUG = BuildEnv.DEBUG;

    /**
     * 是否关闭摘要下载功能？这是针对正式服务器暂时不支持摘要下载而设计的。
     */
    public static final boolean CLOSE_ABSTRACT_ARTICLES = false;

    /**
     * 最多订阅的个数
     */
    public static final int MAX_SUBSCRIBE_NUM = 36;

    /**
     * 当前客户端的频道版本
     */
    public static final int CURRENT_CHANNEL_VERSION = 20120226;

    public static final String LOCAL_PATH_BASE = "/sdcard/qihoo_browser/";
    public static final String LOCAL_PATH_READER = LOCAL_PATH_BASE + "reader/";
    public static final String LOCAL_PATH_IMAGES = LOCAL_PATH_READER + "images/";

    /**
     * 切换全屏模式的Broadcast
     * */
    public static final String BROADCAST_SWITCH_FULL_SCREEN_MODE = "broadcast_switch_full_screen_mode";
    /**
     * 切换为夜间/白天模式的Broadcast
     */
    public static final String READER_BROADCAST_SWITCH_THE_DAY_AND_NIGHT_MODE = "broadcast_switch_the_day_and_night_mode";

    /**
     * 切换有图/无图模式的Broadcast
     */
    public static final String READER_BROADCAST_SWITCH_WHETHER_THE_IMAGE_MODE = "broadcast_switch_whether_the_image_mode";

    /**
     * 当退出阅读器时会发送的Broadcast
     */
    public static final String READER_BROADCAST_EXITING_READER = "reader_broadcast_existing_all_activities";

    /**
     * 当需要刷新文章列表时会发送的Broadcast
     */
    public static final String READER_BROADCAST_NEED_REFRESH_ARTICLE_LIST = "broadcast_need_refresh_article_list";

    /**
     * 当切换了我喜欢的进入方式会发送的broadcast
     */
    public static final String READER_BROADCAST_SWITCH_ILIKE_MODE = "broadcast_switch_ilike_mode";

    public static final String IMAGE_CHANNEL_SRC_WOXIHUAN = "woxihuan.com";

    /**
     * 应用程序版本号
     */
    public static final int APP_VERSION = 5;

    /**
     * “我喜欢”程序的版本号
     */
    public static final int ILIKE_VERSION = 1;

    /***
     * 订阅中心浏览模式
     */
    public static final int MODE_LIST = 1;
    public static final int MODE_GRID = 2;

    /**
     * 这是两小时的意思
     */
    public static final int TWO_HOURS = 2 * 60 * 60 * 1000;
}
