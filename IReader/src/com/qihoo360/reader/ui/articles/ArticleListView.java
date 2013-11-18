
package com.qihoo360.reader.ui.articles;

import com.qihoo360.reader.R;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.support.Utils;
import com.qihoo360.reader.ui.Nightable;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.LayoutAnimationController;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

public class ArticleListView extends ListView implements Nightable, OnScrollListener {
    // private static final int TAP_TO_REFRESH = 1;
    private static final int PULL_TO_REFRESH = 2;
    private static final int RELEASE_TO_REFRESH = 3;
    private static final int REFRESHING = 4;

    private static final String TAG = "PullToRefreshListView";

    private OnRequestListener mOnRefreshListener;

    /**
     * Listener that will receive notifications every time the list scrolls.
     */
    private OnScrollListener mOnScrollListener;
    private RelativeLayout mRefreshView;
    private TextView mRefreshViewText;
    private ImageView mRefreshViewImage;
    private ProgressBar mRefreshViewProgress;
    private TextView mRefreshViewLastUpdated;

    private int mCurrentScrollState;
    private int mRefreshState;
    private List<View> mHeaders;
    private List<View> mFooters;

    private RotateAnimation mFlipAnimation;
    private RotateAnimation mReverseFlipAnimation;

    private int mRefreshViewHeight;
    private int mRefreshOriginalTopPadding;
    private int mLastMotionY;
    private View mProcessView;

    private boolean mBounceHack;
    // the process-bar visible state
    private boolean mProcessVisible;
    // the list require state
    private boolean mIsLoading = false;
    // all header-view & footer-view clear state
    private boolean mCleared = true;
    // the list first visible item
    private int mFirstVisiblity = 1;
    // record last require result
    private int mLastRequireState = ArticleUtils.SUCCESS;
    // add up scroll distance in Y-axis
    private int addUpOffsetY = 0;

    private boolean mAnimatingChildren = false;
    private int mProcessPaddingBottom = 0;
    private int mProcessPadding = 0;
    /*
     * mHadRefreshOrLoadMore: 区分文章列表是否刷加载过数据(包括刷新和加载更多)
     */
    private boolean mHadRefreshOrLoadMore = false;
    private int mLastScrollState = OnScrollListener.SCROLL_STATE_IDLE;

    public static final int MODE_NEWS = 1;
    public static final int MODE_IMAGES = 2;

    private int mMode = MODE_NEWS;
    //加载的最多数量
    private int mLoadMoreThreshold = 10;

    public ArticleListView(Context context) {
        super(context);
        init(context);
    }

    public ArticleListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ArticleListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        // Load all of the animations we need in code rather than through XML
        mFlipAnimation = new RotateAnimation(0, -180, RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                        RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        mFlipAnimation.setInterpolator(new LinearInterpolator());
        mFlipAnimation.setDuration(250);
        mFlipAnimation.setFillAfter(true);
        mReverseFlipAnimation = new RotateAnimation(-180, 0, RotateAnimation.RELATIVE_TO_SELF,
                0.5f,
                        RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        mReverseFlipAnimation.setInterpolator(new LinearInterpolator());
        mReverseFlipAnimation.setDuration(250);
        mReverseFlipAnimation.setFillAfter(true);

        // mInflater = (LayoutInflater)
        // context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // 下拉的布局
        mRefreshView = (RelativeLayout) LayoutInflater.from(context).inflate(
                R.layout.rd_pull_to_refresh_header, this,
                        false);
        mRefreshViewText = (TextView) mRefreshView.findViewById(R.id.pull_to_refresh_text);
        mRefreshViewImage = (ImageView) mRefreshView.findViewById(R.id.pull_to_refresh_image);
        mRefreshViewProgress = (ProgressBar) mRefreshView
                .findViewById(R.id.pull_to_refresh_progress);
        mRefreshViewLastUpdated = (TextView) mRefreshView
                .findViewById(R.id.pull_to_refresh_updated_at);

        mRefreshViewImage.setMinimumHeight(50);
        mRefreshView.setOnClickListener(new OnClickRefreshListener());
        mRefreshOriginalTopPadding = mRefreshView.getPaddingTop();

        mRefreshState = PULL_TO_REFRESH;// TAP_TO_REFRESH;

        addHeaderView(mRefreshView);

        super.setOnScrollListener(this);

        measureView(mRefreshView);
        mRefreshViewHeight = mRefreshView.getMeasuredHeight();

        mProcessView = LayoutInflater.from(context).inflate(R.layout.rd_article_list_process, null);

        mProcessPaddingBottom = context.getResources().getDimensionPixelSize(R.dimen.rd_main_padding_bottom);
        mProcessPadding = mProcessView.getPaddingBottom();
    }

    /*
     * @Override protected void onAttachedToWindow() { //setSelection(1); }
     */

    @Override
    public void setAdapter(ListAdapter adapter) {
        setAdapter(adapter, 1);
    }

    public void setAdapter(ListAdapter adapter, int position) {
        super.setAdapter(adapter);
        setSelection(position);
        if (mCleared){
            if (mFooters != null && !mFooters.isEmpty())
                for (View view : mFooters)
                    super.addFooterView(view);
        }
    }

    public int getBeforeFirst() {
        if (mRefreshState == REFRESHING)
            return 1;
        return mFirstVisiblity;
    }

    public void recoveryHeader() {
        if (mCleared) {
            if (mHeaders != null && !mHeaders.isEmpty())
                for (View view : mHeaders)
                    super.addHeaderView(view);
        }
    }

    public void clearAdapter() {

        try {
            if (mHeaders != null && mHeaders.size() > 0 && getAdapter() != null) {
                for (View view : mHeaders)
                    super.removeHeaderView(view);
            }

            if (mFooters != null && mFooters.size() > 0) {
                for (View view : mFooters)
                    super.removeFooterView(view);
            }
        } catch (Exception e) {
            Utils.error(getClass(), Utils.getStackTrace(e));
        }
        mFirstVisiblity = getFirstVisiblePosition();
        super.setAdapter(null);
        mCleared = true;
    }

    @Override
    public void addHeaderView(View v) {
        if (v != null) {
            if (mHeaders == null)
                mHeaders = new LinkedList<View>();
            mHeaders.add(v);
            mCleared = false;
        }
        super.addHeaderView(v);
    }

    @Override
    public boolean removeHeaderView(View v) {
        if (mHeaders != null && mHeaders.contains(v)) {
            mHeaders.remove(v);
        }
        return super.removeHeaderView(v);
    }

    @Override
    public void addFooterView(View v) {
        if (v != null) {
            if (mFooters == null)
                mFooters = new LinkedList<View>();
            mFooters.add(v);
            mCleared = false;
        }
        super.addFooterView(v);
    }

    @Override
    public boolean removeFooterView(View v) {
        if (mFooters != null && mFooters.contains(v)) {
            mFooters.remove(v);
            return super.removeFooterView(v);
        }
        return false;
    }

    /**
     * Set the listener that will receive notifications every time the list
     * scrolls.
     *
     * @param l The scroll listener.
     */
    @Override
    public void setOnScrollListener(AbsListView.OnScrollListener l) {
        mOnScrollListener = l;
    }

    /**
     * Register a callback to be invoked when this list should be refreshed.
     *
     * @param onRefreshListener The callback to run.
     */
    public void setOnRefreshListener(OnRequestListener onRefreshListener) {
        mOnRefreshListener = onRefreshListener;
    }

    /**
     * Set a text to represent when the list was last updated.
     *
     * @param lastUpdated Last updated at.
     */
    public void setLastUpdated(CharSequence lastUpdated) {
        if (lastUpdated != null) {
            mRefreshViewLastUpdated.setVisibility(View.VISIBLE);
            mRefreshViewLastUpdated.setText(lastUpdated);
        } else {
            mRefreshViewLastUpdated.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int y = (int) event.getY();
        mBounceHack = false;

        switch (event.getAction()) {
        case MotionEvent.ACTION_UP:
            if (!isVerticalScrollBarEnabled()) {
                setVerticalScrollBarEnabled(true);
            }
            if (getFirstVisiblePosition() == 0 && mRefreshState != REFRESHING) {
                Utils.debug(TAG, "startPullToRefresh: " + mRefreshState);
                if ((mRefreshView.getBottom() >= mRefreshViewHeight || mRefreshView.getTop() >= 0)
                                && mRefreshState == RELEASE_TO_REFRESH) {
                    if (mIsLoading && mOnRefreshListener != null){
                        mOnRefreshListener.onCancel();
                        mIsLoading = false;
                    }
                    // Initiate the refresh
                    mRefreshState = REFRESHING;
                    prepareForRefresh();
                    onRefresh(false);
                } else if (mRefreshView.getBottom() < mRefreshViewHeight || mRefreshView.getTop() <= 0) {
                    // Abort refresh and scroll down below the refresh view
                    resetHeader();
                    setSelection(1);
                }
            }
            break;
        case MotionEvent.ACTION_DOWN:
            mLastMotionY = y;
            addUpOffsetY = 0;
            break;
        case MotionEvent.ACTION_MOVE:
            if (mCurrentScrollState == SCROLL_STATE_TOUCH_SCROLL && mRefreshState != REFRESHING
                            && getFirstVisiblePosition() == 0) {
                checkStateUpdate(y - mLastMotionY);
            }

            addUpOffsetY += y - mLastMotionY;

            applyHeaderPaddingEx(event);
            break;
        }
        return super.onTouchEvent(event);
    }

    private boolean mIsFirstPullDown = true;
    private int mFirstPullPosition = 0;

    private void applyHeaderPaddingEx(MotionEvent ev) {
        if (mRefreshState == RELEASE_TO_REFRESH) {
            if (mIsFirstPullDown) {
                mFirstPullPosition = (int) ev.getY();
                mIsFirstPullDown = false;
            }

            if (isVerticalFadingEdgeEnabled()) {
                setVerticalScrollBarEnabled(false);
            }

            int y = (int) ev.getY();
            int topPadding = (int) ((y - mFirstPullPosition) / 1.7);

            mRefreshView.setPadding(mRefreshView.getPaddingLeft(), mRefreshOriginalTopPadding + topPadding,
                            mRefreshView.getPaddingRight(), mRefreshView.getPaddingBottom());
        }
    }

    /**
     * Sets the header padding back to original size.
     */
    private void resetHeaderPadding() {
        mRefreshView.setPadding(mRefreshView.getPaddingLeft(), mRefreshOriginalTopPadding,
                        mRefreshView.getPaddingRight(), mRefreshView.getPaddingBottom());
        mIsFirstPullDown = true;
    }

    /**
     * Resets the header to the original state.
     */
    private void resetHeader() {
        if (mRefreshState != PULL_TO_REFRESH/* TAP_TO_REFRESH */) {
            // mRefreshState = TAP_TO_REFRESH;
            mRefreshState = PULL_TO_REFRESH;

            // Set refresh view text to the pull label
            // mRefreshViewText.setText(R.string.pull_to_refresh_tap_label);
            mRefreshViewText.setText(R.string.rd_pull_to_refresh_pull_label);
        }

        resetHeaderPadding();
        mRefreshViewImage
                .setImageResource(Settings.isNightMode() ? R.drawable.rd_ic_pull_to_refresh_night
                        : R.drawable.rd_ic_pulltorefresh_arrow);
        mRefreshViewImage.clearAnimation();
        mRefreshViewImage.setVisibility(View.GONE);
        mRefreshViewProgress.setVisibility(View.GONE);
    }
    /**
     * 确定大小
     * @param child
     */
    private void measureView(View child) {
        ViewGroup.LayoutParams p = child.getLayoutParams();
        if (p == null) {
            p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0 + 0, p.width);
        int lpHeight = p.height;
        int childHeightSpec;
        if (lpHeight > 0) {
            childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, MeasureSpec.EXACTLY);
        } else {
            childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }
        child.measure(childWidthSpec, childHeightSpec);
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        // When the refresh view is completely visible, change the text to say
        // "Release to refresh..." and flip the arrow drawable.
        if (mCurrentScrollState == SCROLL_STATE_TOUCH_SCROLL && mRefreshState != REFRESHING) {
            if (firstVisibleItem == 0) {
                checkStateUpdate(0);
            } else {
                mRefreshViewImage.setVisibility(View.GONE);
                resetHeader();
            }

        } else if (mCurrentScrollState == SCROLL_STATE_FLING && firstVisibleItem == 0 && mRefreshState != REFRESHING) {
            setSelection(1);
            mBounceHack = true;
            if(addUpOffsetY > 0 && mOnScrollListener != null) { // scrolling up
                mOnScrollListener.onScrollStateChanged(this, SCROLL_STATE_IDLE);
            }
        } else if (mBounceHack && mCurrentScrollState == SCROLL_STATE_FLING) {
            setSelection(1);
        }

        if (mCurrentScrollState == SCROLL_STATE_FLING || mCurrentScrollState == SCROLL_STATE_TOUCH_SCROLL) {
            // addUpOffsetY < 0, list's moving direction is down
            // 3 is the starting loading offset
            mProcessVisible = false;
            if (firstVisibleItem > 0 && totalItemCount <= firstVisibleItem + visibleItemCount + mLoadMoreThreshold && addUpOffsetY < 0
                            && mLastRequireState != ArticleUtils.NOEXIST) {
                showProcess();
                mProcessVisible = true;
            }

            if (totalItemCount <= firstVisibleItem + visibleItemCount + 1 && addUpOffsetY < 0) {
                processShouldPadding(mOnRefreshListener == null ? false : mOnRefreshListener.shouldPaddingFooter());
            }

        }

        if (mOnScrollListener != null) {
            mOnScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
        }
    }

    private void checkStateUpdate(int deltaY) {
        if (deltaY >= 0) {
            mRefreshViewImage.setVisibility(View.VISIBLE);
        }

        if ((mRefreshView.getBottom() > mRefreshViewHeight) && mRefreshState != RELEASE_TO_REFRESH) {
            mRefreshViewText.setText(R.string.rd_pull_to_refresh_release_label);
            mRefreshViewImage.clearAnimation();
            mRefreshViewImage.startAnimation(mFlipAnimation);
            mRefreshState = RELEASE_TO_REFRESH;
            Utils.debug(TAG, "startPullToRefresh: " + RELEASE_TO_REFRESH);
        } else if (mRefreshView.getBottom() <= mRefreshViewHeight
                && mRefreshState != PULL_TO_REFRESH) {
            mRefreshViewText.setText(R.string.rd_pull_to_refresh_pull_label);
            if (mRefreshState == RELEASE_TO_REFRESH) {
                mRefreshViewImage.clearAnimation();
                mRefreshViewImage.startAnimation(mReverseFlipAnimation);
            }
            mRefreshState = PULL_TO_REFRESH;
            Utils.debug(TAG, "startPullToRefresh: " + PULL_TO_REFRESH);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == SCROLL_STATE_IDLE || scrollState == SCROLL_STATE_FLING) {
            if (scrollState == SCROLL_STATE_IDLE)
                mBounceHack = false;

            if (mProcessVisible && !mIsLoading && mLastRequireState != ArticleUtils.NOEXIST
                            && mOnRefreshListener != null) {
                mIsLoading = true;
                if (mRefreshState == REFRESHING) {
                    mOnRefreshListener.onCancel();
                    mRefreshState = PULL_TO_REFRESH;
                    resetHeader();
                }
                mOnRefreshListener.onLoadOld();
                Utils.debug("onLoading", "State: activited load more.......");
            } else if (mProcessVisible && mIsLoading && mOnRefreshListener != null) {
                mOnRefreshListener.onLoading();
            }
        }

        Utils.debug("onLoading", "ScrollState: " + scrollState);

        if (mOnScrollListener != null) {
            mOnScrollListener.onScrollStateChanged(view, scrollState);
        }
        mCurrentScrollState = scrollState;
    }

    public void reset(){
        mIsLoading = false;
        resetHeader();
    }

    public void onChannelChanged() {
        mIsLoading = false;
        resetLastRequireState();
        resetHeader();
    }

    public void resetLastRequireState() {
        mLastRequireState = ArticleUtils.SUCCESS;
        mProcessView.findViewById(R.id.rd_article_list_process_bar).setVisibility(VISIBLE);
        ((TextView) mProcessView.findViewById(R.id.rd_article_list_process_text))
                .setText(getResources().getString(R.string.rd_loading_text));
    }

    public void hideProcess(final boolean addFooter) {
        if (mProcessView != null && mLastRequireState != ArticleUtils.NOEXIST) {
            if (getFooterViewsCount() >= 1) {
                /**
                 * 1. set the visibility to invisible so that the progress bar will
                 * stop posting messages to do the animation;
                 * 2. remove the existing the messages from the message queue.
                 * Otherwise, those remaining messages would holds references of the progress bar,
                 * which indirectly cause the whole Activity can not be release during GC.
                 */
                ProgressBar pb = (ProgressBar) mProcessView.findViewById(R.id.rd_article_list_process_bar);
                pb.setVisibility(View.INVISIBLE);
                Drawable drawable = pb.getIndeterminateDrawable();
                if (drawable != null && drawable instanceof Runnable) {
                    pb.removeCallbacks((Runnable) drawable);
                }

                removeFooterView(mProcessView);
                if (addFooter) {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            showProcess();
                        }
                    });
                } else {
                    if (getLastVisiblePosition() >= getCount()) {
                        mIsLoading = true;
                        mOnRefreshListener.onLoadOld();
                    }
                }
            }
        }
    }

    public void tipNoAnyMore() {
        if (mProcessView != null) {
            mProcessView.findViewById(R.id.rd_article_list_process_bar).setVisibility(GONE);

            String noMoreTipContent = getContext().getString(
                    mMode == MODE_NEWS ? R.string.rd_article_no_older_news
                            : R.string.rd_article_no_older_images);
            ((TextView) mProcessView.findViewById(R.id.rd_article_list_process_text))
                    .setText(noMoreTipContent);
        }
    }

    public final void showProcess() {
        if (mProcessView != null && getFooterViewsCount() <= 0) {
            mProcessView.findViewById(R.id.rd_article_list_process_bar).setVisibility(
                        View.VISIBLE);
            addFooterView(mProcessView);
        }
    }

    private boolean mProcessPadded = false;

    public final void processShouldPadding(final boolean state) {
        if (mProcessView == null || mProcessPadded == state)
            return;
        post(new Runnable() {

            @Override
            public void run() {
                mProcessView.setPadding(mProcessView.getPaddingLeft(), mProcessView.getPaddingTop(),
                                mProcessView.getPaddingRight(), mProcessPadding + (state ? mProcessPaddingBottom : 0));
                mProcessPadded = state;
            }
        });
    }
    /**
     * 准备刷新
     */
    public void prepareForRefresh() {
        resetHeaderPadding();

        mRefreshViewImage.setVisibility(View.GONE);
        // We need this hack, otherwise it will keep the previous drawable.
        //我们需要调用这个方法，否则它会保持先前的drawable。
        mRefreshViewImage.setImageDrawable(null);
        mRefreshViewProgress.setVisibility(View.VISIBLE);

        // Set refresh view text to the refreshing label 正在刷新......
        mRefreshViewText.setText(R.string.rd_pull_to_refresh_refreshing_label);
        mRefreshState = REFRESHING;
    }

    /**
     * @return if loading more, return false
     */
    public boolean manualRefresh() {
        if (mIsLoading == false) {
            setSelection(0);
            prepareForRefresh();
            onRefresh(false);
            return true;
        }
        return false;
    }

    public void onRefresh(boolean isAuto) {
        if (mOnRefreshListener != null)
            mOnRefreshListener.onRefresh(isAuto);
    }

    /**
     * Resets the list to a normal state after a refresh.
     * 刷新完成
     */
    public void onRefreshComplete(int state) {
        // Log.d(TAG, "onRefreshComplete");

        resetHeader();
        // showProcess();

        // If refresh view is visible when loading completes, scroll down to
        // the next item.
        if (mRefreshView.getBottom() > 0) {
            invalidateViews();
            if (state == ArticleUtils.SUCCESS
                            ||(state != ArticleUtils.SUCCESS && getFirstVisiblePosition() == 0)) {
                setSelection(1);
            }
        }

    }

    public void switchNoPicMode() {
        // fix a crash
        try {
            View secondItem = getChildAt(1);
            int toTop = secondItem.getTop();
            if (toTop >= mRefreshViewHeight) {
                setSelection(1);
            }
        } catch (Exception e) {
            Utils.error(getClass(), Utils.getStackTrace(e));
        }
    }

    /**
     * reset listView UI state
     * @param state
     * @param count
     * @return
     */
    public void onCompletion(int state, long count) {
        onCompletion(state, count, false);
    }

    public void onCompletion(int state, long count, boolean refresh) {
        mHadRefreshOrLoadMore = true;
        if (mIsLoading) {
            mIsLoading = false;
            mRefreshState = PULL_TO_REFRESH;
            mLastRequireState = state;
            if (state == ArticleUtils.NOEXIST)
                tipNoAnyMore();
            else
                hideProcess(true);
            postInvalidate();
        }

        if (mRefreshState == REFRESHING || refresh){
            onRefreshComplete(state);
        }
    }

    public void loadMore(){
        if(mOnRefreshListener != null){
            mOnRefreshListener.onLoadOld();
            mIsLoading = true;
        }
    }

    @Override
    public void onUpdateNightMode() {
        Resources rs = getResources();
        if (Settings.isNightMode() == true) {
            mRefreshViewText.setTextColor(rs.getColor(R.color.rd_night_text));
            mRefreshViewImage.setImageResource(R.drawable.rd_ic_pull_to_refresh_night);
        } else {
            mRefreshViewText.setTextColor(rs.getColor(R.color.rd_gray));
            mRefreshViewImage.setImageResource(R.drawable.rd_ic_pulltorefresh_arrow);
        }
        rs = null;
    }

    @Override
    public void setLayoutAnimation(LayoutAnimationController controller) {
        super.setLayoutAnimation(controller);

        mAnimatingChildren = true;
        setLayoutAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation paramAnimation) {
            }

            @Override
            public void onAnimationEnd(Animation paramAnimation) {
                mAnimatingChildren = false;
                // manualRefresh();
                if (!mHadRefreshOrLoadMore) {
                    setSelection(0);
                    prepareForRefresh();
                    onRefresh(true);
                }
            }

            @Override
            public void onAnimationRepeat(Animation paramAnimation) {
            }
        });
    }

    public boolean IsAnimatingChildren() {
        return mAnimatingChildren;
    }

    public void setScrollState(int state) {
        mLastScrollState = state;
    }

    public int getScrollState() {
        return mLastScrollState;
    }

    /**
     * Invoked when the refresh view is clicked on. This is mainly used when
     * there's only a few items in the list and it's not possible to drag the
     * list.
     */
    private class OnClickRefreshListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            if (mRefreshState != REFRESHING) {
                prepareForRefresh();
                onRefresh(false);
            }
        }

    }

    /**
     * Interface definition for a callback to be invoked when list should be
     * refreshed.
     * 当列表刷新的 接口
     */
    public interface OnRequestListener {
        /**
         * Called when the list should be refreshed.
         * <p>
         * A call to {@link ArticleListView #onRefreshComplete()} is expected to
         * indicate that the refresh has completed.
         */
        public void onRefresh(boolean isAutoRefresh);

        public void onLoadOld();

        public void onLoading();

        public void onCancel();

        public boolean shouldPaddingFooter();
    }

    /**
     * Listener handles the size changed event of this view
     * @author fanguofeng
     */
    public interface OnSizeChangedListener {
        public void onSizeChanged(int width, int height);
    }

    private OnSizeChangedListener mOnSizeChangedListener = null;
    public void setOnSizeChangedListener(OnSizeChangedListener listener) {
        mOnSizeChangedListener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if(w > 0 && h > 0 && (w != oldw || h != oldh) && mOnSizeChangedListener != null) {
            mOnSizeChangedListener.onSizeChanged(w, h);
        }
    }

    public void setMode(int mode) {
        mMode = mode;
    }
    
    /**
     * 加载的数量
     * @param threshold
     */
    public void setLoadMoreThreshold(int threshold) {
        mLoadMoreThreshold = threshold;
    }

    public boolean isItemCompletelyVisible(int positon) {
        int firstItem = getFirstVisiblePosition();
        int lastItem = getLastVisiblePosition();
        if(positon > firstItem && positon < lastItem) {
            return true;
        } else if (positon == firstItem) {
            View item = getChildAt(0);
            return item!= null && item.getTop() >= 0;
        } else  if (positon == lastItem){
            View item = getChildAt(getChildCount() - 1);
            return item!= null && item.getBottom() <= getHeight();
        }

        return false;
    }
}
