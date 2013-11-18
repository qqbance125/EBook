package com.qihoo.ilike.vo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.qihoo360.reader.json.JsonUtils;

public class CategoryVo {
    public Collect[] collectList;

    public int total;

    public int offset; //截止

    public int cache_expire;// 缓存到期

    public int new_cnt;

    public CategoryVo builder(JSONObject json) throws JSONException {
        JSONObject obj = json;
        if (JsonUtils.isEmpty(obj, "res")) {
            JSONObject resJson = (JSONObject) obj.get("res");

            if (JsonUtils.isEmpty(resJson, "total")) {
                JSONObject totalJson = resJson.getJSONObject("total");
                if (JsonUtils.isEmpty(totalJson, "cnt")) {
                    this.total = totalJson.getInt("cnt");
                }
            }

            if (JsonUtils.isEmpty(resJson, "offset")) {
                JSONObject totalJson = resJson.getJSONObject("offset");
                if (JsonUtils.isEmpty(totalJson, "cnt")) {
                    this.offset = totalJson.getInt("cnt");
                }
            }

            if (JsonUtils.isEmpty(resJson, "new_cnt")) {
                JSONObject totalJson = resJson.getJSONObject("new_cnt");
                if (JsonUtils.isEmpty(totalJson, "cnt")) {
                    this.new_cnt = totalJson.getInt("cnt");
                }
            }

            if (JsonUtils.isEmpty(resJson, "cache_expire")) {
                JSONObject totalJson = resJson.getJSONObject("cache_expire");
                if (JsonUtils.isEmpty(totalJson, "cnt")) {
                    this.cache_expire = totalJson.getInt("cnt");
                }
            }

            if (JsonUtils.isEmpty(resJson, "category")) {
                JSONArray categoryJSONArray = resJson.getJSONArray("category");
                collectList = new Collect[categoryJSONArray.length()];
                for (int i = 0; i < categoryJSONArray.length(); i++) {
                    collectList[i] = Collect.parse(categoryJSONArray
                            .getJSONObject(i));
                }

            }

        }
        return this;
    }
}
