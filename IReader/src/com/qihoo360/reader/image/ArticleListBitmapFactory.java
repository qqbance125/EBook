package com.qihoo360.reader.image;

import com.qihoo360.reader.Constants;
import com.qihoo360.reader.support.ArithmeticUtils;
import com.qihoo360.reader.support.FileUtils;
import com.qihoo360.reader.support.Utils;
import com.qihoo360.reader.ui.articles.ArticleListView;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.ViewParent;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ImageView;

import java.io.File;

public class ArticleListBitmapFactory extends BitmapFactoryBase {
    public final String TAG = "ArticleListBitmapFactory";

    private static ArticleListBitmapFactory mInstance = null;
    public static ArticleListBitmapFactory getInstance(Context context) {
        if(mInstance == null) {
            mInstance = new ArticleListBitmapFactory(context);
        }

        return mInstance;
    }

    protected ArticleListBitmapFactory(Context context) {
        super(context);
    }

    @Override
    protected void ensureImageDir() {
        super.ensureImageDir();

        FileUtils.ensureImageDir();
    }

    @Override
    protected String getFilePath(String url) {
        if(ImageDownloadStrategy.getInstance(mContext).isUsingWifiStrategy()) {
            return Constants.LOCAL_PATH_IMAGES + "full_size_images/" + ArithmeticUtils.getMD5(url) + ".jpg";
        } else {
            return Constants.LOCAL_PATH_IMAGES + "articles/" + ArithmeticUtils.getMD5(url) + ".jpg";
        }
    }

    @Override
    protected String processUrl(String url) {
        if (!TextUtils.isEmpty(url) && (url.contains("qhimg.com") || url.contains("xihuan"))) {
            int pos = url.lastIndexOf("/");
            if (pos > 0) {
                String config = ImageDownloadStrategy.getInstance(mContext)
                        .getArticleImageConfiguration(mContext);
                url = url.substring(0, pos) + config + url.substring(pos);
            }
        }

        return url;
    }

    @Override
    protected Bitmap loadBitmapFromSDCard(String url)
            throws LoadBitmapFromSDCardFailedException {
        Bitmap bitmap = null;

        // try loading the full size image first
        String filePath = Constants.LOCAL_PATH_IMAGES + "full_size_images/" + ArithmeticUtils.getMD5(url) + ".jpg";
        File pf = new File(filePath);
        if (pf.exists()) {
            Utils.debug(TAG, "load full size image from sdcard: " + url);
            bitmap = decodeFile(filePath);
            if (bitmap == null) {
                throw new LoadBitmapFromSDCardFailedException();
            } else {
                return bitmap;
            }
        }

        filePath = Constants.LOCAL_PATH_IMAGES + "articles/" + ArithmeticUtils.getMD5(url) + ".jpg";
        pf = new File(filePath);
        if (pf.exists()) {
            Utils.debug(TAG, "load from sdcard: " + url);
            bitmap = decodeFile(filePath);
            if (bitmap == null) {
                throw new LoadBitmapFromSDCardFailedException();
            } else {
                return bitmap;
            }
        }

        return bitmap;
    }

    protected Bitmap decodeFile(String filePath) {
        return BitmapHelper.getCompressedBitmap(filePath,
                ImageDownloadStrategy.getInstance(mContext).getScreenWidth());
    }

    protected Bitmap decodeByteArray(byte[] bytes) {
        return BitmapHelper.getCompressedBitmap(bytes,
                ImageDownloadStrategy.getInstance(mContext).getScreenWidth());
    }

    @Override
    protected void setImageBackToView (Bitmap bitmap, ImageView iv) {
        iv.setImageBitmap(bitmap);
        boolean shouldAnimate = true;
        ViewParent parent = iv.getParent();
        while(parent != null) {
            if (parent instanceof ArticleListView) {
                shouldAnimate = !(((ArticleListView) parent).IsAnimatingChildren() || ((ArticleListView) parent)
                        .getScrollState() != OnScrollListener.SCROLL_STATE_IDLE);
                break;
            }
            parent = parent.getParent();
        }

        if(shouldAnimate) {
            Animation anim = new AlphaAnimation(0.0f, 1.0f);
            anim.setDuration(300);
            iv.startAnimation(anim);
        }
    }
}
