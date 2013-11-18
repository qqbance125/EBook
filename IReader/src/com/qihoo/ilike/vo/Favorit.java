package com.qihoo.ilike.vo;

import com.qihoo360.reader.json.JsonUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Favorit {
    public User user;
    public int total;
    public Bookmark[] bookmarks;
    public int cache_expire;
    public int new_cnt;

    // [Local Only, Not From Json]
    public int svc_grab_index = -1;

    public Favorit builder(JSONObject json) throws JSONException {
        JSONObject obj = json;
        if (JsonUtils.isEmpty(json, "res")) {

            user = new User();
            user.builder(obj);
            JSONObject resJson = (JSONObject) obj.get("res");

            if (JsonUtils.isEmpty(resJson, "total")) {
                JSONObject totalJson = resJson.getJSONObject("total");
                if (JsonUtils.isEmpty(totalJson, "cnt")) {
                    this.total = totalJson.getInt("cnt");
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

            if (JsonUtils.isEmpty(resJson, "bookmarks")) {
                JSONArray bookmarksArray = resJson.getJSONArray("bookmarks");
                bookmarks = new Bookmark[bookmarksArray.length()];
                for (int i = 0; i < bookmarksArray.length(); i++) {
                    Bookmark bookmark = new Bookmark();

                    JSONObject bookmarkTop = bookmarksArray.getJSONObject(i);
                    if (JsonUtils.isEmpty(bookmarkTop, "bookmark")) {
                        bookmark = Bookmark.parse(bookmarkTop
                                .getJSONObject("bookmark"));

                        // 如果有正在提取URL的图片
                        if (bookmark.svc_grab) {
                            svc_grab_index = i;
                        }
                    }
                    bookmarks[i] = bookmark;
                }

            }
        }
        return this;
    }
}
