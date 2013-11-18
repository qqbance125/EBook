package com.qihoo360.reader.ui.channels;

import com.qihoo360.browser.hip.CollectSubScribe;
import com.qihoo360.browser.hip.UXHelper;
import com.qihoo360.browser.hip.UXHelperConfig;
import com.qihoo360.common.IGridSizeChangeListener;
import com.qihoo360.reader.R;
import com.qihoo360.reader.image.BitmapFactoryBase;
import com.qihoo360.reader.image.ChannelBitmapFacotry;
import com.qihoo360.reader.image.ChannelBitmapFacotry.SetDefaultChannelCoverResult;
import com.qihoo360.reader.image.ImageUtils;
import com.qihoo360.reader.subscription.SubscribedChannel;
import com.qihoo360.reader.subscription.reader.RssSubscribedChannel;
import com.qihoo360.reader.support.Utils;
import com.qihoo360.reader.ui.CommonUtil;
import com.qihoo360.reader.ui.view.ReaderMainView;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.LayoutParams;
import android.widget.CursorAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;

public class ImageAdapter extends CursorAdapter implements IGridSizeChangeListener {
    private final AbsListView.LayoutParams mLayoutParams = new AbsListView.LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT);

    private final AbsListView.LayoutParams mLastParam = new AbsListView.LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT);

    public interface OnSubscriptionRemoveListener {
        public void onSubscriptionRemoved();
    }

    public static final String TAG = "ImageAdapter";
    private Context mContext = null;
    private OnSubscriptionRemoveListener mOnSubscriptionRemoveListener = null;
    private final int mPaddingBottom, mDeletePadding;
    HashMap<String, Bitmap> mCoverCache = new HashMap<String, Bitmap>();

    public ImageAdapter(Context context, Cursor c, OnSubscriptionRemoveListener subscriptionRemoveListener) {
        super(context, c);
        mContext = context;
        mOnSubscriptionRemoveListener = subscriptionRemoveListener;
        mPaddingBottom = (int) context.getResources().getDimension(R.dimen.rd_main_padding_bottom);
        mDeletePadding = (int) context.getResources().getDimension(R.dimen.rd_main_delete_padding);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        final View r = LayoutInflater.from(context).inflate(R.layout.rd_gridview_item, null);
        return r;
    }

    private void changeMargin(View view1, View view2, int l, int t, int r, int b) {
        final android.widget.FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) view1.getLayoutParams();
        lp.leftMargin = l;
        lp.rightMargin = r;
        lp.bottomMargin = b;
        lp.topMargin = t;
        final android.widget.FrameLayout.LayoutParams lp1 = (FrameLayout.LayoutParams) view2.getLayoutParams();
        lp1.leftMargin = l;
        lp1.rightMargin = r;
        lp1.bottomMargin = b;
        lp1.topMargin = t;
        view2.setLayoutParams(lp1);
    }

    @Override
    public void bindView(final View mainViewLayout, Context context, Cursor cursor) {
        mainViewLayout.setVisibility(View.VISIBLE);
        final SubscribedChannel subscription = RssSubscribedChannel.inject(cursor);
        ImageView imageView = (ImageView) mainViewLayout.findViewById(R.id.home_button);
        TextView title = (TextView) mainViewLayout.findViewById(R.id.title);

        /*if (cursor.getPosition() == getCount() - 1) {
            mainViewLayout.setLayoutParams(mLastParam);
            mainViewLayout.setPadding(0, 0, 0, mPaddingBottom);
        } else {*/
        mainViewLayout.setPadding(0, 0, 0, 0);
        mainViewLayout.setLayoutParams(mLayoutParams);
        //        }
        mainViewLayout.setTag(subscription);

        String channel = subscription.channel;
        boolean isImmutableChannel = channel.equals(RssSubscribedChannel.RANDOM_READ)
                        || channel.equals(RssSubscribedChannel.OFFLINE_DOWNLOAD)
                        || channel.equals(RssSubscribedChannel.ADD_SUBSCRIBE)
                        || channel.equals(RssSubscribedChannel.FEEDBACK)
                        || channel.equals(RssSubscribedChannel.MY_CONLLECTION);
        if (isImmutableChannel) {
            loadImmutableChannelCover(imageView,title, channel);
        } else {
            imageView.setImageResource(R.drawable.rd_reader_main_default);
            imageView.setTag(BitmapFactoryBase.IMAGE_STATE_DEFAULT);
            title.setText(subscription.title);
            loadChannelImage(imageView, cursor.getPosition());
        }

        ImageView delectBtn = (ImageView) mainViewLayout.findViewById(R.id.rss_home_delete_btn);
        if (isDelete) {
            if (!isImmutableChannel) {
                delectBtn.setVisibility(View.VISIBLE);
            } else {
                delectBtn.setVisibility(View.GONE);
            }
            delectBtn.setOnClickListener(mListener);
            delectBtn.setOnTouchListener(mDeleteBtnTouchListener);
            changeMargin(imageView,title, mDeletePadding, mDeletePadding, mDeletePadding, mDeletePadding);
        } else {
            delectBtn.setVisibility(View.GONE);
            changeMargin(imageView,title, 5, 5, 5, 5);
        }
    }

    private void loadImmutableChannelCover(ImageView imageView,TextView title, String channel) {
        Bitmap coverBitmap = mCoverCache.get(channel);
        setTitle(title, channel);
        if (coverBitmap != null) {
            imageView.setImageBitmap(coverBitmap);
            imageView.setTag(BitmapFactoryBase.IMAGE_STATE_LOADED);
            return;
        }

        int drawable = -1;
        if (channel.equals(RssSubscribedChannel.RANDOM_READ)) {
            drawable = R.drawable.rd_main_random_read;
        } else if (channel.equals(RssSubscribedChannel.OFFLINE_DOWNLOAD)) {
            drawable = R.drawable.rd_main_offline;
        } else if (channel.equals(RssSubscribedChannel.ADD_SUBSCRIBE)) {
            drawable = R.drawable.rd_main_reader_article;
        } else if (channel.equals(RssSubscribedChannel.FEEDBACK)) {
            drawable = R.drawable.rd_feed_back_icon;
        } else if (channel.equals(RssSubscribedChannel.MY_CONLLECTION)) {
            drawable = R.drawable.rd_reader_collection;
        }

        if (drawable > 0) {
            coverBitmap = CommonUtil.combineBmp(mContext, drawable);
            if (coverBitmap != null) {
                imageView.setImageBitmap(coverBitmap);
                imageView.setTag(BitmapFactoryBase.IMAGE_STATE_LOADED);
                mCoverCache.put(channel, coverBitmap);
            }
        }
    }

    private void setTitle(TextView title,String channel){
        if (channel.equals(RssSubscribedChannel.RANDOM_READ)) {
            title.setText("摇一摇");
        } else if (channel.equals(RssSubscribedChannel.OFFLINE_DOWNLOAD)) {
            title.setText("离线下载");
        } else if (channel.equals(RssSubscribedChannel.ADD_SUBSCRIBE)) {
            title.setText("");
        } else if (channel.equals(RssSubscribedChannel.FEEDBACK)) {
            title.setText("意见反馈");
        } else if (channel.equals(RssSubscribedChannel.MY_CONLLECTION)) {
            title.setText("收藏的文章");
        }
    }

    @Override
    public int getCount() {
        if (isDelete) {
            return super.getCount() - 1;
        }
        return super.getCount();
    }

    public void updateImages(AbsListView view) {
        int firstPos = view.getFirstVisiblePosition();
        int count = view.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = view.getChildAt(i);
            ImageView imageView = (ImageView) child.findViewById(R.id.home_button);
            if (imageView != null && imageView.getTag() != null) {
                loadChannelImage(imageView, firstPos + i);
            }
        }
    }

    private void loadChannelImage(ImageView view, int position) {
        if (view.getTag() == null || !BitmapFactoryBase.IMAGE_STATE_DEFAULT.equals((String) view.getTag())) {
            return;
        }

        Cursor cursor = getCursor();
        cursor.moveToPosition(position);
        SubscribedChannel subscription = RssSubscribedChannel.inject(cursor);

        Bitmap coverBitmap = mCoverCache.get(subscription.channel);
        if(coverBitmap != null) {
            Utils.debug(TAG, "Hard Cover Cache Hitted! 加载频道的图片");
            view.setImageBitmap(coverBitmap);
            view.setTag(BitmapFactoryBase.IMAGE_STATE_LOADED);
            return;
        } else {
            Utils.debug(TAG, "Hard Cover Cache Missed!");
        }

        if (TextUtils.isEmpty(subscription.photo_url)) {
            return;
        }

        String url = subscription.photo_url;
        if (subscription.image_version > 0) {
            url += ChannelBitmapFacotry.IMAGE_VERSION_PREFIX + subscription.image_version;
        }

        ChannelBitmapFacotry cbf = ChannelBitmapFacotry.getInstance(mContext);
        coverBitmap = cbf.getBitmapByUrlFromCache(url);
        if (coverBitmap == null) {
            SetDefaultChannelCoverResult result = ChannelBitmapFacotry.getInstance(mContext)
                    .setDefaultChannelCover(mContext, subscription, view);
            if (result.mNeedDownload) {
                view.setTag(url);
                cbf.requestLoading(url, view);
            } else {
                view.setTag(BitmapFactoryBase.IMAGE_STATE_LOADED);
                if (result.mCoverBitmap != null) {
                    mCoverCache.put(subscription.channel, result.mCoverBitmap);
                }
            }
        } else {
            view.setImageBitmap(coverBitmap);
            view.setTag(BitmapFactoryBase.IMAGE_STATE_LOADED);
            mCoverCache.put(subscription.channel, coverBitmap);
        }

    }

    private boolean isDelete = false;

    public void setDeleteCondition(boolean isDelete) {
        this.isDelete = isDelete;
    }

    public boolean getDeleteCondition() {
        return isDelete;
    }

    @Override
    public void onGridSizeChange(int width, int height) {
        mLayoutParams.width = width;
        mLayoutParams.height = height;
        mLastParam.width = width;
        mLastParam.height = height + mPaddingBottom;
    }

    private OnClickListener mListener = new OnClickListener() {

        @Override
        public void onClick(final View v) {

            ReaderMainView.mTapConsumed = true;
            final View view = (View) v.getParent();
            final SubscribedChannel subscription = (SubscribedChannel) view.getTag();

            String channel = subscription.channel;
            final boolean isImmutableChannel = channel.equals(RssSubscribedChannel.RANDOM_READ)
                            || channel.equals(RssSubscribedChannel.OFFLINE_DOWNLOAD)
                            || channel.equals(RssSubscribedChannel.ADD_SUBSCRIBE)
                            || channel.equals(RssSubscribedChannel.FEEDBACK)
                            || channel.equals(RssSubscribedChannel.MY_CONLLECTION);

            if (!isImmutableChannel) {
                subscription.unsubscribe(mContext.getContentResolver());
            }

            if (mOnSubscriptionRemoveListener != null) {
                mOnSubscriptionRemoveListener.onSubscriptionRemoved();
            }
            /**
             * 打点，取消订阅时，删除记录订阅频道的key
             */
            CollectSubScribe.getInstance().clearSubScribeKey(subscription.channel);

            UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Home_Icon_Deleted, 1);

        }
    };

    private OnTouchListener mDeleteBtnTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View paramView, MotionEvent paramMotionEvent) {
            int action = paramMotionEvent.getAction();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP
                            || action == MotionEvent.ACTION_CANCEL) {
                View targetImageView = ((ViewGroup) paramView.getParent()).findViewById(R.id.home_button);
                if (targetImageView != null) {
                    int[] stateList = { action == MotionEvent.ACTION_DOWN ? android.R.attr.state_pressed
                                    : android.R.attr.state_empty };
                    targetImageView.getBackground().setState(stateList);
                }
            }

            return false;
        }
    };

    public void clearCoverCache() {
        Utils.debug(TAG, "Cover Cache cleared!");
        mCoverCache.clear();
    }

}