package com.qihoo360.reader.ui.channels;

import com.qihoo360.reader.R;

import android.view.View;

import java.util.Random;

/**
 * 随机设置频道的背景类
 *
 * @author Zhihui Tang & Jiongxuan Zhang
 *
 */
public class ChannelBackgroundSetter {
    private final int[] mBgResIds = { R.drawable.rd_subscribe_bg_1, R.drawable.rd_subscribe_bg_2,
            R.drawable.rd_subscribe_bg_3, R.drawable.rd_subscribe_bg_4, R.drawable.rd_subscribe_bg_5,
            R.drawable.rd_subscribe_bg_6, R.drawable.rd_subscribe_bg_7 };

    private Random mRandom = new Random();

    /**
     * 随机设置频道的背景
     * @param view
     */
    public void setRandomBg(View view) {
        int bgPosition = mRandom.nextInt(mBgResIds.length);
        setBgFromPosition(view, bgPosition);
    }

    /**
     * 通过当前项目的位置，设置随机频道背景
     * @param view
     * @param itemPosition
     * @return
     */
    public void setRandomBg(View view, int itemPosition) {
        setBgFromPosition(view, itemPosition % getLength());
    }

    private void setBgFromPosition(View view, int position) {
        if (position < 0 || position > getLength()) {
            throw new IllegalArgumentException("position < 0 or position > mBgResIds.length");
        }

        view.setBackgroundResource(mBgResIds[position]);
    }

    private int getLength() {
        return mBgResIds.length;
    }
}
