package com.qihoo360.reader.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.KeyEvent;

public class TaskProgressDialog {
    private CustomProgressDialog mProgressDialog;
    private AsyncTask<?, ?, ?> mTask = null;
    public TaskProgressDialog(Context context, AsyncTask<?, ?, ?> task, String title, String msg) {
        mProgressDialog = new CustomProgressDialog(context);
        mTask = task;
        if (msg == null)
            msg = "";
        if (!TextUtils.isEmpty(title))
            mProgressDialog.setTitle(title);
            mProgressDialog.setMessage(msg);
            mProgressDialog.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent evnet) {
                if (keyCode == KeyEvent.KEYCODE_BACK)  {
                    if(mTask != null) {
                        mTask.cancel(true);
                    }
                    return false;
                }  else if (keyCode == KeyEvent.KEYCODE_SEARCH) {
                    return true;
                }
                return false;
            }
        });
    }

    public void show() {
        if(mProgressDialog.getContext() != null && !mProgressDialog.isShowing())
            mProgressDialog.show();
    }

    public void setTask(AsyncTask<?, ?, ?> task) {
        mTask = task;
    }

    public void setInfo(String title, String msg) {
        mProgressDialog.setTitle(title);
        mProgressDialog.setMessage(msg);
    }
    public void setInfoMsg(CharSequence msg) {
        mProgressDialog.setMessage(msg);
    }

    public void setCancelable(boolean flag){
        mProgressDialog.setCancelable(flag);
    }

    public void dismiss() {
        try {
            if (mProgressDialog != null && mProgressDialog.isShowing())
                mProgressDialog.dismiss();
            mProgressDialog = null;
        } catch (Exception ex) {
        }
    }

}
