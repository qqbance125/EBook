package com.qihoo360.browser.hip;

import com.qihoo360.reader.BuildEnv;

import android.net.Uri;
import android.provider.BaseColumns;

public class UXDataTables {
    public static final String AUTHORITY = BuildEnv.AUTHORITY_UXHELPER;
    
    public static final class Record implements BaseColumns{
        private Record(){}
        public static final String TABLE_NAME = "subscriptions";

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri
                .parse("content://" + AUTHORITY + "/record");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         * 数据的URL
         * 数据集的MIME类型字符串
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.qihoo.android.common.uxhelper.record";      

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         * 单一数据的MIME 类型字符串
         * 数据的内容
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.qihoo.android.common.uxhelper.record";       
        
        /**
         * 动作名称
         */
        public static final String ACTION_NAME = "actionname";
        /**
         * 执行时间
         */
        public static final String PERFORMEDTIMES = "performedtimes";
        /**
         * 日期
         */
        public static final String DATE = "date";        
    }
}
