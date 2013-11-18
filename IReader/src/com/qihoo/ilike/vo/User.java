package com.qihoo.ilike.vo;

import org.json.JSONException;
import org.json.JSONObject;

import com.qihoo360.reader.json.JsonUtils;

public class User {
    public String id;

    public Pic icon;

    public String name;

    public int articleNum;

    public int privateNum;

    public int whoview;

    public String bind;

    public User builder(JSONObject json) throws JSONException {
        JSONObject obj = json;
        if (JsonUtils.isEmpty(obj, "res")) {
            JSONObject resJson = (JSONObject) obj.get("res");

            if (JsonUtils.isEmpty(resJson, "user")) {
                JSONObject userJson = (JSONObject) resJson.get("user");
                if (JsonUtils.isEmpty(userJson, "qid")) {
                    this.id = userJson.getString("qid");
                }
                if (JsonUtils.isEmpty(userJson, "nickname")) {
                    this.name = userJson.getString("nickname");
                }
                if (JsonUtils.isEmpty(userJson, "article_num")) {
                    this.articleNum = userJson.getInt("article_num");
                }
                if (JsonUtils.isEmpty(userJson, "private_num")) {
                    this.privateNum = userJson.getInt("private_num");
                }
                if (JsonUtils.isEmpty(userJson, "whoview")) {
                    this.whoview = userJson.getInt("whoview");
                }
                if (JsonUtils.isEmpty(userJson, "imageid")) {
                    Pic pic = new Pic();
                    pic.url = userJson.getString("imageid");
                    this.icon = pic;
                }
                if (JsonUtils.isEmpty(userJson, "bind")) {
                    this.bind = userJson.getString("bind");
                }

            }

        }
        return this;
    }

    /**
     * 解析Json对象，并将其转换成User对象，方便使用
     *
     * @param userObj
     * @return
     * @throws JSONException
     * @author Jiongxuan Zhang
     */
    public static User parse(JSONObject userObj) throws JSONException {
        User user = new User();
        if (userObj.has("qid") && !userObj.isNull("qid")) {
            user.id = userObj.getString("qid");
        }
        if (userObj.has("nickname")) {
            user.name = userObj.getString("nickname");
        }
        if (userObj.has("imageid")) {
            Pic pic = new Pic();
            pic.url = userObj.getString("imageid");
            user.icon = pic;
        }

        return user;
    }
}
