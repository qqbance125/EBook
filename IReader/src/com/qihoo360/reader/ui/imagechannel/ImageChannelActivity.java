
package com.qihoo360.reader.ui.imagechannel;

import com.qihoo.ilike.subscription.IlikeChannel;
import com.qihoo.ilike.ui.ILikeArticleDetailActivity;
import com.qihoo360.browser.hip.CollectSubScribe;
import com.qihoo360.browser.hip.UXHelper;
import com.qihoo360.browser.hip.UXHelperConfig;
import com.qihoo360.reader.Constants;
import com.qihoo360.reader.R;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.data.Tables.Articles;
import com.qihoo360.reader.image.ImageDownloadStrategy;
import com.qihoo360.reader.listener.OnGetArticlesResultListener;
import com.qihoo360.reader.offline.OfflineTask;
import com.qihoo360.reader.subscription.Channel;
import com.qihoo360.reader.subscription.SubscribedChannel;
import com.qihoo360.reader.subscription.reader.RssChannel;
import com.qihoo360.reader.subscription.reader.RssSubscribedChannel;
import com.qihoo360.reader.support.FileUtils;
import com.qihoo360.reader.support.NetUtils;
import com.qihoo360.reader.support.Utils;
import com.qihoo360.reader.ui.ActivityBase;
import com.qihoo360.reader.ui.AlbumGallaryActivity;
import com.qihoo360.reader.ui.CommonUtil;
import com.qihoo360.reader.ui.CustomProgressDialog;
import com.qihoo360.reader.ui.ReaderMenuContainer;
import com.qihoo360.reader.ui.ReaderPlugin;
import com.qihoo360.reader.ui.articles.ArticleListView;
import com.qihoo360.reader.ui.articles.ArticleListView.OnRequestListener;
import com.qihoo360.reader.ui.articles.ArticleListView.OnSizeChangedListener;
import com.qihoo360.reader.ui.articles.ArticleUtils;
import com.qihoo360.reader.ui.imagechannel.ImageChannelVerticleListAdapter.OnAlbumClickListener;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Random;

public class ImageChannelActivity extends ActivityBase implements
        OnRequestListener, OnAlbumClickListener, OnItemClickListener,
        OnSizeChangedListener {
    private class RefreshListener implements OnGetArticlesResultListener {
        @Override
        public void onCompletion(long getFrom, long getTo, int getCount, boolean isDeleted) {
            Cursor oldCursor = mCursor;
            updateCursor();
            if (mListerAdapter != null) {
                mListerAdapter.updateCursor(mCursor, false);
                mListerAdapter.resetPages();
            } else {
                mListerAdapter = new ImageChannelVerticleListAdapter(
                        ImageChannelActivity.this, mCursor, mChannel.getAlbumCoverDataFieldName(),
                        ImageChannelActivity.this);
                if(mPageHeight > 0 && mPageWidth > 0) {
                    mListerAdapter.setPageHeight(mPageWidth, mPageHeight);
                }
                setPageRect();
                mListView.setAdapter(mListerAdapter);
                if (mChannel instanceof IlikeChannel
                        || Constants.IMAGE_CHANNEL_SRC_WOXIHUAN.equals(mChannel.src)) {
                    mListerAdapter.setImageQuality(ImageDownloadStrategy.IMAGE_CONFIG_SMALL_QUALITY);
                }
            }

            if (oldCursor != null && !oldCursor.isClosed()) {
                oldCursor.close();
            }

            postUpdateImages(ImageChannelVerticleListAdapter.PRELOAD_STRATEGY_FORWARD, 200);

            mListView.onCompletion(ArticleUtils.SUCCESS, getCount, true);
            if(isDeleted) {
                mListView.resetLastRequireState();
                mNoMoreAlbums = false;
            }

            String tipContent = "更新了 " + getCount + " 个图集";
            updateRefreshTime(-1);

            int count = mCursor.getCount();
            if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
                mLoadingDialog.dismiss();

                if (count > 0 && !showFullScreenTip()) {
                    showSubscribeTips();
                }

                mNoCache = (count <= 0);
                Toast.makeText(getApplicationContext(), tipContent, Toast.LENGTH_SHORT).show();
            } else {
                showTopBarTip(tipContent);
            }

            if(count > 0) {
                if (mEmptyTipsShowing) {
                    findViewById(R.id.rd_image_channel_empty).setVisibility(View.GONE);
                    mEmptyTipsShowing = false;
                }
                mListView.setVisibility(View.VISIBLE);
            } else {
                showEmptyTips(ArticleUtils.NOEXIST);
                mListView.setVisibility(View.INVISIBLE);
            }

            mRefreshing = false;
        }

        @Override
        public void onFailure(int error) {
            if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
                mLoadingDialog.dismiss();
            }

            mRefreshing = false;

            if (mCursor.getCount() <= 0 && mNoCache) {
                showEmptyTips(ArticleUtils.FAILURE);
                return;
            } else {
                updateRefreshTime(-1);
                showTopBarTip(getString(R.string.rd_article_network_expception));
            }

            mListView.onCompletion(ArticleUtils.FAILURE, -1);

            if (!mAllPagesLoaded) {
                // mImageChannelView.loadLeftAndRightPages();
                mAllPagesLoaded = true;
            }
        }

        @Override
        public void onNotExists(boolean isDeleted) {
            if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
                mLoadingDialog.dismiss();
                if (mNoCache) {
                    showEmptyTips(ArticleUtils.NOEXIST);
                    return;
                }
            } else if (isDeleted && mListerAdapter != null) {
                Cursor oldCursor = mCursor;
                updateCursor();

                mListerAdapter.resetPages();
                mListerAdapter.updateCursor(mCursor, false);

                if (oldCursor != null && !oldCursor.isClosed()) {
                    oldCursor.close();
                }

                mListView.resetLastRequireState();
                mNoMoreAlbums = false;
                postUpdateImages(ImageChannelVerticleListAdapter.PRELOAD_STRATEGY_FORWARD, 200);
            }

            updateRefreshTime(-1);
            mListView.onCompletion(ArticleUtils.NOEXIST, 0);

            showTopBarTip(getString(R.string.rd_article_no_update));
            mRefreshing = false;
        }
    }

    /**
     * 上次刷新时间
     */
    private void updateRefreshTime(long time) {
        mLastUpdateTime = System.currentTimeMillis();
        if (mListView != null) {
            mListView.setLastUpdated("上次刷新: " + new Date(time > 0 ? time : mLastUpdateTime).toLocaleString());
        }
    }

    @Override
    public boolean shouldPaddingFooter() {
        if(mMenu!=null)
            return mMenu.getBottomBarVisibility() == View.VISIBLE;
        return false;
    }

    /** 
     * 加载旧的图集时间监听
     */
    private class LoadOldAlbumsListener implements OnGetArticlesResultListener {
        @Override
        public void onCompletion(long getFrom, long getTo, int getCount, boolean isDeleted) {
            Cursor oldCursor = mCursor;
            updateCursor();
            mLoadingOldAlbums = false;

            if (mListerAdapter != null) {
                mListerAdapter.updateCursor(mCursor, false);
                mListerAdapter.preloadImageInfoOnLoadOldFinished(mListView);
            }

            if (oldCursor != null && !oldCursor.isClosed()) {
                oldCursor.close();
            }

            mListView.onCompletion(ArticleUtils.SUCCESS, getCount);
        }

        @Override
        public void onFailure(int error) {
            mLoadingOldAlbums = false;

            mListView.onCompletion(ArticleUtils.FAILURE, -1);
        }

        @Override
        public void onNotExists(boolean isDeleted) {
            mLoadingOldAlbums = false;
            mNoMoreAlbums = true;

            mListView.onCompletion(ArticleUtils.NOEXIST, 0);
        }
    }

    private Channel mChannel;
    private Cursor mCursor = null;
    private ArticleListView mListView = null;
    private boolean mRefreshing = false;
    private boolean mLoadingOldAlbums = false;
    private OnGetArticlesResultListener mRefreshListener = null;
    private OnGetArticlesResultListener mLoadOldAlbumsListener = null;
    private boolean mNoMoreAlbums = false;
    private CustomProgressDialog mLoadingDialog = null;
    private boolean mIsRandomChannel = false;
    private boolean mBackToDesktop = false;
    private boolean mSubscribed = true;
    private ReaderMenuContainer mMenu;
    private ImageView mListEmptyView;
    private long mStartTimeByMillis = 0;
    private boolean mAllPagesLoaded = false;
    private boolean mEmptyTipsShowing = false;
    private boolean mNoCache = false;
    private int mPageWidth = -1;
    private int mPageHeight = -1;

    private int mLastVisibleItemIndex = 0;
    private TextView mTipView = null;
    private View mSubBtn = null;
    private long mLastUpdateTime;
    protected Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mHideTipRunnable = new Runnable() {
        @Override
        public void run() {
            Animation outAnim = AnimationUtils.loadAnimation(ImageChannelActivity.this,
                    R.anim.rd_article_list_tips_hide);
            mTipView.startAnimation(outAnim);
            mTipView.setVisibility(View.GONE);
        }
    };

    private ImageChannelVerticleListAdapter mListerAdapter;
    protected int mScrollState = OnScrollListener.SCROLL_STATE_IDLE;
    private OnScrollListener mScrollListener = new OnScrollListener() {
        private long mLastScrollTime = 0;
        private int mFirstVisiblePageOffsetY = 0;
        private int mFirstVisiblePageIndex = 0;
        private boolean mPagesPreloadedDuringFling = false;
        @Override
        public void onScrollStateChanged(AbsListView list, int scrollState) {
            if (mListerAdapter != null && scrollState != OnScrollListener.SCROLL_STATE_IDLE) {
                mListerAdapter.setBusy(true);

                if (scrollState == OnScrollListener.SCROLL_STATE_FLING) {
                    mLastScrollTime = System.currentTimeMillis();
                    boolean headerShown = list.getChildAt(0).getId() == R.id.pull_to_refresh_header;
                    mFirstVisiblePageIndex = list.getFirstVisiblePosition() - (headerShown ? 0 : 1);
                    View firstVisiblePage = list.getChildAt(headerShown ? 1 : 0);
                    if (firstVisiblePage != null) {
                        mFirstVisiblePageOffsetY = firstVisiblePage.getTop();
                    }
                } else {
                    mPagesPreloadedDuringFling = true;
                    mListerAdapter.setLoadAsFling(true);
                }
            } else if (mListerAdapter != null && mScrollState != OnScrollListener.SCROLL_STATE_IDLE) {
                mListerAdapter.setBusy(false);

                int preloadDirection;
                int currentVisibleItemIndex = mListView.getFirstVisiblePosition();
                if (mLastVisibleItemIndex == currentVisibleItemIndex
                            || (mLastVisibleItemIndex == 0 && currentVisibleItemIndex == 1)) {
                    preloadDirection = ImageChannelVerticleListAdapter.PRELOAD_STRATEGY_STAY_STILL;
                } else if (mLastVisibleItemIndex < currentVisibleItemIndex) {
                    preloadDirection = ImageChannelVerticleListAdapter.PRELOAD_STRATEGY_FORWARD;
                } else {
                    preloadDirection = ImageChannelVerticleListAdapter.PRELOAD_STRATEGY_BACKWARD;
                }
                mLastVisibleItemIndex = currentVisibleItemIndex;
                mListerAdapter.updateImages(mListView, preloadDirection);
            }
            handleScrollStateChange(scrollState);
        }

        @Override
        public void onScroll(AbsListView list, int firstVisibleItem, int visibleItemCount,
                int totalItemCount) {
            if(mListerAdapter == null || list.getChildCount() <= 0) {
                return;
            }
            boolean headerShown = list.getChildAt(0).getId() == R.id.pull_to_refresh_header;
            View firstVisiblePage = list.getChildAt(headerShown ? 1 : 0);
            if (firstVisiblePage != null) {
                int firstVisiblePageIndex = firstVisibleItem - (headerShown ? 0 : 1);
                int firtsItemOffsetY = firstVisiblePage.getTop();
                int scrollDistance = firtsItemOffsetY - mFirstVisiblePageOffsetY
                        + (mFirstVisiblePageIndex - firstVisiblePageIndex) * mPageHeight;

                long now = System.currentTimeMillis();
                long duration = now - mLastScrollTime;
                if (now - mLastScrollTime > 5) {
                    int scrollVelocity = (int) (scrollDistance * 1000 / duration);
                    //Utils.debug("xxx", "scroll: " + scrollVelocity);
                    if (!mPagesPreloadedDuringFling
                            && scrollDistance != 0
                            && Math.abs(scrollVelocity) < 3000
                            && !(firstVisiblePageIndex == 0 && firtsItemOffsetY >= 0 && scrollDistance < 0)) {
                        mPagesPreloadedDuringFling = true;
                        mListerAdapter.setLoadAsFling(true);
                    } else if (mScrollState != OnScrollListener.SCROLL_STATE_TOUCH_SCROLL
                            && mPagesPreloadedDuringFling && Math.abs(scrollVelocity) > 6000) {
                        // Utils.debug("xxx", "mFirstVisiblePageOffsetY: " + mFirstVisiblePageOffsetY);
//                        Utils.debug("xxx", "firtsItemOffsetY: " + firtsItemOffsetY);
//                        Utils.debug("xxx", "mFirstVisiblePageIndex: " + mFirstVisiblePageIndex);
//                        Utils.debug("xxx", "firstVisiblePageIndex: " + firstVisiblePageIndex);
//                        Utils.debug("xxx", "mLastScrollTime: " + mLastScrollTime);
//                        Utils.debug("xxx", "now: " + now);
//                        Utils.debug("xxx", "scrollDistance: " + scrollDistance);
//                        Utils.debug("xxx", "scrollVelocity: " + scrollVelocity);
                        mPagesPreloadedDuringFling = false;
                        mListerAdapter.setLoadAsFling(false);
                    }
                    mLastScrollTime = now;
                }
                mFirstVisiblePageIndex = firstVisiblePageIndex;
                mFirstVisiblePageOffsetY = firtsItemOffsetY;
            }
        }
    };

    private Point mSingleTapPoint = new Point();
    private BroadcastReceiver mOfflineCompleteReceiver;

    private View mFullScreenTipView = null;
    private int mLastViewedAlbum = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContent();
        initList();
        initBottomButtons();
        mTipView = (TextView) findViewById(R.id.rd_tip_bar);

        mSubBtn = findViewById(R.id.top_subscribe_btn);
        if(mSubBtn != null) {
            mSubBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    doSubscribe();
                }
            });
        }

        if (!initialize(getIntent())) {
            finish();
            return;
        }

        ensureImageDir();

        if (NetUtils.isNetworkAvailable() && !NetUtils.isWifiConnected()) {
            Toast.makeText(getApplicationContext(), R.string.rd_image_in_2G_mode, Toast.LENGTH_LONG)
                    .show();
        }
    }

    protected void setContent() {
        setContentView(R.layout.rd_image_channel);
    }

    protected void handleScrollStateChange(int scrollState) {
        mScrollState = scrollState;
    }

    protected Channel getCurrentChannel() {
        return mChannel;
    }

    private void showSubscribeTips() {
        SharedPreferences sp = Settings.getSharedPreferences();
        int times = sp.getInt(ArticleUtils.CHANNEL_SUBSCRIBE_TIPS, 0);
        if (times < 2 && !mChannel.isSubscribed(getContentResolver())) {
            final View view = findViewById(R.id.top_subscribe_tips);
            if (view != null) {
                final Animation anim = AnimationUtils.loadAnimation(this,
                        R.anim.rd_article_list_tips_show);
                view.setVisibility(View.VISIBLE);
                view.startAnimation(anim);

                SharedPreferences.Editor editor = sp.edit();
                editor.putInt(ArticleUtils.CHANNEL_SUBSCRIBE_TIPS, times + 1);
                editor.commit();
                sp = null;
                editor = null;
            }
        }
    }

    private boolean initialize(Intent intent) {
        mNoMoreAlbums = false;
        mIsRandomChannel = intent.getBooleanExtra("random_read", false);
        mBackToDesktop = intent.getBooleanExtra(BACK_TO_DESKTOP, false);
        if (!mIsRandomChannel) {
            ReaderPlugin.mIsRunning = true;
        }

        if (!FileUtils.isExternalStorageAvail()) {
            Toast.makeText(getApplicationContext(), R.string.rd_sdcard_not_available,
                    Toast.LENGTH_LONG).show();
            return false;
        }

        if (mEmptyTipsShowing) {
            findViewById(R.id.rd_image_channel_empty).setVisibility(View.GONE);
            mEmptyTipsShowing = false;
        }

        String channelName = intent.getStringExtra("channel");
        mChannel = Channel.get(channelName);
        if (mChannel == null) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.rd_article_channel_expception), Toast.LENGTH_SHORT).show();
            return false;
        }

        mSubscribed = mChannel.isSubscribed(getContentResolver());
        updateCursor();
        if (mCursor == null) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.rd_article_initialize_exception), Toast.LENGTH_SHORT).show();
            return false;
        } else {
            /*
             * 设置当前显示的Channel
             */
            ArticleUtils.addDisplaying(mChannel);
            updateChannelTitle();

            if (mCursor.getCount() > 0) {
                mNoCache = false;
                mListerAdapter = new ImageChannelVerticleListAdapter(this, mCursor,
                        mChannel.getAlbumCoverDataFieldName(), this);
                if (mPageHeight > 0 && mPageWidth > 0) {
                    mListerAdapter.setPageHeight(mPageWidth, mPageHeight);
                }

                try {
                    updateRefreshTime(mChannel.getSubscribedChannel(getContentResolver())
                            .getLastRefreshTime());
                } catch (Exception e) {
                }

                mListerAdapter.setBusy(true);
                mListView.setAdapter(mListerAdapter, 1);
                mListView.setVisibility(View.VISIBLE);

                // 如果该频道离线下载不过2小时，且没有进入过，则直接提示用户已离线下载条数
                ContentResolver cr = getContentResolver();
                if (mChannel.offline().isOfflinedAndIn2Hours(cr)) {
                    mChannel.getSubscribedChannel(cr).setOfflineTime(cr, 0);

                    final int count = mChannel.getSubscribedChannel(cr).offline_count;
                    if (count > 0) {
                        showTopBarTip("已离线下载 " + count + " 个图集");
                    }
                } else if (needAutoRefresh()){
                    mListView.manualRefresh();
                }

                postUpdateImages(ImageChannelVerticleListAdapter.PRELOAD_STRATEGY_FORWARD, 200);

                showFullScreenTip();
            } else {
                mNoCache = true;
                mListerAdapter = null;
                mListView.setVisibility(View.INVISIBLE);
                refresh();
            }

            if (!TextUtils.isEmpty(mChannel.src)) {
                //updateChannelSrc();
                if (mChannel.src.equals(Constants.IMAGE_CHANNEL_SRC_WOXIHUAN)
                        && mListerAdapter != null) {
                    mListerAdapter.setImageQuality(ImageDownloadStrategy.IMAGE_CONFIG_SMALL_QUALITY);
                }
            } else if(mChannel instanceof IlikeChannel && mListerAdapter != null) {
                mListerAdapter.setImageQuality(ImageDownloadStrategy.IMAGE_CONFIG_SMALL_QUALITY);
            }

            titleBarSubScribeBtnShow(!mChannel.isSubscribed(getContentResolver()));
            mStartTimeByMillis = System.currentTimeMillis();
            return true;
        }
    }

    protected void updateChannelTitle() {
        TextView tv_name = (TextView) findViewById(R.id.rd_image_channel_name);
        tv_name.setText(mChannel.title);
    }

    protected void updateChannelSrc() {
        ((TextView) findViewById(R.id.rd_image_channel_source)).setText(mChannel.src);
    }

    private void initList() {
        mListEmptyView = (ImageView) findViewById(R.id.rd_empty_tips);

        ArticleListView listView = (ArticleListView) findViewById(R.id.rd_image_channel_list);
        listView.setLoadMoreThreshold(3);
        listView.setOnScrollListener(mScrollListener);
        listView.setOnRefreshListener(this);
        listView.setOnItemClickListener(this);
        listView.setOnSizeChangedListener(this);
        listView.setMode(ArticleListView.MODE_IMAGES);
        listView.setOnTouchListener(new OnTouchListener() {
            int mLastY = 0;
            @Override
            public boolean onTouch(View View, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        if (mListerAdapter != null) {
                            mListerAdapter.setTouchMoveOffsetY(0);
                        }
                        mLastY = (int) event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int y = (int) event.getY();
                        if (mListerAdapter != null) {
                            mListerAdapter.setTouchMoveOffsetY((int) (y - mLastY));
                        }
                        mLastY = y;
                        break;

                }
                return false;
            }
        });

        mListView = listView;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            mSingleTapPoint.x = -1;
            mSingleTapPoint.y = -1;

            View subTipView = findViewById(R.id.top_subscribe_tips);
            if(subTipView != null) {
                subTipView.setVisibility(View.GONE);
            }
        } else if(action == MotionEvent.ACTION_UP) {
            int[] loc = new int[2];
            mListView.getLocationOnScreen(loc);
            mSingleTapPoint.x = (int) ev.getRawX() - loc[0];
            mSingleTapPoint.y = (int) ev.getRawY() - loc[1];
        }

        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        String channel = intent.getStringExtra("channel");
        if (!mChannel.channel.equals(channel)) {
            if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
                mLoadingDialog.dismiss();
            }

            onCancel();
            handleSubscription();

            if (mSubscribed) {
                UXHelper.getInstance().addActionRecord(getPvHipId(), 1);
                CollectSubScribe
                        .getInstance()
                                .updateSubScribeValue(
                                                mChannel.channel,
                                                Integer.parseInt(String.valueOf((System
                                                        .currentTimeMillis() - mStartTimeByMillis) / 1000)));
            }

            if(mListerAdapter != null) {
                if(mListView != null) {
                    for(int i = 0; i < mListView.getChildCount(); i++) {
                        View Page = mListView.getChildAt(i);
                        mListerAdapter.clearPageContent(Page);
                    }

                    mListView.onChannelChanged();
                }

                mListerAdapter.setBusy(true);
                mListerAdapter.setLoadAsFling(false);
                mListerAdapter.doFinialize();
                mListerAdapter = null;
            }

            if(mCursor != null && !mCursor.isClosed()) {
                mCursor.close();
            }
            initialize(intent);
        }
    }

    private void postUpdateImages(final int strategy, int delay) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mListerAdapter.setBusy(false);
                mListerAdapter.updateImages(mListView, strategy);
            }
        }, delay);
    }

    private void initBottomButtons() {
        mMenu = (ReaderMenuContainer) findViewById(R.id.menu_layout);
        findViewById(R.id.rd_menu_back).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                UXHelper.getInstance().addActionRecord(
                        UXHelperConfig.Reader_ImageTabloid_ControlBar_Back_OnClick, 1);
                finish();
            }
        });

        findViewById(R.id.rd_menu_reflesh).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                UXHelper.getInstance()
                                .addActionRecord(
                                        UXHelperConfig.Reader_ImageTabloid_ControlBar_Refresh_OnClick,
                                        1);
                refresh();
            }
        });

        findViewById(R.id.rd_menu_share_btn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                doShare();
            }
        });

        View btnMenu = findViewById(R.id.menu_menu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onKeyUp(KeyEvent.KEYCODE_MENU, null);
                }
            });
        }
    }

    protected void doShare() {
        UXHelper.getInstance().addActionRecord(
                UXHelperConfig.Reader_ImageTabloid_ControlBar_Share_OnClick, 1);
        CommonUtil.shareChannel(ImageChannelActivity.this, mChannel.title);
    }

    private void doSubscribe() {
        if (mSubscribed) {
            Toast.makeText(getApplicationContext(), R.string.rd_subscribe_channel,
                    Toast.LENGTH_SHORT).show();
        } else {
            if (RssSubscribedChannel.isAllowToInsert(getContentResolver()) == false) {
                Toast.makeText(getApplicationContext(), R.string.rd_add_notice, Toast.LENGTH_SHORT)
                        .show();
            } else {
                Toast.makeText(getApplicationContext(), R.string.rd_subscribe_channel_succeed,
                        Toast.LENGTH_SHORT)
                                .show();
                mSubscribed = true;
            }
            titleBarSubScribeBtnShow(false);
        }
    }

    private void titleBarSubScribeBtnShow(boolean show) {
        if(mSubBtn != null) {
            mSubBtn.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        }
    }

    @Override
    public void finish() {
        /*
         * 移除当前显示的Channel
         */
        ArticleUtils.removeDisplaying(mChannel);
        mHandler.removeCallbacks(mHideTipRunnable);
        handleSubscription();

        if (mChannel != null) {
            onCancel();

            if (mSubscribed) {
                CollectSubScribe.getInstance()
                                .updateSubScribeValue(
                                                mChannel.channel,
                                                Integer.parseInt(String.valueOf((System.currentTimeMillis() - mStartTimeByMillis) / 1000)));
            }

            try {
                mChannel.getSubscribedChannel(getContentResolver()).setLastRefreshTime(getContentResolver(),
                                mLastUpdateTime);
            } catch (Exception e) {
            }
        }

        if (!mIsRandomChannel && !mBackToDesktop) {
            ReaderPlugin.mIsRunning = false;

            if(!ReaderPlugin.getBrowserActivityRunning()) {
                ReaderPlugin.bringBrowserForeground(this);
            }
        }

        if (mListerAdapter != null) {
            mListerAdapter.setBusy(true);
            mListerAdapter.setLoadAsFling(false);
            mListerAdapter.doFinialize();
        }

        super.finish();
    }

    private void handleSubscription() {
        if (mChannel != null) {
            if (mSubscribed && mChannel.isSubscribed(getContentResolver()) == false) {
                mChannel.subscribe(getContentResolver());
            } else if (!mSubscribed && mChannel.isSubscribed(getContentResolver()) == true) {
                mChannel.unsubscribe(getContentResolver());
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMenu.saveBottomBarState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mMenu.onResume();
    }

    private void showProgressDialog() {
        if (mLoadingDialog == null) {
            mLoadingDialog = new CustomProgressDialog(this);
            mLoadingDialog.setTitle(getString(R.string.rd_article_updating_process));
            mLoadingDialog.setMessage(R.string.rd_image_channel_loading_process);
            mLoadingDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent evnet) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        mChannel.stopGet();
                        if (mCursor.getCount() <= 0) {
                            finish();
                        } else {
                            // mImageChannelView.loadLeftAndRightPages();
                        }
                        return false;
                    } else if (keyCode == KeyEvent.KEYCODE_SEARCH) {
                        return true;
                    }
                    return false;
                }
            });
        }
        mLoadingDialog.show();
    }

    private void refresh() {
        if (!mRefreshing) {
            if (mCursor == null || mCursor.getCount() <= 0) {
                showProgressDialog();
            }

            if (mRefreshListener == null) {
                mRefreshListener = new RefreshListener();
            }

            if (mChannel.offline().isRunningInArticle()) {
                registerOfflineCompleteListener();
            } else {
                mChannel.getNewArticlesAsync(getContentResolver(), mRefreshListener, 30, false);
            }

            mRefreshing = true;
            mLoadingOldAlbums = false;
        }
    }

    private void loadOldAlbums() {
        if (!mLoadingOldAlbums) {
            if (mLoadOldAlbumsListener == null) {
                mLoadOldAlbumsListener = new LoadOldAlbumsListener();
            }

            mChannel.getOldArticlesAsync(getContentResolver(), mLoadOldAlbumsListener, 30, false);
            mLoadingOldAlbums = true;
            mRefreshing = false;
        }
    }

    @Override
    public void onDestroy() {
        if (mCursor != null) {
            mCursor.close();
        }

        if(mListerAdapter != null && mListView != null) {
            for(int i = 0; i < mListView.getChildCount(); i++) {
                View Page = mListView.getChildAt(i);
                mListerAdapter.clearPageContent(Page);
            }
            System.gc();
        }
        mListerAdapter = null;
        mListView = null;

        if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
            mLoadingDialog.dismiss();
        }

        if(mOfflineCompleteReceiver != null) {
            unregisterReceiver(mOfflineCompleteReceiver);
        }

        if (mChannel != null) {
            SubscribedChannel subscribedChannel = mChannel
                    .getSubscribedChannel(getContentResolver());
            if (subscribedChannel != null) {
                subscribedChannel.calculateNumberOfVisited(getContentResolver());
            } else {
                // 清除不要的频道，一个是“不再订阅的频道”，一个是从随便看看摇进来的频道 - Jiongxuan
                mChannel.clearAllArticles(getContentResolver());
            }
        }

        super.onDestroy();
    }

    public boolean loadMoreAlbums() {
        if (mNoMoreAlbums || isFinishing()) {
            return false;
        }

        UXHelper.getInstance().addActionRecord(getPvHipId(), 1);
        loadOldAlbums();
        return true;
    }

    private void updateCursor() {
        mCursor = mChannel.getCursorOfSpecificFields(getContentResolver(), new String[] {
            Articles.TITLE, mChannel.getAlbumCoverDataFieldName()
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == AlbumGallaryActivity.REQUEST_CODE_ALBUM_GALLARY && data != null) {
            boolean needRefreshCursor = data.getBooleanExtra(ArticleUtils.DETAIL_LOADED, false);
            int position = data.getIntExtra(ArticleUtils.DETAIL_POSITION, 0);

            if (needRefreshCursor) {
                Cursor oldCursor = mCursor;
                updateCursor();
                mListerAdapter.updateCursor(mCursor, false);

                if (oldCursor != null && !oldCursor.isClosed()) {
                    oldCursor.close();
                }
            }

            int pageNumber = position / ImageChannelVerticleListAdapter.ALBUM_COUNT_FOR_EACH_PAGE;
            int currentVisibleItemIndex = pageNumber + 1;

            if(position != mLastViewedAlbum) {
                mListView.setSelection(currentVisibleItemIndex); // consider the header view
            }

            if(!needRefreshCursor) {
                final int preloadDirection;
                if(mLastVisibleItemIndex == currentVisibleItemIndex) {
                    preloadDirection = ImageChannelVerticleListAdapter.PRELOAD_STRATEGY_STAY_STILL;
                } else if(mLastVisibleItemIndex < currentVisibleItemIndex) {
                    preloadDirection = ImageChannelVerticleListAdapter.PRELOAD_STRATEGY_FORWARD;
                } else {
                    preloadDirection = ImageChannelVerticleListAdapter.PRELOAD_STRATEGY_BACKWARD;
                }
                mLastVisibleItemIndex = currentVisibleItemIndex;

                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(!isFinishing()) {
                            mListerAdapter.updateImages(mListView, preloadDirection);
                        }
                    }
                }, 500);
            }
        }
    }

    @Override
    public void onUpdateNightMode() {
        super.onUpdateNightMode();
        setNightMask();

        int color;
        if (Settings.isNightMode() == true) {
            findViewById(R.id.rd_image_channel_top_bar_text_container).setBackgroundColor(
                            getResources().getColor(R.color.rd_night_bg));
            ((TextView) findViewById(R.id.rd_image_channel_name)).setTextColor(getResources().getColor(
                            R.color.rd_night_title));
            color = ArticleUtils.getRandomColor(this, new Random().nextInt(7), true);
            findViewById(R.id.rd_image_channel_top_bar).setBackgroundColor(color);
            //((TextView) findViewById(R.id.rd_image_channel_source)).setTextColor(color);
            findViewById(R.id.rd_image_channel_frame).setBackgroundColor(getResources().getColor(R.color.rd_night_bg));
            findViewById(R.id.rd_title_bottom_line).setBackgroundResource(R.drawable.rd_title_bottom_cover_nightly);

            if (mListEmptyView.getVisibility() == View.VISIBLE){
                // mErrorWithNetWork
                mListEmptyView.setImageResource(R.drawable.rd_article_loading_exception_night);
            }
        } else {
            findViewById(R.id.rd_image_channel_top_bar_text_container).setBackgroundColor(
                            getResources().getColor(R.color.rd_ivory));
            ((TextView) findViewById(R.id.rd_image_channel_name)).setTextColor(getResources()
                            .getColor(R.color.rd_black));
            color = ArticleUtils.getRandomColor(this, new Random().nextInt(7), false);
            findViewById(R.id.rd_image_channel_top_bar).setBackgroundColor(color);
            //((TextView) findViewById(R.id.rd_image_channel_source)).setTextColor(color);
            findViewById(R.id.rd_image_channel_frame).setBackgroundColor(getResources().getColor(R.color.rd_ivory));
            findViewById(R.id.rd_title_bottom_line).setBackgroundResource(R.drawable.rd_title_bottom_cover_list);
            if (mListEmptyView.getVisibility() == View.VISIBLE){
                // mErrorWithNetWork
                mListEmptyView.setImageResource(R.drawable.rd_article_loading_exception);
            }
        }
    }

    public boolean onKeyUp(int keyCode, android.view.KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (mFullScreenTipView != null) {
                hideFullScreenTip();
            }
            mMenu.onMenuClick();
            return true;
        }

        return super.onKeyUp(keyCode, event);
    };

    private void showEmptyTips(int state) {
        if (state == ArticleUtils.NOEXIST) {
            findViewById(R.id.rd_image_channel_empty).setVisibility(View.VISIBLE);
            findViewById(R.id.rd_empty_refresh).setVisibility(View.GONE);
            mListEmptyView.setImageResource(getEmptyListTipResource());
            mEmptyTipsShowing = true;
        } else if (state == ArticleUtils.FAILURE) {
            mEmptyTipsShowing = true;
            findViewById(R.id.rd_image_channel_empty).setVisibility(View.VISIBLE);
            mListEmptyView.setImageResource(getNetErrorTipResource());
            findViewById(R.id.rd_empty_refresh).setVisibility(View.VISIBLE);
            findViewById(R.id.rd_empty_refresh).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    refresh();
                }
            });
        }
    }

    protected int getEmptyListTipResource() {
        return R.drawable.rd_no_article;
    }

    protected int getNetErrorTipResource() {
        return Settings.isNightMode() ? R.drawable.rd_article_loading_exception_night
                : R.drawable.rd_article_loading_exception;
    }

    @Override
    public void onBackPressed() {
        if (mMenu.onBackBtnPressed() == true) {
            return;
        } else if (mFullScreenTipView != null) {
            hideFullScreenTip();
            return;
        }
        super.onBackPressed();
    }

    private void ensureImageDir() {
        String path = Constants.LOCAL_PATH_IMAGES + "full_size_images/";
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
            FileUtils.createNoMediaFileIfPathExists(path);
        } catch (IOException e) {
            Utils.error(getClass(), Utils.getStackTrace(e));
        }
    }

    @Override
    public void onRefresh(boolean isAutoRefresh) {
        refresh();
    }

    @Override
    public void onLoadOld() {
        loadMoreAlbums();
    }

    @Override
    public void onLoading() {
    }

    @Override
    public void onCancel() {
        mChannel.stopGet();
        mRefreshing = false;
        mLoadingOldAlbums = false;
    }

    @Override
    public void albumClicked(int albumIndex) {
        if (albumIndex >= 0 && albumIndex < mCursor.getCount()) {
            mLastViewedAlbum = albumIndex;

            if (mChannel.isDbBusy()) {
                Toast.makeText(this, R.string.rd_inserting_db_photo, Toast.LENGTH_LONG).show();
                return;
            }

            if (mRefreshing || mLoadingOldAlbums) {
                mChannel.stopGet();
                mRefreshing = false;
                mLoadingOldAlbums = false;
                mListView.reset();
            }

            mListerAdapter.removePendingMessages();

            Intent intent = null;
            if (mChannel instanceof RssChannel) {
                intent = new Intent(this, AlbumGallaryActivity.class);
                intent.putExtra("position", albumIndex);
            } else {
                intent = new Intent(this, ILikeArticleDetailActivity.class);
                intent.putExtra(ArticleUtils.LIST_POSITION, albumIndex);
            }
            intent.putExtra("channel", mChannel.channel);
            startActivityForResult(intent, AlbumGallaryActivity.REQUEST_CODE_ALBUM_GALLARY);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mListerAdapter.handlePageClicked(view, mSingleTapPoint);
        mSingleTapPoint.x = -1;
        mSingleTapPoint.y = -1;
    }

    @Override
    public void onSizeChanged(int width, int height) {
        if(mListerAdapter != null) {
            mListerAdapter.setPageHeight(width, height);
            setPageRect();
            mListerAdapter.notifyDataSetChanged();
        }
        mPageHeight = height;
        mPageWidth = width;
    }

    private void setPageRect() {
        int[] loc = new int[2];
        mListView.getLocationInWindow(loc);
        Rect r = new Rect(loc[0], loc[1], loc[0] + mListView.getWidth(), +loc[1]
                + mListView.getHeight());
        mListerAdapter.setPageRectInWindow(r);
    }

    private final void showTopBarTip(String content) {
        if (mTipView == null || TextUtils.isEmpty(content)) {
            return;
        }
        mHandler.removeCallbacks(mHideTipRunnable);
        mTipView.clearAnimation();

        mTipView.setText(content);
        mTipView.setVisibility(View.VISIBLE);

        Animation animIn = AnimationUtils.loadAnimation(this, R.anim.rd_article_list_tips_show);
        mTipView.startAnimation(animIn);

        mHandler.postDelayed(mHideTipRunnable, animIn.getDuration() + 1000);
    }

    private void registerOfflineCompleteListener() {
        if (mOfflineCompleteReceiver == null) {
            mOfflineCompleteReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String channel = intent.getStringExtra(OfflineTask.ACTION_CHANNEL);
                    if (Channel.get(channel).equals(mChannel)) {
                        int result = intent.getIntExtra(OfflineTask.ACTION_RESULT,
                                Channel.RESULT_OK);
                        boolean isDeleted = intent.getBooleanExtra(OfflineTask.ACTION_IS_DELETED, false);
                        switch (result) {
                            case Channel.RESULT_OK:
                                int count = intent.getIntExtra(OfflineTask.ACTION_COUNT, 0);
                                if (count <= 0) {
                                    mRefreshListener.onNotExists(isDeleted);
                                } else {
                                    mRefreshListener.onCompletion(0, 0, count, isDeleted);
                                }
                                break;
                            case Channel.RESULT_FAILURE:
                                mRefreshListener.onFailure(result);
                                break;
                            case Channel.RESULT_NOT_EXISTS:
                                mRefreshListener.onNotExists(isDeleted);
                                break;
                    }
                }
            }
            };
        }

        registerReceiver(mOfflineCompleteReceiver, new IntentFilter(
                Constants.READER_BROADCAST_NEED_REFRESH_ARTICLE_LIST));
    }

    private boolean showFullScreenTip() {
        // This tip is for reader channel only
        if (!(mChannel instanceof RssChannel)
                || Constants.IMAGE_CHANNEL_SRC_WOXIHUAN.equals(mChannel.src)) {
            return false;
        }

        SharedPreferences sp = Settings.getSharedPreferences();
        if (!sp.getBoolean(ArticleUtils.CLICK_ALBUM_TO_BROWSE_MORE_TIP, false)) {
            mFullScreenTipView = LayoutInflater.from(this).inflate(
                    R.layout.rd_image_channel_full_screen_tip, null);
            ((ViewGroup) getWindow().getDecorView()).addView(mFullScreenTipView);
            mFullScreenTipView.startAnimation(AnimationUtils.loadAnimation(this,
                    R.anim.rd_article_list_tips_show));
            mFullScreenTipView.findViewById(R.id.rd_article_detail_tips_btn).setOnClickListener(
                    new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            hideFullScreenTip();
                        }
                    });

            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean(ArticleUtils.CLICK_ALBUM_TO_BROWSE_MORE_TIP, true);
            editor.commit();

            return true;
        } else {
            return false;
        }
    }

    private void hideFullScreenTip() {
        if(mFullScreenTipView != null) {
            ViewGroup rootView = (ViewGroup) getWindow().getDecorView();
            rootView.removeView(mFullScreenTipView);
            mFullScreenTipView = null;

            showSubscribeTips();
        }
    }

    protected boolean needAutoRefresh() {
        return true;
    }
}
