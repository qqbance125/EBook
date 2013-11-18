package com.qihoo360.reader.ui.channels;

import com.qihoo360.reader.ui.ActivityBase;
import com.qihoo360.reader.ui.ReaderPlugin;
import com.qihoo360.reader.ui.view.ReaderMainView;

import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Toast;

public class TestActivity extends ActivityBase {
    private ReaderMainView mReaderMainView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        disableHipOpenPage();
        super.onCreate(savedInstanceState);
        mReaderMainView = ReaderPlugin.getInstance().getReaderMain(this);
        setContentView(mReaderMainView);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mReaderMainView.onKeyUp(keyCode, event) == true) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "test destory", Toast.LENGTH_SHORT).show();
        ReaderPlugin.getInstance().onDestory();
    }
}
