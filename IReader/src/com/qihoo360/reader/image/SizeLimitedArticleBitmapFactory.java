package com.qihoo360.reader.image;

import com.qihoo360.reader.R;
import com.qihoo360.reader.subscription.reader.RssChannel;
import com.qihoo360.reader.subscription.reader.RssManager;

import android.content.Context;
import android.graphics.Bitmap;

public class SizeLimitedArticleBitmapFactory extends ArticleListBitmapFactory {
    public final String TAG = "ArticleListBitmapFactory";

    private static SizeLimitedArticleBitmapFactory mInstance = null;

    public static SizeLimitedArticleBitmapFactory getInstance(Context context) {
        if(mInstance == null) {
            mInstance = new SizeLimitedArticleBitmapFactory(context);
        }

        return mInstance;
    }

    private SizeLimitedArticleBitmapFactory(Context context) {
        super(context);
    }

    @Override
    protected Bitmap decodeFile(String filePath) {
        return BitmapHelper.decodeFile(filePath,
                mContext.getResources().getDimensionPixelSize(R.dimen.rd_article_list_image_width));
    }

    @Override
    protected Bitmap decodeByteArray(byte[] bytes) {
        return BitmapHelper.decodeByteArray(bytes, 0, bytes.length,
                mContext.getResources().getDimensionPixelSize(R.dimen.rd_article_list_image_width));
    }

    @Override
    protected boolean shouldSkipDownload() {
        return RssChannel.isRunningTask();
    }
}
