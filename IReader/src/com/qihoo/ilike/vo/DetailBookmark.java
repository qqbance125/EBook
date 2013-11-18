package com.qihoo.ilike.vo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.qihoo360.reader.json.JsonUtils;

public class DetailBookmark {
    public String id;
    public String title;
    public int like_num;
    public Note[] notes;
    public String create_time;
    public int whoview;

    /**
     * 解析Json对象，并将其转换成DetailBookmark对象，方便使用
     *
     * @param detailBookmarkObject
     * @return
     * @throws JSONException
     * @author Jiongxuan Zhang
     */
    public static DetailBookmark parse(JSONObject detailBookmarkObject)
            throws JSONException {
        DetailBookmark detailBookmark = new DetailBookmark();
        if (JsonUtils.isEmpty(detailBookmarkObject, "id")) {
            detailBookmark.id = detailBookmarkObject.getString("id");
        }
        if (JsonUtils.isEmpty(detailBookmarkObject, "title")) {
            detailBookmark.title = detailBookmarkObject.getString("title");
        }
        if (JsonUtils.isEmpty(detailBookmarkObject, "like_num")) {
            detailBookmark.like_num = detailBookmarkObject.getInt("like_num");
        }
        if (JsonUtils.isEmpty(detailBookmarkObject, "whoview")) {
            detailBookmark.whoview = detailBookmarkObject.getInt("whoview");
        }
        if (JsonUtils.isEmpty(detailBookmarkObject, "create_time")) {
            detailBookmark.create_time = detailBookmarkObject
                    .getString("create_time");
        }

        if (JsonUtils.isEmpty(detailBookmarkObject, "notes")) {
            JSONArray notes = detailBookmarkObject.optJSONArray("notes");
            if (notes != null) {
                detailBookmark.notes = Note.parseList(notes);
            }
        }

        return detailBookmark;
    }
}
