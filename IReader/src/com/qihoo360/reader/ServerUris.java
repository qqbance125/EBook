/**
 *
 */

package com.qihoo360.reader;

import com.qihoo360.reader.support.SystemUtils;
import com.qihoo360.reader.support.UniqueDevice;
import com.qihoo360.reader.support.Utils;

import android.graphics.Point;
import android.text.TextUtils;

/**
 * 用来获取服务器的Uri
 * 
 * @author Jiongxuan Zhang
 */
public class ServerUris {
    public static final String SERVER_ADDR = "http://s.mse.360.cn";

    // private static final String SERVER_ADDR = "http://58.68.227.101:9090";

    private static String sHttpServer = SERVER_ADDR;

    private static String sClient = null;

    private static final String RSS_URL = "%s/reader/%s?ua=%s_%s&%s&h=%s";

    private static final String RSS_VERSION_POSTFIX = "&v=%s";

    private static final String RSS_CONTENT_TITLE = "titlelist";

    private static final String RSS_CONTENT = "contentlist";

    private static final String RSS_CONTENT_ARGUMENTS = "&channel=%s&from=%d&pn=%d&sinceid=%d";

    private static final String RSS_CHANNEL = "channellist";

    private static final String RSS_CHANNEL_ARGUMENTS = "&type=%d";

    private static final String RSS_SORT_CHANNEL = "sortchannel";

    private static final String RSS_PUSH = "push";

    private static final String RSS_PUSH_URI_ARGUMENTS = "&channel=%s&contentid=%d";

    private static final String RSS_CLIENT_FORMAT = "android_%d_%d";

    private static final String RSS_CLIENT_VERSION = "1.5.0.5";

    private static final String IMAGE_PREFIX = "/" + "images/rss_at/";

    /**
     * 设置Http的地址（测试？正式？替换？） 08-23 09:40:13.911: D/ServerUris(7291): getUrl() ->
     * url = http://s.mse.360.cn/reader/contentlist?ua=android_960_540_1.5.0.5&&
     * channel =rednet&from=761388805&pn=1&sinceid=0&h=15
     * a35460b441f26fb7a002fd965be453   ----> 08-23 09:40:13.911: D/FillTask(7291):
     * doInBackground() >> sinceId = 761388805; url =
     * http://s.mse.360.cn/reader/
     * contentlist?ua=android_960_540_1.5.0.5(版本信息，手机屏幕)&&channel
     * =rednet&from=761388805&pn=1&sinceid=0&h=15a35460b441f26fb7a002fd965be453
     * 
     * @param httpServer
     */
    public static void setHttpServer(String httpServer) {
        if (TextUtils.isEmpty(httpServer)) {
            throw new IllegalArgumentException("httpServer is null");
        }

        sHttpServer = httpServer;
    }

    /**
     * 获取文章的详情List的URL
     * 
     * @param channel
     * @param fromId
     * @param count
     * @param sinceId
     * @return
     */
    public static String getContentList(String channel, long fromId, int count, long sinceId) {
        return getContentOrTitleList(RSS_CONTENT, channel, fromId, count, sinceId);
    }

    /**
     * 获取文章的摘要List的URL
     * 
     * @param channel
     * @param fromId
     * @param count
     * @param sinceId
     * @return
     */
    public static String getTitleList(String channel, long fromId, int count, long sinceId) {
        return getContentOrTitleList(RSS_CONTENT_TITLE, channel, fromId, count, sinceId);
    }

    private static String getContentOrTitleList(String prefixUrl, String channel, long fromId,
            int count, long sinceId) {
        String arguments = String.format(RSS_CONTENT_ARGUMENTS, channel, fromId, count, sinceId);

        return getAllUrl(prefixUrl, arguments);
    }

    /**
     * 获取频道列表的URL
     * 
     * @param version
     * @param type
     * @return
     */
    public static String getChannelList(String version, int type) {
        String arguments = "";

        if (TextUtils.isEmpty(version) == false) {
            String versionUrl = String.format(RSS_VERSION_POSTFIX, version);
            arguments += versionUrl;
        }

        if (type != -1) {
            String typeUrl = String.format(RSS_CHANNEL_ARGUMENTS, type);
            arguments += typeUrl;
        }

        return getAllUrl(RSS_CHANNEL, arguments);
    }

    /**
     * 获取Push的URL
     * 
     * @param channel
     * @param contentId
     * @return
     */
    public static String getPush(String channel, long contentId) {
        String arguments = "";

        if (!TextUtils.isEmpty(channel) && contentId != 0) {
            String postfixString = String.format(RSS_PUSH_URI_ARGUMENTS, channel, contentId);
            arguments += postfixString;
        }

        return getAllUrl(RSS_PUSH, arguments);
    }

    /**
     * 获取摇一摇频道排序的URL
     * 
     * @param version
     * @return
     */
    public static String getSortChannel(String version) {
        String arguments = "";

        if (TextUtils.isEmpty(version) == false) {
            String versionUrl = String.format(RSS_VERSION_POSTFIX, version);
            arguments += versionUrl;
        }

        return getAllUrl(RSS_SORT_CHANNEL, arguments);
    }

    /**
     * 获取图片的URL
     * 
     * @param url
     * @return
     */
    public static String getImage(String url) {
        return sHttpServer + IMAGE_PREFIX + url;
    }

    private static String getAllUrl(String prefix, String arguments) {
        String url = String.format(RSS_URL, sHttpServer, prefix, getRssClient(),
                RSS_CLIENT_VERSION, arguments, UniqueDevice.getString());

        Utils.debug(ServerUris.class, "getUrl() -> url = %s", url);

        return url;
    }

    private static String getRssClient() {
        if (sClient == null) {
            Point point = SystemUtils.getDeviceResolution(ReaderApplication.getContext());
            sClient = String.format(RSS_CLIENT_FORMAT, point.y, point.x);
        }

        return sClient;
    }
}
