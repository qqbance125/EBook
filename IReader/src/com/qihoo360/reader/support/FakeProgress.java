/**
 *
 */
package com.qihoo360.reader.support;

import com.qihoo360.reader.listener.OnProgressChangedListener;

import android.os.Handler;

/**
 * 假的进度，它可以以无限接近1/X的方式来实现
 *
 * @author Jiongxuan Zhang
 */
public class FakeProgress {

    private int mMax = 100;
    private int mInterval = 600;
    private int mDivide = 3;
    private int mInit = 0;

    private Object mTag;
    private Handler mHandler;
    private OnProgressChangedListener mListener;

    private int mProgress = 0;
    private int mRemain = mMax;

    private boolean isCanceling = false;

    /**
     * 初始化假进度
     * @param listener
     */
    public FakeProgress(OnProgressChangedListener listener) {
        this(new Handler(), listener);
    }

    /**
     * 初始化假进度
     * @param handler
     * @param listener
     */
    public FakeProgress(Handler handler, OnProgressChangedListener listener) {
        mListener = listener;
        mHandler = handler;
    }

    private Runnable mProgressRunnable = new Runnable() {

        @Override
        public void run() {
            if (isCanceling) {
                isCanceling = false;
                return;
            }

            double divide = mRemain / mDivide;
            mProgress += divide;
            mRemain -= divide;
            mListener.onProgressChanged(mTag, mProgress, mMax);
            mHandler.postDelayed(mProgressRunnable, mInterval);
        }
    };

    /**
     * 开始刷新假进度
     */
    public void start() {
        if (mProgress < mInit) {
            mProgress = mInit;
            mRemain = mMax - mInit;
        }
        mHandler.post(mProgressRunnable);
    }

    /**
     * 停止刷新
     */
    public void finish() {
        isCanceling = true;
        mProgress = 0;
        mRemain = mMax;
    }

    /**
     * 设置进度的最大值
     * @param max
     */
    public void setMax(int max) {
        if (mRemain == mMax) {
            mRemain = max;
        }

        mMax = max;
    }

    /**
     * 设置每一次刷新进度的时间间隔
     * @param interval
     */
    public void setInterval(int interval) {
        mInterval = interval;
    }

    /**
     * 设置进度的份数，这直接关系到每次刷新的步长
     * @param divide
     */
    public void setDivide(int divide) {
        mDivide = divide;
    }

    /**
     * 设置在Listener中传递的Tag
     * @param tag
     */
    public void setTag(Object tag) {
        mTag = tag;
    }

    /**
     * 获取初始的值
     * @param init
     */
    public void setInit(int init) {
        mInit = init;
    }
}
