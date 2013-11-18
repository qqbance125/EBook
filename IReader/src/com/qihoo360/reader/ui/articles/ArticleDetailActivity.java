package com.qihoo360.reader.ui.articles;

import com.qihoo.ilike.data.DataEntryManager;
import com.qihoo.ilike.http.core.IHttpRequest.HttpRequestStatus;
import com.qihoo.ilike.json.listener.OnILikeItPostResultListener;
import com.qihoo.ilike.manager.IlikeManager;
import com.qihoo.ilike.manager.IlikeManager.LikeType;
import com.qihoo.ilike.ui.ILikeArticleDetailAdapter;
import com.qihoo.ilike.vo.ErrorInfo;
import com.qihoo360.browser.hip.UXHelper;
import com.qihoo360.browser.hip.UXHelperConfig;
import com.qihoo360.reader.Constants;
import com.qihoo360.reader.R;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.listener.CommonListener;
import com.qihoo360.reader.listener.OnGetArticlesResultListener;
import com.qihoo360.reader.subscription.Article;
import com.qihoo360.reader.subscription.Channel;
import com.qihoo360.reader.subscription.StaredArticle;
import com.qihoo360.reader.subscription.reader.RssChannel;
import com.qihoo360.reader.support.Utils;
import com.qihoo360.reader.ui.ActivityBase;
import com.qihoo360.reader.ui.CommonUtil;
import com.qihoo360.reader.ui.ReaderMenuContainer;
import com.qihoo360.reader.ui.ReaderMenuContainer.OnControllBtnClickListener;
import com.qihoo360.reader.ui.ReaderPlugin;
import com.qihoo360.reader.ui.articles.ArticleDetailAdapter.ArticleDownloadListener;
import com.qihoo360.reader.ui.articles.ArticleReadView.OnOverScrollListener;
import com.qihoo360.reader.ui.view.FontChoiceView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class ArticleDetailActivity extends ActivityBase implements OnOverScrollListener, OnGetArticlesResultListener,
                Callback, ArticleDownloadListener {
    private final String TAG = ArticleDetailActivity.class.getSimpleName();
    protected ArticleReadView mDetailReadView;
    protected ArticleDetailAdapter mDetailAdapter;
    protected ReaderMenuContainer mMenu;
    /**
     * @important
     * if this started by collectionAcitivity,mChannel will set with NULL
     */
    protected Channel mChannel;
    private boolean mHadLoaded = false;
    protected FontChoiceView mFontView;
    private View mOnceTipsView = null;
    boolean mIsMyCollection = false;
    private boolean mScrollChangeTextSizeHadTips = false;
    ArrayList<Long> mUnstarredArticleIndices = null;
    private int mLastRequreResult = ArticleUtils.SUCCESS;
    private boolean fromPush = false;
    private int mDetailViewCurrentPosition = -1;
    private boolean mPendingRequestForMyCollection = false;
    private BroadcastReceiver mBroadcastReceiver = null;
    boolean mStayedWidthTopNews = false;

    private BroadcastReceiver mImageableReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            mDetailReadView.changeImageMode(!Settings.isNoPicMode());
            //updateSwitchPicModeView();
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.rd_article_detail_gallery);

        IntentFilter imageableFilter = new IntentFilter(Constants.READER_BROADCAST_SWITCH_WHETHER_THE_IMAGE_MODE);
        registerReceiver(mImageableReceiver, imageableFilter);

        init();
        buildButtom();
        buildMenu();

        initFontPref();
        //mChannelNameView = (TextView) findViewById(R.id.rd_list_channel_name);
        //mChannelNameView.setText(mChannel != null ? mChannel.title : getString(R.string.rd_article_my_collection));

        /*findViewById(R.id.top_subscribe_btn).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mIsMyCollection) {
                    executeStar();
                } else {
                    Article article = getCurrentArticle();
                    if (isStaredArticle(article)) {
                        Toast.makeText(ArticleDetailActivity.this, "已收藏该文章", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ArticleDetailActivity.this, getString(R.string.rd_article_star),
                                        Toast.LENGTH_SHORT).show();
                        article.markStar(getContentResolver());
                        ((ImageView) findViewById(R.id.top_subscribe_btn))
                                        .setImageResource(R.drawable.rd_article_star_btn_drawable);
                        ((ImageView) mMenu.findViewById(R.id.menu_add_bookmark))
                                        .setImageResource(R.drawable.rd_menu_item_remove_bookmark);
                        mDetailAdapter.changeCursor(mChannel.getArticlesCursor(getContentResolver()));
                    }
                }
            }
        });

        mSwitchPicModeView = (ImageView) findViewById(R.id.top_switch_pic_mode);
        mSwitchPicModeView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Settings.setNoPicMode(ArticleDetailActivity.this, !Settings.isNoPicMode());
                updateSwitchPicModeView();
            }
        });*/
    }

    /**
     * 执行添加文章收藏和删除文章收藏
     * */
    private void executeStar() {
        Article article = getCurrentArticle();
        boolean isStared = onClickFavorite(article);
        if (!isStared) {
            changeFavMenuState(false);
            if (Constants.DEBUG) {
                Toast.makeText(ArticleDetailActivity.this, getString(R.string.rd_cancel_article_star),
                                Toast.LENGTH_SHORT).show();
            }
        } else {
            changeFavMenuState(true);
            Toast.makeText(ArticleDetailActivity.this, getString(R.string.rd_article_star), Toast.LENGTH_SHORT).show();
        }
    }

    protected void changeFavMenuState(boolean isStared) {
        final TextView menuFavBtn = ((TextView) mMenu.findViewById(R.id.menu_add_bookmark));
        if (isStared) {
            menuFavBtn.setText(R.string.rd_cancel_article_star); //  取消收藏
            menuFavBtn.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.rd_menu_item_our_removebookmark_icon, 0, 0);
        } else {
            menuFavBtn.setText(R.string.rd_add_fav); //  添加收藏
            menuFavBtn.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.rd_menu_item_our_addbookmark_icon, 0, 0);
        }
    }

    private void init() {
        infalteLayout();

        Intent intent = getIntent();
        int position = intent.getIntExtra(ArticleUtils.LIST_POSITION, 0);
        Cursor cursor = null;
        String channel = intent.getStringExtra("channel");
        mStayedWidthTopNews = intent.getBooleanExtra("top_news", false);
        if (!channel.equals(ArticleUtils.COLLECTION_LIST)) {
            fromPush = getIntent().getBooleanExtra("from_push", false);
            Channel mChannel = Channel.get(channel);
            cursor = mChannel.getFullCursor(getContentResolver());
            this.mChannel = mChannel;
            if (fromPush) {
                ArticleUtils.addDisplaying(mChannel);
            }
        } else {
            mIsMyCollection = true;
            mUnstarredArticleIndices = new ArrayList<Long>();
            mDetailReadView.setInternalType(ArticleUtils.TYPE_COLLECTION);
            cursor = StaredArticle.getCursor(getContentResolver());
        }
        mDetailReadView.setOnOverScrollListener(this);

        if (cursor != null) {
            if(mChannel instanceof RssChannel || mChannel == null) {
                mDetailAdapter = new ArticleDetailAdapter(this, cursor);
            } else {
                mDetailAdapter = new ILikeArticleDetailAdapter(this, cursor);
            }

            mDetailAdapter.reference(mChannel, new Handler(this), mIsMyCollection);
            mDetailAdapter.setArticleDownloadListener(this);
            mDetailReadView.setAdapter(mDetailAdapter, position);
            mDetailViewCurrentPosition = position;
            if (!channel.equals(ArticleUtils.COLLECTION_LIST)) {
                ((Article) mDetailAdapter.getItem(position)).markRead(getContentResolver());
            }
        }
    }

    private void infalteLayout() {
        mDetailReadView = (ArticleReadView) findViewById(R.id.rd_article_gallery);
        mMenu = (ReaderMenuContainer) findViewById(R.id.menu_layout);
        mFontView = (FontChoiceView) findViewById(R.id.reader_font_choice);
        //mTitleBar = findViewById(R.id.rd_article_detail_title_bar);
    }

    protected void buildButtom() {
        Article article = getCurrentArticle();
        if (mIsMyCollection || isStaredArticle(article)) {
            changeFavMenuState(true);
        }

        updateLikeBtnStatues(article);

        mMenu.setBottomBarListener(new CommonListener() {
            @Override
            public void actionPerform(int resId) {
                if (resId == R.id.menu_back_btn) {
                    handleBackBtnClicked();
                } else if (resId == R.id.menu_share_btn) {
                   handleShareBtnClicked();
                } else if (resId == R.id.menu_likeit_btn) {
                    handleLikedBtnClicked();
                    mMenu.hideMenuOrBottomBar();
                } else if (resId == R.id.menu_menu) {
                    onKeyUp(KeyEvent.KEYCODE_MENU, null);
                }
            }
        });
    }

    protected void handleBackBtnClicked() {
        UXHelper.getInstance()
                .addActionRecord(UXHelperConfig.Reader_Article_Detial_Back_With_Action_Bar, 1);
        finish();
    }

    protected void handleShareBtnClicked() {
        Article article = getCurrentArticle();
        UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Article_Detail_Share_Article, 1);
        CommonUtil.sharePage(ArticleDetailActivity.this, article.title, article.link);
        mFontView.setVisibility(View.GONE);
    }

    private boolean onClickFavorite(Article article) {
        boolean starred;
        if (mIsMyCollection) {
            //  添加收藏
            UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Article_Detail_Collection_Article, 1);
            if (mUnstarredArticleIndices.contains(article.contentid)) {
                starred = true;
                mUnstarredArticleIndices.remove(article.contentid);
            } else {
                starred = false;
                mUnstarredArticleIndices.add(article.contentid);
            }
        } else {
            // 取消收藏
            UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Article_Detail_Cancel_Collection, 1);
            if (isStaredArticle(article) == true) {
                starred = false;
                article.unmarkStar(getContentResolver());
            } else {
                starred = true;
                article.markStar(getContentResolver());
            }
            updateCursor();
        }

        return starred;
    }

    protected boolean isStaredArticle(Article article) {
        //Article article = getCurrentArticle();
        if (mIsMyCollection
                        && !mUnstarredArticleIndices.contains(article.contentid)
                        || (!mIsMyCollection && (article.star == Article.STAR_APPEARANCE || article.star == Article.STAR_DISAPPEARANCE))) {
            return true;
        }
        return false;
    }

    private void buildMenu() {
        OnClickListener listener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.small) {
                    UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Menu_Font_Small_Selected, 1);
                    mDetailReadView.changeTextSize(ArticleDetailAdapter.TEXT_SIZE_SMALL);
                } else if (v.getId() == R.id.middle) {
                    UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Menu_Font_Middle_Selected, 1);
                    mDetailReadView.changeTextSize(ArticleDetailAdapter.TEXT_SIZE_MIDDLE);
                } else if (v.getId() == R.id.large) {
                    UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Menu_Font_Large_Selected, 1);
                    mDetailReadView.changeTextSize(ArticleDetailAdapter.TEXT_SIZE_LARGE);
                }
                mFontView.changFontSelection(ArticleDetailAdapter.mTextSize);
            }
        };
        findViewById(R.id.small).setOnClickListener(listener);
        findViewById(R.id.middle).setOnClickListener(listener);
        findViewById(R.id.large).setOnClickListener(listener);

        mMenu.setOnControllBtnClick(new OnControllBtnClickListener() {

            @Override
            public void onControllClick(int visibility) {
                if (mFontView.getVisibility() == View.VISIBLE) {
                    mFontView.setVisibility(View.GONE);
                }
            }
        });

        mMenu.findViewById(R.id.menu_add_bookmark).setEnabled(true);
        mMenu.setMenuClickListener(new CommonListener() {

            @Override
            public void actionPerform(int resId) {
                if (resId == R.id.menu_night_mode) {
                    UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Menu_Nightly_Mode, 1);
                    Settings.setNightMode(ArticleDetailActivity.this, !Settings.isNightMode());
                    mMenu.hideMenuOrBottomBar();
                } else if (resId == R.id.menu_text_size) {
                    if (mFontView.getVisibility() == View.GONE) {
                        mFontView.setVisibility(View.VISIBLE, true);
                        mMenu.hideMenu();
                        mMenu.setBottomBarVisibility(View.VISIBLE);
                    }
                } else if (resId == R.id.menu_add_bookmark) {
                    executeStar();
                    mMenu.hideMenuOrBottomBar();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (mMenu.onBackBtnPressed() == true) {
            return;
        } else if (mFontView.getVisibility() == View.VISIBLE) {
            mFontView.setVisibility(View.GONE, true);
            mMenu.restoreBottomBarState();
            return;
        } else if (mOnceTipsView != null) {
            ((ViewGroup) getWindow().getDecorView()).removeView(mOnceTipsView);
            //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            mOnceTipsView = null;
        } else {
            finish();
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, android.view.KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (mOnceTipsView != null) {
                ((ViewGroup) getWindow().getDecorView()).removeView(mOnceTipsView);
                mOnceTipsView = null;
            } else {
                toggleMenuView();
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    };

    private int[] mLocation = new int[2];

    //private boolean mDoubleTapDetected = false;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        float downY = ev.getY();
        int action = ev.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            if (mFontView.getVisibility() == View.VISIBLE) {
                mFontView.getLocationOnScreen(mLocation);
                if (downY < mLocation[1]) {
                    mFontView.setVisibility(View.GONE, true);
                    mMenu.setBottomBarVisibility();
                    return true;
                } /*else if (downY > mLocation[1] + mFontView.getMeasuredHeight()) {
                    mFontView.setVisibility(View.GONE, true);
                    return true;
                  }*/
            }

            mDetailAdapter.enbaleSingleTap();
        }

        return super.dispatchTouchEvent(ev);
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();
        mMenu.onResume();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //updateSwitchPicModeView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMenu.saveBottomBarState();
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onDestroy()
     */
    @Override
    protected void onDestroy() {
        Cursor cursor = mDetailAdapter.getCursor();
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        saveFontPref();
        unregisterReceiver(mImageableReceiver);
        if(mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
            mBroadcastReceiver = null;
        }

        super.onDestroy();
    }

    private void initFontPref() {
        SharedPreferences sp = Settings.getSharedPreferences();
        mScrollChangeTextSizeHadTips = sp.getBoolean(ArticleUtils.SCROLL_CHANGE_TEXT_SIZE_TIP, false);
        if (!mScrollChangeTextSizeHadTips) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean(ArticleUtils.SCROLL_CHANGE_TEXT_SIZE_TIP, true);
            editor.commit();
        }

        ArticleDetailAdapter.mTextSize = Settings.getSharedPreferences().getInt("text_size",
                        ArticleDetailAdapter.TEXT_SIZE_MIDDLE);
        mFontView.changFontSelection(ArticleDetailAdapter.mTextSize);

        if (!mScrollChangeTextSizeHadTips) {
            mOnceTipsView = LayoutInflater.from(this).inflate(R.layout.rd_article_detail_once_tips, null);
            final ViewGroup rootView = (ViewGroup) getWindow().getDecorView();
            rootView.addView(mOnceTipsView);
            //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            mOnceTipsView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rd_article_list_tips_show));
            mOnceTipsView.findViewById(R.id.rd_article_detail_tips_btn).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    rootView.removeView(mOnceTipsView);
                    //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    mOnceTipsView = null;
                }
            });
        }
    }

    private void saveFontPref() {
        Settings.getSharedPreferences().edit().putInt("text_size", ArticleDetailAdapter.mTextSize).commit();
    }

    @Override
    public void finish() {
        if (mChannel != null) {
            mChannel.stopGet();
        }

        if (fromPush) {
            ArticleUtils.removeDisplaying(mChannel);
            if (ReaderPlugin.mIsRunning) {
                super.finish();
            } else {
                startActivity(new Intent(this, ArticleReadActivity.class).putExtra("channel", mChannel.channel)
                                .putExtra("from_push", true));
            }
        }

        Intent intent = new Intent();
        intent.putExtra(ArticleUtils.DETAIL_POSITION, mDetailViewCurrentPosition)
                        .putExtra(ArticleUtils.DETAIL_LOADED, mHadLoaded)
                        .putExtra(ArticleUtils.DETAIL_LOADED_RESULT, mLastRequreResult)
                        .putExtra("stay_with_top_news", mStayedWidthTopNews);

        if (mIsMyCollection && mUnstarredArticleIndices.size() > 0) {
            long[] unstarredArticleIndices = new long[mUnstarredArticleIndices.size()];
            for (int i = 0; i < mUnstarredArticleIndices.size(); i++) {
                unstarredArticleIndices[i] = mUnstarredArticleIndices.get(i);
            }
            intent.putExtra(ArticleUtils.UNSTARRED_ARTICLE_INDICES, unstarredArticleIndices);
        }
        setResult(this.hashCode(), intent);
        super.finish();
    }

    @Override
    public void onUpdateNightMode() {
        super.onUpdateNightMode();
        setNightMask();

        for (View headerView : mDetailAdapter.mHeaderViewMap.values()) {
            TextView titleView = (TextView) headerView.findViewById(R.id.rd_article_title);
            TextView timeView = (TextView) headerView.findViewById(R.id.rd_article_time);
            // 夜间模式的判断
            Resources rs = getResources();
            if (Settings.isNightMode() == true) {
                titleView.setTextColor(rs.getColor(R.color.rd_night_text));
                titleView.setBackgroundColor(rs.getColor(R.color.rd_night_bg));
                if(timeView != null) {
                    timeView.setTextColor(rs.getColor(R.color.rd_night_text));
                }
                mDetailReadView.setBackgroundColor(rs.getColor(R.color.rd_night_bg));
                findViewById(R.id.rd_title_bottom_line).setBackgroundResource(R.drawable.rd_title_bottom_cover_nightly);
            } else {
                titleView.setBackgroundColor(rs.getColor(R.color.rd_article_detail_bg));
                titleView.setTextColor(rs.getColor(R.color.rd_black));
                if(timeView != null) {
                    timeView.setTextColor(rs.getColor(R.color.rd_article_content_time));
                }
                mDetailReadView.setBackgroundColor(rs.getColor(R.color.rd_article_detail_bg));
                findViewById(R.id.rd_title_bottom_line).setBackgroundResource(R.drawable.rd_title_bottom_cover);
            }
            rs = null;
        }
        mDetailReadView.onUpdateNightMode();
    }

    private boolean isLoadingProcess = false;

    /*
     * @see com.qihoo360.reader.ui.articles.ArticleReadView.OnOverScrollListener#onStartLoad()
     */
    @Override
    public boolean onStartLoad() {
        if (mChannel != null && !isLoadingProcess) {
            mChannel.getOldArticlesAsync(getContentResolver(), this, true);
            Utils.debug(TAG, "Channel Name: " + mChannel.channel + "| Begin Loading...");
            isLoadingProcess = true;
        }
        return true;
    }

    /*
     * @see com.qihoo360.reader.ui.articles.ArticleReadView.OnOverScrollListener#onLoading()
     */
    @Override
    public void onLoading() {
        ((TextView) mDetailReadView.findViewById(R.id.rd_article_detail_loading_tips))
                        .setText(getString(R.string.rd_article_loading_process));
    }

    /*
     * @see com.qihoo360.reader.ui.articles.ArticleReadView.OnOverScrollListener#onSwitched(int)
     */
    @Override
    public void onSwitched(int position) {
        try {
            Article article = (Article) mDetailAdapter.getItem(position);
            changeFavMenuState(isStaredArticle(article));

            if (article != null) {
                article.markRead(getContentResolver());
            }

            updateLikeBtnStatues(article);

            mDetailViewCurrentPosition = position;
        } catch (Exception e) {
        }
        mStayedWidthTopNews = false;
    }

    /*
     * @see com.qihoo360.reader.listener.OnGetArticlesResultListener#onCompletion(long, long)
     */
    @Override
    public void onCompletion(long getFrom, long getTo, final int getCount, boolean isDeleted) {
        mHadLoaded = true;
        //int lastCount = mDetailReadView.getLastPosition() + 1;
        mLastRequreResult = ArticleUtils.SUCCESS;
        updateCursor();
        /*Article article = (Article) mDetailAdapter.getItem(lastCount);
        Utils.debug(TAG, "Current Position: " + mDetailReadView.getPosition() + "|Title: "
                        + (article == null ? "NULL" : article.title));
        Utils.debug(TAG, "Channel Name: " + mChannel.channel + " | 成功加载 " + getCount + " 篇文章");
        if (article != null) {
            Utils.debug(TAG, "Info: Begin Fill this Aritlce(Title:" + article.title + ")'s content...");
            article.fill(getContentResolver(), new OnFillArticleResultListener() {

                @Override
                public void onFailure(int error) {
                    Utils.debug(TAG,
                                    "Excepton: Get article content failure! (ArtilceDetailActivity#onCompletion(long,long,int))");
                    mDetailReadView.onCompletion(ArticleUtils.SUCCESS, getCount);
                }

                @Override
                public void onCompletion() {
                    Utils.debug(TAG, "Success! Get article content success...");
                    mDetailReadView.onCompletion(ArticleUtils.SUCCESS, getCount);
                }
            });
        } else {*/
        Utils.debug(TAG, "Exception: Article is NULL!() (ArtilceDetailActivity#onCompletion(long,long,int))");
        mDetailReadView.onCompletion(ArticleUtils.SUCCESS, getCount);
        isLoadingProcess = false;
        //}
    }

    /*
     * @see com.qihoo360.reader.listener.OnGetArticlesResultListener#onFailure(int)
     */
    @Override
    public void onFailure(int error) {
        mLastRequreResult = ArticleUtils.FAILURE;
        mDetailReadView.onCompletion(ArticleUtils.FAILURE, -1);
        isLoadingProcess = false;
    }

    /*
     * @see com.qihoo360.reader.listener.OnGetArticlesResultListener#onNotExists()
     */
    @Override
    public void onNotExists(boolean isDeleted) {
        mLastRequreResult = ArticleUtils.NOEXIST;
        mDetailReadView.onCompletion(ArticleUtils.NOEXIST, 0);
        isLoadingProcess = false;
    }

    public void onZoomChanged() {
        mFontView.changFontSelection(-1);
    }

    /*private void updateSwitchPicModeView() {
        if (Settings.isNoPicMode()) {
            mSwitchPicModeView.setImageResource(R.drawable.rd_article_top_pic);
        } else {
            mSwitchPicModeView.setImageResource(R.drawable.rd_article_top_no_pic);
        }
    }*/

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
        case ArticleUtils.MSG_ARTICLE_DETAIL_VIEW_SHOULD_RELOAD:
            Utils.debug(TAG, "Channel Name: " + mChannel.channel + " | => Receive Reload Detail View...");
            mDetailReadView.asynPreLoading();
            return true;
        }
        return false;
    }

    protected Article getCurrentArticle() {
        return (Article) mDetailAdapter.getItem(mDetailReadView.getPosition());
    }

    protected void updateCursor () {
        mDetailAdapter.changeCursor(mChannel.getFullCursor(getContentResolver()));
    }

    protected boolean isArticleUrlLiked(Article article) {
        if (article == null || TextUtils.isEmpty(article.link)) {
            return false;
        }

        return DataEntryManager.urlLiked(getContentResolver(), article.link);
    }

    protected void handleLikedBtnClicked() {
        Article article = getCurrentArticle();
        if(!article.isDownloaded || isArticleUrlLiked(article)) {
            return;
        }

        if(!com.qihoo.ilike.util.Utils.accountConfigured(this)) {
            startWaitingForAccountConfiguration(article);
            return;
        }

        likeArticle(article);
    }

    protected void startWaitingForAccountConfiguration(final Article article) {
        mPendingRequestForMyCollection = true;

        if(mBroadcastReceiver == null) {
            mBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (com.qihoo.ilike.util.Utils.accountConfigSucceed(intent)) {
                        if (mPendingRequestForMyCollection) {
                            likeArticle(article);
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

    protected void likeArticle(Article article) {
        if (!com.qihoo.ilike.util.Utils.checkNetworkStatusBeforeLike(ArticleDetailActivity.this)) {
            return;
        }

        boolean success = IlikeManager.likeUrl(getContentResolver(), article.link, article.title,
                LikeType.Webpage,
                new OnILikeItPostResultListener() {
                    @Override
                    public void onResponseError(ErrorInfo errorInfo) {
                        Toast.makeText(ArticleDetailActivity.this,
                                R.string.ilike_collect_url_failure,
                                Toast.LENGTH_LONG).show();
                        handleLikeFail();
                    }

                    @Override
                    public void onRequestFailure(HttpRequestStatus errorStatus) {
                        Toast.makeText(ArticleDetailActivity.this,
                                R.string.ilike_collect_url_failure,
                                Toast.LENGTH_LONG).show();
                        handleLikeFail();
                    }

                    @Override
                    public void onComplete() {
                        Toast.makeText(ArticleDetailActivity.this,
                                R.string.ilike_collect_url_successful,
                                Toast.LENGTH_LONG).show();
                    }
                });

        if (success) {
            changeLikedBtnStatus();
        }
    }

    protected void handleLikeFail() {
        updateCursor();

        if(!isArticleUrlLiked(getCurrentArticle())) {
            ImageView imageView = (ImageView) findViewById(R.id.menu_likeit_btn);
            imageView.clearAnimation();
            imageView.setImageResource(R.drawable.ilike_likeit_btn_normal);
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

    protected void updateLikeBtnStatues(Article article) {
        if(isArticleUrlLiked(article)) {
            ((ImageView) findViewById(R.id.menu_likeit_btn))
                    .setImageResource(R.drawable.ilike_likeit_btn_liked);
        } else {
            ((ImageView) findViewById(R.id.menu_likeit_btn))
                    .setImageResource(R.drawable.ilike_likeit_btn_normal);
        }
    }

    protected void toggleMenuView() {
        mMenu.onMenuClick();
        mFontView.setVisibility(View.GONE);
    }

    @Override
    public void articleDownloaded(int position, Article article) {
        if(position == mDetailReadView.getPosition()) {
            updateLikeBtnStatues(article);
        }
    }
}
