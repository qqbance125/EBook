package com.qihoo360.reader.ui.channels;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.qihoo360.reader.R;
import com.qihoo360.reader.ReaderApplication;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.image.BitmapFactoryBase;
import com.qihoo360.reader.image.ChannelBitmapFacotry;
import com.qihoo360.reader.subscription.Channel;
import com.qihoo360.reader.subscription.reader.RssChannel;
import com.qihoo360.reader.support.Utils;

public class AddReaderGridAdapter extends BaseAdapter {

    private List<RssChannel> mChannels = new ArrayList<RssChannel>();
    private Context mContext;
    private boolean mLoadDefaultImage = true;
    private boolean mBusy = false;
    private final int mPaddingBottom;

    private final ChannelBackgroundSetter mBackgroundSetter = new ChannelBackgroundSetter();

    public AddReaderGridAdapter(Context context, List<RssChannel> list) {
        this.mChannels = list;
        this.mContext = context;
        mPaddingBottom = (int) context.getResources().getDimension(R.dimen.rd_main_padding_bottom);
    }

    public void setLoadDefaultImage(boolean value) {
        mLoadDefaultImage = value;
    }

    public void setBusy(boolean value) {
        mBusy = value;
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
            convertView = LayoutInflater.from(mContext).inflate(R.layout.rd_reader_gird_item, null);
            ImageView imageView = (ImageView) convertView.findViewById(R.id.home_button);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            convertView.findViewById(R.id.rss_home_delete_btn).setTag(mChannels.get(position));

            holder = new ViewHolder();
            holder.iv_image = imageView;
            holder.tv_title = (TextView) convertView.findViewById(R.id.rss_home_textview);
            holder.v_delete = convertView.findViewById(R.id.rss_home_delete_btn);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        convertView.setPadding(0, 0, 0, 0);
        if (position == getCount() - 1) {
            convertView.setPadding(0, 0, 0, mPaddingBottom);
        }

        holder.iv_image.setImageBitmap(null);
        holder.iv_image.setTag(BitmapFactoryBase.IMAGE_STATE_DEFAULT);
        mBackgroundSetter.setRandomBg(holder.iv_image);
        if (!mLoadDefaultImage) {
            loadChannelImage(holder, position);
        }

        if (Settings.isNightMode() == true) {
            holder.tv_title.setTextColor(ReaderApplication.getContext().getResources().getColor(R.color.rd_night_text));
        } else {
            holder.tv_title.setTextColor(ReaderApplication.getContext().getResources().getColor(R.color.rd_black));
        }
        holder.tv_title.setText(mChannels.get(position).title);

        holder.v_delete.setVisibility(View.INVISIBLE);

        if (mSubScribedChannel.contains(mChannels.get(position).channel)) {
            holder.v_delete.setVisibility(View.VISIBLE);
        } else {
            holder.v_delete.setVisibility(View.GONE);
        }

        holder.tv_title.setVisibility(View.INVISIBLE);

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
        View v_delete;
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
}
