package com.qihoo360.reader;

import java.util.ArrayList;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;

import com.qihoo.ilike.manager.IlikeManager;
import com.qihoo.ilike.manager.LoginManager;
import com.qihoo.ilike.subscription.IlikeChannel;
import com.qihoo.ilike.subscription.OnDownloadCategoryListResultListener;
import com.qihoo.ilike.ui.ILikeImageChannelActivity;
import com.qihoo360.reader.listener.CommonListener;
import com.qihoo360.reader.listener.OnDownloadIndexResultListener;
import com.qihoo360.reader.push.PushManager;
import com.qihoo360.reader.subscription.SubscribedChannel;
import com.qihoo360.reader.subscription.reader.RssManager;
import com.qihoo360.reader.subscription.reader.RssSortedChannel;
import com.qihoo360.reader.subscription.reader.RssSubscribedChannel;
import com.qihoo360.reader.support.UiUtils;
import com.qihoo360.reader.support.Utils;
import com.qihoo360.reader.ui.Nightable;
import com.qihoo360.reader.ui.ReaderMenuContainer;
import com.qihoo360.reader.ui.articles.ArticleReadActivity;
import com.qihoo360.reader.ui.channels.ArticleCollectionActivity;
import com.qihoo360.reader.ui.channels.ImageAdapter;
import com.qihoo360.reader.ui.channels.ImageAdapter.OnSubscriptionRemoveListener;
import com.qihoo360.reader.ui.channels.RandomReadActivity;
import com.qihoo360.reader.ui.channels.SubscribedActivity;
import com.qihoo360.reader.ui.imagechannel.ImageChannelActivity;
import com.qihoo360.reader.ui.offline.OfflineActivity;

public class ReaderMainPageActivity extends Activity implements Nightable,
        OnSubscriptionRemoveListener {

    private GridView mGridView;
    private ImageAdapter mHomeAdapter;

    public static final String SUBSCRIBED_CHANNEL = "subscribed";
    public static final String DATABASE_INITIALIZED = "database_initialized";
    private Cursor mCursor;
    private ReaderMenuContainer mMenu;

    private BroadcastReceiver mNightableReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            onUpdateNightMode();
            mMenu.updateMenuStatus();
        }
    };

    private OnScrollListener mScrollListener = new OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (scrollState != OnScrollListener.SCROLL_STATE_IDLE) {
            } else {
                mHomeAdapter.updateImages(view);
            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                int visibleItemCount, int totalItemCount) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rd_home_main_layout);

        /*
         * Utils.backupDatabase(this); Utils.backupSharedPreferences(this);
         * Utils.restoreDataBase(this);
         */

        IntentFilter nightableFilter = new IntentFilter(
                Constants.READER_BROADCAST_SWITCH_THE_DAY_AND_NIGHT_MODE);
        registerReceiver(mNightableReceiver, nightableFilter);

        init();
        customHomePageChannel();

        // init bottom bar
        buildButtom();

        // sync data 同步数据
        RssManager.checkIfNeedUpdateAsync(new OnDownloadIndexResultListener() {

            @Override
            public void onUpdated() {
                Utils.debug(getClass(), "Channel: Updated Complete!");
                RssManager.getIndex();
            }

            @Override
            public void onFailure() {
                Utils.debug(getClass(), "Channel: Updated Error!");
            }

            @Override
            public void onAlreadyLastestVersion() {
                Utils.debug(getClass(),
                        "SortedChannel: Updated- Already Lastest Version!");
            }
        });

        IlikeChannel.checkIfNeedUpdateAsync(new OnDownloadCategoryListResultListener() {

            @Override
            public void onUpdated() {
                Utils.debug(getClass(), "Channel: Updated Complete!");
                RssManager.getIndex();
            }

            @Override
            public void onFailure() {
                Utils.debug(getClass(), "Channel: Updated Error!");
            }

            @Override
            public void onAlreadyLastestVersion() {
                Utils.debug(getClass(),
                        "SortedChannel: Updated- Already Lastest Version!");
            }
        });

        RssSortedChannel // 有没有必要的更新
                .checkIfNeedUpdateAsync(new OnDownloadIndexResultListener() {

                    @Override
                    public void onUpdated() {
                        Utils.debug(getClass(),
                                "SortedChannel: Updated Complete!");
                        RssManager.getIndex();
                    }

                    @Override
                    public void onFailure() {
                        Utils.debug(getClass(), "SortedChannel: Updated Error!");
                    }

                    @Override
                    public void onAlreadyLastestVersion() {
                        Utils.debug(getClass(),
                                "SortedChannel: Updated- Already Lastest Version!");
                    }
                });

        PushManager.start(this);
    }

    private void init() {
        mGridView = (GridView) findViewById(R.id.gridview);
        mMenu = (ReaderMenuContainer) findViewById(R.id.menu_layout);
        mCursor = RssSubscribedChannel.getCursor(this, getContentResolver());
        mHomeAdapter = new ImageAdapter(ReaderMainPageActivity.this, mCursor,
                this);
        mGridView.setAdapter(mHomeAdapter);
        mGridView.setOnScrollListener(mScrollListener);

        mGridView.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                    int position, long id) {
                if (mHomeAdapter.getDeleteCondition() == false) {
                    mHomeAdapter.setDeleteCondition(true);
                    mHomeAdapter.notifyDataSetChanged();
                }
                return true;
            }
        });

        mGridView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                mTapConsumed = true;

                final SubscribedChannel sub = (SubscribedChannel) view.getTag();
                if (mHomeAdapter.getDeleteCondition() == true) {
                    /*
                     * if (sub.channel.equals(MY_CONLLECTION) ||
                     * sub.channel.equals(RANDOM_READ) ||
                     * sub.channel.equals(ADD_SUBSCRIBE)) { return; } Animation
                     * animation = AnimationHelper.animationScaleHide();
                     * view.startAnimation(animation);
                     * animation.setAnimationListener(new AnimationListener() {
                     *
                     * @Override public void onAnimationStart(Animation
                     * animation) { }
                     *
                     * @Override public void onAnimationRepeat(Animation
                     * animation) { }
                     *
                     * @Override public void onAnimationEnd(Animation animation)
                     * { sub.unsubscribe(getContentResolver()); } });
                     */

                    return;
                }
                Intent intent;
                if (sub.channel.equals(RssSubscribedChannel.SITE_NAVIGATION)) {
                    //网址导航
                    intent = new Intent(ReaderMainPageActivity.this,
                            ArticleCollectionActivity.class);
                    startActivity(intent);
                } else if (sub.channel
                        .equals(RssSubscribedChannel.OFFLINE_DOWNLOAD)) {
                    // 离线下载
                    OfflineActivity.startActivity(ReaderMainPageActivity.this,
                            true);
                } else if (sub.channel.equals(RssSubscribedChannel.RANDOM_READ)) {
                    // 随便看看
                    intent = new Intent(ReaderMainPageActivity.this,
                            RandomReadActivity.class);
                    startActivity(intent);
                } else if (sub.channel
                        .equals(RssSubscribedChannel.ADD_SUBSCRIBE)) {
                    // 添加订阅
                    intent = new Intent(ReaderMainPageActivity.this,
                            SubscribedActivity.class);
                    ArrayList<String> subScribedChannel = new ArrayList<String>();
                    if (mCursor != null) {
                        for (int i = 0; i < mCursor.getCount(); i++) {
                            if (mCursor.moveToFirst()) {
                                mCursor.move(i);
                                SubscribedChannel sub1 = RssSubscribedChannel
                                        .inject(mCursor);
                                subScribedChannel.add(sub1.channel);
                            }
                        }
                    }
                    intent.putStringArrayListExtra(SUBSCRIBED_CHANNEL,
                            subScribedChannel);
                    startActivity(intent);
                    mMenu.hideMenuOrBottomBar();
                    mHomeAdapter.setDeleteCondition(false);

                } else if (sub.getChannel() != null) {
                    if (sub.getChannel().type == 3) {
                        intent = new Intent(ReaderMainPageActivity.this,
                                ImageChannelActivity.class);
                    } else {
                        intent = new Intent(ReaderMainPageActivity.this,
                                ArticleReadActivity.class);
                    }
                    intent.putExtra("channel", sub.channel);
                    startActivity(intent);
                }
                mHomeAdapter.setDeleteCondition(false);
            }
        });
    }

    private void customHomePageChannel() {
        RssManager.initalizeWhenInstalledOrUpgrade(getContentResolver());
        IlikeManager.initalizeWhenInstalledOrUpgrade(getContentResolver());

        if (RssSubscribedChannel.isNeedRefreshSortFloat()) {
            RssSubscribedChannel.calculateSortFloat(getContentResolver());
        }
    }

    private void buildButtom() {
        mMenu.setBottomBarListener(new CommonListener() {

            @Override
            public void actionPerform(int resId) {
                if (resId == R.id.menu_myfavour) {
                    if (Constants.DEBUG) {
                        LoginManager
                                .testLogin(
                                        "153105310",
                                        "u%3D360H153105310%26r%3D%26qid%3D153105310%26im%3D190144aq118a4e%26s%3D360%26src%3Dwoxihuan%26t%3D1%26le%3Djiongj.zhang%40yahoo.com",
                                        "s%3Dd1fc84ab0443cfba412a4fe3a91f7e85%26t%3D1336709363%26a%3D1%26v%3D1.0");
                    }
                    mMenu.hideMenuOrBottomBar();
                } else if (resId == R.id.menu_add_channel) {
                    Intent intent = new Intent();
                    intent.setClass(getApplicationContext(),
                            ILikeImageChannelActivity.class);
                    intent.putExtra(
                            "channel",
                            IlikeChannel.get(IlikeChannel.ILIKE_HOT_COLLECTION).channel);
                    startActivity(intent);
                    mMenu.hideMenuOrBottomBar();
                } else if (resId == R.id.menu_delete) {
                    /*
                     * if (mHomeAdapter.getDeleteCondition() == false) {
                     * mHomeAdapter.setDeleteCondition(true); } else {
                     * mHomeAdapter.setDeleteCondition(false); }
                     * mHomeAdapter.notifyDataSetChanged();
                     */
                    mMenu.hideMenuOrBottomBar();
                    OfflineActivity.startActivity(ReaderMainPageActivity.this,
                            false);
                }
            }
        });
    }

    public boolean onKeyUp(int keyCode, android.view.KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_MENU) {
            mMenu.onMenuClick();
            return true;
        }

        return super.onKeyUp(keyCode, event);
    };

    public void onBackPressed() {
        if (mMenu.onBackBtnPressed() == true) {
            return;
        }
        if (mHomeAdapter.getDeleteCondition() == true) {
            mHomeAdapter.setDeleteCondition(false);
            mHomeAdapter.notifyDataSetChanged();
            return;
        }
        super.onBackPressed();
    };

    protected void onResume() {
        super.onResume();
        mMenu.onResume();
        onUpdateNightMode();
    };

    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mNightableReceiver);
    }

    @Override
    public void onUpdateNightMode() {
        if (Settings.isNightMode() == true) {
            UiUtils.insertNightMask(this, getWindow().getDecorView());
            findViewById(R.id.container).setBackgroundColor(
                    getResources().getColor(R.color.rd_night_bg));
        } else {
            UiUtils.removeNightMask(this, getWindow().getDecorView());
            findViewById(R.id.container).setBackgroundColor(
                    getResources().getColor(R.color.rd_bg));
        }

        mHomeAdapter.notifyDataSetChanged();
    };

    Runnable mCancelDeleteConditionRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mTapConsumed && mHomeAdapter.getDeleteCondition()) {
                mHomeAdapter.setDeleteCondition(false);
                mHomeAdapter.notifyDataSetChanged();
            }
        }
    };

    private GestureDetector mGestureDetector = new GestureDetector(
            new GestureDetector.SimpleOnGestureListener() {
                public boolean onSingleTapUp(MotionEvent e) {
                    return true;
                };
            });

    private boolean mTapConsumed = false;
    Handler mHandler = new Handler();

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mHandler.removeCallbacks(mCancelDeleteConditionRunnable);
            mTapConsumed = false;
        }

        if (mGestureDetector.onTouchEvent(event)) {
            // single tap detected
            if (!mTapConsumed) {
                mHandler.postDelayed(mCancelDeleteConditionRunnable, 200);
            }
        }

        return super.dispatchTouchEvent(event);
    }

    @Override
    public void finish() {
        mMenu.saveBottomBarState();
        super.finish();
    }

    @Override
    public void onSubscriptionRemoved() {
        mTapConsumed = true;
    }
}
