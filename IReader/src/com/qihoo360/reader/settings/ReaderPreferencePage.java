package com.qihoo360.reader.settings;

import com.qihoo360.reader.R;
import com.qihoo360.reader.Settings;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;

public class ReaderPreferencePage extends Activity {

    private ReaderCheckBoxPreference mUpdateInWifiPreference, mEnableOfflineDownLoadPreference;
    private ReaderListPreference mNewAddChannelPositionPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rd_preference_page);

        //3g自动更新
        mUpdateInWifiPreference = (ReaderCheckBoxPreference) findViewById(R.id.pref_update_only_wifi);
        mUpdateInWifiPreference.setTitle(R.string.rd_pref_auto_update_wifi);
        mUpdateInWifiPreference.setKey(Settings.SPKEY_ENABLE_AUTOUPDATE_KEY);
        mUpdateInWifiPreference.setOriginalChecked(Settings.getSharedPreferences().getBoolean(
                        Settings.SPKEY_ENABLE_AUTOUPDATE_KEY, true));

        /*//允许离线下载
        mEnableOfflineDownLoadPreference = (ReaderCheckBoxPreference) findViewById(R.id.pref_enable_offline_downoad);
        mEnableOfflineDownLoadPreference.setTitle(R.string.rd_setting_enable_offline);
        mEnableOfflineDownLoadPreference.setKey(Settings.SPKEY_ENABLE_OFFLINE_DOWNOAD);
        mEnableOfflineDownLoadPreference.setOriginalChecked(Settings.isEnableOfflineWithoutWifi());*/

        /*//仅文本离线下载
        mEnableOfflineDownLoadPreference = (ReaderCheckBoxPreference) findViewById(R.id.pref_offline_download_text);
        mEnableOfflineDownLoadPreference.setTitle(R.string.rd_offline_down_text);
        mEnableOfflineDownLoadPreference.setKey(Settings.SPKEY_OFFLINE_DONWLOAD_TEXT_ONLY_KEY);
        mEnableOfflineDownLoadPreference.setOriginalChecked(Settings.getSharedPreferences().getBoolean(
                        Settings.SPKEY_OFFLINE_DONWLOAD_TEXT_ONLY_KEY, false));*/

        //新添加频道位置
        mNewAddChannelPositionPreference = (ReaderListPreference) findViewById(R.id.pref_new_channel_position);
        mNewAddChannelPositionPreference.setTitle(R.string.rd_new_add_channel_position);
        mNewAddChannelPositionPreference.setKey(Settings.SPKEY_NEW_CHANNEL_ADD_POSTION);
        mNewAddChannelPositionPreference.setEntries(R.array.rd_pref_new_channel_position);
        mNewAddChannelPositionPreference.setValues(R.array.rd_pref_new_channel_position_values);
        mNewAddChannelPositionPreference.setSelectItem(Settings.getSharedPreferences().getString(
                        Settings.SPKEY_NEW_CHANNEL_ADD_POSTION, "after"));
    }
}
