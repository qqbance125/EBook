/**
 *
 */
package com.qihoo360.reader.support;

import com.qihoo360.reader.ReaderApplication;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.CharArrayBuffer;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.text.TextUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.zip.GZIPInputStream;

/**
 * 有关网络的工具
 *
 * @author Jiongxuan Zhang
 *
 */
public final class NetUtils {

    /**
     * 如果当前是WAP连接，则给它返回个代理
     *
     * @param context
     * @return
     */
    public static String checkNetInfo(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo == null) {
            return "";
        }

        int net_type = networkInfo.getType();
        if (net_type == ConnectivityManager.TYPE_WIFI) {
            return "";
        }

        if (net_type == ConnectivityManager.TYPE_MOBILE) {
            String apn = networkInfo.getExtraInfo();
            if (apn != null) {
                apn = apn.toLowerCase();

                if (apn.indexOf("cmwap") != -1
                        || apn.indexOf("3gwap") != -1
                        || apn.indexOf("uniwap") != -1) {
                    return "10.0.0.172";
                } else if (apn.indexOf("ctwap") != -1) {
                    return "10.0.0.200";
                }
            }
        }

        return "";
    }

    /**
     * 将entity转换成String
     *
     * @param entity
     * @param defaultCharset
     * @return
     * @throws IOException
     * @throws ParseException
     */
    public static String toString(
            final HttpEntity entity, final String defaultCharset) throws IOException, ParseException {
        if (entity == null) {
            throw new IllegalArgumentException("HTTP entity may not be null");
        }
        InputStream instream = entity.getContent();
        if (instream == null) {
            return "";
        }
        if (entity.getContentLength() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("HTTP entity too large to be buffered in memory");
        }
        int i = (int)entity.getContentLength();
        if (i < 0) {
            i = 4096;
        }
        String charset = EntityUtils.getContentCharSet(entity);
        if (charset == null) {
            charset = defaultCharset;
        }
        if (charset == null) {
            charset = HTTP.DEFAULT_CONTENT_CHARSET;
        }

        // 看看是不是需要解压缩
        Header header = entity.getContentEncoding();
        if (header != null) {
            String value = header.getValue();
            if (TextUtils.isEmpty(value) == false && value.contains("gzip")) {
                instream= new GZIPInputStream(instream);
            }
        }
        Reader reader = new InputStreamReader(instream, charset);
        CharArrayBuffer buffer = new CharArrayBuffer(i);
        try {
            char[] tmp = new char[1024];
            int l;
            while((l = reader.read(tmp)) != -1) {
                buffer.append(tmp, 0, l);
            }
        } finally {
            reader.close();
        }
        return buffer.toString();
    }

    /**
     * 将entity转换成String
     * @param entity
     * @return
     * @throws IOException
     * @throws ParseException
     */
    public static String toString(final HttpEntity entity)
        throws IOException, ParseException {
        return toString(entity, null);
    }

    /**
     * 看当前网络是否可用
     *
     * @param mContext
     * @return
     */
    public static boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) ReaderApplication.getContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetInfo != null && activeNetInfo.isConnected();
    }

    /**
     * 获取当前Wifi的连接状态
     *
     * @param c Context
     * @return 是否在线
     */
    public static boolean isWifiConnected() {
        ConnectivityManager conMan = (ConnectivityManager) ReaderApplication.getContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo info = conMan.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (info == null)
            return false;

        State wifi = info.getState();
        return "CONNECTED".equalsIgnoreCase(wifi.toString());

    }

}
