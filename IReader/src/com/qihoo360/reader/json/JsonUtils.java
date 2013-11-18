/**
 *
 */
package com.qihoo360.reader.json;

import org.json.JSONObject;

import com.qihoo360.reader.support.Utils;

/**
 * 有关Json的访问类
 *
 * @author Jiongxuan Zhang
 *
 */
public class JsonUtils {

    public static String getJsonString(JSONObject jsonObject, String name) {
        try {
            if (jsonObject != null && jsonObject.has(name)) {
                return jsonObject.getString(name);
            }
        } catch (Exception e) {
            Utils.error(JsonUtils.class, Utils.getStackTrace(e));
        }
        return "";
    }

    public static int getJsonInt(JSONObject jsonObject, String name) {
        try {
            if (jsonObject != null && jsonObject.has(name)) {
                return jsonObject.getInt(name);
            }
        } catch (Exception e) {
            Utils.error(JsonUtils.class, Utils.getStackTrace(e));
        }
        return 0;
    }

    public static boolean isEmpty(JSONObject jsonObject, String name) {
        try {
            return jsonObject != null && !jsonObject.isNull(name)
                    && jsonObject.has(name);
        } catch (Exception e) {
            Utils.error(JsonUtils.class, Utils.getStackTrace(e));
        }
        return false;
    }

}
