/**
 *
 */

package com.qihoo360.reader.support;

import com.qihoo360.reader.ReaderApplication;
import com.qihoo360.reader.Settings;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.TelephonyManager;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

/**
 * 有关调用系统的工具
 *
 * @author Jiongxuan Zhang
 */
public class SystemUtils {

    /**
     * 立即刷新系统的媒体文件
     */
    public static void refreshSystemMedia() {
        ReaderApplication.getContext().sendBroadcast(
                new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"
                        + Environment.getExternalStorageDirectory())));
    }

    /**
     * 获取Mac地址
     *
     * @param context
     * @return
     */
    public static String getMacAddress(Context context) {
        try {
            WifiManager wifi = (WifiManager) context
                    .getSystemService(Context.WIFI_SERVICE);

            WifiInfo info = wifi.getConnectionInfo();
            return info.getMacAddress();
        } catch (Exception e) {
            Utils.error(SystemUtils.class, Utils.getStackTrace(e));
            return null;
        }
    }

    /**
     * 获取IMEI号
     *
     * @param c
     * @return
     */
    public static String getIMEI(Context c) {
        TelephonyManager telephonyManager = (TelephonyManager) c
                .getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getDeviceId();
    }

    /**
     * 获取设备的分辨率，用Point表示
     *
     * @param context
     * @return
     */
    public static Point getDeviceResolution(Context context) {
        Point point = new Point();
        WindowManager manager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        if (manager != null) {
            Display display = manager.getDefaultDisplay();
            if (display != null) {
                point.set(display.getWidth(), display.getHeight());
            }
        }

        return point;
    }

    public static int getSystemBrightness(ContentResolver resolver) {
        try {
            return android.provider.Settings.System.getInt(resolver,
                    "screen_brightness");
        } catch (SettingNotFoundException e) {
            return 0;
        }
    }

    /**
     * 设置夜间模式，调节屏幕亮度
     *
     * @param activity
     * @param isNightMode
     */
    public static void setNightModeBrightness(Activity activity,
            boolean isNightMode) {
        Window window = activity.getWindow();
        LayoutParams params = window.getAttributes();
        if (isNightMode == true) {
            Settings.setBrightness(activity);
            if (CompatabilitySupport.isM9()) {
                params.screenBrightness = 0.8f;
            } else {
                params.screenBrightness = 0.2f;
            }
        } else {
            params.screenBrightness = getSystemBrightness(activity
                    .getContentResolver());
            if (params.screenBrightness == 0) {
                params.screenBrightness = Settings.getBrightness();
            }
        }
        window.setAttributes(params);
    }

}
