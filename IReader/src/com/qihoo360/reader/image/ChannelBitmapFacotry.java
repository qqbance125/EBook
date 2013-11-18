
package com.qihoo360.reader.image;

import com.qihoo360.reader.Constants;
import com.qihoo360.reader.ServerUris;
import com.qihoo360.reader.subscription.Channel;
import com.qihoo360.reader.subscription.SubscribedChannel;
import com.qihoo360.reader.support.AnimationHelper;
import com.qihoo360.reader.support.ArithmeticUtils;
import com.qihoo360.reader.support.FileUtils;
import com.qihoo360.reader.support.Utils;
import com.qihoo360.reader.ui.CommonUtil;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;


public class ChannelBitmapFacotry extends BitmapFactoryBase{
    public final int RES_DEFAULT_CHANNEL_COVER_VERSION = 3;
    public final String TAG = "ChannelBitmapFacotry";
    public static final String IMAGE_VERSION_PREFIX = "&image_version:";
    private static ChannelBitmapFacotry mInstance = null;
    public static ChannelBitmapFacotry getInstance(Context context) {
        if(mInstance == null) {
            mInstance = new ChannelBitmapFacotry(context);
        }
        return mInstance;
    }

    private ChannelBitmapFacotry(Context context) {
        super(context);
    }

    @Override
    protected void ensureImageDir() {
        super.ensureImageDir();

        File dir = new File(Constants.LOCAL_PATH_IMAGES + "channel/");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
            FileUtils.createNoMediaFileIfPathExists(Constants.LOCAL_PATH_IMAGES + "channel/");
        } catch (IOException e) {
            Utils.error(getClass(), Utils.getStackTrace(e));
        }
    }

    @Override
    public String getFilePath(String url) {
        return Constants.LOCAL_PATH_IMAGES + "channel/" + ArithmeticUtils.getMD5(url) + ".jpg";
    }

    @Override
    protected String processUrl(String url) {
        int index = url.indexOf(IMAGE_VERSION_PREFIX);
        if(index > 0) {
            url = url.substring(0, index);
        }

        return ServerUris.getImage(url);
    }

    @Override
    protected void setBitmap(String url, Bitmap bitmap) {
        if(bitmap != null) {
            Bitmap oldBitmap = bitmap;
            bitmap = CommonUtil.combineBmp(mContext, bitmap);
            oldBitmap.recycle();
            if(bitmap == null) {
                Utils.debug(TAG, "composing cover failed!!!");
                synchronized (mRunningRequestMap) {
                    mRunningRequestMap.remove(url);
                }

                return;
            }
        }

        super.setBitmap(url, bitmap);
    };




    public static class SetDefaultChannelCoverResult {
        public Bitmap mCoverBitmap = null;
        public boolean mNeedDownload = true;
    }

    public boolean setDefaultChannelCover(Context context, Channel channel, ImageView view) {
        int imageVersion = 0;
        if (!TextUtils.isEmpty(channel.imageversion)) {
            try {
                imageVersion = Integer.valueOf(channel.imageversion);
            } catch (Exception e) {
                Utils.error(getClass(), Utils.getStackTrace(e));
            }
        }
        return setDefaultChannelCover(context, channel.image, imageVersion, channel.channel, view).mNeedDownload;
    }

    public SetDefaultChannelCoverResult setDefaultChannelCover(Context context,
            SubscribedChannel channel, ImageView view) {
        return setDefaultChannelCover(context, channel.photo_url, channel.image_version,
                channel.channel, view);
    }

    public SetDefaultChannelCoverResult setDefaultChannelCover(Context context, String imageUrl,
            int imageVersion, String channel, ImageView view) {
        SetDefaultChannelCoverResult result = new SetDefaultChannelCoverResult();
        if (channel == null || view == null) {
            return result;
        } else {
            Bitmap bitmap = null;
            String url = null;

            int loadedVersion = imageVersion;
            while (loadedVersion > 0) {
                url = imageUrl + IMAGE_VERSION_PREFIX + loadedVersion;
                final String filePath = getFilePath(url);
                File pf = new File(filePath);
                if (pf.exists()) {
                    bitmap = decodeFile(filePath);
                    if (bitmap != null) {
                        break;
                    }
                }

                loadedVersion--;
            }

            if (loadedVersion < RES_DEFAULT_CHANNEL_COVER_VERSION) {
                Resources res = context.getResources();
                String dName = "rd_" + channel;

                int drawable = res.getIdentifier(dName, "drawable", context.getPackageName());
                if (drawable > 0) {
                    bitmap = BitmapHelper.decodeResource(res, drawable);

                    if (bitmap != null) {
                        loadedVersion = RES_DEFAULT_CHANNEL_COVER_VERSION;
                    }
                }
            }

            if (bitmap != null) {
                Bitmap oldBitmap = bitmap;
                bitmap = CommonUtil.combineBmp(context, bitmap);
                oldBitmap.recycle();
                if (bitmap != null) {
                    view.setImageBitmap(bitmap);
                    if (loadedVersion >= imageVersion) {
                        mBitmapCache.put(url, new SoftReference<Bitmap>(bitmap));
                    }
                }
            }

            result.mCoverBitmap = bitmap;
            result.mNeedDownload = (bitmap == null || (loadedVersion < imageVersion));
            return result;
        }
    }

    public void setRawChannelCover(Context context, String imageUrl, int imageVersion,
            String channel, ImageView view) {
        if (channel != null || view != null) {
            Bitmap bitmap = null;
            String url = null;

            int loadedVersion = imageVersion;
            while (loadedVersion > 0) {
                url = imageUrl + IMAGE_VERSION_PREFIX + loadedVersion;
                final String filePath = getFilePath(url);
                File pf = new File(filePath);
                if (pf.exists()) {
                    bitmap = decodeFile(filePath);
                    if (bitmap != null) {
                        break;
                    }
                }

                loadedVersion--;
            }

            if (loadedVersion < RES_DEFAULT_CHANNEL_COVER_VERSION) {
                Resources res = context.getResources();
                String dName = "rd_" + channel;

                int drawable = res.getIdentifier(dName, "drawable", context.getPackageName());
                if (drawable > 0) {
                    bitmap = BitmapHelper.decodeResource(res, drawable);
                }
            }

            if (bitmap != null) {
                view.setImageBitmap(bitmap);
            }
        }
    }
}
