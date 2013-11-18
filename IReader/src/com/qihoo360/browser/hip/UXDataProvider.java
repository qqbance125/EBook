package com.qihoo360.browser.hip;

import com.qihoo360.browser.hip.UXDataTables.Record;
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

import java.util.HashMap;
import java.util.List;
/**
 * 统计
 * @author lidy
 *
 */
public class UXDataProvider extends ContentProvider{
    public static final String DB_NAME = "statistic.db";
    public static final int DB_VERSION = 1;    
    
    /** The database that lies underneath this content provider */
    private SQLiteOpenHelper mOpenHelper = null;
    /** URI matcher used to recognize URIs sent by applications */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private static HashMap<String, String> sRecordProjectionMap;
    private static final int RECORD = 1;
    private static final int RECORD_ID = 2;    
    static {
        sUriMatcher.addURI(UXDataTables.AUTHORITY, "record", RECORD);
        sUriMatcher.addURI(UXDataTables.AUTHORITY, "record/#", RECORD_ID);
        
        sRecordProjectionMap = new HashMap<String, String>();
        sRecordProjectionMap.put(Record.ACTION_NAME, Record.ACTION_NAME);
        sRecordProjectionMap.put(Record.PERFORMEDTIMES, Record.PERFORMEDTIMES);
        sRecordProjectionMap.put(Record.DATE, Record.DATE);
    }
    
    private class DataBaseHelper extends SQLiteOpenHelper {

        public DataBaseHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + UXDataTables.Record.TABLE_NAME
                    + " ("
                    + Record._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + Record.ACTION_NAME +" TEXT,"
                    + Record.PERFORMEDTIMES + " INTEGER,"
                    + Record.DATE + " TEXT"
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {            
            // TODO add code for db update in future
        }
        
    }
    
    class ParamStruct {
        String tableName = null;
        ContentValues values = null;
        Uri content_uri = null;
    }

    private ParamStruct getInsertParam(Uri uri, ContentValues initialValues) {
        ParamStruct param = new ParamStruct();

        int type = sUriMatcher.match(uri);
        if (type != RECORD) {
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
        if (type == RECORD) {
            tableName = Record.TABLE_NAME;
            content_uri = Record.CONTENT_URI;
        }

        param.tableName = tableName;
        param.values = values;
        param.content_uri = content_uri;

        return param;
    }
    
    private String getIdByUri(Uri uri) {
        String id = "";
        List<String> parts = uri.getPathSegments();
        if (parts != null && parts.size() > 0) {
            id = parts.get(parts.size()-1);
        }
        
        return id;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case RECORD:
                count = db.delete(Record.TABLE_NAME, selection, selectionArgs);
                break;
            case RECORD_ID:
                String record_id = getIdByUri(uri);
                count = db.delete(Record.TABLE_NAME, Record._ID + "=" +record_id
                        + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }        
        
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case RECORD:
                return Record.CONTENT_TYPE;
            case RECORD_ID:
                return Record.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        ParamStruct param = getInsertParam(uri, values);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert(param.tableName, null, param.values);

        if (rowId > 0) {
            Uri ret_uri = ContentUris.withAppendedId(param.content_uri, rowId);
            getContext().getContentResolver().notifyChange(ret_uri, null);
            return ret_uri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new DataBaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        int type = sUriMatcher.match(uri);
        if (type == RECORD || type == RECORD_ID) {
            qb.setTables(Record.TABLE_NAME);
        } else {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        
        switch (type) {
            case RECORD:
                qb.setProjectionMap(sRecordProjectionMap);
                break;
            case RECORD_ID:
                qb.setProjectionMap(sRecordProjectionMap);
                qb.appendWhere(Record._ID + "=" + getIdByUri(uri));
                break;
            default:
                break;
        }
        
        // Get the database and run the query
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, null);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = 0;
        switch (sUriMatcher.match(uri)) {
            case RECORD:
                count = db.update(Record.TABLE_NAME, values, selection, selectionArgs);
                break;
            case RECORD_ID:
                String record_id = getIdByUri(uri);
                count = db.update(Record.TABLE_NAME, values, Record._ID + "="
                        + record_id
                        + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        return count;
    }

}
