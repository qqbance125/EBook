package com.qihoo.ilike.vo;

import org.json.JSONException;
import org.json.JSONObject;

import com.qihoo360.reader.json.JsonUtils;
import com.qihoo360.reader.support.Utils;

public class Bookmark {
    public String id;
    public String title;
    public Pic[] pics;
    public boolean svc_grab = false;

    /**
     * 解析Json对象，并将其转换成Volume对象，方便使用
     *
     * @param bookmarkObj
     * @return
     * @throws JSONException
     * @author Jiongxuan Zhang
     */
    public static Bookmark parse(JSONObject bookmarkObj) throws JSONException {
        Bookmark bookmark = new Bookmark();

        if (JsonUtils.isEmpty(bookmarkObj, "id")) {
            bookmark.id = bookmarkObj.getString("id");
        }
        if (JsonUtils.isEmpty(bookmarkObj, "title")) {
            bookmark.title = (String) bookmarkObj.get("title");
        }
        if (JsonUtils.isEmpty(bookmarkObj, "bookmark_res")) {
            try {
                bookmark.pics = Pic.parseList(bookmarkObj
                        .getJSONObject("bookmark_res"));
            } catch (JSONException e) {
                Utils.error(Bookmark.class, Utils.getStackTrace(e));
            }
        }

        if (JsonUtils.isEmpty(bookmarkObj, "svc_grab")) {
            bookmark.svc_grab = bookmarkObj.getInt("svc_grab") == 0;
        }

        return bookmark;
    }
}
