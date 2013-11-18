package com.qihoo.ilike.vo;

import com.qihoo360.reader.json.JsonUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Note {
    public static final String TYPE_TEXT = "word";
    public static final String TYPE_PIC = "pic";

    public String type;
    public String src;
    public String h;
    public String w;

    /**
     * 解析Json对象，并将其转换成Note对象，方便使用
     *
     * @param noteArray
     * @param index
     * @return
     * @throws JSONException
     * @author Jiongxuan Zhang
     */
    public static Note[] parseList(JSONArray noteArray) throws JSONException {
        Note[] ns = new Note[noteArray.length()];
        for (int i = 0; i < noteArray.length(); i++) {
            Note note = new Note();
            JSONObject noteJson = noteArray.getJSONObject(i);
            if (JsonUtils.isEmpty(noteJson, "type")) {
                note.type = noteJson.getString("type");
            }
            if (JsonUtils.isEmpty(noteJson, "src")) {
                note.src = noteJson.getString("src");
            }
            if (JsonUtils.isEmpty(noteJson, "h")) {
                note.h = noteJson.getString("h");
            }
            if (JsonUtils.isEmpty(noteJson, "w")) {
                note.w = noteJson.getString("w");
            }
            ns[i] = note;
        }

        return ns;
    }
}
