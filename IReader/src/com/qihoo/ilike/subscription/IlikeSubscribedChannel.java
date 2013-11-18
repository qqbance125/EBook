/**
 *
 */

package com.qihoo.ilike.subscription;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;

import com.qihoo.ilike.data.Tables;
import com.qihoo.ilike.data.Tables.Subscriptions;
import com.qihoo360.reader.subscription.Channel;
import com.qihoo360.reader.subscription.SubscribedChannel;
import com.qihoo360.reader.support.Utils;

/**
 * 表示在主页上显示的“已订阅频道”
 *
 * @author Jiongxuan Zhang
 */
public class IlikeSubscribedChannel extends SubscribedChannel {
    public int type = -1;

    @Override
    public void calculateNumberOfVisited(ContentResolver resolver) {

    }

    @Override
    public int unsubscribe(ContentResolver resolver) {
        return IlikeSubscribedChannel.deleteByChannel(resolver, channel);
    }

    @Override
    public void setLastContentId(ContentResolver resolver, long content_id) {

    }

    @Override
    public void setLastRefreshTime(ContentResolver resolver, long refresh_time) {

    }

    @Override
    public void setSortFloat(ContentResolver resolver, SortFloat sortFloat) {
    }

    @Override
    public double calculateSortFloat(ContentResolver resolver,
            SortFloat sortFloat) {
        return 0;
    }

    @Override
    public void updateNewestImageOfContentId(ContentResolver resolver,
            long content_id) {
    }

    @Override
    public void setOffline(ContentResolver resolver, boolean isChecked) {
    }

    @Override
    public void setOfflineTime(ContentResolver resolver, long offlineTime) {
    }

    @Override
    public void setOfflineCount(ContentResolver resolver, int count) {
    }

    static int deleteByChannel(ContentResolver resolver, String channel) {
        if (resolver.delete(Tables.Bookmarks.CONTENT_URI,
                Tables.Bookmarks.CATEGORY_SRV_ID + "=?",
                new String[] { channel }) > -1) {
            return resolver.delete(Tables.Subscriptions.CONTENT_URI,
                    Tables.Subscriptions.CATEGORY_SRV_ID + "=?",
                    new String[] { channel });
        }

        return -1;
    }

    boolean insertToDb(ContentResolver resolver) {
        if (resolver == null) {
            return false;
        }

        if (contains(resolver)) {
            return true;
        }

        ContentValues values = new ContentValues();
        values.put(Tables.Subscriptions.CATEGORY_SRV_ID, channel);
        values.put(Tables.Subscriptions.TITLE, title);
        values.put(Tables.Subscriptions.SUB_DATE, System.currentTimeMillis());
        values.put(Tables.Subscriptions.TYPE,
                Channel.TYPE_ILIKE_HOT_COLLECTION);
        try {
            resolver.insert(Subscriptions.CONTENT_URI, values);
        } catch (Exception e) {
            Utils.error(getClass(), Utils.getStackTrace(e));
            return false;
        }

        return true;
    }

    boolean contains(ContentResolver resolver) {
        Cursor cursor = null;
        try {
            cursor = resolver.query(Subscriptions.CONTENT_URI,
                    new String[] { Subscriptions._ID },
                    Tables.Subscriptions.CATEGORY_SRV_ID + "=?",
                    new String[] { channel }, null);

            if (cursor != null && cursor.moveToNext()) {
                cursor.close();
                return true;
            }
        } catch (Exception e) {
            Utils.error(getClass(), Utils.getStackTrace(e));
        }

        if (cursor != null) {
            cursor.close();
        }
        return false;
    }
}
