
package com.qihoo360.reader.ui.view;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.text.Html;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.Toast;

import com.qihoo360.reader.Constants;
import com.qihoo360.reader.R;
import com.qihoo360.reader.image.BitmapFactoryBase;
import com.qihoo360.reader.image.BitmapHelper;
import com.qihoo360.reader.image.ImageUtils;
import com.qihoo360.reader.subscription.Article;
import com.qihoo360.reader.support.ArithmeticUtils;
import com.qihoo360.reader.support.AsyncTaskEx;
import com.qihoo360.reader.support.FileUtils;
import com.qihoo360.reader.support.Utils;

public class AlbumGallaryView extends FrameLayout {
    public interface LoadOldAlbumsHandler {
        public boolean loadMoreAlbums();
    }

    public interface ChangeAlbumsListener {
        public void onAlbumChanged();
    }

    private LoadOldAlbumsHandler mLoadMoreAlbumsHandler;

    private int swipe_min_distance = 120;
    private int mViewPaddingWidth = 0;
    private float mSnapBorderRatio = 0.80f;
    private OnScrollListener mScrollListener;

    private Scroller mScroller;

    private int mGalleryWidth = 0;
    private boolean mIsTouched = false;
    private boolean mIsDragging = false;
    private int mFlingDirection = 0;
    private int mCurrentViewNumber = 0;

    private int lastDownXPos;
    private int lastDownYPos;
    private int addUpXOffset;
    private int mTouchSlop;
    private static final int TOUCH_STATE_REST = 0;
    private static final int TOUCH_STATE_SCROLLING = 1;
    private int mTouchState = TOUCH_STATE_REST;

    private Context mContext;
    private GallaryPageView[] mViews;
    private ScrollHelper mScrollHelper;
    private VelocityTracker mVelocityTracker;
    private final int MAX_FLING_VELOCITY = 1000;
    private LinearLayout.LayoutParams mLLParams;
    private Runnable mHolderRunable = null;

    private Cursor mCursor = null;
    private ArrayList<String> EMPTY_IMAGE_LIST;
    private TextView mAlbumTitleView = null;
    private TextView mPageNumberView = null;
    private int mImageQuality = -1;
    private View mBtnPrevAlbum = null;
    private View mBtnNextAlbum = null;

    private boolean mNoMoreAlbums = false;
    private static final int INDEX_LOADING_MORE = Integer.MAX_VALUE;

    private static class AlbumInfo {
        String title = "";
        ArrayList<String> images = null;
        int positonOfCursor = -1;
    }

    ArrayList<AlbumInfo> mLoadedAlbums = new ArrayList<AlbumInfo>();

    private static class PositionInfo {
        int albumIndex = -1;
        int positionInAlbum = -1;
    }

    private PositionInfo mCurrentPosition = new PositionInfo();

    private static class pendingSetImageTask {
        String url = "";;
        GallaryPageView view = null;
        Bitmap bitmap = null;
    }

    CopyOnWriteArrayList<pendingSetImageTask> mPendingTaskList = new CopyOnWriteArrayList<pendingSetImageTask>();

    public AlbumGallaryView(Context context) {
        this(context, null);
    }

    public AlbumGallaryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        mLLParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.FILL_PARENT);
        mViews = new GallaryPageView[3];
        mViews[0] = new GallaryPageView(0, this);
        mViews[1] = new GallaryPageView(1, this);
        mViews[2] = new GallaryPageView(2, this);

        mScrollHelper = new ScrollHelper();
        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

        mScroller = new Scroller(context);
        mHolderRunable = new Runnable() {
            @Override
            public void run() {
                if (shouldListen == true) {
                    mViews[postReloadViewNumber].recycleView(postReloadPosition);

                    updateTitleAndPageNumber();

                    AlbumInfo ai = getCurrentAlbum();
                    if (ai != null) {
                        int curAlbumIndex = mCurrentPosition.albumIndex;
                        int curPositionInAlbum = mCurrentPosition.positionInAlbum;
                        if ((curAlbumIndex == mLoadedAlbums.size() - 1
                                        && curPositionInAlbum == ai.images.size() - 2)
                                || (curAlbumIndex == mLoadedAlbums.size() - 2
                                        && curPositionInAlbum == ai.images.size() - 1
                                        && mLoadedAlbums.get(curAlbumIndex + 1).images.size() == 1)) {
                            // 需要载入更多旧的图集
                            loadOlderAlbum();
                        }

                        int existingAlbumsCount = mCursor.getCount();
                        boolean needLoadMoreAlbums = (ai.images.size() == 1 && curAlbumIndex >= existingAlbumsCount - 6)
                                || (ai.images.size() > 1
                                        && curPositionInAlbum == ai.images.size() - 1 && curAlbumIndex >= existingAlbumsCount - 3);
                        if (needLoadMoreAlbums && mLoadMoreAlbumsHandler != null) {
                            // 需要联网下载更多图集
                            mLoadMoreAlbumsHandler.loadMoreAlbums();
                        }
                    } else if (mCurrentPosition.albumIndex == INDEX_LOADING_MORE
                            && !mNoMoreAlbums && mLoadMoreAlbumsHandler != null) {
                        mLoadMoreAlbumsHandler.loadMoreAlbums();
                    }

                    if(mScrollListener != null) {
                        mScrollListener.onPageScrolled(getCurrentImageUrl());
                    }

                    System.gc();
                }

                handlePendingTasks();
            }
        };

        EMPTY_IMAGE_LIST = new ArrayList<String>(1);
        EMPTY_IMAGE_LIST.add("empty");
    }

    public AlbumInfo getCurrentAlbum() {
        if (mCurrentPosition.albumIndex == INDEX_LOADING_MORE) {
            return null;
        } else {
            return mLoadedAlbums.get(mCurrentPosition.albumIndex);
        }
    }

    public String getCurrentAlbumName() {
        AlbumInfo ai = getCurrentAlbum();
        return ai != null ? ai.title : null;
    }

    public final String getCurrentImageUrl() {
        if (mCurrentPosition.albumIndex == INDEX_LOADING_MORE) {
            return null;
        }

        return getCurrentAlbum().images
                .get(mCurrentPosition.positionInAlbum);
    }

    public final int getCurrentCursorPosition() {
        if (mCurrentPosition.albumIndex == INDEX_LOADING_MORE) {
            return mLoadedAlbums.get(mLoadedAlbums.size() - 1).positonOfCursor;
        }

        return getCurrentAlbum().positonOfCursor;
    }

    public final PositionInfo getPrevPosition(PositionInfo pi) {
        PositionInfo result = new PositionInfo();

        if (mCurrentPosition.albumIndex == INDEX_LOADING_MORE) {
            result.albumIndex = mLoadedAlbums.size() - 1;
            result.positionInAlbum = mLoadedAlbums.get(result.albumIndex).images.size() - 1;
        } else if (mCurrentPosition.positionInAlbum > 0) {
            // 此图集前一张图片
            result.albumIndex = mCurrentPosition.albumIndex;
            result.positionInAlbum = mCurrentPosition.positionInAlbum - 1;
        } else {
            // 下一图集
            if (mCurrentPosition.albumIndex == 0) {
                // 需要载入新的图集
                AlbumInfo newAlbum = loadNewerAlbum();
                if (newAlbum != null) {
                    result.albumIndex = 0;
                    result.positionInAlbum = newAlbum.images.size() - 1;
                }
            } else {
                AlbumInfo ai = mLoadedAlbums.get(mCurrentPosition.albumIndex - 1);
                result.albumIndex = mCurrentPosition.albumIndex - 1;
                result.positionInAlbum = ai.images.size() - 1;
            }
        }

        return result;
    }

    public final PositionInfo getNextPosition(PositionInfo pi) {
        PositionInfo result = new PositionInfo();
        if (pi.albumIndex == INDEX_LOADING_MORE) {
            return result;
        }

        AlbumInfo ai = mLoadedAlbums.get(pi.albumIndex);
        if (ai.images.size() > pi.positionInAlbum + 1) {
            // 此图集下一张图片
            result.albumIndex = pi.albumIndex;
            result.positionInAlbum = pi.positionInAlbum + 1;
        } else {
            // 下一图集
            if (pi.albumIndex == mLoadedAlbums.size() - 1) {
                // 需要载入新的图集
                if (loadOlderAlbum()) {
                    result.albumIndex = pi.albumIndex + 1;
                    result.positionInAlbum = 0;
                } else if (!mNoMoreAlbums) {
                    result.albumIndex = INDEX_LOADING_MORE;
                }
            } else {
                result.albumIndex = pi.albumIndex + 1;
                result.positionInAlbum = 0;
            }
        }

        return result;
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

    public boolean setCursor(Cursor cursor, int position) {
        if (cursor == null || cursor.getCount() == 0) {
            return false;
        }

        mCursor = cursor;

        position = Math.max(position, 0);
        cursor.moveToPosition(position);
        Article article = Article.inject(cursor);
        AlbumInfo ai = new AlbumInfo();
        ai.title = article.title;
        ai.positonOfCursor = position;
        ai.images = buildImageList(article.images360);
        mLoadedAlbums.add(ai);

        PositionInfo pi = new PositionInfo();
        pi.albumIndex = 0;
        pi.positionInAlbum = 0;
        JumpToPosition(pi);
        return true;
    }

    private AlbumInfo loadNewerAlbum() {
        AlbumInfo ai = mLoadedAlbums.get(0);
        if (ai.positonOfCursor <= 0) {
            // 没有更新的图集
            return null;
        }

        int position = ai.positonOfCursor - 1;
        mCursor.moveToPosition(position);
        Article article = Article.inject(mCursor);
        AlbumInfo newerAlbum = new AlbumInfo();
        newerAlbum.title = article.title;
        newerAlbum.positonOfCursor = position;
        newerAlbum.images = buildImageList(article.images360);
        mLoadedAlbums.add(0, newerAlbum);
        mCurrentPosition.albumIndex++;

        return newerAlbum;
    }

    private boolean loadOlderAlbum() {
        AlbumInfo ai = mLoadedAlbums.get(mLoadedAlbums.size() - 1);
        if (ai.positonOfCursor >= mCursor.getCount() - 1) {
            // 没有更旧的图集
            return false;
        }

        try {
            int position = ai.positonOfCursor + 1;
            mCursor.moveToPosition(position);
            Article article = Article.inject(mCursor);
            AlbumInfo newerAlbum = new AlbumInfo();
            newerAlbum.title = article.title;
            newerAlbum.positonOfCursor = position;
            newerAlbum.images = buildImageList(article.images360);
            mLoadedAlbums.add(newerAlbum);

            return true;
        } catch (Exception e) {
            Utils.error(getClass(), Utils.getStackTrace(e));
            return false;
        }
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

    public void movePrevious() {
        // Slide to previous view
        if (!mScroller.isFinished()) {
            mScroller.abortAnimation();
        }
        mFlingDirection = 1;
        processGesture();
    }

    public void moveNext() {
        if (!mScroller.isFinished()) {
            mScroller.abortAnimation();
        }
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
            case MotionEvent.ACTION_MOVE:
                int scrollDistance = lastDownXPos - curPointerXPos;
                lastDownXPos = curPointerXPos;
                lastDownYPos = curPointerYPos;
                addUpXOffset += scrollDistance;

                if (mIsDragging == false) {
                    mIsTouched = true;
                    mIsDragging = true;
                    mFlingDirection = 0;
                }

                mViews[0].setOffset(addUpXOffset, 0, mCurrentViewNumber);
                mViews[1].setOffset(addUpXOffset, 0, mCurrentViewNumber);
                mViews[2].setOffset(addUpXOffset, 0, mCurrentViewNumber);
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mIsTouched || mIsDragging) {
                    processScrollSnap();
                    processGesture();
                }

                mTouchState = TOUCH_STATE_REST;
                break;
            case MotionEvent.ACTION_UP:
                mVelocityTracker.computeCurrentVelocity(1000);
                float xVelocity = mVelocityTracker.getXVelocity();
                boolean flingStriked = false;
                if (Math.abs(xVelocity) >= MAX_FLING_VELOCITY) {
                    if (addUpXOffset < -swipe_min_distance) {
                        flingStriked = true;
                        movePrevious();
                    }

                    if (addUpXOffset > swipe_min_distance) {
                        flingStriked = true;
                        moveNext();
                    }
                }

                addUpXOffset = 0;
                mFlingDirection = 0;

                if ((mIsTouched || mIsDragging) && flingStriked == false) {
                    processScrollSnap();
                    processGesture();
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

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        if (mViews[mCurrentViewNumber].isBrowsing()) {
            return false;
        }

        if ((action == MotionEvent.ACTION_MOVE) && (mTouchState != TOUCH_STATE_REST)) {
            return true;
        }
        final float x = ev.getX();
        final float y = ev.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                lastDownXPos = (int) x;
                lastDownYPos = (int) y;
                mTouchState = mScroller.isFinished() ? TOUCH_STATE_REST : TOUCH_STATE_SCROLLING;
                break;
            case MotionEvent.ACTION_MOVE:
                final int xDiff = (int) Math.abs(lastDownXPos - x);
                if (xDiff > mTouchSlop) {
                    if (Math.abs(lastDownYPos - y) / Math.abs(lastDownXPos - x) < 0.7) {
                        mTouchState = TOUCH_STATE_SCROLLING;
                        if (mScrollListener != null) {
                            mScrollListener.onScrollStarted();
                        }
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

    private void processGesture() {
        int newViewNumber = mCurrentViewNumber;
        int reloadViewNumber = 0;
        PositionInfo reloadPosition = null;

        mIsTouched = false;
        mIsDragging = false;

        if (mFlingDirection > 0) {
            PositionInfo pi = getPrevPosition(mCurrentPosition);
            if (pi.albumIndex >= 0 && pi.albumIndex < mLoadedAlbums.size()
                    && pi.positionInAlbum >= 0
                    && pi.positionInAlbum < mLoadedAlbums.get(pi.albumIndex).images.size()) {
                // Determine previous view and outgoing view to recycle
                newViewNumber = getPrevViewNumber(mCurrentViewNumber);
                mCurrentPosition = pi;
                reloadViewNumber = getNextViewNumber(mCurrentViewNumber);
                reloadPosition = getPrevPosition(pi);
            } else {
                Toast.makeText(mContext,
                        mContext.getString(R.string.rd_article_no_newer_images),
                        Toast.LENGTH_SHORT).show();
            }
        } else if (mFlingDirection < 0) {
            PositionInfo pi = getNextPosition(mCurrentPosition);
            if ((pi.albumIndex >= 0 && pi.albumIndex < mLoadedAlbums.size()
                            && pi.positionInAlbum >= 0
                            && pi.positionInAlbum < mLoadedAlbums.get(pi.albumIndex).images.size())
                    || pi.albumIndex == INDEX_LOADING_MORE) {
                // Determine the next view and outgoing view to recycle
                newViewNumber = getNextViewNumber(mCurrentViewNumber);
                mCurrentPosition = pi;
                reloadViewNumber = getPrevViewNumber(mCurrentViewNumber);
                reloadPosition = getNextPosition(pi);
            } else {
                if (mNoMoreAlbums) {
                    Toast.makeText(mContext,
                            mContext.getString(R.string.rd_article_no_older_images),
                            Toast.LENGTH_SHORT).show();
                }
            }
        }

        if (newViewNumber != mCurrentViewNumber) {
            shouldListen = true;
            mCurrentViewNumber = newViewNumber;
            postCurrentViewNum = mCurrentViewNumber;
            postReloadViewNumber = reloadViewNumber;
            postReloadPosition = reloadPosition;
        } else {
            shouldListen = false;
        }

        mScrollHelper.prepare(mCurrentViewNumber);
        mScroller.startScroll(mScrollHelper.mInitialOffset, 0, mScrollHelper.mTargetDistance, 0,
                mScrollHelper.duration);
        invalidate();
    }

    boolean shouldListen = false;
    int postCurrentViewNum;
    int postReloadViewNumber;
    PositionInfo postReloadPosition;

    private void onScrollFinished(final int targetOffset, final int referenceNumber) {
        if (mContext instanceof Activity) {
            ((Activity) mContext).runOnUiThread(mHolderRunable);
        } else {
            getHandler().removeCallbacks(mHolderRunable);
            post(mHolderRunable);
        }
    }

    private void handlePendingTasks() {
        while (mPendingTaskList.size() > 0) {
            for (pendingSetImageTask pt : mPendingTaskList) {
                ImageViewEx imageView = pt.view.mImageView;
                if (pt.url.equals((String) imageView.getTag())) {
                    pt.view.hideProcess();

                    imageView.setImageBitmap(pt.bitmap);
                    imageView.setTag(BitmapFactoryBase.IMAGE_STATE_LOADED);
                    pt.view.applyAnimation();
                }
            }

            mPendingTaskList.clear();
        }
    }

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

    public class GallaryPageView {
        private int mViewNumber;
        private FrameLayout mContainer = null;
        private ImageViewEx mImageView = null;
        private View mProcessView = null;
        private boolean mValid = false;

        public GallaryPageView(int viewNumber, FrameLayout parentLayout) {
            mViewNumber = viewNumber;
            mContainer = (FrameLayout) LayoutInflater.from(mContext).inflate(
                            R.layout.rd_image_gallary_page, null);
            mImageView = (ImageViewEx) mContainer.findViewById(R.id.rd_imageviewex);
            mProcessView = mContainer.findViewById(R.id.rd_process_container);

            mContainer.setLayoutParams(mLLParams);
            parentLayout.addView(mContainer);
        }

        public void recycleView(PositionInfo pi) {
            mImageView.setTag(BitmapFactoryBase.IMAGE_STATE_LOADED);
            mValid = true;

            if (pi == null || pi.albumIndex < 0 || pi.albumIndex > mLoadedAlbums.size() - 1) {
                mValid = false;
            } else {
                AlbumInfo ai = mLoadedAlbums.get(pi.albumIndex);
                if (ai.images == null || pi.positionInAlbum < 0
                            || pi.positionInAlbum > ai.images.size() - 1
                            || (ai.images.size() == 1 && "empty".equals(ai.images.get(0)))) {
                    mValid = false;
                }
            }

            if (pi != null && pi.albumIndex == INDEX_LOADING_MORE) {
                showProcess();
            } else {
                hideProcess();
            }

            if (!mValid) {
                mImageView.setImageBitmap(null);
                return;
            }

            String url = mLoadedAlbums.get(pi.albumIndex).images.get(pi.positionInAlbum);
            if (!TextUtils.isEmpty(url)) {
                String filePath = getFilePath(url);
                if (new File(filePath).exists()) {
                    Bitmap bitmap = BitmapHelper.decodeFile(filePath);
                    if (bitmap != null) {
                        mImageView.setImageBitmap(bitmap);
                        hideProcess();
                        return;
                    }
                }

                showProcess();
                mImageView.setImageBitmap(null);
                mImageView.setTag(url);
                downloadImage(url, mImageView);
            }
        }

        public void showProcess() {
            mProcessView.setVisibility(VISIBLE);
        }

        public void hideProcess() {
            mProcessView.setVisibility(GONE);
        }

        public void setOffset(int xOffset, int yOffset, int relativeViewNumber) {
            mContainer.scrollTo(getViewOffset(mViewNumber, relativeViewNumber) + xOffset, yOffset);
        }

        public int getCurrentOffset() {
            return mContainer.getScrollX();
        }

        public boolean isBrowsing() {
            return mImageView.isBrowsing();
        }

        private void downloadImage(final String url, final ImageView iv) {
            new AsyncTaskEx<Void, Void, Bitmap>() {
                @Override
                protected Bitmap doInBackground(Void... params) {
                    HttpClient httpClient = new DefaultHttpClient();
                    HttpUriRequest httpRequest = ImageUtils.getHttpRequestForImageDownload(mContext, url, mImageQuality, null);
                    try {
                        HttpResponse response = httpClient.execute(httpRequest);
                        if (response.getStatusLine().getStatusCode() == 200) {
                            byte[] bytes = EntityUtils.toByteArray(response.getEntity());
                            if (bytes != null && bytes.length > 0) {
                                Bitmap downloadedBitmap = BitmapHelper.decodeByteArray(bytes, 0,
                                        bytes.length);

                                if(FileUtils.isExternalStorageAvail()) {
                                    ImageUtils.writeImageDataToFile(mContext, getFilePath(url), bytes);
                                }

                                return downloadedBitmap;
                            }
                        }
                    } catch (Exception e) {
                        Utils.error(getClass(), Utils.getStackTrace(e));
                    } catch (OutOfMemoryError e) {
                        System.gc();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(final Bitmap result) {
                    if (result != null) {
                        if (mIsDragging || !mScrollerhadFinshed) {
                            pendingSetImageTask pt = new pendingSetImageTask();
                            pt.url = url;
                            pt.view = GallaryPageView.this;
                            pt.bitmap = result;
                            mPendingTaskList.add(pt);
                        } else {
                            Runnable r = new Runnable() {
                                @Override
                                public void run() {
                                    if (url.equals((String) iv.getTag())) {
                                        hideProcess();
                                        iv.setImageBitmap(result);
                                        iv.setTag(BitmapFactoryBase.IMAGE_STATE_LOADED);
                                        applyAnimation();
                                    }
                                }
                            };

                            if (mContext instanceof Activity) {
                                ((Activity) mContext).runOnUiThread(r);
                            } else {
                                getHandler().post(r);
                            }
                        }
                    }
                }

            }.execute();
        }

        private void applyAnimation() {
            if (mViewNumber == mCurrentViewNumber) {
                Animation anim = new AlphaAnimation(0.0f, 1.0f);
                anim.setDuration(300);
                mImageView.startAnimation(anim);
            }
        }

        public boolean isValid() {
            return mValid;
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

    public void setOnScrollListener(OnScrollListener listener) {
        mScrollListener = listener;
    }

    private String getFilePath(String url) {
        return Constants.LOCAL_PATH_IMAGES + "full_size_images/"
                + ArithmeticUtils.getMD5(url) + ".jpg";
    }

    public ArrayList<String> buildImageList(String images360) {
        if (TextUtils.isEmpty(images360)) {
            return EMPTY_IMAGE_LIST;
        }

        ArrayList<String> imageUrls = new ArrayList<String>();
        String[] urls = images360.split(";");
        for (String url : urls) {
            url = url.trim();
            if (TextUtils.isEmpty(url) || url.equals("*")) {
                continue;
            }

            String[] tmp_str_array = url.split("\\|");
            if (tmp_str_array == null || tmp_str_array.length == 0) {
                continue;
            } else {
                url = tmp_str_array[0];
            }

            imageUrls.add(url);
        }

        return imageUrls.size() == 0 ? EMPTY_IMAGE_LIST : imageUrls;
    }

    public interface OnOverScrollListener {
        /**
         * on start load
         *
         * @return start true<SUCCESS> Or false <FAILURE>
         */
        public boolean onStartLoad();

        public void onLoading();

        /**
         * @param position the article postion in adapter
         */
        public void onSwitched(final PositionInfo pi);
    }

    public interface OnScrollListener {
        public void onScrollStarted();

        public void onPageScrolled(String imageUrl);
    }

    private boolean mScrollerhadFinshed = false;

    @Override
    public void computeScroll() {
        if (!mScrollerhadFinshed && mScroller.computeScrollOffset()) {
            for (int viewNumber = 0; viewNumber < 3; viewNumber++) {
                // Only need to animate the visible views as the other view will
                // always be off-screen
                if ((mScrollHelper.mTargetDistance > 0 && viewNumber != mScrollHelper.rightLimit)
                                || (mScrollHelper.mTargetDistance < 0 && viewNumber != mScrollHelper.leftLimit)) {
                    mViews[viewNumber].setOffset(mScroller.getCurrX(), 0,
                            mScrollHelper.mRelativeViewNumber);
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

    public void setChangeAlbumButtons(View prev, View next, final ChangeAlbumsListener albumChangedListener) {
        mBtnPrevAlbum = prev;
        mBtnNextAlbum = next;

        OnClickListener listener = new OnClickListener() {
            @Override
            public void onClick(View paramView) {
                PositionInfo pi = null;
                if(paramView == mBtnPrevAlbum) {
                    pi = prevAlbum();
                    if(pi == null) {
                        Toast.makeText(mContext, mContext.getString(R.string.rd_no_prev_album),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else if(paramView == mBtnNextAlbum) {
                    pi = nextAlbum();
                    if(pi == null) {
                        Toast.makeText(mContext, mContext.getString(R.string.rd_no_next_album),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                if(pi != null) {
                    JumpToPosition(pi);
                    albumChangedListener.onAlbumChanged();

                    if(mViews[mCurrentViewNumber].mProcessView.getVisibility() != View.VISIBLE) {
                        Animation anim = new AlphaAnimation(0.3f, 1.0f);
                        anim.setDuration(300);
                        mViews[mCurrentViewNumber].mImageView.startAnimation(anim);
                    }
                }
            }
        };

        prev.setOnClickListener(listener);
        next.setOnClickListener(listener);
    }

    private PositionInfo nextAlbum() {
        if (mCurrentPosition.albumIndex == INDEX_LOADING_MORE) {
            return null;
        }

        if(mCurrentPosition.albumIndex == mLoadedAlbums.size() - 1) {
            if (!loadOlderAlbum()) {
                if (mNoMoreAlbums) {
                    return null;
                } else {
                    PositionInfo pi = new PositionInfo();
                    pi.albumIndex = INDEX_LOADING_MORE;
                    return pi;
                }
            }
        }

        PositionInfo pi = new PositionInfo();
        pi.albumIndex = mCurrentPosition.albumIndex + 1;
        pi.positionInAlbum = 0;
        return pi;
    }

    private PositionInfo prevAlbum() {
        int albumIndex = mCurrentPosition.albumIndex;
        PositionInfo pi = new PositionInfo();
        if (albumIndex == INDEX_LOADING_MORE) {
            pi.albumIndex = mLoadedAlbums.size() - 1;
            pi.positionInAlbum = 0;
        } else {
            if (albumIndex == 0) {
                AlbumInfo ai = loadNewerAlbum();
                if (ai == null) {
                    return null;
                }
            }
            pi.albumIndex = albumIndex - 1;
            pi.positionInAlbum = 0;
        }
        return pi;
    }

    private void JumpToPosition(PositionInfo pi) {
        if (pi == null
                || (pi.albumIndex == mCurrentPosition.albumIndex
                        && pi.positionInAlbum == mCurrentPosition.positionInAlbum)) {
            return;
        }

        mCurrentViewNumber = 0;
        mCurrentPosition = pi;

        int cursorCount = mCursor.getCount();
        AlbumInfo ai = getCurrentAlbum();
        boolean needLoadMoreAlbums = pi.albumIndex == INDEX_LOADING_MORE
                || (ai.images.size() == 1 && ai.positonOfCursor >= cursorCount - 6)
                || (ai.images.size() > 1 && ai.positonOfCursor >= cursorCount - 3);
        if (needLoadMoreAlbums && mLoadMoreAlbumsHandler != null) {
            // 需要联网下载更多图集
            mLoadMoreAlbumsHandler.loadMoreAlbums();
        }

        mViews[0].recycleView(mCurrentPosition);
        mViews[1].recycleView(getNextPosition(mCurrentPosition));
        mViews[2].recycleView(getPrevPosition(mCurrentPosition));
        mViews[0].setOffset(0, 0, mCurrentViewNumber);
        mViews[1].setOffset(0, 0, mCurrentViewNumber);
        mViews[2].setOffset(0, 0, mCurrentViewNumber);

        updateTitleAndPageNumber();
    }

    public void setTitleAndPageNumberView(TextView title, TextView pageNumber) {
        mAlbumTitleView = title;
        mPageNumberView = pageNumber;
    }

    private void updateTitleAndPageNumber() {
        if (!shouldShowTitle()) {
            ((ViewGroup) mAlbumTitleView.getParent()).setVisibility(View.INVISIBLE);
            return;
        } else if (((ViewGroup) mBtnPrevAlbum.getParent()).getVisibility() != View.VISIBLE) {
            ((ViewGroup) mAlbumTitleView.getParent()).setVisibility(View.VISIBLE);
        }

        AlbumInfo ai = getCurrentAlbum();
        mAlbumTitleView.setText(Html.fromHtml(ai.title));
        mPageNumberView.setText((mCurrentPosition.positionInAlbum + 1) + "/" + ai.images.size());
    }

    public boolean shouldShowTitle() {
        AlbumInfo ai = getCurrentAlbum();
        if (ai == null) {
            return false;
        }

        return !TextUtils.isEmpty(ai.title) && !ai.title.startsWith("我喜欢-");
    }

    public void updateChangeAlbumButtonStates() {
        if(((ViewGroup) mBtnPrevAlbum.getParent()).getVisibility() != View.VISIBLE) {
            return;
        }

        boolean btnPrevAlbumEnabled = true;
        if(mCurrentPosition.albumIndex == 0) {
            AlbumInfo ai = mLoadedAlbums.get(0);
            if (ai.positonOfCursor <= 0) {
                btnPrevAlbumEnabled = false;
            }
        }

        mBtnPrevAlbum.setEnabled(btnPrevAlbumEnabled);

        boolean btnNextAlbumEnabled = true;
        if (mCurrentPosition.albumIndex == INDEX_LOADING_MORE) {
            btnNextAlbumEnabled = false;
        } else if (mCurrentPosition.albumIndex == mLoadedAlbums.size() - 1) {
            AlbumInfo ai = mLoadedAlbums.get(mLoadedAlbums.size() - 1);
            if(ai.positonOfCursor >= mCursor.getCount() - 1 && mNoMoreAlbums) {
                btnNextAlbumEnabled = false;
            }
        }
        mBtnNextAlbum.setEnabled(btnNextAlbumEnabled);
    }

    public void setLoadMoreAlbumsHandler(LoadOldAlbumsHandler handler) {
        mLoadMoreAlbumsHandler = handler;
    }

    public void onMoreAlbumsLoaded(Cursor cursor) {
        if (cursor == null || cursor.getCount() == 0) {
            return;
        }

        mCursor = cursor;

        if(mCurrentPosition.albumIndex == INDEX_LOADING_MORE) {
            mCurrentPosition = getNextPosition(getLastPosition());
            mViews[mCurrentViewNumber].recycleView(mCurrentPosition);

            updateChangeAlbumButtonStates();
            updateTitleAndPageNumber();
        }

        GallaryPageView nextView = mViews[getNextViewNumber(mCurrentViewNumber)];
        if (!nextView.isValid()) {
            nextView.recycleView(getNextPosition(mCurrentPosition));
        }
    }

    public void onNoMoreAlbums() {
        mNoMoreAlbums = true;

        if (mCurrentPosition.albumIndex == INDEX_LOADING_MORE) {
            Toast.makeText(mContext, mContext.getString(R.string.rd_article_no_older_images),
                    Toast.LENGTH_SHORT).show();
            movePrevious();
            mViews[getNextViewNumber(mCurrentViewNumber)].recycleView(null);
        } else if (isEndOfGallery()) {
            mViews[getNextViewNumber(mCurrentViewNumber)].recycleView(null);
        }
    }

    public void onMoreAlbumsError() {
        if (mCurrentPosition.albumIndex == INDEX_LOADING_MORE) {
            Toast.makeText(mContext, "载入失败", Toast.LENGTH_SHORT).show();
            movePrevious();
        }
    }

    private boolean isEndOfGallery() {
        return mCurrentPosition.albumIndex == mLoadedAlbums.size() - 1
                && mCurrentPosition.positionInAlbum == mLoadedAlbums
                        .get(mCurrentPosition.albumIndex).images.size() - 1;
    }

    private PositionInfo getLastPosition() {
        PositionInfo pi = new PositionInfo();
        pi.albumIndex = mLoadedAlbums.size() - 1;
        pi.positionInAlbum = mLoadedAlbums.get(pi.albumIndex).images.size() - 1;
        return pi;
    }

    public void setImageQuality(int quality) {
        mImageQuality = quality;
    }
}
