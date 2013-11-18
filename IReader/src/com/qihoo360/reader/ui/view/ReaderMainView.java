package com.qihoo360.reader.ui.view;

import com.qihoo360.browser.hip.CollectSubScribe;
import com.qihoo360.browser.hip.UXHelper;
import com.qihoo360.browser.hip.UXHelperConfig;
import com.qihoo360.reader.R;
import com.qihoo360.reader.subscription.SubscribedChannel;
import com.qihoo360.reader.subscription.reader.RssSubscribedChannel;
import com.qihoo360.reader.ui.CustomDialog;
import com.qihoo360.reader.ui.articles.ArticleReadActivity;
import com.qihoo360.reader.ui.channels.ArticleCollectionActivity;
import com.qihoo360.reader.ui.channels.ImageAdapter;
import com.qihoo360.reader.ui.channels.ImageAdapter.OnSubscriptionRemoveListener;
import com.qihoo360.reader.ui.channels.RandomReadActivity;
import com.qihoo360.reader.ui.channels.SubscribedActivity;
import com.qihoo360.reader.ui.imagechannel.ImageChannelActivity;
import com.qihoo360.reader.ui.offline.OfflineActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
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
import android.widget.RelativeLayout;

public class ReaderMainView extends RelativeLayout implements OnSubscriptionRemoveListener {
    public final static String TAG = "ReaderMainView";
    private Context mContext;
    private GridView mGridView;
    private ImageAdapter mHomeAdapter;
    private Cursor mCursor;

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

    public static boolean mTapConsumed = false;
    Handler mHandler = new Handler();

    private OnScrollListener mScrollListener = new OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (scrollState != OnScrollListener.SCROLL_STATE_IDLE) {
            } else {
                mHomeAdapter.updateImages(view);
            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        }
    };

    public ReaderMainView(Context context, Cursor cursor) {
        super(context);
        mContext = context;
        mCursor = cursor;
    }

    public void setGridView(GridView view) {
        mGridView = view;
        LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
        mGridView.setLayoutParams(lp);
        mGridView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        mGridView.setSelector(R.color.rd_transparent);
        view.setVerticalFadingEdgeEnabled(true);
        addView(mGridView);
        init(getContext());
    }

    private void init(Context context) {
        mHomeAdapter = new ImageAdapter(mContext, mCursor, this);
        mGridView.setAdapter(mHomeAdapter);
        mGridView.setOnScrollListener(mScrollListener);
        mGridView.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                //                SubscribedChannel sub = (SubscribedChannel) view.getTag();
                //                if (mHomeAdapter.getDeleteCondition() == false && !sub.channel.equals(ReaderPlugin.ADD_SUBSCRIBE)
                //                                && !sub.channel.equals(ReaderPlugin.MY_CONLLECTION)
                //                                && !sub.channel.equals(ReaderPlugin.RANDOM_READ)) {
                if (mHomeAdapter.getCount() == RssSubscribedChannel.getFixedCount()) {
                    return true;
                }
                mHomeAdapter.setDeleteCondition(true);
                //                mGridView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(mContext, R.anim.rd_layout_scale_small));
                //                mGridView.startLayoutAnimation();
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
                if (sub.channel.equals(RssSubscribedChannel.FEEDBACK)) {
                    if (mFeedBackClickListener != null) {
                        mFeedBackClickListener.onFeedBackClick();
                    } else {
                        throw new IllegalAccessError("navigation click listener is null");
                    }
                    UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Home_My_Collection_OnClick, 1);
                } else if (sub.channel.equals(RssSubscribedChannel.OFFLINE_DOWNLOAD)) {
                    OfflineActivity.startActivity(mContext, true);
                } else if (sub.channel.equals(RssSubscribedChannel.RANDOM_READ)) {
                    intent = new Intent(mContext, RandomReadActivity.class);
                    UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Home_Random_Read_OnClick, 1);
                    CollectSubScribe.getInstance().updateStoreKey(CollectSubScribe.DAILY_SHAKE_USER_COUNT, 1);

                    mContext.startActivity(intent);
                } else if (sub.channel.equals(RssSubscribedChannel.ADD_SUBSCRIBE)) {
                    intent = new Intent(mContext, SubscribedActivity.class);
                    UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Home_Add_New_Channel_OnClick, 1);
                    CollectSubScribe.getInstance().updateStoreKey(CollectSubScribe.DAILY_CLASSIFY_USER_COUNT, 1);

                    mContext.startActivity(intent);
                    mHomeAdapter.setDeleteCondition(false);
                } else if (sub.channel.equals(RssSubscribedChannel.MY_CONLLECTION)) {
                    intent = new Intent(mContext, ArticleCollectionActivity.class);
                    CollectSubScribe.getInstance().updateStoreKey(CollectSubScribe.DAILY_COLLECTION_USER_COUNT, 1);

                    mContext.startActivity(intent);
                } else if (sub.getChannel() != null) {
                    //判断已订阅频道是否已停止服务
                    if (sub.getChannel().disabled) {
                        removeUnServiceChannel(sub);
                        return;
                    }
                    if (sub.getChannel().type == 3) {
                        intent = new Intent(mContext, ImageChannelActivity.class);
                    } else {
                        intent = new Intent(mContext, ArticleReadActivity.class);
                    }
                    UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Home_SubScribe_OnClick, 1);
                    intent.putExtra("channel", sub.channel);
                    mContext.startActivity(intent);
                }

                if (!sub.channel.equals(RssSubscribedChannel.SITE_NAVIGATION)) {
                    UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Daily_Icon_OnClick_Times, 1);
                    CollectSubScribe.getInstance().updateStoreKey(CollectSubScribe.DAILY_READER_USER_COUNT, 1);
                }
            }
        });

    }

    private void removeUnServiceChannel(final SubscribedChannel sub) {
        CustomDialog dialog = new CustomDialog(mContext);
        dialog.setTitle(R.string.rd_reader_delete_title);
        dialog.setMessage(R.string.rd_reader_channel_unservice);
        dialog.setNegativeButton(R.string.rd_dialog_cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        dialog.setPositiveButton(R.string.rd_dialog_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                sub.unsubscribe(mContext.getContentResolver());
                dialog.dismiss();
            }
        });
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mHomeAdapter.getDeleteCondition() == true) {
                mHomeAdapter.setDeleteCondition(false);
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
            mHomeAdapter.notifyDataSetChanged();
            //            mGridView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(mContext, R.anim.rd_layout_scale_large));
            //            mGridView.startLayoutAnimation();
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
            if (mTapConsumed == false) {
                mHandler.postDelayed(mCancelDeleteConditionRunnable, 200);
            }
        }

        return super.dispatchTouchEvent(event);
    }

    @Override
    public void onSubscriptionRemoved() {
        mTapConsumed = true;
        if (mHomeAdapter.getCount() == RssSubscribedChannel.getFixedCount()) {
            cancelDeleteCondition();
        }
    }

    private OnFeedBackClickListener mFeedBackClickListener;

    public void setFeedBackClickListener(OnFeedBackClickListener listener) {
        this.mFeedBackClickListener = listener;
    }

    public interface OnFeedBackClickListener {
        public void onFeedBackClick();
    }

    public void setVerticalScrollBarDisplayable(boolean isVerticalScrollBarDisplayable) {
        mGridView.setVerticalScrollBarEnabled(isVerticalScrollBarDisplayable);
    }

    public void clearCoverCache() {
        if(mHomeAdapter != null) {
            mHomeAdapter.clearCoverCache();
        }
    }
}
