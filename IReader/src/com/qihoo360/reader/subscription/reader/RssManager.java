/**
 *
 */

package com.qihoo360.reader.subscription.reader;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.Context;
import android.text.TextUtils;

import com.qihoo360.reader.Constants;
import com.qihoo360.reader.R;
import com.qihoo360.reader.ReaderApplication;
import com.qihoo360.reader.ServerUris;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.data.DataEntryManager;
import com.qihoo360.reader.image.ImageUtils;
import com.qihoo360.reader.json.JsonGetterBase;
import com.qihoo360.reader.json.JsonUtils;
import com.qihoo360.reader.listener.OnDownloadIndexResultListener;
import com.qihoo360.reader.offline.OfflineNotificationBar;
import com.qihoo360.reader.offline.OfflineQueue;
import com.qihoo360.reader.subscription.Category;
import com.qihoo360.reader.subscription.Index;
import com.qihoo360.reader.support.AsyncTask;
import com.qihoo360.reader.support.FileUtils;
import com.qihoo360.reader.support.NetUtils;
import com.qihoo360.reader.support.Utils;

/**
 * 数据层的管理中心
 *
 * @author Jiongxuan Zhang
 */
public class RssManager {
    
    public static final String LASTEST_VERSION_JSON_TEXT = "{\"response\":3}";

    public final static int UPDATED = 1;
    public final static int FAILED = 2;
    public final static int ALREADY_LASTEST_VERSION = 3;
    // index file name
    private static final String INDEX_FILE_NAME = "c5.dat";

    private static boolean isUpdated = false;

    private static Index sIndex;// 主页的列表

    private static JsonGetterBase sJsonGetter;

    static Map<String, RssChannel> mChannelMap;

    static class ManagerJson extends JsonGetterBase {

        @Override
        public Object parse(String jsonString) {

            Index index = new Index();
            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                index.response = jsonObject.getInt("response");
                index.version = jsonObject.getString("version"); 
                // index.category [start] -> category
                JSONArray categoriesArray = jsonObject.getJSONArray("category");
                List<Category> categories = new ArrayList<Category>();
                for (int i = 0; i < categoriesArray.length(); i++) {
                    Category category = new Category();
                    JSONObject categoryJson = (JSONObject) categoriesArray
                            .opt(i);
                    category.name = categoryJson.getString("name");
                    // index.category[i].entry -> channel
                    JSONArray channelsArray = categoryJson
                            .getJSONArray("entry");
                    List<RssChannel> channels = new ArrayList<RssChannel>();
                    for (int j = 0; j < channelsArray.length(); j++) {
                        RssChannel channel = new RssChannel();
                        JSONObject channelJson = (JSONObject) channelsArray
                                .opt(j);
                        if (channelJson != null) {
                            channel.title = JsonUtils.getJsonString(
                                    channelJson, "title");
                            channel.type = JsonUtils.getJsonInt(channelJson,
                                    "type");
                            channel.desc = JsonUtils.getJsonString(channelJson,
                                    "desc");
                            channel.pinyin = JsonUtils.getJsonString(
                                    channelJson, "pinyin");
                            channel.channel = JsonUtils.getJsonString(
                                    channelJson, "channel");
                            channel.image = JsonUtils.getJsonString(
                                    channelJson, "image");
                            channel.imageversion = JsonUtils.getJsonString(
                                    channelJson, "imageversion");
                            channel.src = JsonUtils.getJsonString(channelJson,
                                    "src");
                            channel.disabled = (JsonUtils.getJsonInt(
                                    channelJson, "disabled") == 1);
                        }
                        channels.add(channel);
                    }
                    category.entry = channels;
                    categories.add(category);
                    // index.category[i].entry
                }
                index.category = categories;
                // index.category [end]
            } catch (JSONException e) {
                Utils.error(getClass(), Utils.getStackTrace(e));
            }
            return index;

        }

    }

    /**
     * 根据版本号判断该不该下载全部的频道列表，并作出选择
     *
     * @param listener
     * @return 有没有必要更新，True表示开始下载并更新
     */
    public static boolean checkIfNeedUpdateAsync(
            final OnDownloadIndexResultListener listener) {
        if (sIndex == null && getIndex() == null) {
            return false;
        }

        /*
         * 何时下载？ 1. 前提：再网，且超过1天 1.1 在蜂窝网络环境下，且确认设置中勾选了允许下载选项 （或者） 1.2 在Wifi环境下
         */
        long lastUpdated = Settings.getLastChannelUpdatedDate();
        if (NetUtils.isNetworkAvailable()
                && System.currentTimeMillis() - lastUpdated > 24 * 60 * 60 * 1000
                && (NetUtils.isWifiConnected() || Settings.is3GModeAutoUpdate())) {
            return forceUpdateAsync(listener);
        }

        return false;
    }

    /**
     * 强制下载全部的频道
     *
     * @param listener
     * @return
     */
    public static boolean forceUpdateAsync(
            final OnDownloadIndexResultListener listener) {
        if (sIndex == null && getIndex() == null
                && NetUtils.isNetworkAvailable()) {
            return false;
        }

        String currentVersion = sIndex.version;
        if (TextUtils.isEmpty(currentVersion) == false) {
            checkVersionAndDownload(currentVersion, listener, false);
            return true;
        }

        return false;
    }

    /**
     * 在程序启动的时候调用，判断数据库是否需要升级，如果需要，则通过resolver进行数据库操作
     *
     * @param contentResolver
     */
    public static void initalizeWhenInstalledOrUpgrade(ContentResolver resolver) {
        int appVersionInSp = Settings.getAppVersionInSp();

        if (appVersionInSp != Constants.APP_VERSION) {
            clearChannelCovers();
        }

        if (appVersionInSp < 5) {
            if (appVersionInSp < 1) {
                DataEntryManager.SubscriptionHelper.delete(resolver, "titan24");
                RssSubscribedChannel.deleteByChannel(resolver,
                        RssSubscribedChannel.SITE_NAVIGATION);
            }
            DataEntryManager.SubscriptionHelper.delete(resolver,
                    RssSubscribedChannel.MY_CONLLECTION);
            RssSubscribedChannel.initDefault(resolver);
        }

        if (!Settings.isDatabaseInit()) {
            RssSubscribedChannel.initDefault(resolver);
        }

        if (RssSubscribedChannel.isNeedRefreshSortFloat()) {
            RssSubscribedChannel.calculateSortFloat(resolver);
        }

        Settings.setAppVersionInSp();
    }

    private static void clearChannelCovers() {
        File f = new File(Constants.LOCAL_PATH_IMAGES + "channel");
        if (f.exists() && f.isDirectory()) {
            File imageFiles[] = f.listFiles();
            for (File imageFile : imageFiles) {
                if (!imageFile.isDirectory()) {
                    imageFile.delete();
                }
            }
        }
    }

    public static byte[] GET_INDEX_LOCK = new byte[0];

    /**
     * 获取Index对象
     *
     * @return
     */
    public static Index getIndex() {
        synchronized (GET_INDEX_LOCK) {
            if (sIndex == null || isUpdated == true) {
                // 我们用的是新版本的客户端吗？是的话，就覆盖掉c5.dat吧！
                if (Utils.isClientUpdated(ReaderApplication.getContext())) {
                    // 拷贝最新的，并重新解析jsonString
                    FileUtils.copyRawToCache(R.raw.c5, INDEX_FILE_NAME);
                }

                initIndex();
                if (sIndex != null) {
                    mChannelMap = initChannelMap();
                    if (mChannelMap == null) {
                        Utils.debug(RssManager.class,
                                "After getContent() -> mChannelMap == null!");
                    }
                } else {
                    Utils.debug(RssManager.class,
                            "After getContent() -> sIndex && mChannelMap == null!");
                }

                isUpdated = false;
            }
            return sIndex;
        }
    }

    /**
     * 清除缓存（异步）
     *
     * @param context
     */
    public static void clearCache(final Context context) {
        if (OfflineQueue.isRunning()) {
            OfflineQueue.stopAll();
        }

        OfflineNotificationBar.get(context).cancelComplete();

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                clearCacheSync(context);
                return null;
            }
        }.execute();
    }

    /**
     * 清除缓存（同步）
     *
     * @param context
     */
    public static void clearCacheSync(Context context) {
        RssSubscribedChannel.clearCache(context.getContentResolver());
        RssArticle.clear(context.getContentResolver());
        ImageUtils.cleanImageCache();

        Utils.debug(RssManager.class, "Clear the reader's cache complete!");
    }

    static void checkVersionAndDownload(final String version,
            final OnDownloadIndexResultListener listener, boolean isSync) {

        AsyncTask<Void, Void, Integer> task = new AsyncTask<Void, Void, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                int result = getJsonGetter().getAndSave(
                        ServerUris.getChannelList(version, 3), INDEX_FILE_NAME);
                switch (result) {
                case UPDATED:
                    isUpdated = true;
                    Settings.setLastChannelUpdatedDate(System
                            .currentTimeMillis());
                    RssSubscribedChannel.update(ReaderApplication.getContext()
                            .getContentResolver());
                    break;

                case ALREADY_LASTEST_VERSION:
                    Settings.setLastChannelUpdatedDate(System
                            .currentTimeMillis());
                    break;
                }

                return result;
            }

            /*
             * (non-Javadoc)
             *
             * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
             */
            @Override
            protected void onPostExecute(Integer result) {
                if (listener != null && !isCancelled()) {
                    switch (result) {
                    case UPDATED:
                        listener.onUpdated();
                        break;

                    case FAILED:
                        listener.onFailure();
                        break;

                    case ALREADY_LASTEST_VERSION:
                        listener.onAlreadyLastestVersion();
                        break;
                    }
                }
            }
        };
        if (isSync) {
            task.doSync();
        } else {
            task.execute();
        }
    }

    static JsonGetterBase getJsonGetter() {
        if (sJsonGetter == null) {
            sJsonGetter = new ManagerJson();
        }

        return sJsonGetter;
    }
    /**
     * 
     *  @return    设定文件 
     */
    private static Map<String, RssChannel> initChannelMap() {
        Map<String, RssChannel> channelMap = new HashMap<String, RssChannel>();
        for (Category category : sIndex.getCategories()) {
            for (RssChannel channel : category.getChannels()) {
                channelMap.put(channel.channel, channel); // 获取相对应的内容
            }
        }

        if (Constants.DEBUG) {
            for (Entry<String, RssChannel> channelEntry : channelMap.entrySet()) {
                Utils.debug("Manager", "channel = %s <-> title = %s",
                        channelEntry.getKey(), channelEntry.getValue().title);
            }
        }
        return channelMap;
    }

    private static void initIndex() {
        String jsonString = FileUtils.getTextFromReaderFile(R.raw.c5, INDEX_FILE_NAME);
        Utils.debug(RssManager.class, "jsonString = %s", jsonString);
        if (TextUtils.isEmpty(jsonString)) {
            Utils.debug(RssManager.class, "jsonString is empty! sIndex = %s",
                    sIndex);
            return;
        }
        sIndex = (Index) getJsonGetter().parse(jsonString);
    }

    private RssManager() {
    }
}
