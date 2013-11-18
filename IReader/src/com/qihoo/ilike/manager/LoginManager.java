/**
 *
 */
package com.qihoo.ilike.manager;

import com.qihoo360.reader.ReaderApplication;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;

/**
 * 管理账户的类
 *
 * @author Jiongxuan Zhang
 *
 */
public class LoginManager {

    private final static String LOG_IN_SHARED_PREFERENCE_NAME = "LogInSharedPreferenceName";

    private final static String LOG_IN_LATEST_USER_Q_STRING = "LogInLatestUserQString";
    private final static String LOG_IN_LATEST_USER_T_STRING = "LogInLatestUserTString";
    private final static String LOG_IN_LATEST_USER_QID = "LogInLatestUserQID";

    /**
     * 判断用户是否已经登录过。
     *
     * @return
     */
    public static boolean isLogin() {
        return !TextUtils.isEmpty(getQ()) || !TextUtils.isEmpty(getT());
    }

    /**
     * 从浏览器“个人中心”中，获取QID
     *
     * @return
     */
    public static String getQid() {
        SharedPreferences loginPreferences = ReaderApplication.getContext()
                .getSharedPreferences(LOG_IN_SHARED_PREFERENCE_NAME, 0);
        return loginPreferences.getString(LOG_IN_LATEST_USER_QID, "");
    }

    /**
     * 获取Q值
     *
     * @return
     */
    public static String getQ() {
        SharedPreferences loginPreferences = ReaderApplication.getContext()
                .getSharedPreferences(LOG_IN_SHARED_PREFERENCE_NAME, 0);
        return loginPreferences.getString(LOG_IN_LATEST_USER_Q_STRING, "");
    }

    /**
     * 获取T值
     *
     * @return
     */
    public static String getT() {
        SharedPreferences loginPreferences = ReaderApplication.getContext()
                .getSharedPreferences(LOG_IN_SHARED_PREFERENCE_NAME, 0);
        return loginPreferences.getString(LOG_IN_LATEST_USER_T_STRING, "");
    }

    // 不可用
    public static void testLogin(String qid, String q, String t) {
        SharedPreferences loginPreferences = ReaderApplication.getContext()
                .getSharedPreferences(LOG_IN_SHARED_PREFERENCE_NAME, 0);
        Editor editor = loginPreferences.edit();
        editor.putString(LOG_IN_LATEST_USER_QID, qid);
        editor.putString(LOG_IN_LATEST_USER_Q_STRING, q);
        editor.putString(LOG_IN_LATEST_USER_T_STRING, t);
        editor.commit();
    }
}
