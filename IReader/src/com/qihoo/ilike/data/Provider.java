
package com.qihoo.ilike.data;

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

import com.qihoo.ilike.data.Tables.Bookmarks;
import com.qihoo.ilike.data.Tables.LikedUrls;
import com.qihoo.ilike.data.Tables.Subscriptions;
import com.qihoo360.reader.support.Utils;

public class Provider extends ContentProvider {

    public static final String DATABASE_NAME = "qihoo_ilike.db";
    public static final int DATABASE_VERSION = 1;

    private static HashMap<String, String> sSubscriptionsProjectionMap;
    private static HashMap<String, String> sBookmarksProjectionMap;
    private static HashMap<String, String> sLikedUrlsProjectionMap;

    private static final int SUBSCRIPTIONS = 1;
    private static final int SUBSCRIPTIONS_ID = 2;
    private static final int BOOKMARKS = 3;
    private static final int BOOKMARKS_ID = 4;
    private static final int LIKEDURLS = 5;

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
                    + Subscriptions.CATEGORY_SRV_ID + " TEXT,"
                    + Subscriptions.TITLE + " TEXT,"
                    + Subscriptions.TYPE + " INTEGER,"
                    + Subscriptions.SUB_DATE + " INTEGER,"
                    + Subscriptions.NUMBER_OF_VISITED + " INTEGER,"
                    + Subscriptions.LAST_BOOKMARK_ID + " INTEGER,"
                    + Subscriptions.LAST_REFRESH_TIME + " INTEGER,"
                    + Subscriptions.SORT_FLOAT + " REAL"
                    + ");");

            db.execSQL("CREATE TABLE IF NOT EXISTS " + Bookmarks.TABLE_NAME + " ("
                    + Bookmarks._ID + " INTEGER PRIMARY KEY,"
                    + Bookmarks.CATEGORY_SRV_ID + " TEXT,"
                    + Bookmarks.BOOKMARK_ID + " INTEGER,"
                    + Bookmarks.TITLE + " TEXT,"
                    + Bookmarks.SNAPSHOT + " TEXT,"
                    + Bookmarks.DESCRIPTION + " TEXT,"
                    + Bookmarks.ALBUM_ID + " TEXT,"
                    + Bookmarks.ALBUM_TITLE + " TEXT,"
                    + Bookmarks.IMAGES + " TEXT,"
                    + Bookmarks.AUTHOR + " TEXT,"
                    + Bookmarks.AUTHOR_IMAGE_URL + " TEXT,"
                    + Bookmarks.AUTHOR_QID + " TEXT, "
                    + Bookmarks.I_LIKE + " INTEGER DEFAULT 0,"
                    + Bookmarks.ISDOWNLOADED + " INTEGER DEFAULT 0,"
                    + Bookmarks.LIKE_COUNT + " INTEGER DEFAULT 0,"
                    + Bookmarks.PUB_DATE + " TEXT,"
                    + Bookmarks.READ + " INTEGER,"
                    + Bookmarks.SORT + " INTEGER"
                    + ");");

            db.execSQL("CREATE TABLE IF NOT EXISTS " + LikedUrls.TABLE_NAME + " ("
                    + LikedUrls._ID + " INTEGER PRIMARY KEY,"
                    + LikedUrls.LIKED_URLS + " TEXT"
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
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
        } else if (type == BOOKMARKS
                || type == BOOKMARKS_ID) {
            qb.setTables(Bookmarks.TABLE_NAME);
        } else if (type == LIKEDURLS) {
            qb.setTables(LikedUrls.TABLE_NAME);
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

            case BOOKMARKS:
                qb.setProjectionMap(sBookmarksProjectionMap);
                break;

            case BOOKMARKS_ID:
                qb.setProjectionMap(sBookmarksProjectionMap);
                qb.appendWhere(Bookmarks._ID + "=" + uri.getPathSegments().get(1));
                break;

            case LIKEDURLS:
                qb.setProjectionMap(sLikedUrlsProjectionMap);
                break;

            default:
        }

        // If no sort order is specified use the default
        String orderBy = "";
        if (TextUtils.isEmpty(sortOrder)) {
            if (type == SUBSCRIPTIONS) {
                orderBy = Subscriptions.DEFAULT_SORT_ORDER;
            } else if (type == BOOKMARKS) {
                orderBy = Bookmarks.DEFAULT_SORT_ORDER;
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

            case BOOKMARKS:
                return Bookmarks.CONTENT_TYPE;

            case BOOKMARKS_ID:
                return Bookmarks.CONTENT_ITEM_TYPE;

            case LIKEDURLS:
                return LikedUrls.CONTENT_TYPE;

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
                && type != BOOKMARKS
                && type != LIKEDURLS) {
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
        } else if (type == BOOKMARKS) {
            tableName = Bookmarks.TABLE_NAME;
            content_uri = Bookmarks.CONTENT_URI;
        } else if (type == LIKEDURLS) {
            tableName = LikedUrls.TABLE_NAME;
            content_uri = LikedUrls.CONTENT_URI;
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
            case BOOKMARKS:
                count = db.delete(Bookmarks.TABLE_NAME, where, whereArgs);
                break;

            case BOOKMARKS_ID:
                String article_id = uri.getPathSegments().get(1);
                count = db.delete(Bookmarks.TABLE_NAME, Bookmarks._ID + "=" + article_id
                        + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
                break;

            case LIKEDURLS:
                count = db.delete(LikedUrls.TABLE_NAME, where, whereArgs);
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
            case BOOKMARKS:
                count = db.update(Bookmarks.TABLE_NAME, values, where, whereArgs);
                break;

            case BOOKMARKS_ID:
                String article_id = uri.getPathSegments().get(1);
                count = db.update(Bookmarks.TABLE_NAME, values, Bookmarks._ID + "="
                        + article_id
                        + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
                break;

            case LIKEDURLS:
                count = db.update(LikedUrls.TABLE_NAME, values, where, whereArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(Tables.AUTHORITY, "subscriptions", SUBSCRIPTIONS);
        sUriMatcher.addURI(Tables.AUTHORITY, "subscriptions/#", SUBSCRIPTIONS_ID);
        sUriMatcher.addURI(Tables.AUTHORITY, "bookmarks", BOOKMARKS);
        sUriMatcher.addURI(Tables.AUTHORITY, "bookmarks/#", BOOKMARKS_ID);
        sUriMatcher.addURI(Tables.AUTHORITY, "likedurls", LIKEDURLS);

        sSubscriptionsProjectionMap = new HashMap<String, String>();
        sSubscriptionsProjectionMap.put(Subscriptions._ID, Subscriptions._ID);
        sSubscriptionsProjectionMap.put(Subscriptions.CATEGORY_SRV_ID, Subscriptions.CATEGORY_SRV_ID);
        sSubscriptionsProjectionMap.put(Subscriptions.TITLE, Subscriptions.TITLE);
        sSubscriptionsProjectionMap.put(Subscriptions.TYPE, Subscriptions.TYPE);
        sSubscriptionsProjectionMap.put(Subscriptions.SUB_DATE, Subscriptions.SUB_DATE);
        sSubscriptionsProjectionMap.put(Subscriptions.NUMBER_OF_VISITED, Subscriptions.NUMBER_OF_VISITED);
        sSubscriptionsProjectionMap.put(Subscriptions.LAST_BOOKMARK_ID, Subscriptions.LAST_BOOKMARK_ID);
        sSubscriptionsProjectionMap.put(Subscriptions.LAST_REFRESH_TIME, Subscriptions.LAST_REFRESH_TIME);
        sSubscriptionsProjectionMap.put(Subscriptions.SORT_FLOAT, Subscriptions.SORT_FLOAT);
        sSubscriptionsProjectionMap.put("count(_id)", "count(_id)");

        sBookmarksProjectionMap = new HashMap<String, String>();
        sBookmarksProjectionMap.put(Bookmarks._ID, Bookmarks._ID);
        sBookmarksProjectionMap.put(Bookmarks.CATEGORY_SRV_ID, Bookmarks.CATEGORY_SRV_ID);
        sBookmarksProjectionMap.put(Bookmarks.BOOKMARK_ID, Bookmarks.BOOKMARK_ID);
        sBookmarksProjectionMap.put(Bookmarks.TITLE, Bookmarks.TITLE);
        sBookmarksProjectionMap.put(Bookmarks.SNAPSHOT, Bookmarks.SNAPSHOT);
        sBookmarksProjectionMap.put(Bookmarks.DESCRIPTION, Bookmarks.DESCRIPTION);
        sBookmarksProjectionMap.put(Bookmarks.ALBUM_ID, Bookmarks.ALBUM_ID);
        sBookmarksProjectionMap.put(Bookmarks.ALBUM_TITLE, Bookmarks.ALBUM_TITLE);
        sBookmarksProjectionMap.put(Bookmarks.IMAGES, Bookmarks.IMAGES);
        sBookmarksProjectionMap.put(Bookmarks.AUTHOR, Bookmarks.AUTHOR);
        sBookmarksProjectionMap.put(Bookmarks.AUTHOR_IMAGE_URL, Bookmarks.AUTHOR_IMAGE_URL);
        sBookmarksProjectionMap.put(Bookmarks.AUTHOR_QID, Bookmarks.AUTHOR_QID);
        sBookmarksProjectionMap.put(Bookmarks.I_LIKE, Bookmarks.I_LIKE);
        sBookmarksProjectionMap.put(Bookmarks.ISDOWNLOADED, Bookmarks.ISDOWNLOADED);
        sBookmarksProjectionMap.put(Bookmarks.LIKE_COUNT, Bookmarks.LIKE_COUNT);
        sBookmarksProjectionMap.put(Bookmarks.PUB_DATE, Bookmarks.PUB_DATE);
        sBookmarksProjectionMap.put(Bookmarks.READ, Bookmarks.READ);
        sBookmarksProjectionMap.put(Bookmarks.SORT, Bookmarks.SORT);

        sLikedUrlsProjectionMap = new HashMap<String, String>();
        sLikedUrlsProjectionMap.put(LikedUrls._ID, LikedUrls._ID);
        sLikedUrlsProjectionMap.put(LikedUrls.LIKED_URLS, LikedUrls.LIKED_URLS);
    }

    public static void execSQLSafely(SQLiteDatabase db, String sql) {
        try {
            db.execSQL(sql);
        } catch (Exception e) {
            Utils.error(Provider.class, Utils.getStackTrace(e));
        }
    }
}
