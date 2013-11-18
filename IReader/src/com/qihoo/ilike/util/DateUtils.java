package com.qihoo.ilike.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {
    private static final String DATE_FORMAT = "yyyy-MM-dd hh:mm:ss";

    public static Date convStringToDate(String dateStr) throws ParseException {
        Date date = null;
        if (dateStr != null) {
            String strDate = null;
            strDate = dateStr;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                    DATE_FORMAT);

            date = simpleDateFormat.parse(strDate);
        }
        return date;
    }
}
