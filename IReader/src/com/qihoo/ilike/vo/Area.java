package com.qihoo.ilike.vo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.qihoo360.reader.json.JsonUtils;
/**
 *  区域 省 城市
 */
public class Area {
    public String[] provinces;
    public String[][] citys;

    public static Area builder(JSONObject json) throws JSONException {
        Area ret = new Area();
        JSONObject obj = json;
        if (JsonUtils.isEmpty(obj, "data")) {
            JSONObject dataJson = obj.optJSONObject("data");
            if (dataJson != null) {
                if (JsonUtils.isEmpty(dataJson, "p")) {
                    JSONArray provinceJSONArray = dataJson.optJSONArray("p");
                    if ((provinceJSONArray != null)
                            && (provinceJSONArray.length() > 0)) {
                        int counter = provinceJSONArray.length();
                        ret.provinces = new String[counter];
                        for (int i = 0; i < counter; i++) {
                            String p = (String) provinceJSONArray.opt(i);
                            if (p != null) {
                                ret.provinces[i] = p.toString();
                            } else {
                                ret.provinces[i] = "";
                            }
                        }
                    }
                }

                if (JsonUtils.isEmpty(dataJson, "c")) {
                    JSONArray cityJSONArray = dataJson.optJSONArray("c");
                    if ((cityJSONArray != null) && (cityJSONArray.length() > 0)) {
                        int counter = cityJSONArray.length();
                        ret.citys = new String[counter][];
                        for (int i = 0; i < counter; i++) {
                            String c = (String) cityJSONArray.opt(i);
                            if (c != null) {
                                ret.citys[i] = c.toString().split(",");
                            } else {
                                ret.citys[i] = new String[] { "" };
                            }
                        }
                    }
                }
            }
        }

        return ret;
    }
}
