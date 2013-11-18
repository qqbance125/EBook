package com.qihoo360.reader.ui.channels;

import com.qihoo360.browser.hip.UXHelper;
import com.qihoo360.browser.hip.UXHelperConfig;
import com.qihoo360.reader.Constants;
import com.qihoo360.reader.R;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.listener.CommonListener;
import com.qihoo360.reader.listener.OnDownloadIndexResultListener;
import com.qihoo360.reader.subscription.Category;
import com.qihoo360.reader.subscription.Channel;
import com.qihoo360.reader.subscription.reader.RssChannel;
import com.qihoo360.reader.subscription.reader.RssManager;
import com.qihoo360.reader.subscription.reader.RssSubscribedChannel;
import com.qihoo360.reader.support.AnimationHelper;
import com.qihoo360.reader.support.Utils;
import com.qihoo360.reader.ui.ActivityBase;
import com.qihoo360.reader.ui.CommonUtil;
import com.qihoo360.reader.ui.CustomProgressDialog;
import com.qihoo360.reader.ui.Nightable;
import com.qihoo360.reader.ui.ReaderMenuContainer;
import com.qihoo360.reader.ui.ReaderPlugin;
import com.qihoo360.reader.ui.channels.ScrollLayout.OnSnapToScreenListener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Gallery;
import android.widget.GridView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class AddReaderActivity extends ActivityBase implements Nightable {

    private Gallery mGalleryTitle;
    private ScrollLayout mScrollLayout;
    private View mTitleBar, mSearchBar;
    private GridView mSearchGridView;
    private EditText mSearchText;
    private TitleGalleryAdapter mTitleGalleryAdapter;
    private List<Category> mCategories = new ArrayList<Category>();
    private List<RssChannel> mCheckSubs = new ArrayList<RssChannel>();
    private ArrayList<String> mSubscribedChannel = new ArrayList<String>();
    private AddReaderGridAdapter mSearchDefaultAdapter;
    private int mSelectedNum;
    private Handler mHandler = new Handler();
    private InputMethodManager mInputMethodManager = null;

    private BroadcastReceiver mNightableReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            onUpdateNightMode();
        }
    };

    private Runnable mReduceAdapterCacheRunnable = new Runnable() {
        @Override
        public void run() {
            mScrollLayout.getChildAt(mScrollLayout.getPreScreen()).setVisibility(View.VISIBLE);

            reduceAdapterCache();
        }
    };

    private ReaderMenuContainer mMenu;
    private static final int MAX_ADAPTER_CACHE = 1;
    private ArrayList<AddReaderGridAdapter> mAdapterCache = new ArrayList<AddReaderGridAdapter>();
    private GridView mCurrentScreen = null;
    private OnScrollListener mScrollListener = new OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            AddReaderGridAdapter currentAdapter = (AddReaderGridAdapter) mCurrentScreen.getAdapter();
            if (scrollState == OnScrollListener.SCROLL_STATE_FLING
                            || scrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                currentAdapter.setBusy(true);
            } else {
                currentAdapter.setBusy(false);
                currentAdapter.updateImages(view);
            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.rd_add_reader);

        mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        IntentFilter nightableFilter = new IntentFilter(Constants.READER_BROADCAST_SWITCH_THE_DAY_AND_NIGHT_MODE);
        registerReceiver(mNightableReceiver, nightableFilter);

        if (Settings.isNightMode() == true) {
            Settings.setNightMode(this, true);
        }

        init();

        AsyncTask<Void, Void, Void> loadDataTask = new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                mCategories = RssManager.getIndex().getCategories();
                mTitleGalleryAdapter = new TitleGalleryAdapter(mCategories, AddReaderActivity.this);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                mGalleryTitle.setAdapter(mTitleGalleryAdapter);
                GridView gView = null;
                AddReaderGridAdapter adapter = null;
                int defaultChannel = -1;
                for (int i = 0; i < mCategories.size(); i++) {
                    gView = (GridView) LayoutInflater.from(AddReaderActivity.this).inflate(R.layout.rd_subscribe_gridview,
                                    null);
                    gView.setOnItemClickListener(mItemClickListener);

                    adapter = new AddReaderGridAdapter(AddReaderActivity.this, mCategories.get(i).getChannels());
                    adapter.setSubScribedChannel(mSubscribedChannel);
                    mScrollLayout.addView(gView);
                    if (mCategories.get(i).name.equals("小编推荐")) {
                        defaultChannel = i;
                    }
                    gView.setAdapter(adapter);
                    gView.setScrollbarFadingEnabled(false);
                }
                mGalleryTitle.setSelection(defaultChannel == -1 ? ((mCategories.size()) % 2 == 0 ? mCategories.size() / 2
                                : mCategories.size() / 2 + 1) : defaultChannel);

                mTitleGalleryAdapter.setHighLightPosition(mGalleryTitle.getSelectedItemPosition());
                mTitleGalleryAdapter.notifyDataSetChanged();
                mScrollLayout.setToScreen(mGalleryTitle.getSelectedItemPosition() - 1);
                mScrollLayout.getChildAt(Math.max(0, mGalleryTitle.getSelectedItemPosition() - 1)).setVisibility(
                                View.INVISIBLE);
                mHandler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        if (mGalleryTitle.getSelectedItemPosition() == 0) {
                            mScrollLayout.scrollBy(-mScrollLayout.getChildAt(0).getMeasuredWidth(), 0);
                            mScrollLayout.getChildAt(0).setVisibility(View.VISIBLE);
                            mScrollLayout.snapToDestination();
                            return;
                        }

                        int currentScreen = mGalleryTitle.getSelectedItemPosition();
                        int duration = mScrollLayout.snapToScreen(currentScreen);

                        AlphaAnimation aa = new AlphaAnimation(0f, 1f);
                        aa.setInterpolator(new DecelerateInterpolator());
                        aa.setDuration(duration);
                        mScrollLayout.getChildAt(currentScreen).startAnimation(aa);

                        aa.setAnimationListener(new AnimationListener() {

                            @Override
                            public void onAnimationStart(Animation paramAnimation) {
                            }

                            @Override
                            public void onAnimationRepeat(Animation paramAnimation) {
                            }

                            @Override
                            public void onAnimationEnd(Animation paramAnimation) {
                                Animation anim = new TranslateAnimation(300, 0, 0, 0);
                                anim.setDuration(300);
                                anim.setInterpolator(new OvershootInterpolator(1f));
                                anim.setFillAfter(true);
                                mTitleBar.startAnimation(anim);
                                mTitleBar.setVisibility(View.VISIBLE);
                                mScrollLayout.getChildAt(Math.max(0, mGalleryTitle.getSelectedItemPosition() - 1))
                                                .setVisibility(View.VISIBLE);
                            }
                        });
                    }
                }, 0);
            }
        };

        //load data
        loadDataTask.execute();

        ReaderPlugin.mIsRunning = true;
    }

    @Override
    public void finish() {
        ReaderPlugin.mIsRunning = false;
        if(!ReaderPlugin.getBrowserActivityRunning()) {
            ReaderPlugin.bringBrowserForeground(this);
        }

        super.finish();
    }
    private AsyncTask<Void, Void, Void> mRefleshTask;

    private AsyncTask<Void, Void, Void> getRefleshTask() {
        if (mRefleshTask == null) {
            mRefleshTask = new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {
                    mCategories = RssManager.getIndex().getCategories();
                    mTitleGalleryAdapter = new TitleGalleryAdapter(mCategories, AddReaderActivity.this);
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    mGalleryTitle.setAdapter(mTitleGalleryAdapter);
                    int count = mCategories.size();
                    GridView gView = null;
                    AddReaderGridAdapter adapter = null;
                    int i = 0;
                    int defaultChannel = -1;
                    mScrollLayout.removeAllViews();
                    for (i = 0; i < count; i++) {
                        adapter = new AddReaderGridAdapter(AddReaderActivity.this, mCategories.get(i).getChannels());
                        adapter.setSubScribedChannel(mSubscribedChannel);
                        gView = (GridView) LayoutInflater.from(AddReaderActivity.this).inflate(
                                        R.layout.rd_subscribe_gridview, null);
                        gView.setScrollbarFadingEnabled(false);
                        gView.setAdapter(adapter);
                        if (mCategories.get(i).name.equals("小编推荐")) {
                            defaultChannel = i;
                        }
                        gView.setOnItemClickListener(mItemClickListener);
                        mScrollLayout.addView(gView);
                    }
                    mGalleryTitle.setSelection(defaultChannel == -1 ? ((mCategories.size()) % 2 == 0 ? mCategories
                                    .size() / 2 : mCategories.size() / 2 + 1) : defaultChannel);
                    mScrollLayout.setToScreen(mGalleryTitle.getSelectedItemPosition());
                    gView = (GridView) mScrollLayout.getChildAt(mGalleryTitle.getSelectedItemPosition());
                    adapter = (AddReaderGridAdapter) gView.getAdapter();
                    adapter.setBusy(false);
                    adapter.setLoadDefaultImage(false);
                    mTitleGalleryAdapter.setHighLightPosition(mGalleryTitle.getSelectedItemPosition());
                    mTitleGalleryAdapter.notifyDataSetChanged();
                    if (mSearchBar.getVisibility() == View.VISIBLE) {
                        setSearchToolViewGone();
                    }
                }
            };
        }
        return mRefleshTask;
    }

    private void init() {
        mSubscribedChannel = (ArrayList<String>) RssSubscribedChannel.getChannelsForNameOnly(getContentResolver());
        mSelectedNum = mSubscribedChannel.size();

        mGalleryTitle = (Gallery) findViewById(R.id.gallery);
        mScrollLayout = (ScrollLayout) findViewById(R.id.scroll);
        mMenu = (ReaderMenuContainer) findViewById(R.id.menu_layout);
        mTitleBar = findViewById(R.id.titleBar);
        mSearchBar = findViewById(R.id.searchBar);
        mSearchGridView = (GridView) findViewById(R.id.search_content);
        mSearchText = (EditText) findViewById(R.id.search_txt);

        mSearchGridView.setOnItemClickListener(mSearchItemClickListener);
        mSearchGridView.setOnScrollListener(mScrollListener);

        findViewById(R.id.subscribed_back_btn).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                doOnBack();
            }
        });

        buildButtom();

        buildSearchBar();

        mGalleryTitle.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Add_Channel_Title_OnClick, 1);
                if (position == mGalleryTitle.getCount() - 1) {
                    findViewById(R.id.pre_btn).setEnabled(false);
                } else if (position == 0) {
                    findViewById(R.id.next_btn).setEnabled(false);
                } else {
                    findViewById(R.id.next_btn).setEnabled(true);
                    findViewById(R.id.pre_btn).setEnabled(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub

            }
        });

        mGalleryTitle.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mScrollLayout.snapToScreen(position);
                mTitleGalleryAdapter.setHighLightPosition(position);
                mTitleGalleryAdapter.notifyDataSetChanged();
            }
        });

        mScrollLayout.setOnSnapToScreenListener(new OnSnapToScreenListener() {

            @Override
            public void onSnapToScreen(final int curScreen, int duration) {
                mHandler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        mGalleryTitle.setSelection(curScreen);
                    }
                }, 100);

                mTitleGalleryAdapter.setHighLightPosition(curScreen);
                mTitleGalleryAdapter.notifyDataSetChanged();

                // 转移scroll listener监听对象
                if (mCurrentScreen != null) {
                    mCurrentScreen.setOnScrollListener(null);
                }

                mCurrentScreen = (GridView) mScrollLayout.getChildAt(curScreen);
                mCurrentScreen.setOnScrollListener(mScrollListener);
                AddReaderGridAdapter adapter = (AddReaderGridAdapter) mCurrentScreen.getAdapter();
                adapter.setLoadDefaultImage(false);
                adapter.setBusy(false);
                adapter.notifyDataSetChanged();

                if (!mAdapterCache.contains(adapter)) {
                    mAdapterCache.add(adapter);
                }

                mHandler.removeCallbacks(mReduceAdapterCacheRunnable);
                if (duration > 0) {
                    mHandler.postDelayed(mReduceAdapterCacheRunnable, duration);
                }

            }

            @Override
            public void onSnapToScreenInterrupted() {

            }
        });
    }

    private void refleshData() {
        final CustomProgressDialog dialog = new CustomProgressDialog(this);
        dialog.setMessage("正在更新...");
        dialog.setTitle("频道更新");
        dialog.show();
        if (RssManager.forceUpdateAsync(new OnDownloadIndexResultListener() {

            @Override
            public void onUpdated() {
                dialog.cancel();
                Toast.makeText(AddReaderActivity.this, "更新成功", Toast.LENGTH_SHORT).show();
                getRefleshTask().execute();
            }

            @Override
            public void onFailure() {
                dialog.cancel();
                Toast.makeText(AddReaderActivity.this, "更新失败", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAlreadyLastestVersion() {
                dialog.cancel();
                Toast.makeText(AddReaderActivity.this, "当前已是最新", Toast.LENGTH_SHORT).show();
            }
        }) == false) {
            dialog.cancel();
            Toast.makeText(AddReaderActivity.this, "当前已是最新", Toast.LENGTH_SHORT).show();
        }
    }

    private void reduceAdapterCache() {
        if (mAdapterCache.size() > MAX_ADAPTER_CACHE) {
            for (int i = 0; i < mAdapterCache.size(); i++) {
                AddReaderGridAdapter adp = mAdapterCache.get(i);
                if (adp != mCurrentScreen.getAdapter()) {
                    adp.setLoadDefaultImage(true);
                    adp.notifyDataSetChanged();
                    mAdapterCache.remove(adp);
                }

                if (mAdapterCache.size() <= MAX_ADAPTER_CACHE) {
                    break;
                }
            }
        }
    }

    private void buildButtom() {
        mMenu.setBottomBarListener(new CommonListener() {

            @Override
            public void actionPerform(int resId) {
                if (resId == R.id.menu_back) {
                    UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Classify_ControlBar_Back_With_Action_Bar, 1);
                    if (mSearchBar.getVisibility() == View.VISIBLE) {
                        setSearchToolViewGone();
                        return;
                    }
                    doOnBack();
                } else if (resId == R.id.menu_reflesh) {
                    UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Classify_ControlBar_Refresh_List, 1);
                    refleshData();
                } /*else if (resId == R.id.menu_search) {
                    UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Add_Channel_Search_Selected, 1);
                    onBottomSearchClick();
                }*/
                mMenu.hideMenuOrBottomBar();
            }
        });
    }

    private FrameLayout mEmptyView;

    private void buildSearchBar() {
        mEmptyView = (FrameLayout) LayoutInflater.from(this).inflate(R.layout.rd_search_empty, null);
        mEmptyView.setVisibility(View.GONE);
        ((FrameLayout) findViewById(R.id.content_container)).addView(mEmptyView);
        findViewById(R.id.search_btn).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                String key = mSearchText.getText().toString();
                if (TextUtils.isEmpty(key)) {
                    Toast.makeText(AddReaderActivity.this, AddReaderActivity.this.getString(R.string.search_tips),
                                    Toast.LENGTH_SHORT).show();
                    return;
                }
                mInputMethodManager.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
                search(key);
            }
        });
    }

    //search method
    private void search(String key) {
        List<RssChannel> channels = null;
        List<RssChannel> searchedChannels = new ArrayList<RssChannel>();
        key = key.trim().toLowerCase();
        for (int i = 0; i < mCategories.size(); i++) {
            channels = mCategories.get(i).getChannels();
            for (RssChannel channel : channels) {
                if (channel.title.contains(key) || channel.getSearchItems().contains(key)) {
                    if (isRepeatChannel(channel, searchedChannels))
                        continue;
                    searchedChannels.add(channel);
                }
            }
        }

        AddReaderGridAdapter adapter = new AddReaderGridAdapter(this, searchedChannels);
        adapter.setSubScribedChannel(mSubscribedChannel);
        adapter.setBusy(false);
        adapter.setLoadDefaultImage(false);
        if (adapter.getCount() == 0) {
            mSearchGridView.setAdapter(null);
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mSearchGridView.setAdapter(adapter);
            mEmptyView.setVisibility(View.GONE);
        }
    }

    //判断搜索频道是否重复
    private boolean isRepeatChannel(Channel channel, List<RssChannel> searchedChannel) {
        for (Channel chan : searchedChannel) {
            if (chan.channel.equals(channel.channel)) {
                return true;
            }
        }
        return false;
    }

    //method execute when search button click
    private void onBottomSearchClick() {
        if (mSearchBar.getVisibility() == View.GONE) {
            mTitleBar.setVisibility(View.GONE);
            mSearchDefaultAdapter = (AddReaderGridAdapter) ((GridView) mScrollLayout.getChildAt(mScrollLayout
                            .getCurScreen())).getAdapter();
            mSearchDefaultAdapter.setSubScribedChannel(mSubscribedChannel);
            mSearchGridView.setAdapter(mSearchDefaultAdapter);
            mSearchBar.setVisibility(View.VISIBLE);
            mSearchBar.startAnimation(AnimationHelper.animationComeOutOrInFromTop(true));
            mScrollLayout.setVisibility(View.GONE);
            mSearchGridView.setVisibility(View.VISIBLE);
            mSearchGridView.setAnimation(AnimationHelper.animationAlphaFade(true));
            mSearchDefaultAdapter.setBusy(false);
            mSearchDefaultAdapter.setLoadDefaultImage(false);
            mSearchDefaultAdapter.notifyDataSetChanged();

            mInputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.RESULT_HIDDEN);
            mInputMethodManager.showSoftInput(mSearchText, InputMethodManager.SHOW_FORCED);
        } else if (mSearchBar.getVisibility() == View.VISIBLE) {
            setSearchToolViewGone();
        }
    }

    //set the search bar gone
    private void setSearchToolViewGone() {
        mInputMethodManager.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);

        Animation animation = AnimationHelper.animationComeOutOrInFromTop(false);
        mSearchBar.startAnimation(animation);
        mSearchGridView.setVisibility(View.GONE);
        mEmptyView.setVisibility(View.GONE);
        animation.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mSearchBar.setVisibility(View.GONE);
                mTitleBar.setVisibility(View.VISIBLE);
                mTitleBar.startAnimation(AnimationHelper.animationAlphaFade(true));
                mScrollLayout.setVisibility(View.VISIBLE);
                mScrollLayout.startAnimation(AnimationHelper.animationAlphaFade(true));
                updateGridViewData();
            }
        });

    }

    //update the channel select
    private void updateGridViewData() {
        mSearchText.setText("");
        for (int i = 0; i < mScrollLayout.getChildCount(); i++) {
            Adapter adapter = ((GridView) mScrollLayout.getChildAt(i)).getAdapter();
            ((BaseAdapter) adapter).notifyDataSetChanged();
        }
    }

    /*private View.OnClickListener mListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
            case R.id.rss_forward:
                doOnBack();
                break;
            case R.id.rss_add_channel:
                break;
            //            case R.id.pre_btn:
            //                mGalleryTitle.onKeyDown(KeyEvent.KEYCODE_DPAD_LEFT, new KeyEvent(0, 0));
            //                mScrollLayout.snapToScreen(mScrollLayout.getCurScreen() - 1);
            //                break;
            //            case R.id.next_btn:
            //                mGalleryTitle.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, new KeyEvent(0, 0));
            //                mScrollLayout.snapToScreen(mScrollLayout.getCurScreen() + 1);
            //                break;
            }

        }
    };*/

    public boolean onKeyUp(int keyCode, android.view.KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_MENU) {
            UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Classify_ControlBar_Back_Key_OnClick, 1);
            mMenu.onMenuClick();
            return true;
        }

        return super.onKeyUp(keyCode, event);
    };

    private void doOnBack() {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                addSubscribe(mCheckSubs);
                return null;
            }

        }.execute();

        finish();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    private OnItemClickListener mSearchItemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final View v = view.findViewById(R.id.rss_home_delete_btn);
            RssChannel entry = ((AddReaderGridAdapter) parent.getAdapter()).getChannels().get(position);
            if (v != null) {
                if (v.getVisibility() != View.VISIBLE) {
                    if (RssSubscribedChannel.isAllowToInsert(mSelectedNum) == false) {
                        Toast.makeText(AddReaderActivity.this, R.string.rd_add_notice, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    v.setVisibility(View.VISIBLE);
                    if (!mSubscribedChannel.contains(entry.channel)) {
                        mCheckSubs.add(entry);
                        mSubscribedChannel.add(entry.channel);
                    }
                    // start a wave animation
                    Animation waveAnim = AnimationUtils.loadAnimation(AddReaderActivity.this, R.anim.rd_wave_scale);
                    waveAnim.setAnimationListener(new AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            v.setVisibility(View.VISIBLE);
                        }
                    });
                    view.startAnimation(waveAnim);
                    mSelectedNum++;
                } else if (v.getVisibility() == View.VISIBLE) {
                    v.setVisibility(View.GONE);
                    if (mSubscribedChannel.contains(entry.channel)) {
                        entry.unsubscribe(getContentResolver());
                        mSubscribedChannel.remove(entry.channel);
                    }
                    mSelectedNum--;
                    mCheckSubs.remove(entry);
                    mSubscribedChannel.remove(entry.channel);
                }
            }
        }
    };

    private OnItemClickListener mItemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final View v = view.findViewById(R.id.rss_home_delete_btn);
            final RssChannel entry = ((AddReaderGridAdapter) parent.getAdapter()).getChannels().get(position);
            if (v != null) {
                if (v.getVisibility() != View.VISIBLE) {
                    if (RssSubscribedChannel.isAllowToInsert(mSelectedNum) == false) {
                        Toast.makeText(AddReaderActivity.this, R.string.rd_add_notice, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    //已订阅首页，不再插入数据库
                    if (!mSubscribedChannel.contains(entry.channel)) {
                        mCheckSubs.add(entry);
                        mSubscribedChannel.add(entry.channel);
                        mSelectedNum++;
                        // start a wave animation
                        Animation waveAnim = AnimationUtils.loadAnimation(AddReaderActivity.this, R.anim.rd_wave_scale);
                        waveAnim.setAnimationListener(new AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                v.setVisibility(View.VISIBLE);
                            }
                        });
                        view.startAnimation(waveAnim);
                    }
                } else if (v.getVisibility() == View.VISIBLE) {
                    v.setVisibility(View.GONE);
                    if (mSubscribedChannel.contains(entry.channel)) {
                        entry.unsubscribe(getContentResolver());
                        mSubscribedChannel.remove(entry.channel);
                    }
                    mCheckSubs.remove(entry);
                    mSelectedNum--;
                }
            }
        }
    };

    public void onBackPressed() {

        if (mMenu.onBackBtnPressed() == true) {
            return;
        }

        if (mSearchBar.getVisibility() == View.VISIBLE) {
            setSearchToolViewGone();
            return;
        }

        doOnBack();
    };

    private void addSubscribe(List<RssChannel> list) {
        Channel sub = null;
        for (int i = 0; i < list.size(); i++) {
            sub = list.get(i);
            sub.subscribe(getContentResolver());
        }
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();
        CommonUtil.showToast("AddReaderActivity.class");
        mMenu.updateMenuStatus();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        onUpdateNightMode();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mNightableReceiver);
    }

    @Override
    public void onUpdateNightMode() {
        super.onUpdateNightMode();
        setNightMask();

        if (Settings.isNightMode() == true) {
            findViewById(R.id.topBar).setBackgroundResource(R.drawable.rd_subscribe_title_night_bg);
            findViewById(R.id.add_reader).setBackgroundColor(getResources().getColor(R.color.rd_night_bg));
        } else {
            findViewById(R.id.topBar).setBackgroundResource(R.drawable.rd_subscribe_title_bg);
            findViewById(R.id.add_reader).setBackgroundColor(getResources().getColor(R.color.rd_bg));
        }

        if (mTitleGalleryAdapter != null) {
            mTitleGalleryAdapter.notifyDataSetChanged();
        }

        if (mScrollLayout != null) {
            for (int i = 0; i < mScrollLayout.getChildCount(); i++) {
                GridView view = (GridView) mScrollLayout.getChildAt(i);
                if (view != null) {
                    AddReaderGridAdapter adapter = (AddReaderGridAdapter) view.getAdapter();
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                }
            }
        }
    }

    /*private void autoScroll() {
        int curScreen = mScrollLayout.getCurScreen();
        int preScreen = mScrollLayout.getPreScreen();

        if (curScreen == preScreen) {
            return;
        }
        int direct = 0;
        if (preScreen > curScreen) {
            direct = KeyEvent.KEYCODE_DPAD_LEFT;
        } else {
            direct = KeyEvent.KEYCODE_DPAD_RIGHT;
        }
        mGalleryTitle.onKeyDown(direct, new KeyEvent(0, 0));
        if (mGalleryTitle.getSelectedItemPosition() != mScrollLayout.getCurScreen()) {
            Utils.debug("info", "--------------not the same position:");
            mGalleryTitle.setSelection(mScrollLayout.getCurScreen());
        }
    }
    */
}
