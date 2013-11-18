package com.qihoo.ilike.vo;

import com.qihoo360.reader.json.JsonUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class Collect {
    public User user;
    public Bookmark bookmark;

    /**
     * 解析Json对象，并将其转换成Collect对象，方便使用
     *
     * @param collectObj
     * @return
     * @throws JSONException
     * @author Jiongxuan Zhang
     */
    public static Collect parse(JSONObject collectObj) throws JSONException {
        Collect categoryO = new Collect();
        if (JsonUtils.isEmpty(collectObj, "user")) {
            JSONObject user0 = collectObj.getJSONObject("user");
            if (JsonUtils.isEmpty(user0, "user")) {
                categoryO.user = User.parse(user0.getJSONObject("user"));
            }
        }
        if (JsonUtils.isEmpty(collectObj, "bookmark")) {
            JSONObject bookmarkOJ1 = collectObj.getJSONObject("bookmark");
            if (bookmarkOJ1.has("bookmark")) {
                categoryO.bookmark = Bookmark.parse(bookmarkOJ1
                        .getJSONObject("bookmark"));
            }
        }

        return categoryO;
    }
}
