package com.qihoo360.reader.ui.articles;

import com.qihoo360.reader.R;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.support.Utils;
import com.qihoo360.reader.ui.Nightable;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Adapter;
import android.widget.FrameLayout;
import android.widget.HeaderViewListAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.Toast;

public class ArticleReadView extends FrameLayout implements Nightable {
    private int swipe_min_distance = 120;
    private int mViewPaddingWidth = 0;
    private float mSnapBorderRatio = 0.67f;
    private boolean mIsGalleryCircular = false;
    private OnOverScrollListener mListener;

    private Scroller mScroller;

    private int mGalleryWidth = 0;
    private boolean mIsTouched = false;
    private boolean mIsDragging = false;
    private boolean mIsLoadProcess = false;
    private int mScrollDirection = 0;
    private int mFlingDirection = 0;
    private int mCurrentPosition = 0;
    private int mCurrentViewNumber = 0;
    private int mAdapterCount;
    private int mGalleryType = ArticleUtils.TYPE_NORMAL;

    // for text size zooming
    Handler mHandler = null;
    Runnable mChangeTextSizeRunnable = null;
    Runnable mChangeTextSizeBottomRunnable = null;
    private float mOldDistance = 0;

    private int lastDownXPos;
    private int lastDownYPos;
    private int addUpXOffset;
    private int mTouchSlop;
    private static final int TOUCH_STATE_REST = 0;
    private static final int TOUCH_STATE_SCROLLING = 1;
    private static final int TOUCH_STATE_ZOOM = 2;
    private int mTouchState = TOUCH_STATE_REST;

    private Context mContext;
    private Adapter mAdapter;
    private FlingGalleryView[] mViews;
    private ScrollHelper mScrollHelper;
    private VelocityTracker mVelocityTracker;
    private final int MAX_FLING_VELOCITY = 1000;
    private LinearLayout.LayoutParams mLLParams;
    private boolean mShoulScroll = false;
    private boolean mIsOverScroll = false;
    private boolean mIsDispatchTouched = false;
    private boolean mShouldForbidTouched = false;
    private Runnable mHolderRunable;
    private boolean mHadTips = false;

    public ArticleReadView(Context context) {
        this(context, null);
    }

    public ArticleReadView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mAdapter = null;

        mLLParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
        mViews = new FlingGalleryView[3];
        mViews[0] = new FlingGalleryView(0, this);
        mViews[1] = new FlingGalleryView(1, this);
        mViews[2] = new FlingGalleryView(2, this);

        mScrollHelper = new ScrollHelper();
        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

        mScroller = new Scroller(context);
    }

    public void setPaddingWidth(int viewPaddingWidth) {
        mViewPaddingWidth = viewPaddingWidth;
    }

    public void setSnapBorderRatio(float snapBorderRatio) {
        mSnapBorderRatio = snapBorderRatio;
    }

    public void changeImageMode(boolean showImage) {
        mViews[0].recycleView(mViews[0].mPosition);
        mViews[1].recycleView(mViews[1].mPosition);
        mViews[2].recycleView(mViews[2].mPosition);

        if (showImage) {
            startLoadingImagesForPage(mCurrentViewNumber, false);
        }
    }

    public void setIsGalleryCircular(boolean isGalleryCircular) {
        if (mIsGalleryCircular != isGalleryCircular) {
            mIsGalleryCircular = isGalleryCircular;

            if (mCurrentPosition == 0) {
                // We need to reload the view immediately to the left to change it to circular view or blank
                // 我们需要重新加载的 View 立即向左侧，将其更改为圆形视图或空白
                mViews[getPrevViewNumber(mCurrentViewNumber)].recycleView(getPrevPosition(mCurrentPosition));
            }

            if (mCurrentPosition == getLastPosition()) {
                // We need to reload the view immediately to the right to change it to circular view or blank
                mViews[getNextViewNumber(mCurrentViewNumber)].recycleView(getNextPosition(mCurrentPosition));
            }
        }
    }

    public final int getPosition() {
        return mCurrentPosition <= getLastPosition() ? mCurrentPosition : getLastPosition();
    }

    public final int getGalleryCount() {
        return (mAdapter == null) ? 0 : mAdapter.getCount();
    }

    public final int getFirstPosition() {
        return 0;
    }

    public final int getLastPosition() {
        return (getGalleryCount() == 0) ? 0 : getGalleryCount() - 1;
    }

    public int getPrevPosition(int relativePosition) {
        int prevPosition = relativePosition - 1;

        if (prevPosition < 0) {
            prevPosition = 0 - 1;

            if (mIsGalleryCircular == true) {
                prevPosition = getLastPosition();
            }
        }

        return prevPosition;
    }

    public final int getNextPosition(int relativePosition) {
        int nextPosition = relativePosition + 1;

        if (nextPosition > getLastPosition()) {
            nextPosition = getLastPosition() + 1;

            if (mIsGalleryCircular == true) {
                nextPosition = 0;
            }
        }

        return nextPosition;
    }

    private final int getPrevViewNumber(int relativeViewNumber) {
        return (relativeViewNumber == 0) ? 2 : relativeViewNumber - 1;
    }

    private final int getNextViewNumber(int relativeViewNumber) {
        return (relativeViewNumber == 2) ? 0 : relativeViewNumber + 1;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mGalleryWidth = right - left;
        swipe_min_distance = mGalleryWidth / 3;
        if (changed == true) {
            // Position views at correct starting offsets
            mViews[0].setOffset(0, 0, mCurrentViewNumber);
            mViews[1].setOffset(0, 0, mCurrentViewNumber);
            mViews[2].setOffset(0, 0, mCurrentViewNumber);
        }
    }

    public void setAdapter(Adapter adapter) {
        mCurrentViewNumber = 0;
        mAdapter = adapter;
        if (mAdapter != null)
            mAdapterCount = mAdapter.getCount();
    }

    public void setInitPosition(int position) {
        if (mAdapter == null)
            throw new IllegalArgumentException("Adapter is null");

        mCurrentPosition = position;
        mShoulScroll = false;

        if (mAdapter == null)
            return;

        if (mCurrentPosition < 0)
            mCurrentPosition = 0;
        else if (mCurrentPosition >= mAdapterCount - 1) {
            mCurrentPosition = mAdapterCount - 1;
        }

        // initialize order: current -> next -> previous
        mViews[0].recycleView(mCurrentPosition);
        mViews[1].recycleView(getNextPosition(mCurrentPosition));
        mViews[2].recycleView(getPrevPosition(mCurrentPosition));
        mViews[0].setOffset(0, 0, mCurrentViewNumber);
        mViews[1].setOffset(0, 0, mCurrentViewNumber);
        mViews[2].setOffset(0, 0, mCurrentViewNumber);

        startLoadingImagesForPage(0, false);
    }

    public void reset() {
        mViews[0].reset();
        mViews[1].reset();
        mViews[2].reset();
    }

    public void setAdapter(Adapter adapter, int position) {
        setAdapter(adapter);
        setInitPosition(position);
    }

    public void asyncSetCache(int position) {
        mCurrentViewNumber = 0;
        startLoadingImagesForPage(0, true);
    }

    private int getViewOffset(int viewNumber, int relativeViewNumber) {
        // Determine width including configured padding width
        int offsetWidth = mGalleryWidth + mViewPaddingWidth;

        // Position the previous view one measured width to left
        if (viewNumber == getPrevViewNumber(relativeViewNumber)) {
            return offsetWidth;
        }

        // Position the next view one measured width to the right
        if (viewNumber == getNextViewNumber(relativeViewNumber)) {
            return offsetWidth * -1;
        }

        return 0;
    }

    public void setInternalType(int type) {
        mGalleryType = (type != ArticleUtils.TYPE_COLLECTION && type != ArticleUtils.TYPE_NORMAL) ? ArticleUtils.TYPE_NORMAL
                        : type;
    }

    public void movePrevious() {
        // Slide to previous view
        if (!mScroller.isFinished())
            return;
        mFlingDirection = 1;
        processGesture();
    }

    public void moveNext() {
        // Slide to next view
        if (!mScroller.isFinished())
            return;
        showProcess();
        mFlingDirection = -1;
        processGesture();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_LEFT:
            movePrevious();
            return true;

        case KeyEvent.KEYCODE_DPAD_RIGHT:
            moveNext();
            return true;

        case KeyEvent.KEYCODE_DPAD_CENTER:
        case KeyEvent.KEYCODE_ENTER:
        }

        return super.onKeyDown(keyCode, event);
    }

    private boolean isLimited = false;
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int action = event.getAction();
        int curPointerXPos = (int) event.getX();
        int curPointerYPos = (int) event.getY();
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
        switch (action) {
        case MotionEvent.ACTION_DOWN:
            lastDownXPos = curPointerXPos;
            mIsTouched = true;
            mFlingDirection = 0;
            addUpXOffset = 0;
            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
            }
            break;
        case MotionEvent.ACTION_POINTER_2_DOWN:
        case MotionEvent.ACTION_POINTER_DOWN:
            if (mTouchState != TOUCH_STATE_ZOOM) {
                // 复位
                if (mIsTouched || mIsDragging) {
                    mFlingDirection = 0;
                    processGesture();
                }

                mOldDistance = spacing(event);
                mTouchState = TOUCH_STATE_ZOOM;
            }
            break;
        case MotionEvent.ACTION_MOVE:
            if (mTouchState == TOUCH_STATE_ZOOM) {
                if (mOldDistance > 0 && event.getPointerCount() > 1) {
                    float newDistance = spacing(event);

                    boolean shoudlChange = false;
                    boolean increase = true;
                    if (newDistance / mOldDistance >= 1.1) {
                        shoudlChange = true;
                    } else if (newDistance / mOldDistance <= 0.9) {
                        shoudlChange = true;
                        increase = false;
                    }

                    if (shoudlChange) {
                        changeTextSize(increase);
                        // increase/decrease one level for every pinch zoom
                        ((ArticleDetailActivity) mContext).onZoomChanged();
                        mOldDistance = newDistance;
                    }
                }
            } else {

                int scrollDistance = lastDownXPos - curPointerXPos;
                lastDownXPos = curPointerXPos;
                lastDownYPos = curPointerYPos;
                addUpXOffset += scrollDistance;

                if (mIsOverScroll || !mScroller.isFinished()){
                    isLimited = true;
                    break;
                }else{
                    isLimited = false;
                }

                if (mIsDragging == false) {
                    mIsTouched = true;
                    mIsDragging = true;
                    mFlingDirection = 0;
                }


                    mViews[0].setOffset(addUpXOffset, 0, mCurrentViewNumber);
                    mViews[1].setOffset(addUpXOffset, 0, mCurrentViewNumber);
                    mViews[2].setOffset(addUpXOffset, 0, mCurrentViewNumber);

                    showProcess();
            }
            break;
        case MotionEvent.ACTION_CANCEL:
            if ((mIsTouched || mIsDragging) && !isLimited) {
                processScrollSnap();
                processGesture();
            }

            mTouchState = TOUCH_STATE_REST;
            break;
        case MotionEvent.ACTION_UP:
            if (mTouchState != TOUCH_STATE_ZOOM) {
                mVelocityTracker.computeCurrentVelocity(1000);
                float xVelocity = mVelocityTracker.getXVelocity();
                boolean flingStriked = false;
                if (Math.abs(xVelocity) >= MAX_FLING_VELOCITY) {
                    if (addUpXOffset < -swipe_min_distance) {
                        flingStriked = true;
                        movePrevious();
                    } else if (addUpXOffset > swipe_min_distance) {
                        flingStriked = true;
                        if (!isLimited)
                            moveNext();
                    }
                }

                mScrollDirection = addUpXOffset == 0 ? 0 : addUpXOffset / Math.abs(addUpXOffset);
                addUpXOffset = 0;
                mFlingDirection = 0;

                if ((mIsTouched || mIsDragging) && !flingStriked && !isLimited) {
                    processScrollSnap();
                    processGesture();
                }
            }

            if (mVelocityTracker != null) {
                mVelocityTracker.recycle();
                mVelocityTracker = null;
            }

            mTouchState = TOUCH_STATE_REST;

            break;
        }
        return true;
    }

    private void changeTextSize(boolean increase) {
        try {
            ListView lv = (ListView) mViews[mCurrentViewNumber].mExternalView
                            .findViewById(R.id.rd_article_detail_content);

            if (!((ArticleDetailAdapter) mAdapter).changeTextSize(lv, increase)) {
                return;
            }

            if (mHandler == null) {
                mHandler = new Handler(getContext().getMainLooper());
            }

            final int viewToSkip = mCurrentViewNumber;
            if (mChangeTextSizeRunnable == null) {
                mChangeTextSizeRunnable = new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < mViews.length; i++) {
                            if (i != viewToSkip && mViews[i] != null && mViews[i].mExternalView != null) {
                                ListView tlv = (ListView) mViews[i].mExternalView
                                                .findViewById(R.id.rd_article_detail_content);
                                ArticleDetailContentAdapter.changeTextSize(tlv, ArticleDetailAdapter.mTextSize);
                            }
                        }
                    }
                };
            } else {
                mHandler.removeCallbacks(mChangeTextSizeRunnable);
            }

            mHandler.postDelayed(mChangeTextSizeRunnable, 500);
        } catch (Exception e) {
            Utils.error(getClass(), Utils.getStackTrace(e));
        }

    }

    public void changeTextSize(final int targetFont) {
        try {
            ListView lv = (ListView) mViews[mCurrentViewNumber].mExternalView
                            .findViewById(R.id.rd_article_detail_content);
            ((ArticleDetailAdapter) mAdapter).changeTextSize(lv, targetFont);

            if (mHandler == null) {
                mHandler = new Handler(getContext().getMainLooper());
            }

            final int viewToSkip = mCurrentViewNumber;
            if (mChangeTextSizeBottomRunnable == null) {
                mChangeTextSizeBottomRunnable = new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < mViews.length; i++) {
                            if (i != viewToSkip && mViews[i] != null && mViews[i].mExternalView != null) {
                                ListView tlv = (ListView) mViews[i].mExternalView
                                                .findViewById(R.id.rd_article_detail_content);
                                ArticleDetailContentAdapter.changeTextSize(tlv, ArticleDetailAdapter.mTextSize);
                            }
                        }
                    }
                };
            } else {
                mHandler.removeCallbacks(mChangeTextSizeBottomRunnable);
            }

            mHandler.postDelayed(mChangeTextSizeBottomRunnable, 500);
        } catch (Exception e) {
            Utils.error(getClass(), Utils.getStackTrace(e));
        }

    }

    private void changeIfNightMode() {
        for (int i = 0; i < mViews.length; i++) {
            if (mViews[i].mExternalView != null) {
                ListView listView = (ListView) mViews[i].mExternalView.findViewById(R.id.rd_article_detail_content);
                ((ImageView) mViews[i].mExternalView.findViewById(R.id.rd_empty_tips)).setImageResource(Settings
                                .isNightMode() ? R.drawable.rd_article_loading_exception_night
                                : R.drawable.rd_article_loading_exception);
                if (mAdapter != null) {
                    ((ArticleDetailAdapter) mAdapter).changeIfNightMode(listView);
                    listView.findViewById(R.id.rd_article_detail_title_container).setBackgroundColor(
                                    ArticleUtils.getRandomColor(mContext, Math.abs(mViews[i].mPosition),
                                                    Settings.isNightMode()));

                    listView.findViewById(R.id.rd_title_bottom_line).setBackgroundResource(
                                    Settings.isNightMode() ? R.drawable.rd_title_bottom_cover_nightly
                                                    : R.drawable.rd_title_bottom_cover);
                }
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mIsDispatchTouched == true) {
            mIsDispatchTouched = false;
            return false;
        }
        mIsDispatchTouched = false;

        final int action = ev.getAction();
        if ((action == MotionEvent.ACTION_MOVE) && (mTouchState != TOUCH_STATE_REST)) {
            return true;
        }
        final float x = ev.getX();
        final float y = ev.getY();
        switch (action) {
        case MotionEvent.ACTION_DOWN:
            lastDownXPos = (int) x;
            lastDownYPos = (int) y;
            mTouchState = mScroller.isFinished() || mShoulScroll == false ? TOUCH_STATE_REST : TOUCH_STATE_SCROLLING;
            break;
        case MotionEvent.ACTION_POINTER_2_DOWN:
        case MotionEvent.ACTION_POINTER_DOWN:
            if (mTouchState != TOUCH_STATE_ZOOM) {
                // 复位
                if (mIsTouched || mIsDragging) {
                    mFlingDirection = 0;
                    processGesture();
                }

                mOldDistance = spacing(ev);
                mTouchState = TOUCH_STATE_ZOOM;
            }
            break;
        case MotionEvent.ACTION_MOVE:
            if (mTouchState != TOUCH_STATE_ZOOM) {
                final int xDiff = (int) Math.abs(lastDownXPos - x);
                if (xDiff > mTouchSlop) {
                    if (Math.abs(lastDownYPos - y) / Math.abs(lastDownXPos - x) < 0.7)
                        mTouchState = TOUCH_STATE_SCROLLING;
                }
            }
            break;
        case MotionEvent.ACTION_CANCEL:
        case MotionEvent.ACTION_UP:
            mTouchState = TOUCH_STATE_REST;
            break;
        }
        return mTouchState != TOUCH_STATE_REST;
    }

    void processGesture() {
        int newViewNumber = mCurrentViewNumber;
        int reloadViewNumber = 0;
        int reloadPosition = 0;

        mIsTouched = false;
        mIsDragging = false;
        int lastPostion = mCurrentPosition;

        if (mFlingDirection > 0) {
            if (mCurrentPosition > 0 || mIsGalleryCircular == true) {
                // Determine previous view and outgoing view to recycle
                newViewNumber = getPrevViewNumber(mCurrentViewNumber);
                mCurrentPosition = getPrevPosition(mCurrentPosition);
                reloadViewNumber = getNextViewNumber(mCurrentViewNumber);
                reloadPosition = getPrevPosition(mCurrentPosition);
            } else {
                if (!mHadTips)
                    Toast.makeText(mContext, mContext.getString(R.string.rd_article_is_the_first), Toast.LENGTH_SHORT)
                                    .show();
                mHadTips = true;
            }
        } else if (mFlingDirection < 0) {
            if (mCurrentPosition <= getLastPosition() || mIsGalleryCircular == true) {
                if (!(mCurrentPosition == getLastPosition() && mGalleryType == ArticleUtils.TYPE_COLLECTION)) {
                    // Determine the next view and outgoing view to recycle
                    newViewNumber = getNextViewNumber(mCurrentViewNumber);
                    mCurrentPosition = getNextPosition(mCurrentPosition);
                    reloadViewNumber = getPrevViewNumber(mCurrentViewNumber);
                    reloadPosition = getNextPosition(mCurrentPosition);
                    if (mCurrentPosition < getLastPosition() + 1) {
                        mIsOverScroll = false;
                    } else if (lastPostion != getLastPosition() + 1)
                        mIsOverScroll = true;
                }
                mHadTips = false;
            }
        } else {
            if (mCurrentPosition <= 0 && mScrollDirection < 0) {
                if (!mHadTips)
                    Toast.makeText(mContext, mContext.getString(R.string.rd_article_is_the_first), Toast.LENGTH_SHORT)
                                    .show();
                mHadTips = true;
            }
            /*else if (mCurrentPosition > getLastPosition() && mScrollDirection > 0)
                mIsOverScroll = true;
            else
                mIsOverScroll = false;*/
        }

        /*if (mIsOverScroll)
            */
        //showProcess();

        if (newViewNumber != mCurrentViewNumber) {
            shouldListen = true;
            mCurrentViewNumber = newViewNumber;
            postCurrentViewNum = mCurrentViewNumber;
            postReloadViewNumber = reloadViewNumber;
            postReloadPosition = reloadPosition;
        }

        mShoulScroll = true;
        mScrollHelper.prepare(mCurrentViewNumber);
        mScroller.startScroll(mScrollHelper.mInitialOffset, 0, mScrollHelper.mTargetDistance, 0, mScrollHelper.duration);
        invalidate();
    }

    // ?should load next/previous view
    boolean shouldListen = false;

    int postCurrentViewNum;
    int postReloadViewNumber;
    int postReloadPosition;

    private void onScrollFinished(final int targetOffset, final int referenceNumber) {
        if (mHolderRunable != null) {
            getHandler().removeCallbacks(mHolderRunable);
        }
        mHolderRunable = new Runnable() {

            @Override
            public void run() {
                if (mIsOverScroll == true && mGalleryType == ArticleUtils.TYPE_NORMAL) {
                    if (!mIsLoadProcess && mListener != null)
                        mIsLoadProcess = mListener.onStartLoad();
                    else if (mListener != null)
                        mListener.onLoading();
                }

                if (shouldListen == true) {
                    shouldListen = false;
                    Utils.debug("onSwitched", "---------------------------------");
                    mViews[postReloadViewNumber].recycleView(postReloadPosition);

                    if (mListener != null)
                        mListener.onSwitched(mCurrentPosition);
                }
                startLoadingImagesForPage(postCurrentViewNum, true);
                if (mShouldForbidTouched)
                    mShouldForbidTouched = false;
            }
        };
        post(mHolderRunable);
    }

    public void asynPreLoading() {
        mViews[getPrevViewNumber(mCurrentViewNumber)].recycleView(getPrevPosition(mCurrentPosition));
        mViews[getNextViewNumber(mCurrentViewNumber)].recycleView(getNextPosition(mCurrentPosition));
    }

    private boolean showProcess() {
        if (getNextPosition(mCurrentPosition) == getLastPosition() + 1 && !mIsLoadProcess
                        && mCurrentPosition != getLastPosition() + 1) {
            int loadingNumber = getNextViewNumber(mCurrentViewNumber);
            mViews[loadingNumber].showProcess(mContext.getString(R.string.rd_article_loading_process));
            return true;
        } else if (mCurrentPosition == getLastPosition() + 1)
            return false;
        else
            return true;
    }

    private void startLoadingImagesForPage(int pageId, boolean updateNow) {
        if (mViews[pageId].mExternalView != null) {
            ListView lv = (ListView) mViews[pageId].mExternalView.findViewById(R.id.rd_article_detail_content);

            ArticleDetailContentAdapter adapter = (ArticleDetailContentAdapter) (((HeaderViewListAdapter) lv
                            .getAdapter()).getWrappedAdapter());
            adapter.setBusy(false);

            if (updateNow) {
                adapter.updateImages(lv);
            }
        }
    }
    // 滚动单元的进度
    void processScrollSnap() {
        // Snap to next view if scrolled passed snap position
        float rollEdgeWidth = mGalleryWidth * mSnapBorderRatio;
        int rollOffset = mGalleryWidth - (int) rollEdgeWidth;
        int currentOffset = mViews[mCurrentViewNumber].getCurrentOffset();

        if (currentOffset <= rollOffset * -1) {
            mFlingDirection = 1;
        }

        if (currentOffset >= rollOffset) {
            mFlingDirection = -1;
        }
    }
//
    public class FlingGalleryView {
        private int mViewNumber;
        private boolean mProcessShowing = false;
        private FrameLayout mInternalLayout = null;
        private View mExternalView = null; //  外部的view
        private int mPosition = -1;
        private View mProcessView = null;

        public FlingGalleryView(int viewNumber, FrameLayout parentLayout) {
            mViewNumber = viewNumber;

            if (mGalleryType == ArticleUtils.TYPE_NORMAL) {
                mInternalLayout = (FrameLayout) LayoutInflater.from(mContext).inflate(
                                R.layout.rd_article_detail_internal, null);
                mProcessView = mInternalLayout.findViewById(R.id.rd_article_detail_process);
            } else
                mInternalLayout = new FrameLayout(mContext);

            mInternalLayout.setLayoutParams(mLLParams);
            parentLayout.addView(mInternalLayout);
        }

        public void recycleView(int newPosition) {
            mPosition = newPosition;

            if (mExternalView != null) {
                mInternalLayout.removeView(mExternalView);
            }

            if (mAdapter != null) {
                if (newPosition >= 0 && newPosition <= getLastPosition() && getGalleryCount() > 0) {
                    mExternalView = mAdapter.getView(newPosition, mExternalView, mInternalLayout);
                    if (mExternalView != null)
                        mExternalView.setVisibility(VISIBLE);
                } else if ((newPosition < 0 || newPosition > getLastPosition()) && mExternalView == null) {
//                    mExternalView = mAdapter.getView(newPosition < 0 ? 0 : getLastPosition(), mExternalView,
//                                    mInternalLayout);

                    if(mExternalView != null) {
                        mExternalView.setVisibility(View.GONE);
                    }
                } else {
                    if (mExternalView != null)
                        mExternalView.setVisibility(mGalleryType == ArticleUtils.TYPE_COLLECTION ? INVISIBLE : GONE);
                    newPosition = -1;
                }
                reset();
            }

            if (mExternalView != null) {
                mInternalLayout.addView(mExternalView, mLLParams);
            }
        }

        public void reset() {
            if (mExternalView != null && mExternalView instanceof ScrollView) {
                post(new Runnable() {

                    @Override
                    public void run() {
                        ((ScrollView) mExternalView).smoothScrollTo(0, 0);
                    }
                });
            }
        }

        public void showProcess(String tips) {
            if (mGalleryType == ArticleUtils.TYPE_NORMAL) {
                if (mProcessShowing == false) {
                    mProcessView.setVisibility(VISIBLE);
                    mProcessShowing = true;
                }
                if (!TextUtils.isEmpty(tips))
                    ((TextView) mProcessView.findViewById(R.id.rd_article_detail_loading_tips)).setText(tips);
            }
        }

        public void hideProcess() {
            if (mGalleryType == ArticleUtils.TYPE_NORMAL && mProcessShowing == false)
                mProcessView.setVisibility(GONE);
        }

        public void setOffset(int xOffset, int yOffset, int relativeViewNumber) {
            mInternalLayout.scrollTo(getViewOffset(mViewNumber, relativeViewNumber) + xOffset, yOffset);
        }

        public int getCurrentOffset() {
            return mInternalLayout.getScrollX();
        }

        public void requestFocus() {
            mInternalLayout.requestFocus();
        }
    }

    private class ScrollHelper {
        private int mRelativeViewNumber;
        private int mInitialOffset;
        private int mTargetOffset;
        private int mTargetDistance;
        private int leftLimit;
        private int rightLimit;
        private int duration = 0;

        public ScrollHelper() {
            mRelativeViewNumber = 0;
            mInitialOffset = 0;
            mTargetOffset = 0;
            mTargetDistance = 0;
        }

        public void prepare(int relativeViewNumber) {
            // If we are animating relative to a new view
            if (mRelativeViewNumber != relativeViewNumber) {
                mRelativeViewNumber = relativeViewNumber;
            }

            mInitialOffset = mViews[mRelativeViewNumber].getCurrentOffset();
            mTargetOffset = getViewOffset(mRelativeViewNumber, mRelativeViewNumber);
            mTargetDistance = mTargetOffset - mInitialOffset;
            leftLimit = getPrevViewNumber(mRelativeViewNumber);
            rightLimit = getNextViewNumber(mRelativeViewNumber);
            duration = Math.abs(mTargetDistance) * 2;
            mScrollerhadFinshed = false;
        }
    }
    /**
     * 空间大小
     */
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return FloatMath.sqrt(x * x + y * y);
    }
    
    synchronized public void onCompletion(int state, long count) {
        if (state == ArticleUtils.SUCCESS) {
            int next = getNextViewNumber(mCurrentViewNumber);
            mViews[next].recycleView(getNextPosition(mCurrentPosition));
            mViews[mCurrentViewNumber].recycleView(mCurrentPosition);
            mViews[mCurrentViewNumber].mProcessShowing = false;
            mViews[mCurrentViewNumber].hideProcess();
            startLoadingImagesForPage(mCurrentViewNumber, true);
        } else if (mIsOverScroll) {
            if (state == ArticleUtils.FAILURE)
                Toast.makeText(mContext, mContext.getString(R.string.rd_article_network_expception), Toast.LENGTH_SHORT)
                                .show();
            else if (state == ArticleUtils.NOEXIST)
                Toast.makeText(mContext, mContext.getString(R.string.rd_article_no_older_news), Toast.LENGTH_SHORT)
                                .show();

            movePrevious();
            mShouldForbidTouched = true;
        }
        mIsOverScroll = false;
        mIsLoadProcess = false;
    }

    public void setOnOverScrollListener(OnOverScrollListener listener) {
        mListener = listener;
    }

    @Override
    public void onUpdateNightMode() {
        changeIfNightMode();
    }

    public interface OnOverScrollListener {
        /**
         * on start load
         * @return start true<SUCCESS> Or false <FAILURE>
         */
        public boolean onStartLoad();

        public void onLoading();

        /**
         * @param position the article postion in adapter
         */
        public void onSwitched(final int position);
    }

    private boolean mScrollerhadFinshed = false;

    @Override
    public void computeScroll() {
        if (!mScrollerhadFinshed && mScroller.computeScrollOffset()) {
            for (int viewNumber = 0; viewNumber < 3; viewNumber++) {
                // Only need to animate the visible views as the other view will always be off-screen
                if ((mScrollHelper.mTargetDistance > 0 && viewNumber != mScrollHelper.rightLimit)
                                || (mScrollHelper.mTargetDistance < 0 && viewNumber != mScrollHelper.leftLimit)) {
                    mViews[viewNumber].setOffset(mScroller.getCurrX(), 0, mScrollHelper.mRelativeViewNumber);
                }
            }
            postInvalidate();
        } else if (!mScrollerhadFinshed) {
            mViews[0].setOffset(mScrollHelper.mTargetOffset, 0, mScrollHelper.mRelativeViewNumber);
            mViews[1].setOffset(mScrollHelper.mTargetOffset, 0, mScrollHelper.mRelativeViewNumber);
            mViews[2].setOffset(mScrollHelper.mTargetOffset, 0, mScrollHelper.mRelativeViewNumber);
            onScrollFinished(mScrollHelper.mTargetOffset, mScrollHelper.mRelativeViewNumber);
            mScrollerhadFinshed = true;
        }
    }
}
