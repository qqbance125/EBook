package com.qihoo360.browser.hip;

import com.qihoo360.reader.Constants;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.subscription.reader.RssSubscribedChannel;
import com.qihoo360.reader.support.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.List;

/**
 * 
 *
 */
public final class CollectSubScribe {
    private static CollectSubScribe mCellector;
    private static final String TAG = "CollectSubScribe";
    public static final String READER_USED_DAILY_TIME = "READER_USED_DAILY_TIME";
    public static final String DAILY_READER_USER_COUNT = "DAILY_READER_USER_COUNT";
    public static final String DAILY_SHAKE_USER_COUNT = "DAILY_SHAKE_USER_COUNT";
    public static final String DAILY_CLASSIFY_USER_COUNT = "DAILY_CLASSIFY_USER_COUNT";
    public static final String DAILY_COLLECTION_USER_COUNT = "DAILY_COLLECTION_USER_COUNT";

    private CollectSubScribe() {
    }

    public static CollectSubScribe getInstance() {
        if (mCellector == null)
            mCellector = new CollectSubScribe();
        return mCellector;
    }

    public void collecting(Context context) {
        UXHelper ux = UXHelper.getInstance();

        List<String> list = RssSubscribedChannel.getChannelsForNameOnly(context.getContentResolver());

        if (list == null || list.size() == 0) {
            Utils.debug("Collector", "No SubscribedChannel!");
            return;
        }

        for (String channel : list) {
            try {
                String str = channel.replace(" ", "").replace("&", "").replace("^", "").replace("%", "")
                                .replace("_", "").replace("-", "").replace("@", "").replace("#", "").replace("!", "");
                if (TextUtils.isEmpty(str))
                    return;

                ux.setActionRecord("3" + str, 1);
                /**
                 * 4 前缀表示，订阅频道阅读的时长
                 */
                ux.setActionRecord("4" + str, getTimeByName(channel));
            } catch (Exception e) {
                continue;
            }
        }

        if (Settings.isFullScreen() == false) {
            ux.addActionRecord(UXHelperConfig.Reader_Article_List_ControlBar_Opening, 1);
        } else {
            ux.addActionRecord(UXHelperConfig.Reader_Article_List_ControlBar_Closing, 1);
        }

        if (Settings.getReaderViewMode() == Constants.MODE_GRID) {
            ux.addActionRecord(UXHelperConfig.Reader_Classify_Grid_Style_State, 1);
        } else {
            ux.addActionRecord(UXHelperConfig.Reader_Classify_List_Style_State, 1);
        }

        ux.setActionRecord("4" + READER_USED_DAILY_TIME, getStore(READER_USED_DAILY_TIME));
        updateStoreKey(READER_USED_DAILY_TIME, 0);

        /**
         * 5 表示独立用户数（UV）
         */
        ux.setActionRecord("5" + DAILY_READER_USER_COUNT, getStore(DAILY_READER_USER_COUNT));
        updateStoreKey(DAILY_READER_USER_COUNT, 0);

        ux.setActionRecord("5" + DAILY_SHAKE_USER_COUNT, getStore(DAILY_SHAKE_USER_COUNT));
        updateStoreKey(DAILY_SHAKE_USER_COUNT, 0);

        ux.setActionRecord("5" + DAILY_CLASSIFY_USER_COUNT, getStore(DAILY_CLASSIFY_USER_COUNT));
        updateStoreKey(DAILY_CLASSIFY_USER_COUNT, 0);

        ux.setActionRecord("5" + DAILY_COLLECTION_USER_COUNT, getStore(DAILY_COLLECTION_USER_COUNT));
        updateStoreKey(DAILY_COLLECTION_USER_COUNT, 0);

    }

    public void updateSubScribeKey(String channelName) {
        if (TextUtils.isEmpty(channelName))
            return;

        String key = channelName.toUpperCase();
        SharedPreferences sp = Settings.getSharedPreferences();
        if (!sp.contains(key)) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putInt(key, 0);
            editor.commit();
        }
    }

    public void clearSubScribeKey(String channelName) {
        if (TextUtils.isEmpty(channelName)) {
            try {
                throw new Exception("A SubScribe will to be delete,but given channel name is empty!");
            } catch (Exception e) {
                Utils.error(getClass(), Utils.getStackTrace(e));
            }
        } else {
            String key = channelName.toUpperCase();
            SharedPreferences sp = Settings.getSharedPreferences();
            if (!sp.contains(key)) {
                Utils.debug(TAG, Utils.getStackTrace(new Throwable(
                                "A SubScribe will to be delete,but the key not exist in Setting ShareReferences")));
            } else {
                SharedPreferences.Editor editor = sp.edit();
                editor.remove(key);
                editor.commit();
            }
        }
    }

    public void updateSubScribeValue(String key, int value) {
        if (TextUtils.isEmpty(key)) {
            Utils.debug(TAG, "The Key you given is empty!");
            return;
        } else {
            key = key.toUpperCase();
            SharedPreferences sp = Settings.getSharedPreferences();
            if (sp.contains(key)) {
                SharedPreferences.Editor editor = sp.edit();
                int preValue = sp.getInt(key, 0);
                editor.putInt(key, value + preValue);
                editor.commit();
            } else {
                updateSubScribeKey(key);
                updateSubScribeValue(key, value);
            }
        }
    }

    public void updateStoreKey(String key, int value) {
        SharedPreferences sp = Settings.getSharedPreferences();
        SharedPreferences.Editor editor = sp.edit();
        if (DAILY_READER_USER_COUNT.equals(key) || DAILY_CLASSIFY_USER_COUNT.equals(key)
                        || DAILY_COLLECTION_USER_COUNT.equals(key) || DAILY_SHAKE_USER_COUNT.equals(key)) {
            editor.putInt(key, 1);
            editor.commit();
        } else {
            int preValue = sp.getInt(key, 0);
            editor.putInt(key, preValue + value);
            editor.commit();
        }
    }

    public int getStore(String key) {
        SharedPreferences sp = Settings.getSharedPreferences();
        if (sp.contains(key)) {
            return sp.getInt(key, 0);
        }
        return 0;
    }

    public void initializeSubScribeKey(Context context) {
        SharedPreferences sp = Settings.getSharedPreferences();
        if (!sp.getBoolean("CollectSubScribe", false)) {
            List<String> list = RssSubscribedChannel.getChannelsForNameOnly(context.getContentResolver());

            if (list == null || list.size() == 0) {
                Utils.debug("Collector", "No SubscribedChannel!");
                return;
            }

            for (String channel : list) {
                updateSubScribeKey(channel);
            }
        }
    }

    public int getTimeByName(String channelName) {
        if (TextUtils.isEmpty(channelName))
            return 0;

        SharedPreferences sp = Settings.getSharedPreferences();
        String key = channelName.toUpperCase();
        if (sp.contains(key)) {
            return sp.getInt(key, 0);
        }
        return 0;
    }
}
