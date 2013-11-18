
package com.qihoo.ilike.util;

import com.qihoo.ilike.manager.LoginManager;

import android.os.Environment;

public class Constants {
    private static String WOXIHUAN_ROOT = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + "/.woxihuan/";
    public static final String LOCAL_PATH_AUTHOR_PHOTO = com.qihoo360.reader.Constants.LOCAL_PATH_IMAGES
            + "ilike_author_photo/";
    private static String QID;

    public static final String BROADCAST_ACTION_LOGIN_SUCCESS = "com.qihoo.browser.LOGIN_SUCCESS";
    public static final String BROADCAST_VALUE_LOGIN_RESULT = "LOGIN_SUCCESS";


    public static String getAndCreateRootPath() {
        IOUtils.checkDirectory(WOXIHUAN_ROOT);
        return getRootPath();
    }

    public static String getRootPath() {
        return WOXIHUAN_ROOT;
    }

    public static String getQid() {
        return LoginManager.getQid();
    }

    public static void setQid(String qid) {
        QID = qid;
        PreferenceUtils.putString(PreferenceUtils.QID, QID);
    }
}
