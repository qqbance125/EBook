
package com.qihoo360.reader.support;

import com.qihoo360.reader.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * UI的帮助类
 *
 * @author Jiongxuan Zhang
 */
public class UiUtils {

    /**
     * 增加夜间模式的蒙版
     *
     * @param context
     * @param decorView
     */
    public static void insertNightMask(Context context, View decorView) {
        View maskView = null;
        if (decorView instanceof FrameLayout && decorView.findViewById(R.id.night_mask) == null) {
            maskView = LayoutInflater.from(context).inflate(R.layout.rd_night_mask, null);
            FrameLayout frameLayout = (FrameLayout) decorView;
            frameLayout.addView(maskView);
        }
    }

    /**
     * 删除夜间模式的蒙版
     *
     * @param context
     * @param decorView
     */
    public static void removeNightMask(Context context, View decorView) {
        if (decorView instanceof FrameLayout) {
            View maskView = decorView.findViewById(R.id.night_mask);
            if (maskView != null) {
                FrameLayout frameLayout = (FrameLayout) decorView;
                frameLayout.removeView(maskView);
            }
        }
    }

    /**
     * 获取大致相对的时间：今天、昨天、前天、3天以前
     * @param millseconds
     * @return
     */
    public static String getRoughlyRelativeDateString(long milliseconds) {
        long nowDays = System.currentTimeMillis() / 86400000;
        long thisDays = milliseconds / 86400000;

        long daysDivided = nowDays - thisDays;

        if (daysDivided > 3) {
            return "3天之前";
        } else {
            SimpleDateFormat format = new SimpleDateFormat("HH:mm");
            String dayString = null;
            if (daysDivided == 2) {
                dayString = "前天";
            } else if (daysDivided == 1) {
                dayString = "昨天";
            } else if (daysDivided == 0) {
                dayString = "今天";
            }

            dayString += " " + format.format(new Date(milliseconds));

            return dayString;
        }
    }
}
