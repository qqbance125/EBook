package com.qihoo360.reader.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;
/**
 * 
 * @author lidy
 *
 */
public class CustomImageView extends ImageView {
    private int mBitmapWidth = 0;
    private int mBitmapHeight = 0;

    public CustomImageView(Context context) {
        super(context);
    }

    public CustomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);

        if (bm != null) {
            mBitmapWidth = bm.getWidth();
            mBitmapHeight = bm.getHeight();
            applyCustomMatrix(mBitmapWidth, mBitmapHeight);
        } else {
            mBitmapWidth = 0;
            mBitmapHeight = 0;
        }
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);

        mBitmapWidth = 0;
        mBitmapHeight = 0;
        setScaleType(ScaleType.CENTER_CROP);
    }

    @Override
    public void setImageResource(int resId) {
        super.setImageResource(resId);

        mBitmapWidth = 0;
        mBitmapHeight = 0;
        setScaleType(ScaleType.CENTER_CROP);
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
        if (imageHeight / imageWidth >= 2) {
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
        if (w > 0 && h > 0 && (w != oldw || h != oldh) && mBitmapWidth > 0 && mBitmapHeight > 0) {
            applyCustomMatrix(mBitmapWidth, mBitmapHeight);
        }
    }

}
