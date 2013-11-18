package com.qihoo360.reader.ui.channels;

import com.qihoo360.browser.hip.UXHelper;
import com.qihoo360.browser.hip.UXHelperConfig;
import com.qihoo360.reader.Constants;
import com.qihoo360.reader.R;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.listener.OnDownloadIndexResultListener;
import com.qihoo360.reader.subscription.Category;
import com.qihoo360.reader.subscription.Channel;
import com.qihoo360.reader.subscription.Index;
import com.qihoo360.reader.subscription.reader.RssChannel;
import com.qihoo360.reader.subscription.reader.RssManager;
import com.qihoo360.reader.subscription.reader.RssSubscribedChannel;
import com.qihoo360.reader.ui.ActivityBase;
import com.qihoo360.reader.ui.CommonUtil;
import com.qihoo360.reader.ui.CustomProgressDialog;
import com.qihoo360.reader.ui.ReaderPlugin;
import com.qihoo360.reader.ui.articles.ArticleReadActivity;
import com.qihoo360.reader.ui.imagechannel.ImageChannelActivity;
import com.qihoo360.reader.ui.view.AddReaderLeftPane;
import com.qihoo360.reader.ui.view.AddReaderLeftPane.OnLeftItemSelectListener;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
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
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class SubscribedActivity extends ActivityBase {

    private AddReaderLeftPane mLeftPane;
    private ListView mListView;
    private List<Category> mCategories;
    private List<String> mSubscribedChannel;
    private AddReaderListAdapter mListAdapter;
    //    private ReaderMenuContainer mMenu;
    private View mSearchBar;
    private TranslateAnimation mTransAnimation;
    private int mViewMode = Constants.MODE_LIST;
    private InputMethodManager mInputMethodManager = null;
    private EditText mSearchText;
    private View mEmptyView;

    private OnScrollListener mScrollListener = new OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (mViewMode == Constants.MODE_GRID) {
                AddReaderGridAdapter currentAdapter = (AddReaderGridAdapter) mListView.getAdapter();
                if (currentAdapter == null) {
                    return;
                }
                if (scrollState == OnScrollListener.SCROLL_STATE_FLING
                                || scrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    currentAdapter.setBusy(true);
                } else {
                    currentAdapter.setBusy(false);
                    currentAdapter.updateImages(view);
                }
            } else {
                AddReaderListAdapter currentAdapter = (AddReaderListAdapter) mListView.getAdapter();
                if (currentAdapter == null) {
                    return;
                }
                if (scrollState == OnScrollListener.SCROLL_STATE_FLING
                                || scrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                } else {
                    currentAdapter.updateImages(view);
                }
            }

        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        }
    };

    private void setLeftPanWidth() {
        if (mLeftPane != null) {
            final DisplayMetrics dm = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(dm);
            mLeftPane.getLayoutParams().width = dm.widthPixels / 3;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rd_add_reader1);
        mLeftPane = (AddReaderLeftPane) findViewById(R.id.add_reader_leftpane);
        mListView = (ListView) findViewById(R.id.content);
        //        mMenu = (ReaderMenuContainer) findViewById(R.id.menu_layout);
        mSearchBar = findViewById(R.id.searchBar);
        mEmptyView = LayoutInflater.from(this).inflate(R.layout.rd_search_empty, null);
        mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mSearchBar.setVisibility(View.VISIBLE);
        mSearchBar.post(new Runnable() {

            @Override
            public void run() {
                mTransAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0,
                                Animation.ABSOLUTE, -mSearchBar.getMeasuredHeight(), Animation.ABSOLUTE, 0);
                mTransAnimation.setDuration(500);
                mTransAnimation.setInterpolator(new DecelerateInterpolator());
                mSearchBar.setVisibility(View.GONE);
            }
        });

        mListView.setOnScrollListener(mScrollListener);
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                Index index = RssManager.getIndex();
                mCategories = new ArrayList<Category>(index.getCategories());
                Category category = new Category();
                category.name = "返回";
                mCategories.add(category); // 分类
                mSubscribedChannel = (ArrayList<String>) RssSubscribedChannel.getChannelsForNameOnly(getContentResolver());
                mListAdapter = new AddReaderListAdapter(SubscribedActivity.this, filterChannel(mCategories.get(
                                AddReaderLeftPaneAdapter.DEFAULT_HIGHLIGHT_POSITION).getChannels()));
                return null;
            }

            protected void onPostExecute(Void result) {
                AddReaderLeftPaneAdapter adapter = new AddReaderLeftPaneAdapter(mCategories, SubscribedActivity.this);
                mLeftPane.setLeftPanAdapter(adapter);
                mListAdapter.setLoadDefaultImage(false);
                mListAdapter.setSubScribedChannel(mSubscribedChannel);
                mListView.setAdapter(mListAdapter);
                mListView.setOnItemClickListener(mItemClickListener);
            }

        }.execute();

        buildButtom();
        buildSearch();

        mLeftPane.setOnLeftItemSelectedListener(new OnLeftItemSelectListener() {

            @Override
            public void onItemSelected(int position) {
                mListView.postInvalidate();
                //                initSearchStatus();
                if (position == mLeftPane.getSearchItemPosition()) {
                    //                    showSearchBar();
                    onBackPressed();
                } else {
                    setListViewAdapter(filterChannel(mCategories.get(position).getChannels()));
                }
            }
        });

        ReaderPlugin.mIsRunning = true;

        setLeftPanWidth();
    }

    private void showSearchBar() {
        if (mSearchBar.getVisibility() == View.GONE) {
            mSearchBar.setVisibility(View.VISIBLE);
            mSearchBar.startAnimation(mTransAnimation);
            mTransAnimation.setAnimationListener(new AnimationListener() {

                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mSearchText.requestFocus();
                    mInputMethodManager.toggleSoftInputFromWindow(mSearchText.getWindowToken(), 0, 0);
                }
            });
            mListView.setAdapter(null);
        }
    }

    @Override
    public void finish() {
        ReaderPlugin.mIsRunning = false;
        if(!ReaderPlugin.getBrowserActivityRunning()) {
            ReaderPlugin.bringBrowserForeground(this);
        }
        Settings.setReaderViewMode(mViewMode);
        super.finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //        mMenu.saveBottomBarState();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //        mMenu.onResume();

        if (mListView.getAdapter() != null) {
            mSubscribedChannel = (ArrayList<String>) RssSubscribedChannel.getChannelsForNameOnly(getContentResolver());
            mListAdapter.setSubScribedChannel(mSubscribedChannel);
            ((BaseAdapter) mListView.getAdapter()).notifyDataSetChanged();
        }

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    private void buildButtom() {/*
                                mMenu.setBottomBarListener(new CommonListener() {
                                @Override
                                public void actionPerform(int resId) {
                                if (resId == R.id.menu_back) {
                                UXHelper.getInstance().addActionRecord(
                                    UXHelperConfig.Reader_Classify_ControlBar_Back_With_Action_Bar, 1);
                                UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Classify_ControlBar_Back_Key_OnClick,
                                    1);
                                onBackPressed();
                                } else if (resId == R.id.menu_reflesh) {
                                UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Classify_ControlBar_Refresh_List, 1);
                                refleshData();
                                } else if (resId == R.id.menu_search) {
                                showSearchBar();
                                }
                                mMenu.hideMenu();
                                }

                                });
                                */
    }

    private void setListViewAdapter(List<RssChannel> list) {
        mListAdapter.setChannels(list);
        mListView.setAdapter(mListAdapter);
        mListView.clearAnimation();
        mListView.startLayoutAnimation();
    }

    private OnItemClickListener mItemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Channel entry;
            if (mViewMode == Constants.MODE_GRID) {
                entry = ((AddReaderGridAdapter) parent.getAdapter()).getChannels().get(position);
            } else {
                entry = ((AddReaderListAdapter) parent.getAdapter()).getChannels().get(position);
            }

            Intent intent = null;
            if (entry.type == 3) {
                intent = new Intent(SubscribedActivity.this, ImageChannelActivity.class);
            } else {
                intent = new Intent(SubscribedActivity.this, ArticleReadActivity.class);
            }
            intent.putExtra("channel", entry.channel);
            intent.putExtra("random_read", true);
            startActivity(intent);
        }
    };

    @Override
    public void onBackPressed() {

        //        if (mMenu.onBackBtnPressed() == true) {
        //            return;
        //        }

        super.onBackPressed();
    }

    public boolean onKeyUp(int keyCode, android.view.KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_MENU) {
            UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Classify_ControlBar_Back_Key_OnClick, 1);
            if (mInputMethodManager.isActive(mSearchText)) {
                mInputMethodManager.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
            }
            //            mMenu.onMenuClick();
            return true;
        }

        return super.onKeyUp(keyCode, event);
    };

    List<RssChannel> mSearchedChannels = new ArrayList<RssChannel>();

    /***
     * 执行搜索
     * @param key
     */
    private void search(String key) {
        List<RssChannel> channels = null;
        mSearchedChannels = new ArrayList<RssChannel>();
        key = key.trim().toLowerCase();
        for (int i = 0; i < mCategories.size(); i++) {
            channels = mCategories.get(i).getChannels();
            for (RssChannel channel : channels) {
                if (channel.title.contains(key) || channel.getSearchItems().contains(key)) {
                    if (isRepeatChannel(channel, mSearchedChannels) || channel.disabled)
                        continue;
                    mSearchedChannels.add(channel);
                }
            }
        }

        //若无数据显示empty view
        if (mSearchedChannels.size() == 0) {
            if (mEmptyView.getParent() == null) {
                ((FrameLayout) findViewById(R.id.content_container)).addView(mEmptyView);
            }
            return;
        } else {
            if (mEmptyView.getParent() != null) {
                ((ViewGroup) mEmptyView.getParent()).removeView(mEmptyView);
            }
        }

        setListViewAdapter(mSearchedChannels);
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

    /***
     * 初始化search bar
     */
    private void buildSearch() {
        mSearchText = (EditText) findViewById(R.id.search_txt);
        findViewById(R.id.search_btn).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                String key = mSearchText.getText().toString();
                if (TextUtils.isEmpty(key)) {
                    CommonUtil.showToast(R.string.search_tips);
                    return;
                }
                mInputMethodManager.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
                search(key);
            }
        });

    }

    /***
     * 初始化search bar状态
     */
    private void initSearchStatus() {
        findViewById(R.id.searchBar).setVisibility(View.GONE);
        if (mEmptyView.getParent() != null) {
            ((ViewGroup) mEmptyView.getParent()).removeView(mEmptyView);
        }
        mSearchText.setText(null);
        mSearchedChannels = null;
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
                CommonUtil.showToast("更新成功");
                getRefleshTask().execute();
            }

            @Override
            public void onFailure() {
                dialog.cancel();
                CommonUtil.showToast("更新失败");
            }

            @Override
            public void onAlreadyLastestVersion() {
                dialog.cancel();
                CommonUtil.showToast("当前已是最新");
            }
        }) == false) {
            dialog.cancel();
            CommonUtil.showToast("当前已是最新");
        }
    }

    private AsyncTask<Void, Void, Void> getRefleshTask() {

        AsyncTask<Void, Void, Void> refleshTask = new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                Index index = RssManager.getIndex();
                mCategories = new ArrayList<Category>(index.getCategories());
                mListAdapter = new AddReaderListAdapter(SubscribedActivity.this, filterChannel(mCategories.get(
                                AddReaderLeftPaneAdapter.DEFAULT_HIGHLIGHT_POSITION).getChannels()));
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                AddReaderLeftPaneAdapter adapter = new AddReaderLeftPaneAdapter(mCategories, SubscribedActivity.this);
                mLeftPane.setLeftPanAdapter(adapter);
                mListAdapter.setLoadDefaultImage(false);
                mListAdapter.setSubScribedChannel(mSubscribedChannel);
                mListView.setAdapter(mListAdapter);
                initSearchStatus();
            }
        };

        return refleshTask;
    }

    private List<RssChannel> filterChannel(List<RssChannel> list) {
        if (list == null || list.size() == 0) {
            throw new IllegalArgumentException("the need to filter channel list is empty!");
        }

        List<RssChannel> filterList = new ArrayList<RssChannel>();
        for (RssChannel channel : list) {
            if (channel.disabled) {
                continue;
            }
            filterList.add(channel);
        }
        return filterList;
    }

    @Override
    public void onUpdateNightMode() {
        super.onUpdateNightMode();
        setNightMask();
        setBackgroud();
        if (mListView.getAdapter() != null) {
            ((BaseAdapter) mListView.getAdapter()).notifyDataSetChanged();
        }
    }

    private void setBackgroud() {
        Bitmap bitmap = ReaderPlugin.getInstance().getBackGroundCommon();
        View topContainer = findViewById(R.id.main_container);
        if(bitmap != null) {
            topContainer.setBackgroundDrawable(new BitmapDrawable(bitmap));
        } else {
            topContainer.setBackgroundResource(R.drawable.rd_bg);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {

        if (mInputMethodManager.isActive(mSearchText) || mInputMethodManager.isActive()) {
            mInputMethodManager.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
            mSearchText.clearFocus();
        }

        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setLeftPanWidth();
    }
}
