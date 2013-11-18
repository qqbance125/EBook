package com.qihoo.ilike.vo;

import com.qihoo360.reader.json.JsonUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class Detail {
    public User user;

    public Volume volume;

    public DetailBookmark detailBookmark;

    public int has_pin = 0;

    public Detail builder(JSONObject json) throws JSONException {
        JSONObject obj = json;
        if (JsonUtils.isEmpty(obj, "res")) {
            user = new User();
            user.builder(json);
            JSONObject resJson = (JSONObject) obj.get("res");

            if (JsonUtils.isEmpty(resJson, "volume")) {
                volume = Volume.parse((JSONObject) resJson.get("volume"));
            }
            if (JsonUtils.isEmpty(resJson, "bookmark")) {
                detailBookmark = DetailBookmark.parse((JSONObject) resJson
                        .get("bookmark"));
            }

            if (JsonUtils.isEmpty(resJson, "has_pin")) {
                JSONObject totalJson = resJson.getJSONObject("has_pin");
                if (JsonUtils.isEmpty(totalJson, "cnt")) {
                    this.has_pin = totalJson.getInt("cnt");
                }
            }
        }
        return this;
    }

}
