package com.qihoo.ilike.vo;

import org.json.JSONException;
import org.json.JSONObject;

import com.qihoo360.reader.json.JsonUtils;

public class Category {
    public String id;
    public String name;

    /**
     * 解析Json对象，并将其转换成Category对象，方便使用
     *
     * @param categoryObj
     * @return
     * @throws JSONException
     * @author Jiongxuan Zhang
     */
    public static Category parse(JSONObject categoryObj) throws JSONException {
        Category category = new Category();
        if (JsonUtils.isEmpty(categoryObj, "category")) {
            JSONObject cate = categoryObj.getJSONObject("category");
            if (JsonUtils.isEmpty(cate, "id")) {
                category.id = cate.getString("id");
            }
            if (JsonUtils.isEmpty(cate, "name")) {
                category.name = cate.getString("name");
            }
        }

        return category;
    }
}
