package com.qihoo.ilike.vo;

import com.qihoo360.reader.json.JsonUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CategoryList {
    public Category[] categoryList;
    public String ver;

    public CategoryList builder(JSONObject json) throws JSONException {
        JSONObject obj = json;

        if (JsonUtils.isEmpty(obj, "res")) {
            JSONArray resJSONArray = (JSONArray) obj.getJSONArray("res");
            categoryList = new Category[resJSONArray.length()];
            for (int i = 0; i < resJSONArray.length(); i++) {
                categoryList[i] = Category.parse(resJSONArray.getJSONObject(i));
            }
        }

        if (JsonUtils.isEmpty(obj, "ver")) {
            ver = obj.getString("ver");
        }
        return this;
    }
}
