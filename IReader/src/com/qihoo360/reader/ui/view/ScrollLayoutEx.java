package com.qihoo360.reader.ui.view;

import com.qihoo360.reader.R;
import com.qihoo360.reader.ui.channels.ScrollLayout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class ScrollLayoutEx extends ScrollLayout {
    private ImageViewEx mCurPage = null;

    public ScrollLayoutEx(Context context) {
        this(context, null);
    }

    public ScrollLayoutEx(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScrollLayoutEx(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public int snapToScreen(int whichScreen) {
        if (getScrollX() != (whichScreen * getWidth()) && whichScreen >= 0
                && whichScreen < getChildCount()) {
            mCurPage = (ImageViewEx) getChildAt(whichScreen).findViewById(R.id.rd_imageviewex);
        }
        return super.snapToScreen(whichScreen);
    }

    @Override
    public void setToScreen(int whichScreen) {
        mCurPage = (ImageViewEx) getChildAt(whichScreen).findViewById(R.id.rd_imageviewex);
        super.setToScreen(whichScreen);
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if(mCurPage != null && mCurPage.isBrowsing()) {
            return false;
        }

        return super.onInterceptTouchEvent(ev);
    }

}
