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

public class AddReaderLeftPaneAdapter extends BaseAdapter {

    private Context mContext;
    private List<Category> mTitles;
    public static final int DEFAULT_HIGHLIGHT_POSITION = 0;

    public AddReaderLeftPaneAdapter(List<Category> list, Context context) {
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
        convertView = LayoutInflater.from(mContext).inflate(R.layout.rd_add_reader_leftpane_item, null);
        TextView tv = (TextView) convertView.findViewById(R.id.category_name);
        tv.setText(mTitles.get(position).name);
        convertView.findViewById(R.id.add_reader_root).setBackgroundResource(0);
        tv.setTextColor(Color.WHITE);
        if (mHighLightPosition == position) {
            // 夜间模式下的判断
            convertView.findViewById(R.id.add_reader_root).setBackgroundResource(R.drawable.rd_add_reader_select_bg);
            mSelectView = convertView;
            if (Settings.isNightMode() == true) {
                //                tv.setTextColor(tv.getContext().getResources().getColor(R.color.rd_night_text));
            } else {
                //                tv.setTextColor(tv.getContext().getResources().getColor(R.color.rd_channel_category_title_selected));
            }
        } else {
            tv.setTextColor(Color.WHITE);
        }
        return convertView;
    }

    private int mHighLightPosition = DEFAULT_HIGHLIGHT_POSITION;

    private int mPreHighLightPosition = DEFAULT_HIGHLIGHT_POSITION;

    public void setHighLightPosition(int position) {
        mPreHighLightPosition = mHighLightPosition;
        mHighLightPosition = position;
    }

    public int getPreHighLightPosition() {
        return mPreHighLightPosition;
    }

    private View mSelectView;

    public View getSelectView() {
        return mSelectView;
    }
}
