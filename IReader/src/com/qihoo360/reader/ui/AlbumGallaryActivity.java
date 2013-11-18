
package com.qihoo360.reader.ui;

import com.qihoo.ilike.data.DataEntryManager;
import com.qihoo.ilike.http.core.IHttpRequest.HttpRequestStatus;
import com.qihoo.ilike.json.listener.OnILikeItPostResultListener;
import com.qihoo.ilike.manager.IlikeManager;
import com.qihoo.ilike.manager.IlikeManager.LikeType;
import com.qihoo.ilike.subscription.IlikeChannel;
import com.qihoo.ilike.vo.ErrorInfo;
import com.qihoo360.browser.hip.UXHelper;
import com.qihoo360.browser.hip.UXHelperConfig;
import com.qihoo360.reader.Constants;
import com.qihoo360.reader.R;
import com.qihoo360.reader.image.ImageDownloadStrategy;
import com.qihoo360.reader.listener.OnGetArticlesResultListener;
import com.qihoo360.reader.subscription.Channel;
import com.qihoo360.reader.support.ArithmeticUtils;
import com.qihoo360.reader.support.SystemUtils;
import com.qihoo360.reader.support.Utils;
import com.qihoo360.reader.ui.articles.ArticleUtils;
import com.qihoo360.reader.ui.view.AlbumGallaryView;
import com.qihoo360.reader.ui.view.AlbumGallaryView.ChangeAlbumsListener;
import com.qihoo360.reader.ui.view.AlbumGallaryView.LoadOldAlbumsHandler;
import com.qihoo360.reader.ui.view.AlbumGallaryView.OnScrollListener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
/**
 *  相册 图片
 */
public class AlbumGallaryActivity extends ActivityBase implements LoadOldAlbumsHandler,
        OnScrollListener, ChangeAlbumsListener {
    private class LoadOldAlbumsListener implements OnGetArticlesResultListener {
        @Override
        public void onCompletion(long getFrom, long getTo, int getCount, boolean isDeleted) {
            if (mCursor != null) {
                mCursor.close();
            }

            mCursor = mChannel.getFullCursor(getContentResolver());
            mGallaryView.onMoreAlbumsLoaded(mCursor);
            mLoadingOldAlbums = false;
        }

        @Override
        public void onFailure(int error) {
            mLoadingOldAlbums = false;

            mGallaryView.onMoreAlbumsError();
        }

        @Override
        public void onNotExists(boolean isDeleted) {
            mLoadingOldAlbums = false;
            mNoMoreAlbums = true;

            mGallaryView.onNoMoreAlbums();
        }
    }

    public static final int REQUEST_CODE_ALBUM_GALLARY = 1;

    private OnGetArticlesResultListener mLoadOldAlbumsListener = null;

    private AlbumGallaryView mGallaryView;
    private View mBottomBar = null;
    private View mTopBar = null;
    private View mTitleBar = null;

    private boolean mIgnoreSingleTap = false;
    private int mVerticleStartForSingleTap = -1;
    private int mVerticleEndForSingleTap = -1;

    private Channel mChannel;
    private Cursor mCursor;
    private boolean mCursorMayChange = false;

    private boolean mLoadingOldAlbums = false;
    private boolean mNoMoreAlbums = false;
    Handler mHandler = new Handler();

    Runnable mHideTopAndBottomBarsRunnable = new Runnable() {
        @Override
        public void run() {
            animateTopAndBottomBarsOut();
        }
    };

    static final int HIDE_TOP_AND_BOTTOM_BAR_DELAY = 6000;
    private GestureDetector mGestureDetector = new GestureDetector(
            new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    toggleTopAndBottomBars();
                    return true;
                };
            });

    private boolean mPendingRequestForMyCollection = false;
    private BroadcastReceiver mBroadcastReceiver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String channelName = getIntent().getStringExtra("channel");
        if (TextUtils.isEmpty(channelName)) {
            finish();
            return;
        }

        mChannel = Channel.get(channelName);
        if (mChannel == null) {
            finish();
            return;
        }

        int position = getIntent().getIntExtra("position", 0);
        mCursor = mChannel.getFullCursor(getContentResolver());

        setContentView(R.layout.rd_album_gallary);
        mGallaryView = (AlbumGallaryView) findViewById(R.id.rd_album_gallary_view);

        mTitleBar = findViewById(R.id.rd_album_gallary_title_bar);
        mTopBar = findViewById(R.id.rd_album_gallary_top_bar);
        mGallaryView.setChangeAlbumButtons(
                mTopBar.findViewById(R.id.rd_btn_prev_album),
                mTopBar.findViewById(R.id.rd_btn_next_album),
                this);
        mGallaryView.setTitleAndPageNumberView(
                (TextView) mTitleBar.findViewById(R.id.rd_album_gallary_title),
                (TextView) mTitleBar.findViewById(R.id.rd_album_gallary_page_number));
        mGallaryView.setLoadMoreAlbumsHandler(this);
        mGallaryView.setCursor(mCursor, position);
        mGallaryView.setOnScrollListener(this);
        if (mChannel instanceof IlikeChannel
                || Constants.IMAGE_CHANNEL_SRC_WOXIHUAN.equals(mChannel.src)) {
            mGallaryView.setImageQuality(ImageDownloadStrategy.IMAGE_CONFIG_SMALL_QUALITY);
        }
        mGallaryView.updateChangeAlbumButtonStates();
        buildBottom();
        updateLikeitBtnStatus(mGallaryView.getCurrentImageUrl());
    }

    @Override
    public void onUpdateNightMode() {
        super.onUpdateNightMode();

        setNightMask();
    }

    @Override
    public void onDestroy() {
        if (mCursor != null) {
            mCursor.close();
        }

        if(mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
            mBroadcastReceiver = null;
        }

        super.onDestroy();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        if (action == MotionEvent.ACTION_DOWN
                && mTopBar.getVisibility() == View.VISIBLE
                && mBottomBar.getVisibility() == View.VISIBLE) {
            if (mVerticleStartForSingleTap < 0 || mVerticleEndForSingleTap < 0) {
                mVerticleStartForSingleTap = mTopBar.getBottom();
                mVerticleEndForSingleTap = mBottomBar.getTop();
            }

            int y = (int) ev.getY();
            if (y <= mVerticleStartForSingleTap || y >= mVerticleEndForSingleTap) {
                mIgnoreSingleTap = true;
                mHandler.removeCallbacks(mHideTopAndBottomBarsRunnable);
                mHandler.postDelayed(mHideTopAndBottomBarsRunnable,
                        HIDE_TOP_AND_BOTTOM_BAR_DELAY);
            }
        } else if (action == MotionEvent.ACTION_UP) {
            mIgnoreSingleTap = false;
        }

        if (!mIgnoreSingleTap) {
            mGestureDetector.onTouchEvent(ev);
        }

        return super.dispatchTouchEvent(ev);
    }

    private void buildBottom() {
        mBottomBar = findViewById(R.id.rd_album_gallary_bottom_bar);
        mBottomBar.findViewById(R.id.menu_back).setOnClickListener(mListener);
        mBottomBar.findViewById(R.id.menu_share_btn).setOnClickListener(mListener);
        mBottomBar.findViewById(R.id.menu_download_btn).setOnClickListener(mListener);
        mBottomBar.findViewById(R.id.menu_likeit_btn).setOnClickListener(mListener);
    }

    private OnClickListener mListener = new OnClickListener() {

        @Override
        public void onClick(View v) {

            if (v.getId() == R.id.menu_back) {
                onBackPressed();
            } else if (v.getId() == R.id.menu_share_btn) {
                UXHelper.getInstance().addActionRecord(
                        UXHelperConfig.Reader_Article_Detail_Share_Image, 1);

                String filePath = getFilePath(mGallaryView.getCurrentImageUrl());
                if (!TextUtils.isEmpty(filePath)) {
                    CommonUtil.shareImage(AlbumGallaryActivity.this, filePath, "“" + mChannel.title
                            + "”频道");
                }
            } else if (v.getId() == R.id.menu_likeit_btn) {
                handleLikedBtnClicked();
            } else if (v.getId() == R.id.menu_download_btn) {
                UXHelper.getInstance().addActionRecord(
                        UXHelperConfig.Reader_Article_Detail_Save_Image, 1);
                String newFilePath = ensureSaveImageDir()
                        + ArithmeticUtils.getMD5(mGallaryView.getCurrentImageUrl())
                                + ".jpg";
                try {
                    if (!new File(newFilePath).exists()) {
                        InputStream is = new FileInputStream(
                                getFilePath(mGallaryView.getCurrentImageUrl()));
                        OutputStream os = new FileOutputStream(newFilePath);
                        byte[] b = new byte[4098];
                        int length;
                        while ((length = is.read(b)) > 0) {
                            os.write(b, 0, length);
                        }
                        is.close();
                        os.close();
                    }
                    SystemUtils.refreshSystemMedia();
                    Toast.makeText(AlbumGallaryActivity.this, "图片已成功保存至相册", Toast.LENGTH_SHORT)
                            .show();
                } catch (Exception e) {
                    Utils.error(getClass(), Utils.getStackTrace(e));

                    Toast.makeText(AlbumGallaryActivity.this, "图片保存失败", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    private String getFilePath(String url) {
        return Constants.LOCAL_PATH_IMAGES + "full_size_images/" + ArithmeticUtils.getMD5(url)
                + ".jpg";
    }

    protected String ensureSaveImageDir() {
        String dirPath = Constants.LOCAL_PATH_IMAGES + "qihoo_reader_download/";
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        return dirPath;
    }

    @Override
    public boolean loadMoreAlbums() {
        if (mNoMoreAlbums) {
            return false;
        }

        loadOldAlbums();
        return true;
    }

    private void loadOldAlbums() {
        if (!mLoadingOldAlbums) {
            if (mLoadOldAlbumsListener == null) {
                mLoadOldAlbumsListener = new LoadOldAlbumsListener();
            }

            mChannel.getOldArticlesAsync(getContentResolver(), mLoadOldAlbumsListener, 30, false);
            mLoadingOldAlbums = true;
            mCursorMayChange = true;
        }
    }

    @Override
    public void finish() {
        if (mLoadingOldAlbums) {
            mChannel.stopGet();
        }

        Intent data = new Intent();
        data.putExtra(ArticleUtils.DETAIL_POSITION, mGallaryView.getCurrentCursorPosition());
        data.putExtra(ArticleUtils.DETAIL_LOADED, mCursorMayChange);
        setResult(REQUEST_CODE_ALBUM_GALLARY, data);
        super.finish();
    }

    private void toggleTopAndBottomBars() {
        mHandler.removeCallbacks(mHideTopAndBottomBarsRunnable);

        if (mBottomBar.getVisibility() == View.VISIBLE
                || mTopBar.getVisibility() == View.VISIBLE) {
            animateTopAndBottomBarsOut();
        } else {
            animateTopAndBottomBarsIn();
            mHandler.postDelayed(mHideTopAndBottomBarsRunnable,
                    HIDE_TOP_AND_BOTTOM_BAR_DELAY);

            mGallaryView.updateChangeAlbumButtonStates();
        }
    }

    @Override
    public void onScrollStarted() {
        mHandler.removeCallbacks(mHideTopAndBottomBarsRunnable);
        if(mBottomBar.getVisibility() == View.VISIBLE
                || mTopBar.getVisibility() == View.VISIBLE) {
            animateTopAndBottomBarsOut();
            animateTitleBar(true);
        }
    }

    @Override
    public void onPageScrolled(String imageUrl) {
        updateLikeitBtnStatus(imageUrl);
    }

    private void animateTopAndBottomBarsIn() {
        animateTitleBar(false);

        if(mBottomBar.getVisibility() == View.VISIBLE
                && mTopBar.getVisibility() == View.VISIBLE) {
            return;
        }

        // Bottom bar
        mBottomBar.setVisibility(View.VISIBLE);

        AnimationSet animSet = new AnimationSet(true);
        Animation tranAnim = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.0f);
        Animation alphaAnim = new AlphaAnimation(0.0f, 1.0f);
        animSet.addAnimation(tranAnim);
        animSet.addAnimation(alphaAnim);
        animSet.setDuration(200);
        animSet.setInterpolator(new DecelerateInterpolator());
        mBottomBar.startAnimation(animSet);

        // Top bar
        mTopBar.setVisibility(View.VISIBLE);
        animSet = new AnimationSet(true);
        tranAnim = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, -1.0f,
                Animation.RELATIVE_TO_SELF, 0.0f);
        animSet.addAnimation(tranAnim);
        animSet.addAnimation(alphaAnim);
        animSet.setDuration(200);
        animSet.setInterpolator(new DecelerateInterpolator());
        mTopBar.startAnimation(animSet);
    }

    private void animateTopAndBottomBarsOut() {
        if(mBottomBar.getVisibility() != View.VISIBLE
                && mTopBar.getVisibility() != View.VISIBLE) {
            return;
        }

        // Bottom bar
        AnimationSet animSet = new AnimationSet(true);

        Animation tranAnim = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 1.0f);
        Animation alphaAnim = new AlphaAnimation(1.0f, 0.0f);
        animSet.addAnimation(tranAnim);
        animSet.addAnimation(alphaAnim);
        animSet.setDuration(200);
        animSet.setInterpolator(new AccelerateInterpolator());
        animSet.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mBottomBar.setVisibility(View.INVISIBLE);
            }
        });

        mBottomBar.startAnimation(animSet);

        // Top bar
        animSet = new AnimationSet(true);
        tranAnim = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, -1.0f);
        animSet.addAnimation(tranAnim);
        animSet.addAnimation(alphaAnim);
        animSet.setDuration(200);
        animSet.setInterpolator(new AccelerateInterpolator());
        animSet.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mTopBar.setVisibility(View.INVISIBLE);
            }
        });

        mTopBar.startAnimation(animSet);

        animateTitleBar(true);
    }

    private void animateTitleBar(final boolean visible) {
        if(!mGallaryView.shouldShowTitle()) {
            return;
        }

        int visibility = mTitleBar.getVisibility();
        if((visibility == View.VISIBLE && visible)
                || (visibility != View.VISIBLE && !visible)) {
            return;
        }

        Animation anim = null;
        if(visible) {
            anim = new AlphaAnimation(0.0f, 1.0f);
        } else {
            anim = new AlphaAnimation(1.0f, 0.0f);
        }
        anim.setDuration(200);
        anim.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mTitleBar.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
            }
        });

        mTitleBar.startAnimation(anim);
    }

    @Override
    public void onAlbumChanged() {
        animateTopAndBottomBarsOut();
    }

    private void updateLikeitBtnStatus(String url) {
        ImageView imageView = (ImageView) findViewById(R.id.menu_likeit_btn);
        imageView.clearAnimation();
        if (!TextUtils.isEmpty(url) && DataEntryManager.urlLiked(getContentResolver(), url)) {
            imageView.setImageResource(R.drawable.ilike_likeit_btn_liked);
        } else {
            imageView.setImageResource(R.drawable.ilike_likeit_btn_normal);
        }
    }

    private void handleLikedBtnClicked() {
        if(!com.qihoo.ilike.util.Utils.accountConfigured(this)) {
            startWaitingForAccountConfiguration();
            return;
        }

        likeCurrentImage();
    }

    private void likeCurrentImage() {
        mHandler.removeCallbacks(mHideTopAndBottomBarsRunnable);
        mTopBar.setVisibility(View.VISIBLE);
        mBottomBar.setVisibility(View.VISIBLE);
        mHandler.postDelayed(mHideTopAndBottomBarsRunnable, HIDE_TOP_AND_BOTTOM_BAR_DELAY);

        String url = mGallaryView.getCurrentImageUrl();
        if(TextUtils.isEmpty(url)) {
            return;
        }

        String title = mGallaryView.getCurrentAlbumName();

        if (!com.qihoo.ilike.util.Utils.checkNetworkStatusBeforeLike(AlbumGallaryActivity.this)) {
            return;
        }

        boolean success = IlikeManager.likeUrl(getContentResolver(),
                url, title, LikeType.Image,
                new OnILikeItPostResultListener() {
                    @Override
                    public void onResponseError(ErrorInfo errorInfo) {
                        Toast.makeText(AlbumGallaryActivity.this,
                                R.string.ilike_collect_url_failure,
                                Toast.LENGTH_LONG).show();
                        updateLikeitBtnStatus(mGallaryView.getCurrentImageUrl());
                    }

                    @Override
                    public void onRequestFailure(HttpRequestStatus errorStatus) {
                        Toast.makeText(AlbumGallaryActivity.this,
                                R.string.ilike_collect_url_failure,
                                Toast.LENGTH_LONG).show();
                        updateLikeitBtnStatus(mGallaryView.getCurrentImageUrl());
                    }

                    @Override
                    public void onComplete() {
                        Toast.makeText(AlbumGallaryActivity.this,
                                R.string.ilike_collect_url_successful,
                                Toast.LENGTH_SHORT).show();
                    }
                });

        if (success) {
            changeLikedBtnStatus();
        }
    }


    private void startWaitingForAccountConfiguration() {
        mPendingRequestForMyCollection = true;

        if (mBroadcastReceiver == null) {
            mBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (com.qihoo.ilike.util.Utils.accountConfigSucceed(intent)) {
                        if (mPendingRequestForMyCollection) {
                            likeCurrentImage();
                            mPendingRequestForMyCollection = false;
                        }
                    } else {
                        mPendingRequestForMyCollection = false;
                    }
                }
            };

            IntentFilter filter = com.qihoo.ilike.util.Utils.getAccountConfigResultFilter();
            registerReceiver(mBroadcastReceiver, filter);
        }
    }


    protected void changeLikedBtnStatus() {
        final ImageView likedBtn = (ImageView) findViewById(R.id.menu_likeit_btn);
        Animation animOut = new AlphaAnimation(1.0f, 0.0f);
        animOut.setDuration(300);
        likedBtn.startAnimation(animOut);
        animOut.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                likedBtn.setImageResource(R.drawable.ilike_likeit_btn_liked);
                Animation animIn = new AlphaAnimation(0.0f, 1.0f);
                animIn.setDuration(300);
                likedBtn.startAnimation(animIn);
            }
        });
    }
}
