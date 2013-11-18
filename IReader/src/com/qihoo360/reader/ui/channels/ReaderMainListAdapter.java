package com.qihoo360.reader.ui.channels;

import com.qihoo360.browser.hip.CollectSubScribe;
import com.qihoo360.browser.hip.UXHelper;
import com.qihoo360.browser.hip.UXHelperConfig;
import com.qihoo360.common.IGridSizeChangeListener;
import com.qihoo360.reader.R;
import com.qihoo360.reader.image.BitmapFactoryBase;
import com.qihoo360.reader.image.ChannelBitmapFacotry;
import com.qihoo360.reader.subscription.SubscribedChannel;
import com.qihoo360.reader.subscription.reader.RssSubscribedChannel;
import com.qihoo360.reader.support.AnimationHelper;
import com.qihoo360.reader.ui.channels.ImageAdapter.OnSubscriptionRemoveListener;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.AbsListView;
import android.widget.AbsListView.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import java.util.List;

public class ReaderMainListAdapter extends BaseAdapter implements IGridSizeChangeListener {

    private List<RssSubscribedChannel> mList;
    private Context mContext;
    private OnSubscriptionRemoveListener mOnSubscriptionRemoveListener = null;
    private final LayoutParams mLayoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    private boolean mBusy = false;

    public ReaderMainListAdapter(Context context, List<RssSubscribedChannel> list,
                    OnSubscriptionRemoveListener subscriptionRemoveListener) {
        this.mList = list;
        this.mContext = context;
        mOnSubscriptionRemoveListener = subscriptionRemoveListener;
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View mainViewLayout, ViewGroup parent) {
        if (mainViewLayout == null) {
            mainViewLayout = LayoutInflater.from(mContext).inflate(R.layout.rd_gridview_item, null);
        }
        mainViewLayout.setLayoutParams(mLayoutParams);

        final SubscribedChannel subscription = mList.get(position);
        ImageView imageView = (ImageView) mainViewLayout.findViewById(R.id.home_button);
        TextView titleView = (TextView) mainViewLayout.findViewById(R.id.rss_home_textview);
        mainViewLayout.setTag(subscription);

        ImageView delectBtn = (ImageView) mainViewLayout.findViewById(R.id.rss_home_delete_btn);
        delectBtn.setOnClickListener(mListener);

        if (subscription.channel.equals(RssSubscribedChannel.RANDOM_READ)) {
            imageView.setImageResource(R.drawable.rd_main_random_read);
            imageView.setScaleType(ScaleType.FIT_XY);
            titleView.setText("");
        } else if (subscription.channel.equals(RssSubscribedChannel.OFFLINE_DOWNLOAD)) {
            imageView.setImageResource(R.drawable.rd_main_offline);
            imageView.setScaleType(ScaleType.FIT_XY);
            titleView.setText("");
        } else if (subscription.channel.equals(RssSubscribedChannel.ADD_SUBSCRIBE)) {
            imageView.setImageResource(R.drawable.rd_main_reader_article);
            imageView.setScaleType(ScaleType.FIT_XY);
            titleView.setText("");
        } else {
            imageView.setImageResource(R.drawable.rd_reader_main_default);
            imageView.setTag(BitmapFactoryBase.IMAGE_STATE_DEFAULT);
            loadChannelImage(imageView, position);
            titleView.setText(subscription.title);
        }

        delectBtn.setVisibility(View.GONE);
        if (isDelete) {
            if (!subscription.channel.equals(RssSubscribedChannel.MY_CONLLECTION)
                            && !subscription.channel.equals(RssSubscribedChannel.OFFLINE_DOWNLOAD)
                            && !subscription.channel.equals(RssSubscribedChannel.SITE_NAVIGATION)
                            && !subscription.channel.equals(RssSubscribedChannel.ADD_SUBSCRIBE)
                            && !subscription.channel.equals(RssSubscribedChannel.RANDOM_READ)) {
                delectBtn.setVisibility(View.VISIBLE);
            }
        } else {
            delectBtn.setVisibility(View.GONE);
        }

        return mainViewLayout;
    }

    public void setBusy(boolean value) {
        mBusy = value;
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
        if (view.getTag() == null || BitmapFactoryBase.IMAGE_STATE_LOADED.equals((String) view.getTag())) {
            return;
        }
        SubscribedChannel subscription = mList.get(position);

        if (TextUtils.isEmpty(subscription.photo_url)) {
            return;
        }

        String url = subscription.photo_url;
        if (subscription.image_version > 0) {
            url += ChannelBitmapFacotry.IMAGE_VERSION_PREFIX + subscription.image_version;
        }

        ChannelBitmapFacotry cbf = ChannelBitmapFacotry.getInstance(mContext);
        Bitmap bitmap = cbf.getBitmapByUrlFromCache(url);
        if (bitmap == null) {
            if (!mBusy) {
                boolean needDownload = ChannelBitmapFacotry.getInstance(mContext)
                        .setDefaultChannelCover(mContext, subscription, view).mNeedDownload;

                if (needDownload) {
                    view.setTag(url);
                    cbf.requestLoading(url, view);
                } else {
                    view.setTag(BitmapFactoryBase.IMAGE_STATE_LOADED);
                }
            }

        } else {
            view.setImageBitmap(bitmap);
            view.setTag(BitmapFactoryBase.IMAGE_STATE_LOADED);
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
    }

    public void setData(List<RssSubscribedChannel> list) {
        this.mList = list;
    }

    private OnClickListener mListener = new OnClickListener() {

        @Override
        public void onClick(final View v) {
            View view = (View) v.getParent();
            Animation animation = AnimationHelper.animationScaleHide();
            //view.findViewById(R.id.main_content).startAnimation(animation);
            v.setVisibility(View.GONE);
            final SubscribedChannel subscription = (SubscribedChannel) view.getTag();
            animation.setAnimationListener(new AnimationListener() {

                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    subscription.unsubscribe(mContext.getContentResolver());
                    /**
                     * 打点，取消订阅时，删除记录订阅频道的key
                     */
                    CollectSubScribe.getInstance().clearSubScribeKey(subscription.channel);

                    if (mOnSubscriptionRemoveListener != null) {
                        mOnSubscriptionRemoveListener.onSubscriptionRemoved();
                    }
                    UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Home_Icon_Deleted, 1);
                }
            });
        }
    };

}
