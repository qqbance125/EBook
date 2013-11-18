/**
 *
 */
package com.qihoo360.reader.support;

/**
 * @author Jiongxuan Zhang
 *
 */

import android.os.Build;

/**
 * 机型适配
 *
 * @author zhangjiongxuan
 */
public class CompatabilitySupport {
    public final static boolean canSupportPlugin() {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.ECLAIR_MR1;
    }

    public final static boolean isM9(){
        if(android.os.Build.MODEL.equals("M9")){
            return true;
        }else {
            return false;
        }
    }
}

