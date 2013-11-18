package com.qihoo.ilike.vo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.qihoo360.reader.json.JsonUtils;

public class Album {
    public User user;

    public Volume volume;

    public Bookmark[] bookmarks;

    public int total;

    public Album builder(JSONObject json) throws JSONException {
        JSONObject obj = json;
        if (JsonUtils.isEmpty(obj, "res")) {
            JSONObject resJson = (JSONObject) obj.get("res");

            if (JsonUtils.isEmpty(resJson, "total")) {
                JSONObject totalJson = resJson.getJSONObject("total");
                if (JsonUtils.isEmpty(totalJson, "cnt")) {
                    this.total = totalJson.getInt("cnt");
                }
            }

            if (JsonUtils.isEmpty(resJson, "user")) {
                JSONObject userOJ1 = resJson.getJSONObject("user");
                if (JsonUtils.isEmpty(userOJ1, "user")) {
                    user = User.parse(userOJ1.getJSONObject("user"));
                }
            }
            if (JsonUtils.isEmpty(resJson, "bookmarks")) {
                JSONArray bookmarkArray = resJson.getJSONArray("bookmarks");
                bookmarks = new Bookmark[bookmarkArray.length()];
                for (int i = 0; i < bookmarkArray.length(); i++) {
                    JSONObject bookmarkJson = bookmarkArray.getJSONObject(i);
                    if (JsonUtils.isEmpty(bookmarkJson, "bookmark")) {
                        bookmarks[i] = Bookmark.parse(bookmarkJson
                                .getJSONObject("bookmark"));
                    }
                }
            }
            if (JsonUtils.isEmpty(resJson, "volume")) {
                volume = Volume.parse(resJson.getJSONObject("volume"));
            }

        }
        return this;
    }
}
