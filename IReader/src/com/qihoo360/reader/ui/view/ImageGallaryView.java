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
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Scroller;
import android.widget.Toast;

import com.qihoo360.reader.Constants;
import com.qihoo360.reader.R;
import com.qihoo360.reader.image.BitmapFactoryBase;
import com.qihoo360.reader.image.BitmapHelper;
import com.qihoo360.reader.image.ImageDownloadStrategy;
import com.qihoo360.reader.image.ImageUtils;
import com.qihoo360.reader.support.ArithmeticUtils;
import com.qihoo360.reader.support.AsyncTaskEx;
import com.qihoo360.reader.support.FileUtils;
import com.qihoo360.reader.support.Utils;

public class ImageGallaryView extends FrameLayout {
    private int swipe_min_distance = 120;
    private int mViewPaddingWidth = 0;
    private float mSnapBorderRatio = 0.80f;
    private boolean mIsGalleryCircular = false;
    private OnScrollListener mScrollListener;

    private Scroller mScroller;

    private int mGalleryWidth = 0;
    private boolean mIsTouched = false;
    private boolean mIsDragging = false;
    private int mFlingDirection = 0;
    private int mCurrentPosition = 0;
    private int mCurrentViewNumber = 0;

    private int lastDownXPos;
    private int lastDownYPos;
    private int addUpXOffset;
    private int mTouchSlop;
    private static final int TOUCH_STATE_REST = 0;
    private static final int TOUCH_STATE_SCROLLING = 1;
    private int mTouchState = TOUCH_STATE_REST;

    private Context mContext;
    private ArrayList<String> mImages;
    private GallaryPageView[] mViews;
    private ScrollHelper mScrollHelper;
    private VelocityTracker mVelocityTracker;
    private final int MAX_FLING_VELOCITY = 1000;
    private LinearLayout.LayoutParams mLLParams;
    private Runnable mHolderRunable = null;

    private static class pendingSetImageTask {
        String url = "";;
        GallaryPageView view = null;
        Bitmap bitmap = null;
    }

    CopyOnWriteArrayList<pendingSetImageTask> mPendingTaskList = new CopyOnWriteArrayList<pendingSetImageTask>();

    public ImageGallaryView(Context context) {
        this(context, null);
    }

    public ImageGallaryView(Context context, AttributeSet attrs) {
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
                    mViews[postReloadViewNumber]
                            .recycleView(postReloadPosition);

                    if (mScrollListener != null) {
                        mScrollListener.onPageScrolled(mImages
                                .get(mCurrentPosition));
                    }
                }

                handlePendingTasks();
            }
        };
    }

    public void setPaddingWidth(int viewPaddingWidth) {
        mViewPaddingWidth = viewPaddingWidth;
    }

    public void setSnapBorderRatio(float snapBorderRatio) {
        mSnapBorderRatio = snapBorderRatio;
    }

    public final int getPosition() {
        return mCurrentPosition;
    }

    public final String getCurrentImageUrl() {
        return mImages.get(mCurrentPosition);
    }

    public final int getGalleryCount() {
        return (mImages == null) ? 0 : mImages.size();
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
    protected void onLayout(boolean changed, int left, int top, int right,
            int bottom) {
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

    public void setImageData(ArrayList<String> images, String curUrl) {
        mCurrentViewNumber = 0;
        mImages = images;

        int position = 0;
        if (!TextUtils.isEmpty(curUrl)) {
            for (int i = 0; i < images.size(); i++) {
                String url = images.get(i);
                if (curUrl.equals(url)) {
                    position = i;
                    break;
                }
            }
        }
        mCurrentPosition = position;

        if (mCurrentPosition < 0)
            mCurrentPosition = 0;
        else if (mCurrentPosition > mImages.size() - 1) {
            mCurrentPosition = mImages.size() - 1;
        }

        resetPages();
    }

    protected void resetPages() {
        mViews[0].recycleView(mCurrentPosition);
        mViews[1].recycleView(getNextPosition(mCurrentPosition));
        mViews[2].recycleView(getPrevPosition(mCurrentPosition));
        mViews[0].setOffset(0, 0, mCurrentViewNumber);
        mViews[1].setOffset(0, 0, mCurrentViewNumber);
        mViews[2].setOffset(0, 0, mCurrentViewNumber);
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
        if (!mScroller.isFinished())
            return;
        mFlingDirection = 1;
        processGesture();
    }

    public void moveNext() {
        // Slide to next view
        if (!mScroller.isFinished())
            return;
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

        if ((action == MotionEvent.ACTION_MOVE)
                && (mTouchState != TOUCH_STATE_REST)) {
            return true;
        }
        final float x = ev.getX();
        final float y = ev.getY();
        switch (action) {
        case MotionEvent.ACTION_DOWN:
            lastDownXPos = (int) x;
            lastDownYPos = (int) y;
            mTouchState = mScroller.isFinished() ? TOUCH_STATE_REST
                    : TOUCH_STATE_SCROLLING;
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
        int reloadPosition = 0;

        mIsTouched = false;
        mIsDragging = false;

        if (mFlingDirection > 0) {
            if (mCurrentPosition > 0 || mIsGalleryCircular == true) {
                // Determine previous view and outgoing view to recycle
                newViewNumber = getPrevViewNumber(mCurrentViewNumber);
                mCurrentPosition = getPrevPosition(mCurrentPosition);
                reloadViewNumber = getNextViewNumber(mCurrentViewNumber);
                reloadPosition = getPrevPosition(mCurrentPosition);
            } else {
                Toast.makeText(
                        mContext,
                        mContext.getString(R.string.rd_article_no_newer_images),
                        Toast.LENGTH_SHORT).show();
            }
        } else if (mFlingDirection < 0) {
            if (mCurrentPosition < getLastPosition()
                    || mIsGalleryCircular == true) {
                // Determine the next view and outgoing view to recycle
                newViewNumber = getNextViewNumber(mCurrentViewNumber);
                mCurrentPosition = getNextPosition(mCurrentPosition);
                reloadViewNumber = getPrevViewNumber(mCurrentViewNumber);
                reloadPosition = getNextPosition(mCurrentPosition);
            } else {
                Toast.makeText(
                        mContext,
                        mContext.getString(R.string.rd_article_no_older_images),
                        Toast.LENGTH_SHORT).show();
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
        mScroller.startScroll(mScrollHelper.mInitialOffset, 0,
                mScrollHelper.mTargetDistance, 0, mScrollHelper.duration);
        invalidate();
    }

    boolean shouldListen = false;
    int postCurrentViewNum;
    int postReloadViewNumber;
    int postReloadPosition;

    private void onScrollFinished(final int targetOffset,
            final int referenceNumber) {
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

        public GallaryPageView(int viewNumber, FrameLayout parentLayout) {
            mViewNumber = viewNumber;
            mContainer = (FrameLayout) LayoutInflater.from(mContext).inflate(
                    R.layout.rd_image_gallary_page, null);
            mImageView = (ImageViewEx) mContainer
                    .findViewById(R.id.rd_imageviewex);
            mProcessView = mContainer.findViewById(R.id.rd_process_container);

            mContainer.setLayoutParams(mLLParams);
            parentLayout.addView(mContainer);
        }

        public void recycleView(int newPosition) {
            if (newPosition < 0 || newPosition >= getGalleryCount()) {
                mImageView.setImageBitmap(null);
                return;
            }

            String url = mImages.get(newPosition);
            if (!TextUtils.isEmpty(url)) {
                String filePath = getFilePath(url);
                // 图片存在
                if (new File(filePath).exists()) {
                    Bitmap bitmap = BitmapHelper.decodeFile(filePath);
                    if (bitmap != null) {
                        mImageView.setImageBitmap(bitmap);
                        mImageView.setTag(BitmapFactoryBase.IMAGE_STATE_LOADED);
                        hideProcess();
                        return;
                    }
                }
                
                showProcess(); //  进度条
                mImageView.setImageBitmap(null);
                mImageView.setTag(url);
                downloadImage(url, mImageView); //  下载图片
            }
        }

        public void showProcess() {
            mProcessView.setVisibility(VISIBLE);
        }

        public void hideProcess() {
            mProcessView.setVisibility(GONE);
        }
        
        public void setOffset(int xOffset, int yOffset, int relativeViewNumber) {
            mContainer.scrollTo(getViewOffset(mViewNumber, relativeViewNumber) // 相对
                    + xOffset, yOffset);
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
                    String fullUrl = url;
                    if (!TextUtils.isEmpty(fullUrl)
                            && (fullUrl.contains("qhimg.com") || fullUrl
                                    .contains("xihuan"))) {
                        int pos = fullUrl.lastIndexOf("/");
                        if (pos > 0) {
                            String config = ImageDownloadStrategy.getInstance(
                                    mContext).getImageGalleryConfiguration(
                                    mContext);
                            fullUrl = fullUrl.substring(0, pos) + config
                                    + fullUrl.substring(pos);
                        }
                    }

                    HttpClient httpClient = new DefaultHttpClient();
                    HttpUriRequest httpRequest = ImageUtils
                            .getHttpRequestForImageDownload(fullUrl);
                    try {
                        HttpResponse response = httpClient.execute(httpRequest);
                        if (response.getStatusLine().getStatusCode() == 200) {
                            byte[] bytes = EntityUtils.toByteArray(response
                                    .getEntity());
                            if (bytes != null && bytes.length > 0) {
                                Bitmap downloadedBitmap = BitmapHelper
                                        .decodeByteArray(bytes, 0, bytes.length);

                                if (FileUtils.isExternalStorageAvail()) {
                                    ImageUtils.writeImageDataToFile(mContext,
                                            getFilePath(url), bytes);
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
                                ((Activity) mContext).runOnUiThread(r); // 更新UI
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
        /**
         *  准备
         */
        public void prepare(int relativeViewNumber) {
            // If we are animating relative to a new view
            if (mRelativeViewNumber != relativeViewNumber) {
                mRelativeViewNumber = relativeViewNumber;
            }

            mInitialOffset = mViews[mRelativeViewNumber].getCurrentOffset();
            mTargetOffset = getViewOffset(mRelativeViewNumber,
                    mRelativeViewNumber);
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

    public static ArrayList<String> buildImageList(Context context,
            String images360) {
        if (TextUtils.isEmpty(images360)) {
            return null;
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

        return imageUrls;
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
        } else if (!mScrollerhadFinshed) { // 计算
            mViews[0].setOffset(mScrollHelper.mTargetOffset, 0,
                    mScrollHelper.mRelativeViewNumber);
            mViews[1].setOffset(mScrollHelper.mTargetOffset, 0,
                    mScrollHelper.mRelativeViewNumber);
            mViews[2].setOffset(mScrollHelper.mTargetOffset, 0,
                    mScrollHelper.mRelativeViewNumber);
            onScrollFinished(mScrollHelper.mTargetOffset,
                    mScrollHelper.mRelativeViewNumber);
            mScrollerhadFinshed = true;
        }
    }
}
