package com.qihoo360.browser.hip;

import com.qihoo360.browser.hip.UXDataTables.Record;
import com.qihoo360.reader.support.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;

public class UXHelper {
    public static final String TAG = "UXHelper";
    public static final String USEHIPCOLLECTION = "USEHIPCOLLECTION";
    private static UXHelper mInstance = new UXHelper();
    private Context mContext = null;

    private Hashtable<String, Integer> mActionDict = new Hashtable<String, Integer>();

    // TODO need to change it to false (maybe)
    private boolean mUseHipCollection = true;

    public static UXHelper getInstance() {
        return mInstance;
    }

    public void init(Context context) {
        mContext = context;
        SharedPreferences sharedPrefer = mContext.getSharedPreferences(UXHelper.TAG, 0);
        mUseHipCollection = sharedPrefer.getBoolean(UXHelper.USEHIPCOLLECTION, true);
    }

    public void setUseHipCollection(boolean useHipCollection) {
        mUseHipCollection = useHipCollection;
        SharedPreferences.Editor editor = mContext.getSharedPreferences(UXHelper.TAG, 0).edit();
        editor.putBoolean(UXHelper.USEHIPCOLLECTION, mUseHipCollection).commit();
    }

    public boolean getUseHipCollection() {
        return mUseHipCollection;
    }

    /**
     * To Add a action record. Example: UXHelper.getInstance().addActionRecord(UXHelperConfig.Updater_CheckUpdate_TryToCheckUpdate,1);
     * @param actionName If a user want add a record with a action name, action name must be defined in UXHelperConfig, rule is:int Module_ChildModuleOrControl_Action = 1020101
     * @param actionPerformedTimes record performed times, if one control clicked once, you can do  addActionRecord(UXHelper.ActionName, 1);
     */

    public void addActionRecord(UXKey key, int actionPerformedTimes) {
        addActionRecord(key.getKey(), actionPerformedTimes);
    }

    /**
     * 增加记录
     */
    public void addActionRecord(String actionName, int actionPerformedTimes) {
        if (mActionDict.containsKey(actionName)) {
            int newAtionPerformedTimes = mActionDict.get(actionName).intValue() + actionPerformedTimes;
            mActionDict.put(actionName, newAtionPerformedTimes);
            Utils.debug(TAG, "addActionRecord: " + actionName + ":" + newAtionPerformedTimes);
        } else {
            mActionDict.put(actionName, new Integer(actionPerformedTimes));
            Utils.debug(TAG, "addActionRecord: " + actionName + ":" + actionPerformedTimes);
        }
    }

    /**
     * To set a action record. Example: UXHelper.getInstance().addActionRecord(UXHelperConfig.Updater_CheckUpdate_TryToCheckUpdate,1);
     * @param actionName If a user want add a record with a action name, action name must be defined in UXHelperConfig, rule is:int Module_ChildModuleOrControl_Action = 1020101
     * @param actionPerformedTimes record performed times, if you want to set a action settings, you can do  setActionRecord(UXHelper.ActionName, 0); or 1
     */
    public void setActionRecord(UXKey key, int actionPerformedTimes) {
        setActionRecord(key.getKey(), actionPerformedTimes);
    }

    /**
     * To set a action record. Example: UXHelper.getInstance().addActionRecord(UXHelperConfig.Updater_CheckUpdate_TryToCheckUpdate,1);
     * @param actionName If a user want add a record with a action name, action name must be defined in UXHelperConfig, rule is:int Module_ChildModuleOrControl_Action = 1020101
     * @param actionPerformedTimes record performed times, if you want to set a action settings, you can do  setActionRecord(UXHelper.ActionName, 0); or 1
     */
    public void setActionRecord(String actionName, int actionPerformedTimes) {
        mActionDict.put(actionName, actionPerformedTimes);
    }
    public int getActionRecord(UXKey key) {
        String[] projection = {Record.ACTION_NAME, Record.PERFORMEDTIMES, Record.DATE};
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(Record.CONTENT_URI,
                                                         projection,
                                                         Record.ACTION_NAME + "=?",
                                                         new String[]{key.getKey()},
                                                         null);
            int index = cursor.getColumnIndexOrThrow(Record.PERFORMEDTIMES);
            int total = 0;
            while(cursor.moveToNext()) {
                total += cursor.getInt(index);
            }
            return total;
        } catch (Exception e) {
            Utils.error(getClass(), Utils.getStackTrace(e));
        } finally {
            if (null != cursor)
                cursor.close();
        }
        return 0;
    }
    public void cleanActionRecord(UXKey key) {
        try {
            mContext.getContentResolver().delete(Record.CONTENT_URI, Record.ACTION_NAME + "=?", new String[]{key.getKey()});
        } catch (Exception e) {
            Utils.error(getClass(), Utils.getStackTrace(e));
        }
    }

    public void cleanRecords(Context context, UXKey[] excepts) {
        if (null == excepts || excepts.length == 0) {
            context.getContentResolver().delete(Record.CONTENT_URI, null, null);
        } else {
            // Need to reserve some UX record
            String keys = "";
            for (int i=0;i < excepts.length;i++) {
                keys += "'" + excepts[i].getKey() + "'";
                if((i != excepts.length-1)){
                    keys +=",";
                }
            }
            int cnt = context.getContentResolver().delete(Record.CONTENT_URI, Record.ACTION_NAME + " not in ("  + keys + ")",null);
            Utils.error(getClass(), "cleanRecords:"+cnt);
        }
    }


    public Hashtable<String, Hashtable<String, Integer>> getRecords(Context context) {
        Hashtable<String, Hashtable<String, Integer>> actionDictGroupByDateToReturen = new Hashtable<String, Hashtable<String, Integer>>();

        try {
            String[] projection = {Record.ACTION_NAME, Record.PERFORMEDTIMES, Record.DATE};

            Cursor cursor = context.getContentResolver().query(Record.CONTENT_URI, projection, null, null, null);
            final int acnInt = 0;
            final int prtInt = 1;
            final int datInt = 2;

            if (cursor == null) {
                return actionDictGroupByDateToReturen;
            }

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                String acnTemp = cursor.getString(acnInt);
                int prtTemp = cursor.getInt(prtInt);
                String dateTemp = cursor.getString(datInt);
                Date date = null;
                if (dateTemp != null && !dateTemp.equals("")) {
                    try {
                        date = new Date(Long.parseLong(dateTemp));
                    } catch (Exception e) {
                        Utils.error(getClass(), Utils.getStackTrace(e));
                        date = new Date();
                    }

                } else {
                    date = new Date();
                }
                String monthday = Integer.toString(10000 + (date.getMonth() + 1) * 100 + date.getDate())
                                .substring(1);

                if (!actionDictGroupByDateToReturen.containsKey(monthday)) {
                    actionDictGroupByDateToReturen.put(monthday, new Hashtable<String, Integer>());

                }

                if (actionDictGroupByDateToReturen.get(monthday).containsKey(acnTemp)) {
                    int newAtionPerformedTimes = actionDictGroupByDateToReturen.get(monthday).get(acnTemp)
                                    .intValue()
                                    + prtTemp;
                    actionDictGroupByDateToReturen.get(monthday).put(acnTemp,
                                    newAtionPerformedTimes);
                    Utils.debug(TAG, "getActionRecord: " + acnTemp + ":" + newAtionPerformedTimes);
                } else {
                    actionDictGroupByDateToReturen.get(monthday)
                                    .put(acnTemp, new Integer(prtTemp));
                    Utils.debug(TAG, "getActionRecord: " + acnTemp + ":" + prtTemp);
                }
            }

            cursor.close();
        } catch (Exception e) {
            Utils.error(getClass(), Utils.getStackTrace(e));
        }

        return actionDictGroupByDateToReturen;
    }

    public void asyncSaveRecords(final Context context) {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... arg0) {
                saveRecords(context);
                return null;
            }
        }.execute();
    }

    public synchronized void saveRecords(Context context) {
        @SuppressWarnings("unchecked")
        Hashtable<String, Integer> actionDictTempHashtable = (Hashtable<String, Integer>) mActionDict.clone();
        mActionDict.clear();
        ContentValues values = new ContentValues();
        Enumeration<String> enumeration = actionDictTempHashtable.keys();
        while (enumeration.hasMoreElements()) {
            String actionNameStr = enumeration.nextElement();
            values.clear();
            values.put(Record.ACTION_NAME, actionNameStr);
            values.put(Record.PERFORMEDTIMES, actionDictTempHashtable.get(actionNameStr));
            values.put(Record.DATE, Long.toString(System.currentTimeMillis()));
            try {
                context.getContentResolver().insert(Record.CONTENT_URI, values);
            } catch (Exception e) {
                Utils.error(getClass(), Utils.getStackTrace(e));
            }
        }
    }
}
