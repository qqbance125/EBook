package com.qihoo.ilike.vo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.qihoo360.reader.json.JsonUtils;
import com.qihoo360.reader.support.Utils;

import android.graphics.Bitmap;

public class Pic {
    public String url;
    public Bitmap bitmap;
    public int originalWidth; // 原始大小
    public int originalHeight;
    public int scaleWidth;
    public int scaleHeight;

    /**
     * 解析Json对象，并将其转换成Pic对象，方便使用
     *
     * @param picObj
     * @param index
     * @return
     * @throws JSONException
     * @author Jiongxuan Zhang
     */
    public static Pic[] parseList(JSONObject jSONArray) throws JSONException {
        Pic[] pics = new Pic[jSONArray.length()];
        JSONArray picJsonObject = jSONArray.getJSONArray("picture");
        for (int m = 0; m < picJsonObject.length(); m++) {
            Pic pic = new Pic();
            Utils.debug(Pic.class, picJsonObject.getJSONObject(m).toString());
            if (JsonUtils.isEmpty(picJsonObject.getJSONObject(m), "big")) {
                pic.url = (String) picJsonObject.getJSONObject(m).get("big");
            }
            if (JsonUtils.isEmpty(picJsonObject.getJSONObject(m), "big_size")) {
                String size = (String) picJsonObject.getJSONObject(m).get(
                        "big_size");
                Utils.debug("Pic.class", size);
                int width = Integer
                        .valueOf(size.substring(0, size.indexOf("x")));
                int height = Integer.valueOf(size.substring(
                        size.indexOf("x") + 1, size.length()));
                pic.originalWidth = width;
                pic.originalHeight = height;
            }
            pics[m] = pic;
        }

        return pics;
    }
}
