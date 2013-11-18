/**
 *
 */
package com.qihoo360.reader.ui;

import com.qihoo360.reader.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

/**
 * 带进度的对话框
 *
 * @author Jiongxuan Zhang
 *
 */
public class CustomProgressDialog extends CustomDialog {

    private View mProcessView;

    /**
     * 初始化
     *
     * @param context
     */
    public CustomProgressDialog(Context context) {
        super(context);

        mProcessView = LayoutInflater.from(context).inflate(R.layout.rd_process_view, null);
        addContentView(mProcessView);
        showDilemiterLineAboveButton(false);
    }

    /**
     * 设置进度条的内容
     */
    public void setMessage(CharSequence message) {
        if (mMessage == null) {
            mMessage = (TextView)mProcessView.findViewById(R.id.message);
        }
        mMessage.setText(message);
    }
}
