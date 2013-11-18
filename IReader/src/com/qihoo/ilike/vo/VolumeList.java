package com.qihoo.ilike.vo;

import com.qihoo360.reader.json.JsonUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class VolumeList {
    public Volume[] volumeList;

    public VolumeList builder(JSONObject json) throws JSONException {

        if (json != null) {
            if (JsonUtils.isEmpty(json, "res")) {
                JSONObject resJSONObject = json.getJSONObject("res");
                if (JsonUtils.isEmpty(resJSONObject, "volumes")) {
                    JSONArray volumesJSONArray = resJSONObject
                            .getJSONArray("volumes");
                    volumeList = new Volume[volumesJSONArray.length()];
                    for (int i = 0; i < volumesJSONArray.length(); i++) {
                        JSONObject volumeArrayJSONObject = volumesJSONArray
                                .getJSONObject(i);
                        Volume volume = new Volume();
                        if (JsonUtils.isEmpty(volumeArrayJSONObject, "volume")) {
                            volume = Volume.parse(volumeArrayJSONObject
                                    .getJSONObject("volume"));
                        }
                        volumeList[i] = volume;
                    }
                }
            }
        }

        return this;
    }
}
