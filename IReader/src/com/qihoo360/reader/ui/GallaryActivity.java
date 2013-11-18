
package com.qihoo360.reader.ui;

import com.qihoo.ilike.data.DataEntryManager;
import com.qihoo.ilike.http.core.IHttpRequest.HttpRequestStatus;
import com.qihoo.ilike.json.listener.OnILikeItPostResultListener;
import com.qihoo.ilike.manager.IlikeManager;
import com.qihoo.ilike.manager.IlikeManager.LikeType;
import com.qihoo.ilike.vo.ErrorInfo;
import com.qihoo360.browser.hip.UXHelper;
import com.qihoo360.browser.hip.UXHelperConfig;
import com.qihoo360.browser.hip.UXKey;
import com.qihoo360.reader.Constants;
import com.qihoo360.reader.R;
import com.qihoo360.reader.support.ArithmeticUtils;
import com.qihoo360.reader.support.FileUtils;
import com.qihoo360.reader.support.SystemUtils;
import com.qihoo360.reader.support.Utils;
import com.qihoo360.reader.ui.view.ImageGallaryView;
import com.qihoo360.reader.ui.view.ImageGallaryView.OnScrollListener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class GallaryActivity extends ActivityBase implements OnScrollListener {
    private ImageGallaryView mGallaryView;

    private boolean mIgnoreSingleTap = false;
    private int mVerticleEndForSingleTap = -1;
    private boolean mIsLikedArticle = false;
    private View mBottomBar = null;
    Runnable mHideBottomBarsRunnable = new Runnable() {
        @Override
        public void run() {
            animateBottomBarOut();
        }
    };

    static final int HIDE_TOP_AND_BOTTOM_BAR_DELAY = 6000;
    private GestureDetector mGestureDetector = new GestureDetector(
            new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    toggleBottomBar();
                    return true;
                };
            });

    Handler mHandler = new Handler();

    private boolean mPendingRequestForMyCollection = false;
    private BroadcastReceiver mBroadcastReceiver = null;
    private String mCategory = null;

    private boolean isFromWoXiHuan() {
        return getString(R.string.i_like_share_category).equals(mCategory);
    }

    @Override
    protected UXKey getPvHipId() {
        return isFromWoXiHuan() ? UXHelperConfig.WoXiHuan_PV_Times
                : UXHelperConfig.Reader_PV_Times;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        mCategory = intent.getStringExtra("category");

        super.onCreate(savedInstanceState);
        
        if (!FileUtils.isExternalStorageAvail()) { // SD卡是否可用
            Toast.makeText(this, R.string.rd_sdcard_not_available, Toast.LENGTH_LONG).show();
            finish();
            return;
        }


        String images360 = intent.getStringExtra("images360");
        if (TextUtils.isEmpty(images360)) {
            finish();
        }

        setContentView(R.layout.rd_gallary_option);
        String curUrl = intent.getStringExtra("cur_url");
        mIsLikedArticle = intent.getBooleanExtra("liked_article", false);

        mGallaryView = (ImageGallaryView) findViewById(R.id.image_gallary_view);
        mGallaryView.setOnScrollListener(this);
        mGallaryView.setImageData(ImageGallaryView.buildImageList(this, images360), curUrl);
        buildBottom();
        updateLikeitBtnStatus(curUrl);
    }

    private void buildBottom() {
        mBottomBar = findViewById(R.id.rd_image_gallary_bottom_bar);
        mBottomBar.findViewById(R.id.menu_back).setOnClickListener(mListener);
        mBottomBar.findViewById(R.id.menu_share_btn).setOnClickListener(mListener);
        mBottomBar.findViewById(R.id.menu_download_btn).setOnClickListener(mListener);
        mBottomBar.findViewById(R.id.menu_likeit_btn).setOnClickListener(mListener);
    }

    private OnClickListener mListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            /* 返回 */
            if (v.getId() == R.id.menu_back) {
                UXHelper.getInstance().addActionRecord(
                        UXHelperConfig.Reader_Article_Detail_Back_Key_OnClick, 1);
                onBackPressed();
            } else if (v.getId() == R.id.menu_share_btn) { /*  分享 */
                UXHelper.getInstance().addActionRecord(
                        UXHelperConfig.Reader_Article_Detail_Share_Image, 1);
                CommonUtil.shareImage(GallaryActivity.this,
                        getFilePath(mGallaryView.getCurrentImageUrl()), mCategory);
            } else if (v.getId() == R.id.menu_likeit_btn) { /* 喜欢 */
                handleLikedBtnClicked();
            } else if (v.getId() == R.id.menu_download_btn) { /*  下载 */
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
                    Toast.makeText(GallaryActivity.this, "图片已成功保存至相册", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Utils.error(getClass(), Utils.getStackTrace(e));

                    Toast.makeText(GallaryActivity.this, "图片保存失败", Toast.LENGTH_SHORT).show();
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
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        if (action == MotionEvent.ACTION_DOWN
                && mBottomBar.getVisibility() == View.VISIBLE) {
            if (mVerticleEndForSingleTap < 0) {
                mVerticleEndForSingleTap = mBottomBar.getTop();
            }

            int y = (int) ev.getY();
            if (y >= mVerticleEndForSingleTap) {
                mIgnoreSingleTap = true;
                mHandler.removeCallbacks(mHideBottomBarsRunnable);
                mHandler.postDelayed(mHideBottomBarsRunnable,
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

    private void toggleBottomBar() {
        mHandler.removeCallbacks(mHideBottomBarsRunnable);

        if (mBottomBar.getVisibility() == View.VISIBLE) {
            animateBottomBarOut();
        } else {
            animateBottomBarIn();
            mHandler.postDelayed(mHideBottomBarsRunnable, HIDE_TOP_AND_BOTTOM_BAR_DELAY);
        }
    }

    @Override
    public void onScrollStarted() {
        mHandler.removeCallbacks(mHideBottomBarsRunnable);
        mBottomBar.setVisibility(View.INVISIBLE);
    }


    private void animateBottomBarIn() {
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
    }

    private void animateBottomBarOut() {
        AnimationSet animSet = new AnimationSet(true); //   设置多个动画
        /**
         * 移动
         */
        Animation tranAnim = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 1.0f);
        Animation alphaAnim = new AlphaAnimation(1.0f, 0.0f); //透明
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
    }

    @Override
    public void onPageScrolled(String imageUrl) {
        updateLikeitBtnStatus(imageUrl);
    }

    private void updateLikeitBtnStatus(String url) {
        ImageView imageView = (ImageView) findViewById(R.id.menu_likeit_btn);
        imageView.clearAnimation();
        if (mIsLikedArticle
                || (!TextUtils.isEmpty(url) && DataEntryManager.urlLiked(getContentResolver(), url))) {
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
        mHandler.removeCallbacks(mHideBottomBarsRunnable);
        mBottomBar.setVisibility(View.VISIBLE);
        mHandler.postDelayed(mHideBottomBarsRunnable, HIDE_TOP_AND_BOTTOM_BAR_DELAY);

        if (!com.qihoo.ilike.util.Utils.checkNetworkStatusBeforeLike(GallaryActivity.this)) {
            return;
        }

        boolean success = (!mIsLikedArticle) && IlikeManager.likeUrl(getContentResolver(),
                mGallaryView.getCurrentImageUrl(), getIntent().getStringExtra("title"), LikeType.Image,
                new OnILikeItPostResultListener() {
                    @Override
                            public void onResponseError(ErrorInfo errorInfo) {
                                Toast.makeText(
                                        GallaryActivity.this,
                                        isFromWoXiHuan() ? R.string.ilike_collect_failure
                                                : R.string.ilike_collect_url_failure,
                                        Toast.LENGTH_LONG).show();
                                updateLikeitBtnStatus(mGallaryView.getCurrentImageUrl());
                            }

                    @Override
                    public void onRequestFailure(HttpRequestStatus errorStatus) {
                        Toast.makeText(
                                GallaryActivity.this,
                                isFromWoXiHuan() ? R.string.ilike_collect_failure
                                        : R.string.ilike_collect_url_failure,
                                Toast.LENGTH_LONG).show();
                        updateLikeitBtnStatus(mGallaryView.getCurrentImageUrl());
                    }

                    @Override
                    public void onComplete() {
                        Toast.makeText(
                                GallaryActivity.this,
                                isFromWoXiHuan() ? R.string.ilike_collect_successful
                                        : R.string.ilike_collect_url_successful,
                                Toast.LENGTH_LONG).show();
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
    /** 改变喜欢按钮的状态 */
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

    @Override
    protected void onDestroy() {
        if(mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
            mBroadcastReceiver = null;
        }
        super.onDestroy();
    }
}
