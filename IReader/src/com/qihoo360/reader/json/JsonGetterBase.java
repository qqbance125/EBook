/**
 *
 */

package com.qihoo360.reader.json;

import com.qihoo360.reader.Constants;
import com.qihoo360.reader.ReaderApplication;
import com.qihoo360.reader.subscription.reader.RssManager;
import com.qihoo360.reader.support.FileUtils;
import com.qihoo360.reader.support.NetUtils;
import com.qihoo360.reader.support.Utils;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.AbstractHttpParams;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;

import android.text.TextUtils;

import java.io.FileWriter;
import java.io.IOException;

/**
 * 从服务器中获取Json字符串的类，应继承此类以实现parse方法
 *
 * @author Jiongxuan Zhang
 */
public abstract class JsonGetterBase {
    private final static int CONNECT_TIMEOUT = 20000;
    private final static int SOCKET_TIMEOUT = 20000;

    private HttpClient mHttpClient = null;

    private byte[] DOWNLOAD_SYNC = new byte[0];

    /**
     * 从服务器端获取Json。注意该方法是同步的。
     *
     * @param urlString
     * @return
     */
    public String get(String urlString) {
        if (TextUtils.isEmpty(urlString)) {
            throw new IllegalArgumentException("urlString is empty");
        }

        // 防止多次execute同一个请求而出现的异常
        synchronized (DOWNLOAD_SYNC) {
            mHttpClient = new DefaultHttpClient();

            HttpPost httpPost = new HttpPost(urlString);
            httpPost.addHeader("Accept-Encoding", "gzip, deflate");

            AbstractHttpParams httpParameters = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParameters, CONNECT_TIMEOUT);
            HttpConnectionParams.setSoTimeout(httpParameters, SOCKET_TIMEOUT);

            // 保证各种APN都能穿过
            String proxy = NetUtils.checkNetInfo(ReaderApplication.getContext());

            if (!TextUtils.isEmpty(proxy)) {
                ConnRouteParams.setDefaultProxy(httpParameters, new HttpHost(proxy, 80));
            }

            httpPost.setParams(httpParameters);
            long startDownloadTime = 0;
            long endDownloadTime = 0;

            if (Constants.DEBUG) {
                Utils.debug(RssManager.class, "download url = %s", urlString);
                startDownloadTime = System.currentTimeMillis();
                Utils.debug(RssManager.class, "download start. time = %d", startDownloadTime);
            }

            String strJsonText = "";

            try {
                HttpResponse httpResponse = mHttpClient.execute(httpPost);
                if (httpResponse.getStatusLine().getStatusCode() == 200) {
                    strJsonText = NetUtils.toString(httpResponse.getEntity());
                    Utils.debug(RssManager.class, "strJsonText.length() = %d", strJsonText.length());
                }
            } catch (Exception e) {
                Utils.error(getClass(), Utils.getStackTrace(e));
            }

            if (Constants.DEBUG) {
                endDownloadTime = System.currentTimeMillis();
                Utils.debug(RssManager.class, "jsonString = %s", strJsonText);
                Utils.debug(RssManager.class, "download complete. time = %d. spent time = %d (%ds)",
                        endDownloadTime, endDownloadTime - startDownloadTime,
                        (endDownloadTime - startDownloadTime) / 1000);
            }

            // check
            if (TextUtils.isEmpty(strJsonText) || !Utils.checkJsonValidation(strJsonText)) {
                return null;
            }

            // 最后，我们要确保连接已经停止
            stop();
            return strJsonText;
        }
    }

    /**
     * 立即停止获取
     */
    public void stop() {
        if (mHttpClient != null) {
            ClientConnectionManager manager = mHttpClient.getConnectionManager();
            if (manager != null) {
                manager.shutdown();
            }
            mHttpClient = null;
        }
    }

    /**
     * 获取JSON，并将其保存起来。
     * @param getter
     * @param url
     * @param fileName
     * @return
     */
    public Integer getAndSave(final String url, String fileName) {
        if (TextUtils.isEmpty(url)) {
            throw new IllegalArgumentException("url is empty");
        }
        if (TextUtils.isEmpty(fileName)) {
            throw new IllegalArgumentException("fileName is empty");
        }

        String jsonString = get(url);

        if (!TextUtils.isEmpty(jsonString)) {
            if (RssManager.LASTEST_VERSION_JSON_TEXT.equalsIgnoreCase(jsonString)) {
                Utils.debug(RssManager.class, "Already Lastest Version");
                return RssManager.ALREADY_LASTEST_VERSION;
            }

            try {
                FileWriter writer = new FileWriter(FileUtils.getFilePathToSave(
                        ReaderApplication.getContext(), fileName), false);
                writer.write(jsonString);
                writer.flush();
                writer.close();
                return RssManager.UPDATED;
            } catch (IOException e) {
                Utils.error(getClass(), Utils.getStackTrace(e));
            }
        }
        return RssManager.FAILED;
    }

    /**
     * 解析Json，使其成为一个对象
     * @param jsonString
     * @return
     */
    public abstract Object parse(String jsonString);
}
