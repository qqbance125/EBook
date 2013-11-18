package com.qihoo.ilike.vo;

import org.json.JSONException;
import org.json.JSONObject;

import com.qihoo360.reader.json.JsonUtils;
import com.qihoo360.reader.support.Utils;

public class Response {
    public int errno;
    public String errmsg;

    public Response builder(JSONObject json) throws JSONException {
        JSONObject obj = json;

        if (JsonUtils.isEmpty(obj, "errno")) {
            try {
                errno = Integer.valueOf((String) obj.getString("errno"));
            } catch (ClassCastException e) {
                Utils.error(getClass(), Utils.getStackTrace(e));
            }
        }
        if (JsonUtils.isEmpty(obj, "errmsg")) {
            try {
                errmsg = (String) obj.get("errmsg");
            } catch (ClassCastException e) {
                Utils.error(getClass(), Utils.getStackTrace(e));
            }
        }

        return this;
    }
}
