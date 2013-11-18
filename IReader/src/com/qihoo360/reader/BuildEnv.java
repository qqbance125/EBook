package com.qihoo360.reader;

import java.lang.reflect.Field;

import com.qihoo360.reader.support.Utils;

public class BuildEnv {
    public static final String AUTHORITY_READER = "com.qihoo.browser.provider.reader";
    public static final String AUTHORITY_ILIKE = "com.qihoo.browser.provider.ilike";
    public static final String AUTHORITY_UXHELPER = "com.qihoo.browser.provider.uxhelper";
    public static boolean DEBUG;
    static {
        boolean debugValue = false;
        try {
            // Class clazz = Class.forName("com.qihoo360.browser.BuildEnv");
            // Field field = clazz.getField("mBuildEnvEnableLog");
            // debugValue = (Boolean) field.get(null);
            debugValue = true;
        } catch (Throwable e) {
            System.err.println("Can not find debug setting under class name :com.qihoo360.browser.BuildEnv. Class name changed?");
            Utils.error(BuildEnv.class, Utils.getStackTrace(e));
        }
        DEBUG = debugValue;
    }
}
