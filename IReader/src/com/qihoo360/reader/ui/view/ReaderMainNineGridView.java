package com.qihoo360.reader.ui.view;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.RelativeLayout;

import com.qihoo360.browser.hip.UXHelper;
import com.qihoo360.browser.hip.UXHelperConfig;
import com.qihoo360.reader.R;
import com.qihoo360.reader.subscription.SubscribedChannel;
import com.qihoo360.reader.subscription.reader.RssSubscribedChannel;
import com.qihoo360.reader.support.Utils;
import com.qihoo360.reader.ui.articles.ArticleReadActivity;
import com.qihoo360.reader.ui.channels.ArticleCollectionActivity;
import com.qihoo360.reader.ui.channels.ImageAdapter.OnSubscriptionRemoveListener;
import com.qihoo360.reader.ui.channels.RandomReadActivity;
import com.qihoo360.reader.ui.channels.ReaderMainListAdapter;
import com.qihoo360.reader.ui.channels.SubscribedActivity;
import com.qihoo360.reader.ui.imagechannel.ImageChannelActivity;
import com.qihoo360.reader.ui.offline.OfflineActivity;
import com.qihoo360.reader.ui.view.ReaderMainView.OnFeedBackClickListener;

public class ReaderMainNineGridView extends RelativeLayout implements OnSubscriptionRemoveListener {

    private Context mContext;
    private GridView mGridView;
    private ReaderMainListAdapter mHomeAdapter;
    private List<RssSubscribedChannel> mList;
    private final int curScreen;

    Runnable mCancelDeleteConditionRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mTapConsumed) {
                cancelDeleteCondition();
            }
        }
    };

    private GestureDetector mGestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
        public boolean onSingleTapUp(MotionEvent e) {
            return true;
        };
    });

    private boolean mTapConsumed = false;
    Handler mHandler = new Handler();

    private OnScrollListener mScrollListener = new OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (scrollState != OnScrollListener.SCROLL_STATE_IDLE) {
                mHomeAdapter.setBusy(true);
            } else {
                mHomeAdapter.setBusy(false);
                mHomeAdapter.updateImages(view);
            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        }
    };

    public ReaderMainNineGridView(Context context, int curScreen) {
        super(context);
        mContext = context;
        //        mList = list;
        this.curScreen = curScreen;
        oncreate(context);
    }

    private void oncreate(final Context context) {
//        LayoutInflater.from(context).inflate(R.layout.rd_home_main_layout1, this);

    }

    public void setGridView(GridView view) {
        mGridView = view;
        LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
        mGridView.setLayoutParams(lp);
        mGridView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        mGridView.setSelector(R.color.rd_transparent);
        addView(mGridView);
        init(getContext());
    }

    private void init(Context context) {
        mList = injectSubscribedList(RssSubscribedChannel.getCursor((Activity) context, context.getContentResolver()),
                        curScreen);
        mHomeAdapter = new ReaderMainListAdapter(context, mList, this);
        mGridView.setAdapter(mHomeAdapter);
        mGridView.setOnScrollListener(mScrollListener);

        //        mGridView.setScrollbarFadingEnabled(false);
        mGridView.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (curScreen == 1 && mHomeAdapter.getCount() == 4) {
                    return true;
                }
                //                SubscribedChannel sub = (SubscribedChannel) view.getTag();
                //                if (mHomeAdapter.getDeleteCondition() == false && !sub.channel.equals(ReaderPlugin.ADD_SUBSCRIBE)
                //                                && !sub.channel.equals(ReaderPlugin.MY_CONLLECTION)
                //                                && !sub.channel.equals(ReaderPlugin.RANDOM_READ)) {
                mHomeAdapter.setDeleteCondition(true);
                mDataChangeListener.onInDeleteCondtion(true);
                UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Home_Icon_Long_OnClick, 1);
                mHomeAdapter.notifyDataSetChanged();
                //                }
                return true;
            }
        });

        mGridView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mTapConsumed = true;

                final SubscribedChannel sub = (SubscribedChannel) view.getTag();
                if (mHomeAdapter.getDeleteCondition()) {
                    return;
                }
                Intent intent;
                if (sub.channel.equals(RssSubscribedChannel.SITE_NAVIGATION)) {
                    if (mNavigationClickListener != null) {
                        mNavigationClickListener.onFeedBackClick();
                    } else {
                        throw new IllegalAccessError("navigation click listener is null");
                    }
                    UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Home_My_Collection_OnClick, 1);
                } else if (sub.channel.equals(RssSubscribedChannel.OFFLINE_DOWNLOAD)) {
                    OfflineActivity.startActivity(mContext, true);
                } else if (sub.channel.equals(RssSubscribedChannel.RANDOM_READ)) {
                    intent = new Intent(mContext, RandomReadActivity.class);
                    UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Home_Random_Read_OnClick, 1);
                    mContext.startActivity(intent);
                } else if (sub.channel.equals(RssSubscribedChannel.ADD_SUBSCRIBE)) {
                    intent = new Intent(mContext, SubscribedActivity.class);
                    UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Home_Add_New_Channel_OnClick, 1);
                    mContext.startActivity(intent);
                    mHomeAdapter.setDeleteCondition(false);
                } else if (sub.channel.equals(RssSubscribedChannel.MY_CONLLECTION)) {
                    intent = new Intent(mContext, ArticleCollectionActivity.class);
                    mContext.startActivity(intent);
                } else if (sub.getChannel() != null) {
                    if (sub.getChannel().type == 3) {
                        intent = new Intent(mContext, ImageChannelActivity.class);
                    } else {
                        intent = new Intent(mContext, ArticleReadActivity.class);
                    }
                    UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Home_SubScribe_OnClick, 1);
                    intent.putExtra("channel", sub.channel);
                    mContext.startActivity(intent);
                }
            }
        });

    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mHomeAdapter.getDeleteCondition() == true) {
                mHomeAdapter.setDeleteCondition(false);
                mDataChangeListener.onInDeleteCondtion(false);
                UXHelper.getInstance()
                                .addActionRecord(UXHelperConfig.Reader_Home_Icon_Delete_State_Cancel_With_Back, 1);
                mHomeAdapter.notifyDataSetChanged();
                return true;
            }
        }

        return super.onKeyUp(keyCode, event);
    }

    public boolean cancelDeleteCondition() {
        if (mHomeAdapter.getDeleteCondition() == true) {
            mHomeAdapter.setDeleteCondition(false);
            mDataChangeListener.onInDeleteCondtion(false);
            mHomeAdapter.notifyDataSetChanged();
            return true;
        }
        return false;
    }

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
    public void onSubscriptionRemoved() {
        remainDeleteState();
        if (mDataChangeListener != null) {
            mDataChangeListener.onGridDataChange(curScreen);
        }
        if (curScreen == 1 && mHomeAdapter.getCount() == 4) {
            cancelDeleteCondition();
        }
    }

    public void remainDeleteState() {
        mTapConsumed = true;
        mHomeAdapter.setDeleteCondition(true);
    }

    private List<RssSubscribedChannel> injectSubscribedList(Cursor cursor, int curScreen) {
        List<RssSubscribedChannel> list = new ArrayList<RssSubscribedChannel>();
        for (int i = (curScreen - 1) * 9; i < curScreen * 9; i++) {
            cursor.moveToFirst();
            if (cursor.move(i)) {
                list.add(RssSubscribedChannel.inject(cursor));
            }
        }
        Utils.debug("info", "----------------load data");
        for (SubscribedChannel subscribedChannel : list) {
            Utils.debug("info", "----------------name:" + subscribedChannel.title);
        }
        if (cursor != null) {
            cursor.close();
        }

        return list;
    }

    /***
     * 更新当前page的数据
     */
    public void updatePageData() {
        mList = injectSubscribedList(
                        RssSubscribedChannel.getCursor((Activity) getContext(), getContext().getContentResolver()),
                        curScreen);
        mHomeAdapter.setData(mList);
        mHomeAdapter.notifyDataSetChanged();
    }

    private OnNineGridDataChangeListener mDataChangeListener;

    public void setOnNineGridDataChangeListener(OnNineGridDataChangeListener listener) {
        mDataChangeListener = listener;
    }

    public void setDeleteCondition(boolean bool) {
        mHomeAdapter.setDeleteCondition(bool);
        mHomeAdapter.notifyDataSetChanged();
        if (bool) {
            mGridView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(mContext, R.anim.rd_layout_scale_small));
        } else {
            mGridView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(mContext, R.anim.rd_layout_scale_large));
        }
        mGridView.startLayoutAnimation();
    }

    public interface OnNineGridDataChangeListener {
        public void onGridDataChange(int curScreen);

        public void onInDeleteCondtion(boolean isDelete);
    }

    private OnFeedBackClickListener mNavigationClickListener;

    public void setReaderNavigationClickListener(OnFeedBackClickListener listener) {
        this.mNavigationClickListener = listener;
    }
}
