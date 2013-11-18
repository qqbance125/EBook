/**
 *
 */
package com.qihoo.ilike.manager;

import com.qihoo.ilike.data.DataEntryManager;
import com.qihoo.ilike.http.core.IHttpRequest.HttpRequestStatus;
import com.qihoo.ilike.json.ILikeItUrlPoster;
import com.qihoo.ilike.json.listener.OnILikeItPostResultListener;
import com.qihoo.ilike.subscription.IlikeChannel;
import com.qihoo.ilike.vo.ErrorInfo;
import com.qihoo360.reader.Settings;

import android.content.ContentResolver;

/**
 * @author Jiongxuan Zhang
 *
 */
public class IlikeManager {

    /**
     * 在程序启动的时候调用，判断数据库是否需要升级，如果需要，则通过resolver进行数据库操作
     *
     * @param contentResolver
     */
    public static void initalizeWhenInstalledOrUpgrade(ContentResolver resolver) {
        int ilikeVersionInSp = Settings.getIlikeVersionInSp();

        if (ilikeVersionInSp != Settings.getCurrentIlikeVersion()) {
            // clearChannelCovers();
        }

        if (ilikeVersionInSp < 1) {
            for (IlikeChannel ilikeChannel : IlikeChannel.getChannels()) {
                ilikeChannel.subscribe(resolver);
            }
        }

        Settings.setIlikeVersionInSp();
    }

    public static enum LikeType {
        Unknown, Webpage, Image
    }

    /**
     * 采集某个URL
     *
     * @param url
     * @param title
     * @param type
     * @param listener
     */
    public static boolean likeUrl(final ContentResolver resolver,
            final String url, String title, LikeType type,
            final OnILikeItPostResultListener listener) {
        if (DataEntryManager.addLikedUrl(resolver, url)) {
            ILikeItUrlPoster poster = new ILikeItUrlPoster();
            poster.post(
                    new OnILikeItPostResultListener() {

                        @Override
                        public void onResponseError(ErrorInfo errorInfo) {
                            DataEntryManager.removeLikeUrl(resolver, url);

                            if (listener != null) {
                                listener.onResponseError(errorInfo);
                            }
                        }

                        @Override
                        public void onRequestFailure(
                                HttpRequestStatus errorStatus) {
                            DataEntryManager.removeLikeUrl(resolver, url);

                            if (listener != null) {
                                listener.onRequestFailure(errorStatus);
                            }
                        }

                        @Override
                        public void onComplete() {
                            if (listener != null) {
                                listener.onComplete();
                            }

                        }
                    }, title, type == LikeType.Webpage ? url : "",
                    type == LikeType.Image ? url : "");

            return true;
        } else {
            return false;
        }
    }
}
