/**
 *
 */
package com.qihoo360.reader.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;

import com.qihoo360.browser.hip.UXHelper;
import com.qihoo360.browser.hip.UXHelperConfig;
import com.qihoo360.browser.hip.UXKey;
import com.qihoo360.reader.Constants;
import com.qihoo360.reader.R;
import com.qihoo360.reader.ReaderApplication;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.support.SystemUtils;
import com.qihoo360.reader.support.UiUtils;

/**
 * 各种阅读Activity的基类
 *
 * @author Jiongxuan Zhang
 *
 */
public abstract class ActivityBase extends Activity implements Nightable {

    public static final String BACK_TO_DESKTOP = "back_to_desktop";

    private boolean mIsHip = true;

    /**
     * 不需要打点：“阅读和我喜欢中打开过的页面”
     *
     * 注意，子类必须在调用super.OnCreate之前去调用它
     */
    protected void disableHipOpenPage() {
        mIsHip = false;
    }

    /**
     * 获取打点数。默认是打“阅读PV”。
     *
     * 如果有特殊需要，如切换到“我喜欢PV”等，请复写该方法。
     * @return
     */
    protected UXKey getPvHipId() {
        return UXHelperConfig.Reader_PV_Times;
    }

    private BroadcastReceiver mExitingReaderReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context paramContext, Intent paramIntent) {
            finish();
        }
    };

    private BroadcastReceiver mNightableReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            onUpdateNightMode();
        }
    };

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mIsHip) {
            UXHelper.getInstance().addActionRecord(getPvHipId(), 1);
        }

        IntentFilter exitingFilter = new IntentFilter(
                Constants.READER_BROADCAST_EXITING_READER);
        registerReceiver(mExitingReaderReceiver, exitingFilter);

        IntentFilter nightableFilter = new IntentFilter(
                Constants.READER_BROADCAST_SWITCH_THE_DAY_AND_NIGHT_MODE);
        registerReceiver(mNightableReceiver, nightableFilter);
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();

        onUpdateNightMode();
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onDestroy()
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mExitingReaderReceiver);
        unregisterReceiver(mNightableReceiver);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.qihoo360.reader.ui.Nightable#onUpdateNightMode()
     */
    @Override
    public void onUpdateNightMode() {
        SystemUtils.setNightModeBrightness(this, Settings.isNightMode());
    }

    protected void setNightMask() {
        if (Settings.isNightMode() == true) {
            UiUtils.insertNightMask(ActivityBase.this, getWindow()
                    .getDecorView());
        } else {
            UiUtils.removeNightMask(ActivityBase.this, getWindow()
                    .getDecorView());
        }
    }
    /**
     * 设置背景图
     */
    protected void setBackground(int containerId) {
        Bitmap bitmap = ReaderPlugin.getInstance().getBackGroundCommon();
        View topContainer = findViewById(containerId);
        if (bitmap != null) {
            topContainer.setBackgroundDrawable(new BitmapDrawable(bitmap));
        } else {
            topContainer.setBackgroundResource(R.drawable.rd_bg);
        }
    }

    /**
     * 关闭所有Activity
     */
    public static void closeAll() {
        ReaderApplication.getContext().sendBroadcast(
                new Intent(Constants.READER_BROADCAST_EXITING_READER));
    }
}
