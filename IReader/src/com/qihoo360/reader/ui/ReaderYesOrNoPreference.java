package com.qihoo360.reader.ui;

import com.qihoo360.reader.R;
import com.qihoo360.reader.subscription.reader.RssManager;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.preference.DialogPreference;
import android.util.AttributeSet;

public class ReaderYesOrNoPreference extends DialogPreference {

    public ReaderYesOrNoPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ReaderYesOrNoPreference(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    @Override
    protected void onClick() {
        getDialog().show();
    }

    private CustomDialog mDialog;

    @Override
    public Dialog getDialog() {
        if (mDialog == null) {
            mDialog = new CustomDialog(getContext());
            mDialog.setTitle(getTitle());
            mDialog.setMessage(getDialogMessage());
            mDialog.setPositiveButton(R.string.rd_dialog_ok, mListener);
            mDialog.setNegativeButton(R.string.rd_dialog_cancel, mListener);
        }
        return mDialog;
    }

    private OnClickListener mListener = new OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON1) {
                //TODO execute method
                RssManager.clearCache(getContext());
                mDialog.dismiss();
            } else if (which == DialogInterface.BUTTON2) {
                mDialog.dismiss();
            }
        }
    };

}
