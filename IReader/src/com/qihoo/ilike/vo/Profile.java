package com.qihoo.ilike.vo;

import com.qihoo360.reader.json.JsonUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Profile {
    public User user;

    public Volume[] volumes;

    public int follow;

    public int consern;

    public Profile builder(JSONObject json) throws JSONException {
        JSONObject obj = json;
        if (JsonUtils.isEmpty(obj, "res")) {
            JSONObject resJson = obj.getJSONObject("res");

            if (JsonUtils.isEmpty(resJson, "follow")) {
                JSONObject followJson = resJson.getJSONObject("follow");
                follow = Integer.valueOf(followJson.getString("cnt"));
            }
            if (JsonUtils.isEmpty(resJson, "consern")) {
                JSONObject consernJson = resJson.getJSONObject("consern");
                consern = Integer.valueOf(consernJson.getString("cnt"));
            }

            User newUser = new User();
            user = newUser.builder(json);

            if (JsonUtils.isEmpty(resJson, "volumes")) {
                JSONArray volumeJSONArray = resJson.getJSONArray("volumes");
                volumes = new Volume[volumeJSONArray.length()];
                for (int i = 0; i < volumeJSONArray.length(); i++) {
                    JSONObject volumeObject = volumeJSONArray.getJSONObject(i);
                    volumes[i] = Volume.parse(volumeObject
                            .getJSONObject("volume"));
                }

            }

        }
        return this;
    }
}
