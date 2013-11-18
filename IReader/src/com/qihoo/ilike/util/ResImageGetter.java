package com.qihoo.ilike.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Html.ImageGetter;

public class ResImageGetter implements ImageGetter {
    private int mWidth;
    private int mHeight;
    private Context mContext;
    private int offsetY = 0;

    public ResImageGetter(int height, Context context) {
        mHeight = height;
        mContext = context;
    }

    @Override
    public Drawable getDrawable(String source) {
        if (source != null) {
            try {
                int id = Integer.parseInt(source);
                Drawable d = mContext.getResources().getDrawable(id);
                if (d.getIntrinsicHeight() > 0) {
                    mWidth = (int) (((float) (d.getIntrinsicWidth()) * (float) mHeight) / d
                            .getIntrinsicHeight());
                }
                // 可绘制指定的矩形边框。这是其draw（）方法被调用时，drawable会画。
                d.setBounds(0, offsetY, mWidth, mHeight+offsetY);
                return d;
            } catch (NumberFormatException e) {
                com.qihoo360.reader.support.Utils.error(getClass(), com.qihoo360.reader.support.Utils.getStackTrace(e));
            }
        }
        return null;
    }

}
