package com.qihoo360.reader.ui.articles;

import com.qihoo360.browser.hip.CollectSubScribe;
import com.qihoo360.browser.hip.UXHelper;
import com.qihoo360.browser.hip.UXHelperConfig;
import com.qihoo360.reader.Constants;
import com.qihoo360.reader.R;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.image.ImageDownloadStrategy;
import com.qihoo360.reader.image.ImageUtils;
import com.qihoo360.reader.listener.CommonListener;
import com.qihoo360.reader.listener.OnGetArticlesResultListener;
import com.qihoo360.reader.offline.OfflineTask;
import com.qihoo360.reader.push.PushManager;
import com.qihoo360.reader.subscription.Article;
import com.qihoo360.reader.subscription.Channel;
import com.qihoo360.reader.subscription.SubscribedChannel;
import com.qihoo360.reader.subscription.reader.RssSubscribedChannel;
import com.qihoo360.reader.support.FileUtils;
import com.qihoo360.reader.support.Utils;
import com.qihoo360.reader.ui.ActivityBase;
import com.qihoo360.reader.ui.CommonUtil;
import com.qihoo360.reader.ui.CustomProgressDialog;
import com.qihoo360.reader.ui.ReaderMenuContainer;
import com.qihoo360.reader.ui.ReaderPlugin;
import com.qihoo360.reader.ui.articles.ArticleListView.OnRequestListener;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;
import java.util.Random;

public class ArticleReadActivity extends ActivityBase implements OnItemClickListener, OnGetArticlesResultListener,
                OnRequestListener {
    public static final String TAG = "ArticleReadActivity";

    private TextView mListTipsView;
    private Animation mTipsViewAnimation;
    private Runnable mHolderRunable;
    private ArticleListView mListView;
    private long mStartTimeByMillis = 0;
    //private ImageView mSwitchPicModeView;

    // Channel List Adapter
    private ArticleListAdapter mListerAdapter;
    // Context Menu
    private ReaderMenuContainer mMenu;
    private CustomProgressDialog mLoadingDialog = null;
    // List & Detail Offset
    private int POSITION_STEP = 2;
    // only changed when the detail-activity loadedrd_
    private boolean mIsRandomChannel = false;
    private boolean mBackToDesktop = false;
    private boolean mNoCacheErrorShowing = false;
    // List HeaderView
    private ViewGroup mListHeaderview;
    // Channel
    private Channel mChannel;
    private ImageView mListEmptyView;
    private long mLastUpdateTime;
    private int mRandomNumber = 7;

    private static final int STATE_REFRESHING = 1;
    private static final int STATE_LOADING = -1;
    private static final int STATE_DELAY_REFRESH = -2;

    private static final int STATE_NORMAL = 0;
    private static final int STATE_DELETE = 1;
    private static final int STATE_ADD = 2;
    private boolean mNoCache = false;
    private int mDetailLoadResult = ArticleUtils.SUCCESS;
    private final int PRE_LOAD_MORE_MAX_COUNT = 10;

    /**
     * -1,0,1} -1: loading more, 0: normal, 1: refreshing
     */
    private int mCurrentListState = STATE_NORMAL;
    private int mChannelState = STATE_NORMAL;

    private BroadcastReceiver mImageableReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mListHeaderview != null && mListerAdapter != null) {
                if (Settings.isNoPicMode()){
                    if(!mListView.removeHeaderView(mListHeaderview))
                        mListerAdapter.notifyDataSetChanged();
                }else {
                    if (mListerAdapter.isInitState()) {
                        Cursor cursor = mListerAdapter.getCursor();
                        long newestImageContentId = mChannel.getNewestImageOfContentId(getContentResolver());
                        if (mIsRandomChannel || newestImageContentId >= 0)
                            bindHeader(cursor, cursor == null ? 0 : cursor.getCount(), newestImageContentId);
                        else
                            mListerAdapter.notifyDataSetChanged();
                    } else {
                        mListView.removeHeaderView(mListHeaderview);
                        mListView.clearAdapter();
                        mListView.recoveryHeader();
                        mListView.addHeaderView(mListHeaderview);
                        mListView.setAdapter(mListerAdapter, mListView.getBeforeFirst());
                    }
                }
                /*
                 * For fix [Push-to-Refresh] Visible
                 */
                mListView.switchNoPicMode();
            }
        }
    };

    BroadcastReceiver mNetStateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ImageDownloadStrategy.getInstance(ArticleReadActivity.this).updateWifiState();
        }
    };

    private BroadcastReceiver mChannelOfflineReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String channel = intent.getStringExtra(OfflineTask.ACTION_CHANNEL);
            if (Channel.get(channel).equals(mChannel)) {
                int result = intent.getIntExtra(OfflineTask.ACTION_RESULT, Channel.RESULT_OK);
                boolean isDeleted = intent.getBooleanExtra(OfflineTask.ACTION_IS_DELETED, false);
                switch (result) {
                case Channel.RESULT_OK:
                    mCurrentListState = STATE_REFRESHING;
                    int count = intent.getIntExtra(OfflineTask.ACTION_COUNT, 0);
                    if (count <= 0) {
                        onNotExists(isDeleted);
                    } else {
                        onCompletion(0, 0, count, isDeleted);
                    }
                    break;
                case Channel.RESULT_FAILURE:
                    onFailure(result);
                    break;
                case Channel.RESULT_NOT_EXISTS:
                    onNotExists(isDeleted);
                    break;
                }
            }
        }
    };

    private int mScrollState = OnScrollListener.SCROLL_STATE_IDLE;
    private OnScrollListener mScrollListener = new OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (scrollState != OnScrollListener.SCROLL_STATE_IDLE) {
                mListerAdapter.setBusy(true);
            } else if (mScrollState != OnScrollListener.SCROLL_STATE_IDLE) {
                mListerAdapter.setBusy(false);
                mListerAdapter.updateImages(view);
            }

            mListView.setScrollState(scrollState);
            mScrollState = scrollState;
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.rd_article_container);

        IntentFilter imageableFilter = new IntentFilter(Constants.READER_BROADCAST_SWITCH_WHETHER_THE_IMAGE_MODE);
        registerReceiver(mImageableReceiver, imageableFilter);

        inflateLayout();

        ImageDownloadStrategy.getInstance(this).updateWifiState();
        registerReceiver(mNetStateChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        registerReceiver(mChannelOfflineReceiver,
                        new IntentFilter(Constants.READER_BROADCAST_NEED_REFRESH_ARTICLE_LIST));

        if (!mIsRandomChannel) {
            ReaderPlugin.mIsRunning = true;
        }

        /* mSwitchPicModeView = (ImageView) findViewById(R.id.top_switch_pic_mode);
         mSwitchPicModeView.setOnClickListener(new OnClickListener() {

             @Override
             public void onClick(View v) {
                 Settings.setNoPicMode(ArticleReadActivity.this, !Settings.isNoPicMode());
                 updateSwitchPicModeView();
             }
         });*/
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMenu.saveBottomBarState();
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (!FileUtils.isExternalStorageAvail()) {
            Toast.makeText(getApplicationContext(), R.string.rd_sdcard_not_available, Toast.LENGTH_LONG).show();
        }

        mMenu.onResume();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //updateSwitchPicModeView();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mNetStateChangeReceiver);
        unregisterReceiver(mImageableReceiver);
        unregisterReceiver(mChannelOfflineReceiver);

        if (mChannel != null) {
            SubscribedChannel subscribedChannel = mChannel.getSubscribedChannel(getContentResolver());
            if (subscribedChannel != null) {
                subscribedChannel.calculateNumberOfVisited(getContentResolver());
            } else {
                // 清除不要的频道，一个是“不再订阅的频道”，一个是从随便看看摇进来的频道 - Jiongxuan
                mChannel.clearAllArticles(getContentResolver());
            }
        }

        if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
            mLoadingDialog.dismiss();
        }
        mLoadingDialog = null;

        try {
             mListerAdapter.getCursor().close();
             mListerAdapter.destory();
        } catch (Exception e) {
        }

        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        mListView.clearAdapter();
        mListerAdapter = null;
        initialize(intent);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (mListHeaderview != null) {
            ImageView imageView = (ImageView) mListHeaderview
                    .findViewById(R.id.rd_article_list_header_iamgeview);
            DisplayMetrics dm = getResources().getDisplayMetrics();
            int newWidth = dm.widthPixels;
            int newHeight = newWidth * 9 / 16;

            if (imageView != null
                    && (newWidth != imageView.getWidth() || newHeight != imageView.getHeight())) {
                LayoutParams lp = new LayoutParams(newWidth, newHeight);
                imageView.setLayoutParams(lp);
            }
        }

        super.onConfigurationChanged(newConfig);
    }

    public void inflateLayout() {
        mListView = (ArticleListView) findViewById(R.id.rd_article_lister);
        mMenu = (ReaderMenuContainer) findViewById(R.id.menu_layout);
        mListHeaderview = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.rd_article_list_header_view, null);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        ImageView imageView = (ImageView) mListHeaderview.findViewById(R.id.rd_article_list_header_iamgeview);

        LayoutParams lp = new LayoutParams(dm.widthPixels, dm.widthPixels * 9 / 16);
        imageView.setLayoutParams(lp);
        mListTipsView = (TextView) findViewById(R.id.rd_article_list_loaded_tips);

        mListView.setOnItemClickListener(this);
        mListView.setOnRefreshListener(this);
        mListView.setOnScrollListener(mScrollListener);

        //mListView.setLayoutAnimation(AnimationFactory.getListLayoutAnimation());
        mListEmptyView = (ImageView) findViewById(R.id.rd_empty_tips);

        mRandomNumber = new Random().nextInt(7);
        findViewById(R.id.rd_article_list_title_container).setBackgroundColor(
                        ArticleUtils.getRandomColor(this, mRandomNumber, Settings.isNightMode()));
        if (initialize(getIntent())) {
            buildButtom();
        }

        findViewById(R.id.top_subscribe_btn).setOnClickListener(mListener);
        findViewById(R.id.rd_empty_refresh).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mCurrentListState = STATE_REFRESHING;
                showNoCacheError(ArticleUtils.SUCCESS, false);
                mChannel.getNewArticlesAsync(getContentResolver(), ArticleReadActivity.this, true);
                initLoading();
                mLoadingDialog.show();
            }
        });

    }

    public void resetGalleryView(int position, boolean topNews) {
        if (mChannel != null) {
            mChannel.stopGet();
            mListView.reset();
        }
        Intent intent = new Intent(ArticleReadActivity.this, ArticleDetailActivity.class);
        intent.putExtra("channel", mChannel.channel);
        intent.putExtra("top_news", topNews);
        intent.putExtra(ArticleUtils.LIST_POSITION, position);
        startActivityForResult(intent, this.hashCode());
    }

    private boolean mSubscribed = true;

    private void buildButtom() {
        /*
         * final boolean b = getIntent().getBooleanExtra("random_read", false); if (b == false) { ((ImageView) findViewById(R.id.menu_delete))
         * .setImageResource(R.drawable.rd_rss_remove_btn_drawable); } else { ((ImageView)
         * findViewById(R.id.menu_delete)).setImageResource(R.drawable.rd_rss_subscribe_btn_drawable); }
         */
        if (mChannel.isSubscribed(getContentResolver())) {
            mSubscribed = true;
            //((ImageView) findViewById(R.id.menu_subscription)).setImageResource(R.drawable.rd_rss_add_btn_subscried);
        } else {
            mSubscribed = false;
            //((ImageView) findViewById(R.id.menu_subscription)).setImageResource(R.drawable.rd_rss_add_btn);
        }
        mMenu.setBottomBarListener(new CommonListener() {

            @Override
            public void actionPerform(int resId) {
                if (resId == R.id.menu_back) {
                    UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Article_List_Back_With_Action_Bar, 1);
                    finish();
                } else if (resId == R.id.menu_share) {
                    CommonUtil.shareChannel(ArticleReadActivity.this, mChannel.title);
                    UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Article_List_Share_OnClick, 1);
                    mMenu.hideMenuOrBottomBar();
                } else if (resId == R.id.menu_reflesh) {
                    UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Article_List_Refresh_Width_Action_Bar,
                                    1);
                    if (mNoCache) {
                        mCurrentListState = STATE_REFRESHING;
                        showNoCacheError(ArticleUtils.SUCCESS, false);
                        mChannel.getNewArticlesAsync(getContentResolver(), ArticleReadActivity.this, true);
                        initLoading();
                        mLoadingDialog.show();
                    } else {
                        mListView.manualRefresh();
                    }
                    mMenu.hideMenuOrBottomBar();
                }  else if (resId == R.id.menu_menu) {
                    onKeyUp(KeyEvent.KEYCODE_MENU, null);
                }
            }
        });
    }

    private final void showTipsWidthTitle(final String title) {
        if (TextUtils.isEmpty(title))
            return;

        if (mTipsViewAnimation == null) {
            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {
                    mTipsViewAnimation = AnimationUtils.loadAnimation(ArticleReadActivity.this, R.anim.rd_article_list_tips_show);
                    mTipsViewAnimation.setAnimationListener(new AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {

                            if (mHolderRunable != null)
                                mListTipsView.getHandler().removeCallbacks(mHolderRunable);

                            mHolderRunable = new Runnable() {

                                @Override
                                public void run() {
                                    mListTipsView.startAnimation(AnimationUtils.loadAnimation(ArticleReadActivity.this,
                                                    R.anim.rd_article_list_tips_hide));
                                    mListTipsView.setVisibility(View.INVISIBLE);
                                }
                            };

                            mListTipsView.postDelayed(mHolderRunable, 1000);

                        }
                    });
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    mListTipsView.setVisibility(View.VISIBLE);
                    mListTipsView.setText(title);
                    mListTipsView.clearAnimation();
                    mListTipsView.startAnimation(mTipsViewAnimation);
                }

            }.execute();
        } else {
            mListTipsView.setVisibility(View.VISIBLE);
            mListTipsView.setText(title);
            mListTipsView.clearAnimation();
            mListTipsView.startAnimation(mTipsViewAnimation);
        }
    }

    private void titleBarSubScribeBtnShow(boolean show) {
        View view = findViewById(R.id.top_subscribe_btn);
        if (view != null) {
            view.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void doSubscribe() {
        if (mSubscribed == true) {
            changeFavUI(false);
            mChannelState = STATE_DELETE;
            mSubscribed = false;
            UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Article_List_Cancel_SubScribe, 1);
            Toast.makeText(getApplicationContext(), R.string.rd_cancel_subscribe, Toast.LENGTH_SHORT).show();
        } else {
            UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Article_List_Add_SubScribe, 1);
            if (RssSubscribedChannel.isAllowToInsert(getContentResolver()) == false) {
                Toast.makeText(getApplicationContext(), R.string.rd_add_notice, Toast.LENGTH_SHORT).show();
            } else {
                changeFavUI(true);
                mChannelState = STATE_ADD;
                Toast.makeText(getApplicationContext(), R.string.rd_subscribe_channel_succeed, Toast.LENGTH_SHORT)
                                .show();
                mSubscribed = true;
                /**
                 * 打点，添加新订阅后，同时添加相应的key
                 */
                CollectSubScribe.getInstance().updateSubScribeKey(mChannel.channel);
                titleBarSubScribeBtnShow(false);
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onBackPressed()
     */
    @Override
    public void onBackPressed() {
        if (mMenu.onBackBtnPressed() == true) {
            return;
        }

        super.onBackPressed();
    }

    public boolean onKeyUp(int keyCode, android.view.KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_MENU) {
            mMenu.onMenuClick();
            return true;
        }

        return super.onKeyUp(keyCode, event);
    };

    /*
     * (non-Javadoc)
     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
        if (view.getId() == R.id.rd_article_loading_processer) {
            return;
        } else {
            if (mChannel.isDbBusy()) {
                Toast.makeText(this, R.string.rd_inserting_db_article, Toast.LENGTH_LONG).show();
                return;
            }

            if (position == 1 && mListView.getHeaderViewsCount() == 2) {
                int pos = mListerAdapter.getHeaderItemPosition();
                if (pos >= 0) {
                    resetGalleryView(pos, true);
                }
            } else if (position != 0) {
                POSITION_STEP = mListView.getHeaderViewsCount();
                resetGalleryView(position - POSITION_STEP, false);
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == this.hashCode()) {
            int newPos = data == null ? -1 : data.getIntExtra(ArticleUtils.DETAIL_POSITION, 1);
            boolean loaded = newPos == -1 ? false : data.getBooleanExtra(ArticleUtils.DETAIL_LOADED,
                            false);
            if (loaded) {
                mDetailLoadResult = data.getIntExtra(ArticleUtils.DETAIL_LOADED_RESULT, ArticleUtils.SUCCESS);
            }

            if (newPos >= 0) {
                backToList(data == null ? false : data.getBooleanExtra("stay_with_top_news", false), newPos);
                mListerAdapter.changeCursor(mChannel.getAbstractCursor(getContentResolver()));
            }
        } else
            super.onActivityResult(requestCode, resultCode, data);
    }

    public void backToList(boolean stayedWithTopNews, final int position) {
        ArticleUtils.addDisplaying(mChannel);
        mListerAdapter.setBusy(false);
        if (mDetailLoadResult == ArticleUtils.NOEXIST) {
            mListView.tipNoAnyMore();
        } else {
            mListView.hideProcess(false);
        }


        if(!stayedWithTopNews && !mListView.isItemCompletelyVisible(position + POSITION_STEP)) {
            mListView.post(new Runnable() {
                @Override
                public void run() {
                    mListView.setSelection(position + POSITION_STEP);
                }
            });
        }

        if (mDetailLoadResult != ArticleUtils.NOEXIST) {
            mListView.postDelayed(new Runnable() {

                @Override
                public void run() {
                    mListView.showProcess();
                }
            }, 500);
        }
    }

    @Override
    public void onUpdateNightMode() {
        super.onUpdateNightMode();
        setNightMask();

        if (Settings.isNightMode() == true) {
            findViewById(R.id.rd_article_list_container).setBackgroundResource(
                            R.drawable.rd_article_list_container_bg_drawable_nightly);
            findViewById(R.id.rd_article_list_title_container).setBackgroundColor(
                            ArticleUtils.getRandomColor(this, mRandomNumber, true));
            findViewById(R.id.rd_list_channel_name).setBackgroundResource(
                            R.drawable.rd_article_list_container_bg_drawable_nightly);
            ((TextView) findViewById(R.id.rd_list_channel_name)).setTextColor(getResources().getColor(
                            R.color.rd_night_title));
            if (mListHeaderview != null) {
                ((TextView) mListHeaderview.findViewById(R.id.rd_article_list_header_title))
                                .setTextColor(getResources().getColor(R.color.rd_night_text));
            }

            if (mNoCacheErrorShowing) {
                mListEmptyView.setImageResource(R.drawable.rd_article_loading_exception_night);
            }

            findViewById(R.id.rd_title_bottom_line).setBackgroundResource(R.drawable.rd_title_bottom_cover_nightly);
        } else {
            findViewById(R.id.rd_article_list_container).setBackgroundResource(
                            R.drawable.rd_article_list_container_bg_drawable);
            findViewById(R.id.rd_article_list_title_container).setBackgroundColor(
                            ArticleUtils.getRandomColor(this, mRandomNumber, false));
            findViewById(R.id.rd_list_channel_name).setBackgroundResource(R.drawable.rd_article_list_container_bg_drawable);
            ((TextView) findViewById(R.id.rd_list_channel_name))
                            .setTextColor(getResources().getColor(R.color.rd_black));
            if (mListHeaderview != null) {
                ((TextView) mListHeaderview.findViewById(R.id.rd_article_list_header_title))
                                .setTextColor(getResources().getColor(R.color.rd_white));
            }

            if (mNoCacheErrorShowing) {
                mListEmptyView.setImageResource(R.drawable.rd_article_loading_exception);
            }

            findViewById(R.id.rd_title_bottom_line).setBackgroundResource(R.drawable.rd_title_bottom_cover_list);
        }

        if (mListView != null) {
            mListView.onUpdateNightMode();
        }

        if (mListerAdapter != null) {
            mListerAdapter.notifyDataSetChanged();
        }

    }

    private boolean initialize(Intent intent) {
        String channelName = intent.getStringExtra("channel");
        mIsRandomChannel = intent.getBooleanExtra("random_read", false);
        mBackToDesktop = intent.getBooleanExtra(BACK_TO_DESKTOP, false);
        Utils.debug("init", "Channel: " + channelName);
        mChannel = Channel.get(channelName);
        if (mChannel == null) {
            Toast.makeText(getApplicationContext(), getString(R.string.rd_article_channel_expception),
                            Toast.LENGTH_SHORT).show();
            Utils.debug("debug", "Exception: channel is null!");
            finish();
            return false;
        }
        Cursor cursor = mChannel.getAbstractCursor(getContentResolver());
        if (cursor == null) {
            Toast.makeText(getApplicationContext(), getString(R.string.rd_article_initialize_exception),
                            Toast.LENGTH_SHORT).show();
            finish();
            return false;
        } else {
            //若果正在Push该频道，则停止Push
            if (mChannel.equals(PushManager.getPushingChannel()))
                PushManager.stop(this);

            //如果该频道Push完成，则移除Push通知
            titleBarSubScribeBtnShow(!mChannel.isSubscribed(getContentResolver()));

            //设置当前显示的Channel
            ArticleUtils.addDisplaying(mChannel);

            mStartTimeByMillis = System.currentTimeMillis();
            ((TextView) findViewById(R.id.rd_list_channel_name)).setText(mChannel.title);
            if (cursor.getCount() == 0) {
                mNoCache = true;
                // 如果正在离线下载，则等待...
                if (mChannel.offline().isRunningInArticle()) {
                    mCurrentListState = STATE_DELAY_REFRESH;
                } else {
                    mChannel.getNewArticlesAsync(getContentResolver(), this, true);
                    mCurrentListState = STATE_REFRESHING;
                }
                initLoading();
                mLoadingDialog.show();
                cursor.close();
            } else {
                try {
                    updateRefreshTime(mChannel.getSubscribedChannel(getContentResolver()).getLastRefreshTime());
                } catch (Exception e) {
                }
                setListCursor(cursor, cursor.getCount());

                // 如果该频道离线下载不过2小时，且没有进入过，则直接提示用户已离线下载条数
                ContentResolver cr = getContentResolver();
                if (mChannel.offline().isOfflinedAndIn2Hours(cr)) {
                    final int count = mChannel.getSubscribedChannel(cr).offline_count;
                    mChannel.getSubscribedChannel(cr).setOfflineTime(cr, 0);
                    mListView.postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            showTipsWidthTitle("已离线下载"+(count<=0?"":(count+"条")));
                        }
                    }, 500);
                }else{
                    mListView.manualRefresh();
                }
            }
        }
        return true;
    }

    private void initLoading() {
        mLoadingDialog = new CustomProgressDialog(this);
        mLoadingDialog.setTitle(getString(R.string.rd_article_updating_process));
        mLoadingDialog.setMessage(R.string.rd_article_is_loading_articles);
        mLoadingDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent evnet) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    finish();
                    return false;
                } else if (keyCode == KeyEvent.KEYCODE_SEARCH) {
                    return true;
                }
                return false;
            }
        });

    }

    private void setListCursor(Cursor cursor, long count) {
        if (mListerAdapter == null) {
            mListerAdapter = new ArticleListAdapter(this, cursor, false);
            mListView.setAdapter(mListerAdapter);
        } else {
            mListerAdapter.changeCursor(cursor);
        }

        if (mListerAdapter.isInitState()) {
            long newestImageContentId = mChannel.getNewestImageOfContentId(getContentResolver());
            if (!Settings.isNoPicMode()
                    && ((mCurrentListState == STATE_NORMAL && newestImageContentId >= 0) || mCurrentListState == STATE_REFRESHING)) {
                bindHeader(cursor, count, newestImageContentId);
            } else if (mCurrentListState == STATE_REFRESHING) {
                // must be in the no picture mode
                mListView.setSelection(1);
            }
        }

        if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
            mLoadingDialog.dismiss();
            SharedPreferences sp = Settings.getSharedPreferences();
            int times = sp.getInt(ArticleUtils.CHANNEL_SUBSCRIBE_TIPS, 0);
            if (times < 2 && !mChannel.isSubscribed(getContentResolver())) {
                final View view = findViewById(R.id.top_subscribe_tips);
                if (view != null) {
                    final Animation anim = AnimationUtils.loadAnimation(this,
                            R.anim.rd_article_list_tips_show);
                    view.postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            view.setVisibility(View.VISIBLE);
                            view.startAnimation(anim);
                        }
                    }, 400);
                }
                SharedPreferences.Editor editor = sp.edit();
                editor.putInt(ArticleUtils.CHANNEL_SUBSCRIBE_TIPS, times + 1);
                editor.commit();
                sp = null;
                editor = null;
            }
        }
    }

    private void bindHeader(final Cursor cursor, long range, long newestImageContentId) {
        Article article = null;
        String url = null;
        int i = 0;

        for (cursor.moveToFirst(); !cursor.isAfterLast() && i < range; cursor.moveToNext()) {
            article = Article.inject(cursor);
            if (article != null && article.contentid == newestImageContentId) {
                url = ArticleUtils.getFirstValidImageUrlForHeaderView(article.images360);
                break;
            }else {
                article = null;
                url = null;
                ++i;
            }
        }

        if (!TextUtils.isEmpty(url) && article != null) {
            if (mListView.getHeaderViewsCount() < 2) {
                mListView.removeHeaderView(mListHeaderview);
                mListView.clearAdapter();
                mListView.recoveryHeader();
                mListView.addHeaderView(mListHeaderview);
                mListView.setAdapter(mListerAdapter);
            } else {
                mListView.setSelection(1);
            }

            mListerAdapter.bindHeaderView(mListHeaderview, url, article.title, i);
            Utils.debug("header", "Position: " + i);
        } else {
            mListView.removeHeaderView(mListHeaderview);
            mListView.setSelection(1);
        }
    }

    /*
     * (non-Javadoc)
     * @see com.qihoo360.reader.listener.OnGetArticlesResultListener#onCompletion()
     */
    @Override
    public void onCompletion(long getFrom, long getTo, int getCount, boolean isDeleted) {
        Utils.debug("onLoading", "State: load old complete....");
        Cursor cursor = mChannel.getAbstractCursor(getContentResolver());
        if (cursor != null) {
            if (mListerAdapter != null) {
                if (mCurrentListState == STATE_REFRESHING) {
                    mListerAdapter.setState(true);
                }
                mListerAdapter.setBusy(false);
            }
            mListView.onCompletion(ArticleUtils.SUCCESS, getCount);
            if(isDeleted) {
                mListView.resetLastRequireState();
            }
            if (mCurrentListState == STATE_REFRESHING) {
                updateRefreshTime(-1);
                if (!mNoCache)
                    showTipsWidthTitle("更新了 " + getCount + " 篇文章");

                if(cursor.getCount() <= PRE_LOAD_MORE_MAX_COUNT) {
                    mListView.loadMore();
                }
            }
            mNoCache = false;
            setListCursor(cursor, getCount);
            mListerAdapter.updateImages(mListView);
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.rd_article_update_failure), Toast.LENGTH_LONG)
                            .show();
            finish();
        }
        mCurrentListState = STATE_NORMAL;
    }

    private void updateRefreshTime(long time) {
        mLastUpdateTime = System.currentTimeMillis();
        if (mListView != null) {
            mListView.setLastUpdated("上次刷新: " + new Date(time > 0 ? time : mLastUpdateTime).toLocaleString());
        }
    }

    private void showNoCacheError(int state, boolean toShow) {
        if (toShow) {
            mNoCacheErrorShowing = true;
            mListEmptyView.setVisibility(View.VISIBLE);
            findViewById(R.id.rd_article_list_empty).setVisibility(View.VISIBLE);
            if (state == ArticleUtils.FAILURE) {
                findViewById(R.id.rd_empty_refresh).setVisibility(View.VISIBLE);
                mListEmptyView.setImageResource(Settings.isNightMode() ? R.drawable.rd_article_loading_exception_night
                                : R.drawable.rd_article_loading_exception);
            } else if (state == ArticleUtils.NOEXIST) {
                findViewById(R.id.rd_empty_refresh).setVisibility(View.GONE);
                mListEmptyView.setImageResource(R.drawable.rd_no_article);
            }
        }else{
            mNoCacheErrorShowing = false;
            findViewById(R.id.rd_article_list_empty).setVisibility(View.GONE);
        }
    }

    /*
     * (non-Javadoc)
     * @see com.qihoo360.reader.listener.OnGetArticlesResultListener#onFailure(int)
     */
    @Override
    public void onFailure(int error) {
        if (mNoCache) {
            if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
                mLoadingDialog.dismiss();
            }
            showNoCacheError(ArticleUtils.FAILURE, true);
            mNoCache = true;
        } else {
            if (mCurrentListState == STATE_REFRESHING) {
                updateRefreshTime(-1);
                showTipsWidthTitle(getString(R.string.rd_article_network_expception));
            }
            mListView.onCompletion(ArticleUtils.FAILURE, -1);
        }
        mCurrentListState = STATE_NORMAL;
    }

    /*
     * (non-Javadoc)
     * @see com.qihoo360.reader.listener.OnGetArticlesResultListener#onNotExists()
     */
    @Override
    public void onNotExists(boolean isDeleted) {
        if (mNoCache) {
            if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
                mLoadingDialog.dismiss();
            }
            showNoCacheError(ArticleUtils.NOEXIST, true);
            mNoCache = false;
        } else {
            if (mCurrentListState == STATE_REFRESHING) {
                updateRefreshTime(-1);
                // 已是最新的
                showTipsWidthTitle(getString(R.string.rd_article_no_update));

                if(isDeleted) {
                    mListView.resetLastRequireState();
                }
            }
            mListView.onCompletion(ArticleUtils.NOEXIST, 0);
        }
        mCurrentListState = STATE_NORMAL;
    }

    /*
     * (non-Javadoc)
     * @see com.qihoo360.reader.ui.articles.ArticleListView.OnRequestListener#onRefresh()
     */
    @Override
    public void onRefresh(boolean isAutoRefresh) {
        UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Article_List_Pull_To_Refresh, 1);
        Utils.debug("Kang", "Pull To Refreshing.....");
        ContentResolver cr = getContentResolver();
        // 如果该频道正在离线下载，则等待离线完成后直接提示用户已更新
        if (mChannel.offline().isRunningInArticle()) {
            mCurrentListState = STATE_DELAY_REFRESH;
        }
        // 如果该频道离线下载不过2小时，且没有进入过，则直接提示用户已离线下载条数
        /*else if (mChannel.offline().isOfflinedAndIn2Hours(cr)) {
            if (!isAutoRefresh) {
                mChannel.getNewArticlesAsync(cr, this, true);
                mCurrentListState = STATE_REFRESHING;
            } else {
                mCurrentListState = STATE_NORMAL;
                int count = mChannel.getSubscribedChannel(cr).offline_count;
                showTipsWidthTitle("已离线下载 " + count + " 篇文章");
                mListView.onCompletion(ArticleUtils.SUCCESS, 0);
            }
            mChannel.getSubscribedChannel(cr).setOfflineTime(cr, 0);
        }*/
        // 若果没有在离线下载，或离线下载过了2小时
        else {
            mChannel.getNewArticlesAsync(cr, this, true);
            mCurrentListState = STATE_REFRESHING;
        }
        cr = null;
    }

    /*
     * (non-Javadoc)
     * @see com.qihoo360.reader.ui.articles.ArticleListView.OnRequestListener#onLoadOld()
     */
    @Override
    public void onLoadOld() {
        UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Article_List_Fling_To_Load_More, 1);
        UXHelper.getInstance().addActionRecord(getPvHipId(), 1);
        mChannel.getOldArticlesAsync(getContentResolver(), this, true);
        mCurrentListState = STATE_LOADING;
        Utils.debug("onLoading", "State: " + getString(R.string.rd_article_load_more));
    }

    @Override
    public void onLoading() {
        if (mCurrentListState == STATE_REFRESHING)
            Utils.debug("onLoading", "State: Refreshing");
        else if (mCurrentListState == STATE_LOADING)
            Utils.debug("onLoading", "State: Loading");
    }

    @Override
    public void onCancel() {
        if (mChannel != null)
            mChannel.stopGet();
    }


    @Override
    public boolean shouldPaddingFooter() {
        if(mMenu!=null)
            return mMenu.getBottomBarVisibility() == View.VISIBLE;
        return false;
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#finish()
     */
    @Override
    public void finish() {
        if (mHolderRunable != null) {
            mListTipsView.getHandler().removeCallbacks(mHolderRunable);
        }

        if (mListerAdapter != null) {
            mListerAdapter.reset();
        }

        if (mLoadingDialog != null) {
            if (mLoadingDialog.isShowing())
                mLoadingDialog.dismiss();
            mLoadingDialog = null;
        }

        if (mChannel != null) {
            mChannel.stopGet();
            try {
                mChannel.getSubscribedChannel(getContentResolver()).setLastRefreshTime(getContentResolver(),
                                mLastUpdateTime);
            } catch (Exception e) {
            }
            ArticleUtils.removeDisplaying(mChannel);
        }

        if (mChannelState == STATE_ADD && mChannel.isSubscribed(getContentResolver()) == false) {
            if (mChannel != null) {
                mChannel.subscribe(getContentResolver());
                CollectSubScribe.getInstance()
                                .updateSubScribeValue(
                                                mChannel.channel,
                                                Integer.parseInt(String.valueOf((System.currentTimeMillis() - mStartTimeByMillis) / 1000)));
            }
        } else if (mChannelState == STATE_DELETE && mChannel.isSubscribed(getContentResolver()) == true) {
            mChannel.unsubscribe(getContentResolver());
        } else {
            if (mChannel.isSubscribed(getContentResolver())) {
                CollectSubScribe.getInstance()
                                .updateSubScribeValue(
                                                mChannel.channel,
                                                Integer.parseInt(String.valueOf((System.currentTimeMillis() - mStartTimeByMillis) / 1000)));
            }
        }

        CollectSubScribe cs = CollectSubScribe.getInstance();
        cs.updateSubScribeValue(CollectSubScribe.READER_USED_DAILY_TIME,
                        Integer.parseInt(String.valueOf((System.currentTimeMillis() - mStartTimeByMillis) / 1000)));
        cs = null;

        if (!mIsRandomChannel && !mBackToDesktop) {
            ReaderPlugin.mIsRunning = false;
            if(!ReaderPlugin.getBrowserActivityRunning()) {
                ReaderPlugin.bringBrowserForeground(this);
            }
        }

        long lastCheckImageCacheDate = Settings.getLastCheckImageCacheDate();
        long time = System.currentTimeMillis();
        if (lastCheckImageCacheDate == 0) {
            Settings.setLastCheckImageCacheDate(time);
        } else if (time - lastCheckImageCacheDate > ImageUtils.CHECK_IMAGE_CACHE_DURATION) {
            ImageUtils.ensureImageCacheSize();
            Settings.setLastCheckImageCacheDate(time);
        }


        super.finish();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            findViewById(R.id.top_subscribe_tips).setVisibility(View.GONE);
        }
        return super.dispatchTouchEvent(ev);
    }

    private void changeFavUI(final boolean fav) {
        /*
        final ImageView favBtn = (ImageView) findViewById(R.id.menu_subscription);
        Animation animOut = new AlphaAnimation(1.0f, 0.0f);
        animOut.setDuration(300);
        favBtn.startAnimation(animOut);
        animOut.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (!fav) {
                    favBtn.setImageResource(R.drawable.rd_rss_add_btn);

                } else {
                    favBtn.setImageResource(R.drawable.rd_rss_add_btn_subscried);
                }
                Animation animIn = new AlphaAnimation(0.0f, 1.0f);
                animIn.setDuration(300);
                favBtn.startAnimation(animIn);
            }
        });
        */
    }

    private OnClickListener mListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.top_subscribe_btn && !mSubscribed) {
                doSubscribe();
            }
        }
    };

    /*private void updateSwitchPicModeView() {
        if (Settings.isNoPicMode()) {
            mSwitchPicModeView.setImageResource(R.drawable.rd_article_top_pic);
        } else {
            mSwitchPicModeView.setImageResource(R.drawable.rd_article_top_no_pic);
        }
    }*/
}
