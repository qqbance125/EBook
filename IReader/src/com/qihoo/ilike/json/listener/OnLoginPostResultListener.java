package com.qihoo.ilike.json.listener;


public interface OnLoginPostResultListener extends IOnGetResultListener {
    void onComplete();
    void onHandleCaptcha(boolean needCaptcha);
}
