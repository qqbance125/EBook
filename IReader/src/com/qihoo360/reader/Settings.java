/**
 *
 */

package com.qihoo360.reader;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

/**
 * Reader的设置中心。
 *
 * @author Jiongxuan Zhang
 */
public class Settings {
    public static final String SPKEY_NEW_CHANNEL_ADD_POSTION = "new_add_channel_position";
    public static final String SPKEY_ENABLE_AUTOUPDATE_KEY = "enable_update_in_wifi";
    public static final String SPKEY_OFFLINE_DONWLOAD_TEXT_ONLY_KEY = "enable_offline_download_text";
    private static final String SPKEY_NIGHT_MODE = "reader_night_mode";
    private static final String SPKEY_IMAGEGONE_MODE = "reader_image_mode";
    private static final String SPKEY_BRIGHTNESS = "brightness";
    private static final String SPKEY_ENABLE_PAGE_KEY = "enable_page";
    private static final String SPKEY_LAST_CHANNEL_UPDATED_DATE = "last_channel_updated_date";
    private static final String SPKEY_LAST_ILIKE_CATEGORY_LIST_UPDATED_DATE = "last_ilike_category_list_updated_date";
    private static final String SPKEY_LAST_SORTED_CHANNEL_UPDATED_DATE = "last_sorted_channel_updated_date";
    private static final String SPKEY_LAST_RESORT_DATE = "last_resort_time";
    private static final String SPKEY_LAST_RANDOM_ID = "last_random_id";
    private static final String SPKEY_PUSHED_TIME = "pushed_time";
    private static final String SPKEY_SCHEDULED_PUSH_TIME = "scheduled_push_time";
    private static final String SPKEY_PUSHED_CHANNEL = "pushed_channel";
    private static final String SPKEY_PUSHED_CONTENT_ID = "pushed_content_id";
    private static final String SPKEY_LAST_CHECK_IMAGE_CACHE_DATE = "last_check_image_cache_date";
    public static final String SPKEY_ENABLE_OFFLINE_DOWNOAD = "enable_offline_downoad";
    // private static final String SPKEY_ARTICLE_BAR_STATE =
    // "article_bar_state";
    private static final String SPKEY_FULL_SCREEN_STATE = "reader_full_screen_state";
    private static final String SPKEY_READER_VIEW_MODE = "reader_view_mode";
    private static final String SPKEY_APP_VERSION = "build_id";
    private static final String SPKEY_ILIKE_VERSION = "ilike_build_id";
    private static final String SPKEY_READER_DIR = "reader_dir";
    private static final String SPKEY_UNIQUE_DEVICES = "unique_devices";
    private static final String SPKEY_IS_USED_ILIKE = "is_used_ilike";

    public static final String READER_PREFERENCE = "reader_preference";

    /**
     * 用户是否已经接受许可协议
     *
     * @param context
     * @return
     */
    public static boolean isAllowStatement(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                "qihoo_browser_guide", Context.MODE_PRIVATE);
        if (preferences != null) {
            return !preferences.getBoolean("showUserAgreement", true);
        }
        return true;
    }

    /**
     * 设置夜间模式，并立即生效
     *
     * @param context
     * @param isNightMode
     */
    public static void setNightMode(Context context, boolean isNightMode) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putBoolean(SPKEY_NIGHT_MODE, isNightMode);
        editor.commit();

        // 最后，为了使结果立即生效，我们需要把亮度做些调整
        if (context != null) {
            Intent intent = new Intent(
                    Constants.READER_BROADCAST_SWITCH_THE_DAY_AND_NIGHT_MODE);
            intent.putExtra("mode", isNightMode);
            context.sendBroadcast(intent);
            // if (context instanceof Activity) {
            // SystemUtils.setNightModeBrightness((Activity) context,
            // isNightMode);
            // }
        }
    }

    /**
     * 获取是否为夜间模式
     *
     * @return
     */
    public static Boolean isNightMode() {
        return getSharedPreferences().getBoolean(SPKEY_NIGHT_MODE, false);
    }

    private static Float sBrightness = null;

    /**
     * 设置屏幕亮度
     *
     * @param activity
     * @param isNightMode
     */
    public static void setBrightness(float brightness) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putFloat(SPKEY_BRIGHTNESS, sBrightness);
        editor.commit();
        sBrightness = brightness;
    }

    /**
     * 设置屏幕亮度
     *
     * @param activity
     */
    public static void setBrightness(Activity activity) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putFloat(SPKEY_BRIGHTNESS,
                activity.getWindow().getAttributes().screenBrightness);
        editor.commit();
        sBrightness = activity.getWindow().getAttributes().screenBrightness;
    }

    /**
     * 从设置中读取屏幕亮度
     *
     * @return
     */
    public static Float getBrightness() {
        if (sBrightness == null) {
            sBrightness = getSharedPreferences().getFloat(SPKEY_BRIGHTNESS,
                    0.8f);
        }
        return sBrightness;
    }

    /**
     * 获取是否为翻页模式
     *
     * @return
     */
    public static Boolean isPageMode() {
        return getSharedPreferences().getBoolean(SPKEY_ENABLE_PAGE_KEY, true);
    }

    /**
     * 获取是否 在3G模式下自动更新
     *
     * @return
     */
    public static Boolean is3GModeAutoUpdate() {
        if (getSharedPreferences()
                .getBoolean(SPKEY_ENABLE_AUTOUPDATE_KEY, true)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * 是否为无图模式
     *
     * @return
     */
    public static Boolean isNoPicMode() {
        return getSharedPreferences().getBoolean(SPKEY_IMAGEGONE_MODE, false);
    }

    /**
     * 设置保存无图模式
     *
     * @return
     */
    public static void setNoPicMode(Context context, boolean wantToMode) {
        if (isNoPicMode() == wantToMode)
            return;

        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putBoolean(SPKEY_IMAGEGONE_MODE, wantToMode);
        editor.commit();
        // 发送广播，立即生效
        if (context != null) {
            Intent intent = new Intent(
                    Constants.READER_BROADCAST_SWITCH_WHETHER_THE_IMAGE_MODE);
            intent.putExtra("mode", wantToMode);
            context.sendBroadcast(intent);
            Toast.makeText(context,
                    Settings.isNoPicMode() ? "已切换到无图模式" : "已切换到有图模式",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 设置最后一次更新的日期，以毫秒数表示
     *
     * @param date
     */
    public static void setLastChannelUpdatedDate(long date) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putLong(SPKEY_LAST_CHANNEL_UPDATED_DATE, date);
        editor.commit();
    }

    /**
     * 获取最后一次更新的时间，以毫秒数表示
     *
     * @param date
     * @return
     */
    public static long getLastChannelUpdatedDate() {
        return getSharedPreferences().getLong(SPKEY_LAST_CHANNEL_UPDATED_DATE,
                0);
    }

    /**
     * 获取最后一次更新我喜欢中，热门收藏列表的时间，以毫秒数表示
     *
     * @param date
     * @return
     */
    public static long getLastIlikeCategoryListUpdatedDate() {
        return getSharedPreferences().getLong(
                SPKEY_LAST_ILIKE_CATEGORY_LIST_UPDATED_DATE, 0);
    }

    /**
     * 设置最后一次更新我喜欢中，热门收藏列表的时间，以毫秒数表示
     *
     * @param date
     */
    public static void setLastIlikeCategoryListUpdatedDate(long date) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putLong(SPKEY_LAST_ILIKE_CATEGORY_LIST_UPDATED_DATE, date);
        editor.commit();
    }

    /**
     * 设置最后一次更新频道推荐排序的日期，以毫秒数表示
     *
     * @param date
     */
    public static void setLastSortedChannelUpdatedDate(long date) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putLong(SPKEY_LAST_SORTED_CHANNEL_UPDATED_DATE, date);
        editor.commit();
    }

    /**
     * 获取最后一次更新频道推荐排序的时间，以毫秒数表示
     *
     * @param date
     * @return
     */
    public static long getLastSortedChannelUpdatedDate() {
        return getSharedPreferences().getLong(
                SPKEY_LAST_SORTED_CHANNEL_UPDATED_DATE, 0);
    }

    public static void setLastResortDate(int date) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putInt(SPKEY_LAST_RESORT_DATE, date);
        editor.commit();
    }

    public static int getLastResortDate() {
        return getSharedPreferences().getInt(SPKEY_LAST_RESORT_DATE, 0);
    }

    /**
     * 设置最后一次摇一摇产生的Id
     *
     * @param date
     */
    public static void setLastRandomId(int id) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putInt(SPKEY_LAST_RANDOM_ID, id);
        editor.commit();
    }

    /**
     * 获取最后一次更新频道推荐排序的时间，以毫秒数表示
     *
     * @param date
     * @return
     */
    public static int getLastRandomId() {
        return getSharedPreferences().getInt(SPKEY_LAST_RANDOM_ID, 0);
    }

    /**
     * 设置Push日期为当前时间
     *
     * @param date
     */
    public static void setPushedTime() {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putLong(SPKEY_PUSHED_TIME, System.currentTimeMillis());
        editor.commit();
    }

    /**
     * 获取Push的日期，以天为单位
     *
     * @return
     */
    public static long getPushedTime() {
        return getSharedPreferences().getLong(SPKEY_PUSHED_TIME, 0);
    }

    /**
     * 设置已定下计划的
     *
     * @param date
     */
    public static void setSchedulePushTime(long scheduled) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putLong(SPKEY_SCHEDULED_PUSH_TIME, scheduled);
        editor.commit();
    }

    /**
     * 获取Push的日期，以天为单位
     *
     * @return
     */
    public static long getSchedulePushTime() {
        return getSharedPreferences().getLong(SPKEY_SCHEDULED_PUSH_TIME, 0);
    }

    /**
     * 设置最后一次Push的频道名
     *
     * @param date
     */
    public static void setPushedChannel(String channelName) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putString(SPKEY_PUSHED_CHANNEL, channelName);
        editor.commit();
    }

    /**
     * 获取最后一次Push的频道名
     *
     * @return
     */
    public static String getPushedChannel() {
        return getSharedPreferences().getString(SPKEY_PUSHED_CHANNEL, "");
    }

    /**
     * 设置最后一次Push的频道id
     *
     * @param date
     */
    public static void setPushedContentId(long contentId) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putLong(SPKEY_PUSHED_CONTENT_ID, contentId);
        editor.commit();
    }

    /**
     * 获取最后一次Push的频道id
     *
     * @return
     */
    public static long getPushedContentId() {
        return getSharedPreferences().getLong(SPKEY_PUSHED_CONTENT_ID, 0);
    }

    private static SharedPreferences spinstance = null; // 不要直接使用

    public static SharedPreferences getSharedPreferences() {
        if (spinstance == null) {
            spinstance = ReaderApplication.getContext().getSharedPreferences(
                    READER_PREFERENCE, Context.MODE_PRIVATE);
        }

        return spinstance;
    }

    public static void setLastCheckImageCacheDate(long date) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putLong(SPKEY_LAST_CHECK_IMAGE_CACHE_DATE, date);
        editor.commit();
    }

    public static long getLastCheckImageCacheDate() {
        return getSharedPreferences().getLong(
                SPKEY_LAST_CHECK_IMAGE_CACHE_DATE, 0);
    }

    private static boolean sIsEnableOfflineWithoutWifi = false;

    public static void setEnableOfflineWithoutWifi(boolean isEnabled) {
        sIsEnableOfflineWithoutWifi = isEnabled;
    }

    public static boolean isEnableOfflineWithoutWifi() {
        return sIsEnableOfflineWithoutWifi;
    }

    public static void setDatabaseInit(boolean init) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putBoolean(DATABASE_INITIALIZED, init);
        editor.commit();
    }

    public static boolean isDatabaseInit() {
        return getSharedPreferences().getBoolean(DATABASE_INITIALIZED, false);
    }

    /**
     * 获取当前客户端的BuildId
     *
     * @param mContext
     * @return
     */
    public static int getCurrentAppVersion() {
        return Constants.APP_VERSION;
    }

    /**
     * 更新BuildId到最新版本
     *
     * @param version
     */
    public static void setAppVersionInSp() {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putInt(SPKEY_APP_VERSION, getCurrentAppVersion());
        editor.commit();
    }

    /**
     * 获取BuildId 0 - 1.6.0 或者全新安装 1 - 1.7.0 2 - 1.7.1
     *
     * @return
     */
    public static int getAppVersionInSp() {
        return getSharedPreferences().getInt(SPKEY_APP_VERSION, 0);
    }

    /**
     * 获取当前客户端的BuildId
     *
     * @param mContext
     * @return
     */
    public static int getCurrentIlikeVersion() {
        return Constants.ILIKE_VERSION;
    }

    /**
     * 更新BuildId到最新版本
     *
     * @param version
     */
    public static void setIlikeVersionInSp() {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putInt(SPKEY_ILIKE_VERSION, getCurrentIlikeVersion());
        editor.commit();
    }

    /**
     * 获取BuildId 0 - 1.6.0 或者全新安装 1 - 1.7.0 2 - 1.7.1
     *
     * @return
     */
    public static int getIlikeVersionInSp() {
        return getSharedPreferences().getInt(SPKEY_ILIKE_VERSION, 0);
    }

    /**
     * 是否已经用过ilike？
     *
     * @param version
     */
    public static void setIsUsedILike(boolean used) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putBoolean(SPKEY_IS_USED_ILIKE, used);
        editor.commit();
    }

    /**
     * 是否已经用过ilike？
     *
     * @return
     */
    public static boolean isUsedILike() {
        return getSharedPreferences().getBoolean(SPKEY_IS_USED_ILIKE, false);
    }

    public static void setReaderDir(String dir) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putString(SPKEY_READER_DIR, dir);
        editor.commit();
    }

    public static String getReaderDir() {
        return getSharedPreferences().getString(SPKEY_READER_DIR, "");
    }

    public static void setUniqueDevice(String uniqueText) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putString(SPKEY_UNIQUE_DEVICES, uniqueText);
        editor.commit();
    }

    public static String getUniqueDevice() {
        return getSharedPreferences().getString(SPKEY_UNIQUE_DEVICES, "");
    }

    public static boolean getAddChannelToTail() {
        String position = getSharedPreferences().getString(
                SPKEY_NEW_CHANNEL_ADD_POSTION, "after");
        if (position.equals("after")) {
            return true;
        } else {
            return false;
        }
    }

    // public static boolean getArticleDetailBarVisibilityState() {
    // return getSharedPreferences().getBoolean(SPKEY_ARTICLE_BAR_STATE, false);
    // }
    //
    // public static void setArticleDetailBarVisibilityState(boolean
    // isVisibility) {
    // getSharedPreferences().edit().putBoolean(SPKEY_ARTICLE_BAR_STATE,
    // isVisibility).commit();
    // }

    public static boolean isFullScreen() {
        return getSharedPreferences()
                .getBoolean(SPKEY_FULL_SCREEN_STATE, false);
    }

    public static void setFullScreen(boolean isFullScreen, boolean shouldSend) {
        getSharedPreferences().edit()
                .putBoolean(SPKEY_FULL_SCREEN_STATE, isFullScreen).commit();
        if (shouldSend) {
            Context context = ReaderApplication.getContext();
            if (context != null) {
                Intent intent = new Intent(
                        Constants.BROADCAST_SWITCH_FULL_SCREEN_MODE);
                intent.putExtra("mode", isFullScreen);
                context.sendBroadcast(intent);
            }
        }
    }

    public static int getReaderViewMode() {
        return getSharedPreferences().getInt(SPKEY_READER_VIEW_MODE,
                Constants.MODE_GRID);
    }

    public static void setReaderViewMode(int mode) {
        if (mode != Constants.MODE_GRID && mode != Constants.MODE_LIST) {
            throw new IllegalArgumentException("mode is not support");
        }
        getSharedPreferences().edit().putInt(SPKEY_READER_VIEW_MODE, mode)
                .commit();
    }

    public static void setBoolean(String key, boolean b) {
        getSharedPreferences().edit().putBoolean(key, b).commit();
    }

    public static void setString(String key, String value) {
        getSharedPreferences().edit().putString(key, value).commit();
    }

    public static final String DATABASE_INITIALIZED = "database_initialized";

    /**
     * 获取是否只离线下载文本
     *
     * @return
     */
    public static boolean getEnableOfflineDonwloadText() {
        return getSharedPreferences().getBoolean(
                SPKEY_OFFLINE_DONWLOAD_TEXT_ONLY_KEY, false);
    }
}
