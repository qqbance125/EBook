package com.qihoo.ilike.vo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.qihoo360.reader.json.JsonUtils;

public class CollectList {
    public Collect[] collectList;
    public int total;

    public CollectList builder(JSONObject json) throws JSONException {

        JSONObject obj = json;
        if (JsonUtils.isEmpty(obj, "res")) {
            JSONObject resJson = (JSONObject) obj.get("res");

            if (JsonUtils.isEmpty(resJson, "total")) {
                JSONObject totalJson = resJson.getJSONObject("total");
                if (JsonUtils.isEmpty(totalJson, "cnt")) {
                    this.total = totalJson.getInt("cnt");
                }
            }

            if (JsonUtils.isEmpty(resJson, "feeds")) {
                JSONArray feedsJSONArray = (JSONArray) resJson
                        .getJSONArray("feeds");
                collectList = new Collect[feedsJSONArray.length()];
                for (int i = 0; i < feedsJSONArray.length(); i++) {
                    collectList[i] = Collect.parse(feedsJSONArray.getJSONObject(i));
                }
            }

        }
        return this;
    }
}
