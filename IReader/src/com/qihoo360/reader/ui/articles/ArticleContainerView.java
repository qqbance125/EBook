package com.qihoo360.reader.ui.articles;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Scroller;
/**
 * 文章
 */
public class ArticleContainerView extends LinearLayout {

    private Scroller mScroller;
    private int mCurScreen;
    private int previous;
    private int next;
    private OnScrollChildListener mListener;
    private boolean mIsEnd;

    public ArticleContainerView(Context context) {
        this(context, null);
    }

    public ArticleContainerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mScroller = new Scroller(context);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childLeft = 0;
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View childView = getChildAt(i);
            if (childView.getVisibility() != View.GONE) {
                final int childWidth = childView.getMeasuredWidth();
                childView.layout(childLeft, 0, childLeft + childWidth, childView.getMeasuredHeight());
                childLeft += childWidth;
            }
        }
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        if (widthMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("ScrollLayout only canmCurScreen run at EXACTLY mode!");
        }
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("ScrollLayout only can run at EXACTLY mode!");
        }
        // The children are given the same width and height as the scrollLayout
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    public void switchScreen(int screen){
     // get the valid(有效的) layout page
        previous = mCurScreen;
        next  = screen;
        screen = Math.max(0, Math.min(screen, getChildCount() - 1));
        mCurScreen = screen;
        if (getScrollX() != (screen * getWidth())) {
            final int delta = screen * getWidth() - getScrollX();
            mScroller.startScroll(getScrollX(), 0, delta, 0, 400);
            mIsEnd = false;
            invalidate(); // Redraw the layout
        }
    }

    public int getCurrent(){
        return mCurScreen;
    }

    @Override
    public void computeScroll() {
        // TODO Auto-generated method stub
        if(mScroller.computeScrollOffset()){
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        }else{
            if(mListener!=null&&mIsEnd == false)
                mListener.onFinish(previous, next);
            mIsEnd = true;
        }
    }

    public void setOnScrollChildListener(OnScrollChildListener listener){
        mListener = listener;
    }

    public interface OnScrollChildListener{
        public void onFinish(int from, int to);
    }
}
