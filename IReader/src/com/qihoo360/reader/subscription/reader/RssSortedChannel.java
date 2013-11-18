/**
 *
 */
package com.qihoo360.reader.subscription.reader;

import com.qihoo360.reader.R;
import com.qihoo360.reader.ReaderApplication;
import com.qihoo360.reader.ServerUris;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.json.JsonGetterBase;
import com.qihoo360.reader.listener.OnDownloadIndexResultListener;
import com.qihoo360.reader.support.AsyncTask;
import com.qihoo360.reader.support.FileUtils;
import com.qihoo360.reader.support.NetUtils;
import com.qihoo360.reader.support.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 有关已排序过的频道
 *
 * @author Jiongxuan Zhang
 *
 */
public class RssSortedChannel {

    static final String SORTED_FILE_NAME = "c1.dat";

    static RssSortedChannel sSortedChannel;

    static boolean isUpdated = false;

    /**
     * 服务器返回的结果
     */
    public int response;

    public String version;

    List<String> channel;

    private static JsonGetterBase sJsonGetter;

    static class SortedChannelJson extends JsonGetterBase {

        @Override
        public Object parse(String jsonString) {
            RssSortedChannel sortedChannel = new RssSortedChannel();
            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                sortedChannel.response = jsonObject.getInt("response");
                sortedChannel.version = jsonObject.getString("v");
                // sortedChannel.entry [start] -> entry
                JSONArray stringArray = jsonObject.getJSONArray("channel");
                List<String> strings = new ArrayList<String>();
                for (int i = 0; i < stringArray.length(); i++) {
                    strings.add(stringArray.getString(i));
                }
                sortedChannel.channel = strings;
                // sortedChannel.entry [end]
            } catch (JSONException e) {
                Utils.error(getClass(), Utils.getStackTrace(e));
            }
            return sortedChannel;

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
        if (sSortedChannel == null && get() == null) {
            return false;
        }

        /*
         * 何时下载？ 1. 前提：再网，且超过1天 1.1 在蜂窝网络环境下，且确认设置中勾选了允许下载选项 （或者） 1.2 在Wifi环境下
         */
        long lastUpdated = Settings.getLastSortedChannelUpdatedDate();
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
        if (sSortedChannel == null && get() == null
                && NetUtils.isNetworkAvailable()) {
            return false;
        }

        String currentVersion = sSortedChannel.version;
        if (TextUtils.isEmpty(currentVersion) == false) {
            checkVersionAndDownload(currentVersion, listener, false);
            return true;
        }

        return false;
    }

    static void checkVersionAndDownload(final String version,
            final OnDownloadIndexResultListener listener, boolean isSync) {

        AsyncTask<Void, Void, Integer> task = new AsyncTask<Void, Void, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                int result = getJsonGetter().getAndSave(
                        ServerUris.getSortChannel(version), SORTED_FILE_NAME);
                switch (result) {
                case RssManager.UPDATED:
                    isUpdated = true;
                    Settings.setLastSortedChannelUpdatedDate(System
                            .currentTimeMillis());
                    Settings.setLastRandomId(0);
                    break;

                case RssManager.ALREADY_LASTEST_VERSION:
                    Settings.setLastSortedChannelUpdatedDate(System
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
                    case RssManager.UPDATED:
                        listener.onUpdated();
                        break;

                    case RssManager.FAILED:
                        listener.onFailure();
                        break;

                    case RssManager.ALREADY_LASTEST_VERSION:
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
            sJsonGetter = new SortedChannelJson();
        }

        return sJsonGetter;
    }

    /**
     * 下载并获取SortedChannel对象
     *
     * @return
     */
    public static RssSortedChannel get() {
        if (sSortedChannel == null || isUpdated == true) {
            // 我们用的是新版本的客户端吗？是的话，就覆盖掉c1.dat吧！
            if (Utils.isClientUpdated(ReaderApplication.getContext())) {
                // 拷贝最新的，并重新解析jsonString
                FileUtils.copyRawToCache(R.raw.c1, SORTED_FILE_NAME);
            }
            init();

            if (sSortedChannel == null) {
                Utils.debug(RssSortedChannel.class,
                        "After getContent() -> sSortedChannelp == null!");
            }

            isUpdated = false;
        }
        return sSortedChannel;

    }

    private static void init() {
        String jsonString = FileUtils.getTextFromReaderFile(R.raw.c1,
                SORTED_FILE_NAME);
        Utils.debug(RssSortedChannel.class, "jsonString = %s", jsonString);
        if (TextUtils.isEmpty(jsonString)) {
            Utils.debug(RssSortedChannel.class,
                    "jsonString is empty! sSortedChannel = %s", sSortedChannel);
            return;
        }
        sSortedChannel = (RssSortedChannel) getJsonGetter().parse(jsonString);
    }

    /**
     * 获取已经排序过的频道List
     *
     * @return
     */
    public List<String> getList() {
        return channel;
    }
}
