
package com.qihoo360.reader.ui.view;

import com.qihoo360.reader.R;
import com.qihoo360.reader.image.BitmapHelper;
import com.qihoo360.reader.support.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.ImageView;

public class ImageAlbumCoverView extends ImageView {
    public static class LocalFileInfo {
        public String url;
        public String path;
        public int oriWidth = 0;
        public int oriHeight = 0;
        public ImageAlbumCoverView target = null;
    }

    public static final String TAG = "ImageAlbumCoverView";
    private LocalFileInfo mFileInfo = null;
    private int mBitmapWidth = 0;
    private int mBitmapHeight = 0;
    private Bitmap mBitmap = null;

    public ImageAlbumCoverView(Context context) {
        super(context);
    }

    public ImageAlbumCoverView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ImageAlbumCoverView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setDefaultCover(Bitmap bm) {
        if(bm == null) {
            return;
        }

        recycleOldBitmap();

        mFileInfo = null;
        mBitmapWidth = 0;
        mBitmapHeight = 0;
        setScaleType(ScaleType.CENTER);
        super.setImageBitmap(bm);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        recycleOldBitmap();

        mFileInfo = null;
        mBitmap = bm;

        if(bm == null) {
            mBitmapWidth = 0;
            mBitmapHeight = 0;
            setImageResource(R.drawable.rd_article_detail_loading);
            setTag("");
            return;
        }

        mBitmapWidth = bm.getWidth();
        mBitmapHeight = bm.getHeight();
        applyCustomMatrix(mBitmapWidth, mBitmapHeight);

        super.setImageBitmap(bm);
    }

    public void setLocalFile(LocalFileInfo info) {
        if (info == null || TextUtils.isEmpty(info.path)
                || (info.target != null && info.target != this)) {
            return;
        }

        if (getWidth() > 0 && getHeight() > 0) {
            setImageBitmap(loadBitmapFromFile(info));
        } else {
            mFileInfo = info;
        }
    }

    @Override
    public void setImageResource(int resId) {
        recycleOldBitmap();
        mFileInfo = null;
        mBitmapWidth = 0;
        mBitmapHeight = 0;
        setScaleType(ScaleType.CENTER);
        super.setImageResource(resId);
    }

    private void applyCustomMatrix(int imageWidth, int imageHeight) {
        // Portrait images
        int viewWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int viewHeight = getHeight() - getPaddingTop() - getPaddingBottom();

        if (viewWidth <= 0 || viewHeight <= 0) {
            return;
        }

        if (viewWidth * imageHeight < viewHeight * imageWidth) {
            // 纵向无需拉伸
            setScaleType(ImageView.ScaleType.CENTER_CROP);
            return;
        }

        float scaleFactor = (float) (viewWidth) / (float) imageWidth;

        float dy;
        if(imageHeight/imageWidth >= 2) {
            dy = 0.0f;
        } else {
            // start drawing the image from 10% of the top
            dy = (viewHeight - imageHeight * scaleFactor) * 0.1f;
        }

        Matrix matrix = new Matrix();
        matrix.setScale(scaleFactor, scaleFactor);
        matrix.postTranslate(0, (int) (dy + 0.5f));

        setScaleType(ImageView.ScaleType.MATRIX);
        setImageMatrix(matrix);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (w > 0 && h > 0 && (w != oldw || h != oldh)) {
            if (mFileInfo != null) {
                setImageBitmap(loadBitmapFromFile(mFileInfo));
            } else if (mBitmapWidth > 0 && mBitmapHeight > 0) {
                applyCustomMatrix(mBitmapWidth, mBitmapHeight);
            }
        }
    }

    public Bitmap loadBitmapFromFile(LocalFileInfo info) {
        if (info == null || TextUtils.isEmpty(info.path)) {
            return null;
        }

        int viewWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int viewHeight = getHeight() - getPaddingTop() - getPaddingBottom();

        if (info.oriWidth <= 0 || info.oriHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) {
            return BitmapHelper.decodeFile(info.path);
        }

        int compressRatio = 1;
        if (viewWidth * info.oriHeight > info.oriWidth * viewHeight) {
            // 考虑宽边
            compressRatio = (int) (info.oriWidth / viewWidth);
        } else {
            // 考虑高边
            compressRatio = (int) (info.oriHeight / viewHeight);
        }

        if (compressRatio <= 1) {
            Utils.debug(TAG, "does not need to compress the bitmap...");
            return BitmapHelper.decodeFile(info.path);
        } else {
            /*
             * if (compressRatio % 2 != 0) { compressRatio -= 1; }
             */
            Utils.debug(TAG, "compressRatio: " + compressRatio);
            return BitmapHelper.decodeSampleFromFile(info.path, compressRatio);
        }
    }

    public Bitmap loadBitmapFromByteArray(LocalFileInfo info, byte[] bytes) {
        if (info == null || TextUtils.isEmpty(info.path)
                || bytes == null || bytes.length <= 0) {
            return null;
        }

        int viewWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int viewHeight = getHeight() - getPaddingTop() - getPaddingBottom();

        if (info.oriWidth <= 0 || info.oriHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) {
            return BitmapHelper.decodeByteArray(bytes, 0, bytes.length);
        }

        int compressRatio = 1;
        if (viewWidth * info.oriHeight > info.oriWidth * viewHeight) {
            // 考虑宽边
            compressRatio = (int) (info.oriWidth / viewWidth);
        } else {
            // 考虑高边
            compressRatio = (int) (info.oriHeight / viewHeight);
        }

        if (compressRatio <= 1) {
            Utils.debug(TAG, "does not need to compress the bitmap...");
            return BitmapHelper.decodeByteArray(bytes, 0, bytes.length);
        } else {
            /*
             * if (compressRatio % 2 != 0) { compressRatio -= 1; }
             */
            Utils.debug(TAG, "compressRatio: " + compressRatio);
            return BitmapHelper.decodeByteArrayWithCompressedRatio(bytes, 0, bytes.length, compressRatio);
        }
    }

    private void recycleOldBitmap() {
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
            mBitmap = null;
        }
    }

    public boolean isLayoutValid() {
        return getWidth() > 0 && getHeight() > 0;
    }
}
