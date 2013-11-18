package com.qihoo360.reader.image;

import com.qihoo360.reader.support.NetUtils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.widget.Toast;

/**
 *	下载的图片
 */
public class ImageDownloadStrategy {
    public static int IMAGE_CONFIG_MAX_HEIGHT = 1280;
    public static int IMAGE_CONFIG_LARGE_WIDTH = 480;
    public static int IMAGE_CONFIG_SMALL_WIDTH = 160;

    public static int IMAGE_CONFIG_SMALL_QUALITY = 61;

    private static ImageDownloadStrategy mInstance = null;

    private Context mContext = null;
    private int mLatestStrategy = 0;


    public static ImageDownloadStrategy getInstance(Context context) {
        if(mInstance == null) {
            mInstance = new ImageDownloadStrategy(context);
        }

        return mInstance;
    }

    private ImageDownloadStrategy(Context context) {
        mContext = context.getApplicationContext();

        Resources res = context.getResources();
        DisplayMetrics ds = res.getDisplayMetrics();
        if (res.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            IMAGE_CONFIG_LARGE_WIDTH = ds.widthPixels;
        } else {
            IMAGE_CONFIG_LARGE_WIDTH = ds.heightPixels;
        }
        IMAGE_CONFIG_SMALL_WIDTH = IMAGE_CONFIG_LARGE_WIDTH * 2 / 3;
    }

    public String getArticleImageConfiguration(final Context context) {
        return "/dr/" + mLatestStrategy + "_" + IMAGE_CONFIG_MAX_HEIGHT + "_"
                + IMAGE_CONFIG_SMALL_QUALITY;
    }

    public String getImageGalleryConfiguration(final Context context) {
        return "/dr/" + IMAGE_CONFIG_LARGE_WIDTH + "_" + IMAGE_CONFIG_MAX_HEIGHT + "_"
                + IMAGE_CONFIG_SMALL_QUALITY;
    }

    public void updateWifiState() {
        if(!NetUtils.isNetworkAvailable()) {
            return;
        }

        boolean wifiAvailable = NetUtils.isWifiConnected();
        int newStrategy = wifiAvailable ? IMAGE_CONFIG_LARGE_WIDTH : IMAGE_CONFIG_SMALL_WIDTH;
        if(newStrategy != mLatestStrategy) {
            if(wifiAvailable) {
                Toast.makeText(mContext, "Wifi下已启用高清图片显示", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(mContext, "移动网络下已启用小图显示", Toast.LENGTH_LONG).show();
            }
            mLatestStrategy = newStrategy;
        }
    }

    public int getScreenWidth() {
        return IMAGE_CONFIG_LARGE_WIDTH;
    }

    public boolean isUsingWifiStrategy() {
        return mLatestStrategy == IMAGE_CONFIG_LARGE_WIDTH;
    }
}
