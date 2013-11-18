package com.qihoo.ilike.ui;

import java.util.Collection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.view.ViewGroup;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.qihoo.ilike.subscription.IlikeChannel;
import com.qihoo.ilike.util.Utils;
import com.qihoo360.browser.hip.UXHelper;
import com.qihoo360.browser.hip.UXHelperConfig;
import com.qihoo360.browser.hip.UXKey;
import com.qihoo360.reader.Constants;
import com.qihoo360.reader.R;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.image.ImageDownloadStrategy;
import com.qihoo360.reader.subscription.Channel;
import com.qihoo360.reader.support.AsyncTask;
import com.qihoo360.reader.support.SystemUtils;
import com.qihoo360.reader.ui.CustomDialog;
import com.qihoo360.reader.ui.imagechannel.ImageChannelActivity;

public class ILikeImageChannelActivity extends ImageChannelActivity {
    public static final int MAX_SUB_CATEGORY_TITLE_LENGTH = 4;

    public static final int CATEGORY_INDEX_MY_COLLECTION = 0;
    public static final int CATEGORY_INDEX_LATEST_COLLECTION = 1;
    public static final int CATEGORY_INDEX_HOT_COLLECTION = 2;
    public static final int CATEGORY_INDEX_SUB_CATEGORY = 3;
    private String[] mCategoryChannelList;

    private LinearLayout mChannelContainer = null;
    private GridView mSubCategoryGridView = null;
    private SubCategoryAdapter mSubCategoryAdapter = null;
    IlikeChannel[] mSubCategories = null;

    int mCategoryIndex = -1;
    int mSubCategoryIndex = -1;
    boolean mNeedToRefreshSubCategoryGridView = false;

    private boolean mPendingRequestForMyCollection = false;
    private BroadcastReceiver mBroadcastReceiver = null;

    private GestureDetector mGestureDetector = null;

    OnClickListener mCategoryClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            int index = (Integer) view.getTag();
            if (index < 0 || index >= mCategoryChannelList.length
                    || index == mCategoryIndex
                    || TextUtils.isEmpty(mCategoryChannelList[index])) {
                return;
            }

            if (IlikeChannel.ILIKE_MY_COLLECTION
                    .equals(mCategoryChannelList[index])) {
                if (!Utils.accountConfigured(ILikeImageChannelActivity.this)) {
                    startWaitingForAccountConfiguration();
                    return;
                }
            }

            Intent intent = new Intent();
            intent.putExtra("channel", mCategoryChannelList[index]);
            onNewIntent(intent);

            switchCategory(index);
        }
    };

    BroadcastReceiver mNetStateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ImageDownloadStrategy.getInstance(ILikeImageChannelActivity.this)
                    .updateWifiState();
        }
    };

    @Override
    protected UXKey getPvHipId() {
        return UXHelperConfig.WoXiHuan_PV_Times;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ImageDownloadStrategy.getInstance(ILikeImageChannelActivity.this)
                .updateWifiState();
        registerReceiver(mNetStateChangeReceiver, new IntentFilter(
                ConnectivityManager.CONNECTIVITY_ACTION));

        mCategoryChannelList = new String[4];
        mCategoryChannelList[0] = IlikeChannel.ILIKE_MY_COLLECTION;
        mCategoryChannelList[1] = IlikeChannel
                .get(IlikeChannel.ILIKE_LASTEST_COLLECTION).channel;
        mCategoryChannelList[2] = IlikeChannel
                .get(IlikeChannel.ILIKE_HOT_COLLECTION).channel;
        mCategoryChannelList[3] = null;

        initUI();
        initInfoBtn();

        if (!Settings.isUsedILike()) {
            UXHelper.getInstance().addActionRecord(
                    UXHelperConfig.Reader_Woxihuan_User, 1);
            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {
                    Settings.setIsUsedILike(true);
                    return null;
                }

            }.execute();
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                View infoBtn = findViewById(R.id.ilike_info_btn);
                if (infoBtn.getVisibility() == View.VISIBLE) {
                    infoBtn.startAnimation(AnimationUtils.loadAnimation(
                            ILikeImageChannelActivity.this, R.anim.rd_blink));
                }
            }
        }, 500);

    }

    private void initUI() {
        mChannelContainer = (LinearLayout) findViewById(R.id.ilike_category_container);

        /*
         * TextView myFocusView = (TextView)
         * mChannelContainer.findViewById(R.id.ilike_category_my_fav);
         * myFocusView.setTag(IlikeChannel.ILIKE_MY_FOCUS);
         * myFocusView.setOnClickListener(listener);
         */

        TextView myCollectionView = (TextView) mChannelContainer
                .findViewById(R.id.ilike_category_my_col);
        myCollectionView.setTag(CATEGORY_INDEX_MY_COLLECTION);
        myCollectionView.setOnClickListener(mCategoryClickListener);

        TextView lastestCollectionView = (TextView) mChannelContainer
                .findViewById(R.id.ilike_category_latest_col);
        lastestCollectionView.setTag(CATEGORY_INDEX_LATEST_COLLECTION);
        lastestCollectionView.setOnClickListener(mCategoryClickListener);

        TextView hotCollectionView = (TextView) mChannelContainer
                .findViewById(R.id.ilike_category_hot_col);
        hotCollectionView.setTag(CATEGORY_INDEX_HOT_COLLECTION);
        hotCollectionView.setOnClickListener(mCategoryClickListener);

        mCategoryIndex = CATEGORY_INDEX_HOT_COLLECTION;
        hotCollectionView.setTextColor(getResources().getColor(
                R.color.ilike_category_highlighted_text));

        TextView openSubCategoryView = (TextView) mChannelContainer
                .findViewById(R.id.ilike_category_open_sub);
        openSubCategoryView.setTag(CATEGORY_INDEX_SUB_CATEGORY);
        openSubCategoryView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSubCategoryGridView();
            }
        });

        mSubCategoryGridView = (GridView) findViewById(R.id.ilike_sub_category_gridview);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ILikeImageChannelActivity.this);
        if (sp.getBoolean(PREF_KEY_SLIDE_TO_ILIKE_ENABLED, true)) {
            mGestureDetector = new GestureDetector(
                    new GestureDetector.SimpleOnGestureListener() {
                        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                                float velocityY) {
                            if (velocityX > 500 && Math.abs(velocityY) * 2 < Math.abs(velocityX)) {
                                return true;
                            } else {
                                return false;
                            }
                        };
                    });
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if(mGestureDetector != null && mGestureDetector.onTouchEvent(ev)) {
            finish();
            return true;
        }

        return super.dispatchTouchEvent(ev);
    }

    public static final String PREF_KEY_SLIDE_TO_ILIKE_ENABLED = "slide_to_ilike_enabled";

    private void initInfoBtn() {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(ILikeImageChannelActivity.this);
        if (/*false && */sp.getBoolean(
                PREF_KEY_SLIDE_TO_ILIKE_ENABLED, true)) {
            View infoBtn = findViewById(R.id.ilike_info_btn);
            infoBtn.setVisibility(View.VISIBLE);
            infoBtn.setOnClickListener(new OnClickListener() {
                private void handleDialogBtnClicked(DialogInterface dialog,
                        boolean positive) {
                    if(!positive) {
                        findViewById(R.id.ilike_info_btn).setVisibility(View.GONE);
                    }

                    commitSlideToOpenILikeSetting(positive);
                    dialog.dismiss();
                }

                @Override
                public void onClick(View view) {
                    CustomDialog dialog = new CustomDialog(
                            ILikeImageChannelActivity.this);
                    dialog.setTitle(R.string.i_like_info_dlg_title);
                    dialog.setMessage(R.string.i_like_info_dlg_content);
                    dialog.setPositiveButton(
                            R.string.i_like_info_dlg_btn_negative,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    handleDialogBtnClicked(dialog, false);
                                }
                            });

                    dialog.setNegativeButton(
                            R.string.i_like_info_dlg_btn_positive,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    handleDialogBtnClicked(dialog, true);
                                }
                            });
                    dialog.show();
                }
            });
        }
    }

    private void commitSlideToOpenILikeSetting(boolean slideToOpen) {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(ILikeImageChannelActivity.this);
        Editor editor = sp.edit();
        editor.putBoolean(PREF_KEY_SLIDE_TO_ILIKE_ENABLED, slideToOpen);
        editor.commit();
        Intent intent = new Intent(Constants.READER_BROADCAST_SWITCH_ILIKE_MODE);
        intent.putExtra("mode", slideToOpen);
        sendBroadcast(intent);
    }

    private boolean switchCategory(int index) {
        if (index == mCategoryIndex) {
            return false;
        }

        TextView oldCategoryView = (TextView) mChannelContainer
                .getChildAt(mCategoryIndex);
        oldCategoryView.setTextColor(getResources().getColorStateList(
                R.color.ilike_category_text_color));
        if (mCategoryIndex == CATEGORY_INDEX_SUB_CATEGORY) {
            oldCategoryView.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                    R.drawable.ilike_sub_category_arrow, 0);
        }

        TextView newCategoryView = (TextView) mChannelContainer
                .getChildAt(index);
        newCategoryView.setTextColor(getResources().getColor(
                R.color.ilike_category_highlighted_text));
        if (index == CATEGORY_INDEX_SUB_CATEGORY) {
            newCategoryView.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                    R.drawable.ilike_sub_category_arrow_pressed, 0);
        } else {
            showSubCategory(false);
            mSubCategoryIndex = -1;
        }

        mCategoryIndex = index;
        return true;
    }

    @Override
    public void onDestroy() {
        if (mNetStateChangeReceiver != null) {
            unregisterReceiver(mNetStateChangeReceiver);
            mNetStateChangeReceiver = null;
        }

        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
            mBroadcastReceiver = null;
        }

        mChannelContainer = null;
        mSubCategoryGridView = null;
        mSubCategoryAdapter = null;
        mSubCategories = null;
        mCategoryChannelList = null;

        super.onDestroy();
    }

    @Override
    protected void setContent() {
        setContentView(R.layout.ilike_image_channel);
    }

    @Override
    protected void handleScrollStateChange(int scrollState) {
        if (mScrollState == OnScrollListener.SCROLL_STATE_IDLE
                && scrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
            showSubCategory(false);
        }

        super.handleScrollStateChange(scrollState);
    }

    @Override
    protected void updateChannelTitle() {
    }

    @Override
    protected void updateChannelSrc() {
    }

    @Override
    public void onUpdateNightMode() {
        SystemUtils.setNightModeBrightness(this, Settings.isNightMode());
        setBackground(R.id.rd_image_channel_frame);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void albumClicked(int albumIndex) {
        showSubCategory(false);

        super.albumClicked(albumIndex);
    }

    @Override
    protected void doShare() {
        Utils.shareMainPage(this);
    }

    @Override
    protected int getEmptyListTipResource() {
        Channel channel = getCurrentChannel();
        if (channel != null && channel.type == Channel.TYPE_ILIKE_MY_COLLECTION) {
            return R.drawable.ilike_empty_list_tip;
        } else {
            return super.getEmptyListTipResource();
        }
    }

    @Override
    protected int getNetErrorTipResource() {
        return R.drawable.ilike_article_loading_exception;
    }

    private void startWaitingForAccountConfiguration() {
        mPendingRequestForMyCollection = true;

        if (mBroadcastReceiver == null) {
            mBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (com.qihoo.ilike.util.Utils.accountConfigSucceed(intent)) {
                        if (mPendingRequestForMyCollection) {
                            Intent i = new Intent();
                            i.putExtra(
                                    "channel",
                                    mCategoryChannelList[CATEGORY_INDEX_MY_COLLECTION]);
                            onNewIntent(i);
                            switchCategory(CATEGORY_INDEX_MY_COLLECTION);
                            mPendingRequestForMyCollection = false;
                        }
                    } else {
                        mPendingRequestForMyCollection = false;
                    }
                }
            };

            IntentFilter filter = Utils.getAccountConfigResultFilter();
            registerReceiver(mBroadcastReceiver, filter);
        }
    }

    private void toggleSubCategoryGridView() {
        showSubCategory(mSubCategoryGridView.getVisibility() != View.VISIBLE);
    }

    private void showSubCategory(boolean visible) {
        if (visible) {
            if (mSubCategoryAdapter == null) {
                Collection<IlikeChannel> subCategoryList = IlikeChannel
                        .getHotCollectionChannels();
                mSubCategories = new IlikeChannel[subCategoryList.size()];
                subCategoryList.toArray(mSubCategories);
                mSubCategoryAdapter = new SubCategoryAdapter();
                mSubCategoryGridView.setAdapter(mSubCategoryAdapter);
                mSubCategoryGridView
                        .setOnItemClickListener(mSubCategoryClickListener);
            } else if (mNeedToRefreshSubCategoryGridView) {
                mSubCategoryAdapter.notifyDataSetChanged();
                mNeedToRefreshSubCategoryGridView = false;
            }
            mSubCategoryGridView.setVisibility(View.VISIBLE);
        } else {
            mSubCategoryGridView.setVisibility(View.INVISIBLE);
        }
    }

    OnItemClickListener mSubCategoryClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {
            if (position == mSubCategoryIndex) {
                return;
            } else {
                mSubCategoryIndex = position;
            }

            IlikeChannel ilikeChannel = mSubCategories[position];

            Intent intent = new Intent();
            intent.putExtra("channel", ilikeChannel.channel);
            onNewIntent(intent);
            switchCategory(CATEGORY_INDEX_SUB_CATEGORY);

            String title = ilikeChannel.title;
            if (!TextUtils.isEmpty(title)) {
                if (title.length() > MAX_SUB_CATEGORY_TITLE_LENGTH) {
                    title = title.substring(0, MAX_SUB_CATEGORY_TITLE_LENGTH);
                }
                ((TextView) mChannelContainer
                        .getChildAt(CATEGORY_INDEX_SUB_CATEGORY))
                        .setText(title);
            }

            showSubCategory(false);
            mNeedToRefreshSubCategoryGridView = true;
        }
    };

    private class SubCategoryAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mSubCategories == null ? 0 : mSubCategories.length;
        }

        @Override
        public Object getItem(int position) {
            if (position >= 0 && position < mSubCategories.length) {
                return mSubCategories[position];
            } else {
                return null;
            }
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (position < 0 || position >= mSubCategories.length) {
                return null;
            }

            if (convertView == null) {
                convertView = LayoutInflater.from(
                        ILikeImageChannelActivity.this).inflate(
                        R.layout.ilike_sub_category_textview, null);
            }

            String subCategory = mSubCategories[position].title;
            if (!TextUtils.isEmpty(subCategory)) {
                if (subCategory.length() > MAX_SUB_CATEGORY_TITLE_LENGTH) {
                    subCategory = subCategory.substring(0,
                            MAX_SUB_CATEGORY_TITLE_LENGTH);
                }
                ((TextView) convertView).setText(subCategory);
                if (position == mSubCategoryIndex) {
                    ((TextView) convertView).setTextColor(getResources()
                            .getColor(R.color.ilike_category_highlighted_text));
                } else {
                    ((TextView) convertView).setTextColor(getResources()
                            .getColorStateList(
                                    R.color.ilike_category_text_color));
                }
            }

            return convertView;
        }
    }

    @Override
    protected boolean needAutoRefresh() {
        return false;
    }
}
