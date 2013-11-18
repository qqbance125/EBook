package com.qihoo360.browser.hip;

import android.text.TextUtils;

import java.util.HashSet;

import com.qihoo360.reader.BuildEnv;

public class UXKey {
    private String mKey = "";
    private static HashSet<String> mKeySet = null;
    private static boolean debug = BuildEnv.DEBUG;
    public UXKey(int key, String memo) throws RuntimeException{
        mKey = Integer.toString(key);
        if (debug) {
            //check duplicate key
            if (mKeySet == null) {
                mKeySet = new HashSet<String>();
            }
            if (!mKeySet.add(mKey)) {
                throw new RuntimeException("UXKeySet found duplicate at key:" + mKey);
            }
            //check empty memo
            if (TextUtils.isEmpty(memo)) {
                throw new RuntimeException("UXKey found empty memo at key:" + mKey);
            }
        }
    }
    public String getKey() {
        return mKey;
    }
}
