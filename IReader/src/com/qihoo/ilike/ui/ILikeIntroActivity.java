package com.qihoo.ilike.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.qihoo.ilike.util.Utils;
import com.qihoo360.reader.R;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.ui.ActivityBase;
import com.qihoo360.reader.ui.ReaderPlugin;

public class ILikeIntroActivity extends ActivityBase {
    public static final String BROADCAST_ILIKE_INTRO_ACTIVITY_FINISHED = "broadcast_ilike_intro_activity_finished";
    private String mPendingCollectionUrl = null;
    private String mPendingCollectiontitle = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        disableHipOpenPage();
        super.onCreate(savedInstanceState);

        mPendingCollectionUrl = getIntent().getStringExtra(
                Utils.INTENT_EXTRA_DATA_URL);
        mPendingCollectiontitle = getIntent().getStringExtra(
                Utils.INTENT_EXTRA_DATA_TITLE);

        setContentView(R.layout.ilike_intro_activity);

        Button btnReturn = (Button) findViewById(R.id.ilike_intro_btn_return);
        btnReturn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        Button btnEnter = (Button) findViewById(R.id.ilike_intro_btn_enter);
        btnEnter.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getIntent().getBooleanExtra(Utils.INTENT_EXTRA_LAUNCH_ILIKE_MAIN_PAGE, false)) {
                    ReaderPlugin.enterILikeMainPage(ILikeIntroActivity.this);
                } else if (Utils.checkLoginStatus(ILikeIntroActivity.this,
                        mPendingCollectionUrl, mPendingCollectiontitle)) {
                    doSendBroadcast(false);
                }

                handleEnterPressed();
            }
        });
    }

    @Override
    public void onBackPressed() {
        doSendBroadcast(true);

        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    public void onUpdateNightMode() {
        super.onUpdateNightMode();

        setBackground(R.id.ilike_intro_container);
    }

    private void doSendBroadcast(boolean canclled) {
        Intent intent = new Intent(BROADCAST_ILIKE_INTRO_ACTIVITY_FINISHED);
        intent.putExtra("cancelled", canclled);
        if (!canclled) {
            if (!TextUtils.isEmpty(mPendingCollectionUrl)) {
                intent.putExtra(Utils.INTENT_EXTRA_DATA_URL,
                        mPendingCollectionUrl);
            }
            if (!TextUtils.isEmpty(mPendingCollectiontitle)) {
                intent.putExtra(Utils.INTENT_EXTRA_DATA_TITLE,
                        mPendingCollectiontitle);
            }
        }
        sendBroadcast(intent);
    }

    private void handleEnterPressed() {
        SharedPreferences sp = Settings.getSharedPreferences();
        Editor editor = sp.edit();
        editor.putBoolean(Utils.PREF_ILIKE_INTRO_SCREEN_SHOWN, true);
        editor.commit();

        finish();
    }
}
