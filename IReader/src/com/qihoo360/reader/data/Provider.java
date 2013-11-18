
package com.qihoo360.reader.data;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.qihoo360.reader.data.Tables.Articles;
import com.qihoo360.reader.data.Tables.ChannelAccess;
import com.qihoo360.reader.data.Tables.Subscriptions;
import com.qihoo360.reader.support.Utils;

public class Provider extends ContentProvider {

    public static final String DATABASE_NAME = "qihoo_reader.db";
    public static final int DATABASE_VERSION = 8;

    private static HashMap<String, String> sSubscriptionsProjectionMap;
    private static HashMap<String, String> sArticlesProjectionMap;
    private static HashMap<String, String> sChannelAccessProjectionMap;

    private static final int SUBSCRIPTIONS = 1;
    private static final int SUBSCRIPTIONS_ID = 2;
    private static final int ARTICLES = 3;
    private static final int ARTICLES_ID = 4;
    private static final int CHANNEL_ACCESS = 5;
    private static final int CHANNEL_ACCESS_ID = 6;

    private static final UriMatcher sUriMatcher;

    /**
     * This class helps open, create, and upgrade the database file.
     */
    public static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + Subscriptions.TABLE_NAME + " ("
                    + Subscriptions._ID + " INTEGER PRIMARY KEY,"
                    + Subscriptions.CHANNEL + " TEXT,"
                    + Subscriptions.TITLE + " TEXT,"
                    + Subscriptions.PHOTO_URL + " TEXT,"
                    + Subscriptions.SUB_DATE + " INTEGER,"
                    + Subscriptions.NUMBER_OF_VISITED + " INTEGER,"
                    + Subscriptions.NEWEST_IMAGE_CONTENT_ID + " INTEGER,"
                    + Subscriptions.IMAGE_VERSION + " INTEGER,"
                    + Subscriptions.LAST_CONTENT_ID + " INTEGER,"
                    + Subscriptions.LAST_REFRESH_TIME + " INTEGER,"
                    + Subscriptions.SORT_FLOAT + " REAL,"
                    + Subscriptions.OFFLINE + " INTEGER,"
                    + Subscriptions.OFFLINE_TIME + " INTEGER,"
                    + Subscriptions.OFFLINE_COUNT + " INTEGER"
                    + ");");

            db.execSQL("CREATE TABLE IF NOT EXISTS " + ChannelAccess.TABLE_NAME + " ("
                    + ChannelAccess._ID + " INTEGER PRIMARY KEY,"
                    + ChannelAccess.CHANNEL + " TEXT,"
                    + ChannelAccess.DAILY_COUNT + " INTEGER DEFAULT 0,"
                    + ChannelAccess.DATE + " INTEGER DEFAULT 0"
                    + ");");

            db.execSQL("CREATE TABLE IF NOT EXISTS " + Articles.TABLE_NAME + " ("
                    + Articles._ID + " INTEGER PRIMARY KEY,"
                    + Articles.CHANNEL + " TEXT,"
                    + Articles.CONTENT_ID + " INTEGER,"
                    + Articles.TITLE + " TEXT,"
                    + Articles.DESCRIPTION + " TEXT,"
                    + Articles.COMPRESSED_IMAGE_URL + " TEXT,"
                    + Articles.PUB_DATE + " TEXT,"
                    + Articles.LINK + " TEXT,"
                    + Articles.AUTHOR + " TEXT,"
                    + Articles.READ + " INTEGER,"
                    + Articles.STAR + " INTEGER,"
                    + Articles.STARDATE + " INTEGER,"
                    + Articles.ISDOWNLOADED + " INTEGER DEFAULT 0,"
                    + Articles.ISOFFLINED + " INTEGER DEFAULT 0"
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 2) {
                //   添加字段
                execSQLSafely(db, "ALTER TABLE subscriptions ADD last_content_id INTEGER DEFAULT 0");
            }

            if (oldVersion < 3) {
                execSQLSafely(db, "ALTER TABLE subscriptions ADD last_refresh_time INTEGER DEFAULT 0");
            }

            if (oldVersion < 4) {
                execSQLSafely(db, "ALTER TABLE articles ADD abstract_content TEXT");
                execSQLSafely(db, "ALTER TABLE articles ADD isdownloaded INTEGER DEFAULT 1"); // 从旧版升级，都是完整的文章了
            }

            if (oldVersion < 5) {
                execSQLSafely(db, "ALTER TABLE subscriptions ADD sort_float REAL");
            }

            if (oldVersion < 6) {
                execSQLSafely(db, "ALTER TABLE subscriptions ADD offline INTEGER");  // 默认全部开启离线下载功能
            }

            if (oldVersion < 8) {
                execSQLSafely(db, "ALTER TABLE subscriptions ADD offline_time INTEGER");
                execSQLSafely(db, "ALTER TABLE subscriptions ADD offline_count INTEGER");
                execSQLSafely(db, "ALTER TABLE articles ADD isofflined INTEGER");
            }
        }
    }

    private DatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        int type = sUriMatcher.match(uri);
        if (type == SUBSCRIPTIONS
                || type == SUBSCRIPTIONS_ID) {
            qb.setTables(Subscriptions.TABLE_NAME);
        } else if (type == ARTICLES
                || type == ARTICLES_ID) {
            qb.setTables(Articles.TABLE_NAME);
        } else if (type == CHANNEL_ACCESS
                || type == CHANNEL_ACCESS_ID) {
            qb.setTables(ChannelAccess.TABLE_NAME);
        } else {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        switch (type) {
            case SUBSCRIPTIONS:
                qb.setProjectionMap(sSubscriptionsProjectionMap);
                break;

            case SUBSCRIPTIONS_ID:
                qb.setProjectionMap(sSubscriptionsProjectionMap);
                qb.appendWhere(Subscriptions._ID + "=" + uri.getPathSegments().get(1));
                break;

            case ARTICLES:
                qb.setProjectionMap(sArticlesProjectionMap);
                break;

            case ARTICLES_ID:
                qb.setProjectionMap(sArticlesProjectionMap);
                qb.appendWhere(Articles._ID + "=" + uri.getPathSegments().get(1));
                break;

            case CHANNEL_ACCESS:
                qb.setProjectionMap(sChannelAccessProjectionMap);
                break;

            case CHANNEL_ACCESS_ID:
                qb.setProjectionMap(sChannelAccessProjectionMap);
                qb.appendWhere(ChannelAccess._ID + "=" + uri.getPathSegments().get(1));
                break;

            default:
        }

        // If no sort order is specified use the default
        String orderBy = "";
        if (TextUtils.isEmpty(sortOrder)) {
            if (type == SUBSCRIPTIONS) {
                orderBy = Subscriptions.DEFAULT_SORT_ORDER;
            } else if (type == ARTICLES) {
                orderBy = Articles.DEFAULT_SORT_ORDER;
            } else if (type == CHANNEL_ACCESS) {
                orderBy = ChannelAccess.DEFAULT_SORT_ORDER;
            }
        } else {
            orderBy = sortOrder;
        }

        // Get the database and run the query
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

        // Tell the cursor what uri to watch, so it knows when its source data
        // changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case SUBSCRIPTIONS:
                return Subscriptions.CONTENT_TYPE;

            case SUBSCRIPTIONS_ID:
                return Subscriptions.CONTENT_ITEM_TYPE;

            case ARTICLES:
                return Articles.CONTENT_TYPE;

            case ARTICLES_ID:
                return Articles.CONTENT_ITEM_TYPE;

            case CHANNEL_ACCESS:
                return ChannelAccess.CONTENT_TYPE;

            case CHANNEL_ACCESS_ID:
                return ChannelAccess.CONTENT_ITEM_TYPE;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        ParamStruct param = getInsertParam(uri, initialValues);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert(param.tableName, null, param.values);

        if (rowId > 0) {
            Uri ret_uri = ContentUris.withAppendedId(param.content_uri, rowId);
            getContext().getContentResolver().notifyChange(ret_uri, null);
            return ret_uri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    class ParamStruct {
        String tableName = null;
        ContentValues values = null;
        Uri content_uri = null;
    }

    private ParamStruct getInsertParam(Uri uri, ContentValues initialValues) {
        ParamStruct param = new ParamStruct();

        int type = sUriMatcher.match(uri);
        if (type != SUBSCRIPTIONS
                && type != ARTICLES
                && type != CHANNEL_ACCESS) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        String tableName = "";
        Uri content_uri = null;
        if (type == SUBSCRIPTIONS) {
            tableName = Subscriptions.TABLE_NAME;
            content_uri = Subscriptions.CONTENT_URI;
        } else if (type == ARTICLES) {
            tableName = Articles.TABLE_NAME;
            content_uri = Articles.CONTENT_URI;
        } else if (type == CHANNEL_ACCESS) {
            tableName = ChannelAccess.TABLE_NAME;
            content_uri = ChannelAccess.CONTENT_URI;
        }

        param.tableName = tableName;
        param.values = values;
        param.content_uri = content_uri;

        return param;
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case SUBSCRIPTIONS:
                count = db.delete(Subscriptions.TABLE_NAME, where, whereArgs);
                break;

            case SUBSCRIPTIONS_ID:
                String subscription_id = uri.getPathSegments().get(1);
                count = db.delete(Subscriptions.TABLE_NAME, Subscriptions._ID + "="
                        + subscription_id
                        + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
                break;
            case ARTICLES:
                count = db.delete(Articles.TABLE_NAME, where, whereArgs);
                break;

            case ARTICLES_ID:
                String article_id = uri.getPathSegments().get(1);
                count = db.delete(Articles.TABLE_NAME, Articles._ID + "=" + article_id
                        + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
                break;

            case CHANNEL_ACCESS:
                count = db.delete(ChannelAccess.TABLE_NAME, where, whereArgs);
                break;

            case CHANNEL_ACCESS_ID:
                String channel_access_id = uri.getPathSegments().get(1);
                count = db.delete(ChannelAccess.TABLE_NAME, ChannelAccess._ID + "=" + channel_access_id
                        + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case SUBSCRIPTIONS:
                count = db.update(Subscriptions.TABLE_NAME, values, where, whereArgs);
                break;

            case SUBSCRIPTIONS_ID:
                String subscription_id = uri.getPathSegments().get(1);
                count = db.update(Subscriptions.TABLE_NAME, values, Subscriptions._ID + "="
                        + subscription_id
                        + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
                break;
            case ARTICLES:
                count = db.update(Articles.TABLE_NAME, values, where, whereArgs);
                break;

            case ARTICLES_ID:
                String article_id = uri.getPathSegments().get(1);
                count = db.update(Articles.TABLE_NAME, values, Articles._ID + "="
                        + article_id
                        + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);

            case CHANNEL_ACCESS:
                count = db.update(ChannelAccess.TABLE_NAME, values, where, whereArgs);
                break;

            case CHANNEL_ACCESS_ID:
                String channel_access_id = uri.getPathSegments().get(1);
                count = db.update(ChannelAccess.TABLE_NAME, values, ChannelAccess._ID + "="
                        + channel_access_id
                        + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    /*
     * @Override public int bulkInsert(Uri uri, ContentValues[] values) {
     * Utils.log(TAG,
     * "bulkInsert ------------------------ values.length:"+values.length);
     * if(values == null || values.length == 0)return 0; ParamStruct param =
     * getInsertParam(uri, values[0]); StringBuilder sqlsb = new
     * StringBuilder("insert into "+param.tableName+"("); Set entrySet =
     * values[0].valueSet(); Iterator entriesIter = entrySet.iterator(); boolean
     * needSeparator = false; while (entriesIter.hasNext()) { if (needSeparator)
     * { sqlsb.append(","); } needSeparator = true; Map.Entry entry =
     * (Map.Entry)entriesIter.next(); sqlsb.append((String)entry.getKey()); }
     * sqlsb.append(")"); String strheader = sqlsb.toString(); SQLiteDatabase db
     * = mOpenHelper.getWritableDatabase(); int MAX_compound = 32; int count =
     * 0; for (int i = 0; i < values.length; i++) { sqlsb.append(" SELECT ");
     * entrySet = values[i].valueSet(); entriesIter = entrySet.iterator();
     * needSeparator = false; while (entriesIter.hasNext()) { if (needSeparator)
     * { sqlsb.append(","); } needSeparator = true; Map.Entry entry =
     * (Map.Entry)entriesIter.next(); // 转义 String value =
     * String.valueOf(entry.getValue()); StringBuilder validsb = new
     * StringBuilder(); for(int m = 0; m < value.length(); m++) { char c =
     * value.charAt(m); if((c < 0x20 && c != '\n' && c != '\r' && c != '\t') ||
     * (c > 0xD7FF && c < 0xE000) || c > 0xFFFD) { }else{ if(c == '\'')
     * validsb.append("''"); else validsb.append(c); } } // Utils.log(TAG,
     * "bulkInsert ----------------- value:"
     * +value+" validsb:"+validsb.toString());
     * sqlsb.append('\'').append(validsb.toString()).append('\''); } if(++count
     * == MAX_compound || i == values.length-1){ count = 0; Utils.log(TAG,
     * "bulkInsert ----------------- execSQL i:"+i); execSQLSafely(db,
     * sqlsb.toString()); sqlsb.setLength(0); sqlsb.append(strheader); }else{
     * sqlsb.append(" UNION ALL"); } } execSQLSafely(db,
     * "insert into message (body, phone_number) select 'this is good', '10086'"
     * ); getContext().getContentResolver().notifyChange(param.content_uri,
     * null); Utils.log(TAG,
     * "bulkInsert ----------------- notifyChange:"+param.content_uri); return
     * values.length; }
     */

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(Tables.AUTHORITY, "subscriptions", SUBSCRIPTIONS);
        sUriMatcher.addURI(Tables.AUTHORITY, "subscriptions/#", SUBSCRIPTIONS_ID);
        sUriMatcher.addURI(Tables.AUTHORITY, "articles", ARTICLES);
        sUriMatcher.addURI(Tables.AUTHORITY, "articles/#", ARTICLES_ID);
        sUriMatcher.addURI(Tables.AUTHORITY, "channel_access", CHANNEL_ACCESS);
        sUriMatcher.addURI(Tables.AUTHORITY, "channel_access/#", CHANNEL_ACCESS_ID);

        sSubscriptionsProjectionMap = new HashMap<String, String>();
        sSubscriptionsProjectionMap.put(Subscriptions._ID, Subscriptions._ID);
        sSubscriptionsProjectionMap.put(Subscriptions.CHANNEL, Subscriptions.CHANNEL);
        sSubscriptionsProjectionMap.put(Subscriptions.TITLE, Subscriptions.TITLE);
        sSubscriptionsProjectionMap.put(Subscriptions.PHOTO_URL, Subscriptions.PHOTO_URL);
        sSubscriptionsProjectionMap.put(Subscriptions.SUB_DATE, Subscriptions.SUB_DATE);
        sSubscriptionsProjectionMap.put(Subscriptions.NUMBER_OF_VISITED, Subscriptions.NUMBER_OF_VISITED);
        sSubscriptionsProjectionMap.put(Subscriptions.NEWEST_IMAGE_CONTENT_ID, Subscriptions.NEWEST_IMAGE_CONTENT_ID);
        sSubscriptionsProjectionMap.put(Subscriptions.IMAGE_VERSION, Subscriptions.IMAGE_VERSION);
        sSubscriptionsProjectionMap.put(Subscriptions.LAST_CONTENT_ID, Subscriptions.LAST_CONTENT_ID);
        sSubscriptionsProjectionMap.put(Subscriptions.LAST_REFRESH_TIME, Subscriptions.LAST_REFRESH_TIME);
        sSubscriptionsProjectionMap.put(Subscriptions.SORT_FLOAT, Subscriptions.SORT_FLOAT);
        sSubscriptionsProjectionMap.put(Subscriptions.OFFLINE, Subscriptions.OFFLINE);
        sSubscriptionsProjectionMap.put(Subscriptions.OFFLINE_TIME, Subscriptions.OFFLINE_TIME);
        sSubscriptionsProjectionMap.put(Subscriptions.OFFLINE_COUNT, Subscriptions.OFFLINE_COUNT);
        sSubscriptionsProjectionMap.put("count(_id)", "count(_id)");

        sChannelAccessProjectionMap = new HashMap<String, String>();
        sChannelAccessProjectionMap.put(ChannelAccess._ID, ChannelAccess._ID);
        sChannelAccessProjectionMap.put(ChannelAccess.CHANNEL, ChannelAccess.CHANNEL);
        sChannelAccessProjectionMap.put(ChannelAccess.DAILY_COUNT, ChannelAccess.DAILY_COUNT);
        sChannelAccessProjectionMap.put(ChannelAccess.DATE, ChannelAccess.DATE);


        sArticlesProjectionMap = new HashMap<String, String>();
        sArticlesProjectionMap.put(Articles._ID, Articles._ID);
        sArticlesProjectionMap.put(Articles.CHANNEL, Articles.CHANNEL);
        sArticlesProjectionMap.put(Articles.CONTENT_ID, Articles.CONTENT_ID);
        sArticlesProjectionMap.put(Articles.TITLE, Articles.TITLE);
        sArticlesProjectionMap.put(Articles.DESCRIPTION, Articles.DESCRIPTION);
        sArticlesProjectionMap.put(Articles.COMPRESSED_IMAGE_URL, Articles.COMPRESSED_IMAGE_URL);
        sArticlesProjectionMap.put(Articles.PUB_DATE, Articles.PUB_DATE);
        sArticlesProjectionMap.put(Articles.LINK, Articles.LINK);
        sArticlesProjectionMap.put(Articles.AUTHOR, Articles.AUTHOR);
        sArticlesProjectionMap.put(Articles.READ, Articles.READ);
        sArticlesProjectionMap.put(Articles.STAR, Articles.STAR);
        sArticlesProjectionMap.put(Articles.STARDATE, Articles.STARDATE);
        sArticlesProjectionMap.put(Articles.ISDOWNLOADED, Articles.ISDOWNLOADED);
        sArticlesProjectionMap.put(Articles.ISOFFLINED, Articles.ISOFFLINED);
    }

    public static void execSQLSafely(SQLiteDatabase db, String sql) {
        try {
            db.execSQL(sql);
        } catch (Exception e) {
            Utils.error(Provider.class, Utils.getStackTrace(e));
        }
    }
}
