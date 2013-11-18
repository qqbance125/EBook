package com.qihoo360.reader.ui;

import com.qihoo360.reader.R;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;
/**
 *复制文字 
 * 
 */
public class SelectTextActivity extends ActivityBase {
    public static final String TAG = "SelectTextActivity";

    public static final String INTENT_PARAM_TEXT_CONTENT = "text_content";
    public void onCreate(Bundle savedInstanceState) {
        disableHipOpenPage();
        super.onCreate(savedInstanceState);

        String content = getIntent().getStringExtra(INTENT_PARAM_TEXT_CONTENT);
        if(content != null) {
            content = content.replace("<img>", "");
        }

        if(TextUtils.isEmpty(content)) {
            Toast.makeText(this, "没有文字以供选择！", Toast.LENGTH_SHORT).show();
            finish();
        }

        if(Build.MODEL.equalsIgnoreCase("X1")) {
            // 优美手机
            setContentView(R.layout.rd_select_text_editable);
        } else {
            setContentView(R.layout.rd_select_text);
        }
        final EditText tv_content = (EditText) findViewById(R.id.text_content);
        tv_content.setText(content);
        tv_content.setSelection(0, content.length() - 1);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                tv_content.onTextContextMenuItem(android.R.id.selectAll);
            }
        }, 500);


        findViewById(R.id.btn_copy).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View paramView) {
                String selectedText = tv_content.getEditableText().subSequence(tv_content.getSelectionStart(), tv_content.getSelectionEnd()).toString();
                if(TextUtils.isEmpty(selectedText)) {
                    Toast.makeText(SelectTextActivity.this, "你还没选择文字哦", Toast.LENGTH_SHORT).show();
                } else {
                    ClipboardManager cp = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    cp.setText(selectedText);

                    Toast.makeText(SelectTextActivity.this, "文本已复制到剪切板", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });

        findViewById(R.id.btn_cancel).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View paramView) {
                finish();
            }
        });
    }
}
