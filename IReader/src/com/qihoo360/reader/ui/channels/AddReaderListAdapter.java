package com.qihoo360.reader.ui.channels;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.qihoo360.reader.R;
import com.qihoo360.reader.image.BitmapFactoryBase;
import com.qihoo360.reader.image.ChannelBitmapFacotry;
import com.qihoo360.reader.subscription.Channel;
import com.qihoo360.reader.subscription.reader.RssChannel;
import com.qihoo360.reader.subscription.reader.RssSubscribedChannel;
import com.qihoo360.reader.support.Utils;
import com.qihoo360.reader.ui.CommonUtil;

public class AddReaderListAdapter extends BaseAdapter {

    private List<RssChannel> mChannels = new ArrayList<RssChannel>();
    private Context mContext;
    private boolean mLoadDefaultImage = true;
    private final int mPaddingBottom;

    public AddReaderListAdapter(Context context, List<RssChannel> list) {
        this.mChannels = list;
        this.mContext = context;
        mPaddingBottom = (int) context.getResources().getDimension(R.dimen.rd_main_padding_bottom);
    }

    public void setLoadDefaultImage(boolean value) {
        mLoadDefaultImage = value;
    }

    @Override
    public int getCount() {
        return mChannels.size();
    }

    @Override
    public Object getItem(int position) {
        return mChannels.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder = null;
        if (convertView == null) {
            convertView = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.rd_add_reader_list_item, null);
            ImageView imageView = (ImageView) convertView.findViewById(R.id.home_button);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

            holder = new ViewHolder();
            holder.iv_image = imageView;
            holder.tv_title = (TextView) convertView.findViewById(R.id.rss_home_textview);
            holder.tv_subscribe_mark = (ImageView) convertView.findViewById(R.id.mark_subscribed);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }


        holder.iv_image.setImageBitmap(null);
        holder.iv_image.setTag(BitmapFactoryBase.IMAGE_STATE_DEFAULT);
        if (!mLoadDefaultImage) {
            loadChannelImage(holder, position);
        }

        final Channel channel = mChannels.get(position);

        //        if (Settings.isNightMode() == true) {
        //            holder.tv_title.setTextColor(ReaderApplication.getContext().getResources().getColor(R.color.rd_night_text));
        //        } else {
        //            holder.tv_title.setTextColor(ReaderApplication.getContext().getResources().getColor(R.color.rd_black));
        //        }
        holder.tv_title.setText(channel.title);

        if (mSubScribedChannel.contains(channel.channel)) {
//            holder.tv_subscribe_mark.setImageResource(R.drawable.rd_subscribe_select);
            holder.tv_subscribe_mark.setImageBitmap(null);
            holder.tv_subscribe_mark.setOnClickListener(null);
        } else {
            holder.tv_subscribe_mark.setImageResource(R.drawable.rd_subscribe_btn);
            holder.tv_subscribe_mark.setTag(channel);
            holder.tv_subscribe_mark.setOnClickListener(mListener);
        }

        return convertView;
    }

    public void updateImages(AbsListView view) {
        int firstPos = view.getFirstVisiblePosition();
        int count = view.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = view.getChildAt(i);
            ViewHolder holder = (ViewHolder) child.getTag();
            if (holder != null) {
                loadChannelImage(holder, firstPos + i);
            }
        }
    }

    private void loadChannelImage(ViewHolder holder, int position) {
        if (holder.iv_image.getTag() == null
                        || !BitmapFactoryBase.IMAGE_STATE_DEFAULT.equals((String) holder.iv_image.getTag())) {
            return;
        }

        Channel channel = mChannels.get(position);

        int imageVersion = 0;
        if (!TextUtils.isEmpty(channel.imageversion)) {
            try {
                imageVersion = Integer.parseInt(channel.imageversion);
            } catch (Exception e) {
                Utils.error(getClass(), Utils.getStackTrace(e));
            }
        }

        String url = channel.image;
        if (imageVersion > 0) {
            url += ChannelBitmapFacotry.IMAGE_VERSION_PREFIX + imageVersion;
        }

        Bitmap bitmap = ChannelBitmapFacotry.getInstance(mContext).getBitmapByUrlFromCache(url);
        if (bitmap == null) {
            boolean needDownload = ChannelBitmapFacotry.getInstance(mContext).setDefaultChannelCover(mContext, channel,
                            holder.iv_image);
            if (needDownload) {
                holder.iv_image.setTag(url);
                ChannelBitmapFacotry.getInstance(mContext).requestLoading(url, holder.iv_image);
            } else {
                holder.iv_image.setTag(BitmapFactoryBase.IMAGE_STATE_LOADED);
            }
        } else {
            holder.iv_image.setImageBitmap(bitmap);
            holder.iv_image.setTag(BitmapFactoryBase.IMAGE_STATE_LOADED);
        }
    }

    private class ViewHolder {
        ImageView iv_image;
        TextView tv_title;
        ImageView tv_subscribe_mark;
    }

    private List<String> mSubScribedChannel;

    public void setSubScribedChannel(List<String> list) {
        this.mSubScribedChannel = list;
    }

    public List<RssChannel> getChannels() {
        return mChannels;
    }

    public void setChannels(List<RssChannel> list) {
        mChannels = list;
    }

    private boolean doSubscribe(Channel channel) {
        if (RssSubscribedChannel.isAllowToInsert(mContext.getContentResolver()) == false) {
            CommonUtil.showToast(R.string.rd_add_notice);
            return false;
        } else {
            channel.subscribe(mContext.getContentResolver());
            CommonUtil.showToast(R.string.rd_subscribe_channel_succeed);
            mSubScribedChannel.add(channel.channel);
            return true;
        }

    }

    private OnClickListener mListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            Channel channel = (Channel) v.getTag();
            if (doSubscribe(channel)) {
                ((ImageView) v).setImageBitmap(null);
                v.setOnClickListener(null);
            }
        }
    };
}
