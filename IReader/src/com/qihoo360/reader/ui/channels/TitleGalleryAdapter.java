package com.qihoo360.reader.ui.channels;

import com.qihoo360.reader.R;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.subscription.Category;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class TitleGalleryAdapter extends BaseAdapter {

    private Context mContext;
    private List<Category> mTitles;

    public TitleGalleryAdapter(List<Category> list, Context context) {
        mContext = context;
        mTitles = list;
    }

    @Override
    public int getCount() {
        return mTitles.size();
    }

    @Override
    public Object getItem(int position) {
        return mTitles.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView tv = (TextView) LayoutInflater.from(mContext).inflate(R.layout.rd_title_gallery, null);
        tv.setText(mTitles.get(position).name);
        if (mHighLightPosition == position) {
            // 夜间模式下的判断
            if (Settings.isNightMode() == true) {
                tv.setTextColor(tv.getContext().getResources()
                        .getColor(R.color.rd_night_text));
            } else {
                tv.setTextColor(tv.getContext().getResources()
                        .getColor(R.color.rd_channel_category_title_selected));
            }
        } else {
            tv.setTextColor(Color.WHITE);
        }
        return tv;
    }

    private int mHighLightPosition;

    public void setHighLightPosition(int position) {
        mHighLightPosition = position;
    }

}
