/**
 *
 */
package com.qihoo360.reader.support;

import com.qihoo360.reader.ReaderApplication;
import com.qihoo360.reader.Settings;

import android.content.Context;
import android.text.TextUtils;

/**
 * 表示唯一设备的类
 *
 * @author Jiongxuan Zhang
 *
 */
public class UniqueDevice {
    /**
     * 获取设备的唯一标示符
     *
     * @return
     */
    public static String getString() {
        String uniqueText = Settings.getUniqueDevice();
        if (TextUtils.isEmpty(uniqueText)) {
            Context context = ReaderApplication.getContext();
            String input = SystemUtils.getIMEI(context);
            input += SystemUtils.getMacAddress(context);
            uniqueText = ArithmeticUtils.getMD5(input);
            Settings.setUniqueDevice(uniqueText);
        }
        return uniqueText;
    }
}
