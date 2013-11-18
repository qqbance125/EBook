package com.qihoo.ilike.vo;

import com.qihoo360.reader.json.JsonUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class Volume {
    public String id;
    public String name;
    public String createTime;
    public int bookmarkCnt;
    public int privateCnt;
    public int fansCnt;
    public int isfollowed;

    /**
     * 解析Json对象，并将其转换成Volume对象，方便使用
     *
     * @param volumeObj
     * @return
     * @throws JSONException
     * @author Jiongxuan Zhang
     */
    public static Volume parse(JSONObject volumeObj) throws JSONException {
        Volume volume = new Volume();
        if (JsonUtils.isEmpty(volumeObj, "id")) {
            volume.id = volumeObj.getString("id");
        }
        if (JsonUtils.isEmpty(volumeObj, "name")) {
            volume.name = (String) volumeObj.get("name");
        }
        if (JsonUtils.isEmpty(volumeObj, "create_time")) {
            volume.createTime = volumeObj.getString("create_time");
        }
        if (JsonUtils.isEmpty(volumeObj, "bookmark_cnt")) {
            volume.bookmarkCnt = (Integer) volumeObj.get("bookmark_cnt");
        }
        if (JsonUtils.isEmpty(volumeObj, "private_cnt")) {
            Object private_cnt = volumeObj.get("private_cnt");
            if (private_cnt instanceof String) {
                volume.privateCnt = Integer.valueOf((String) private_cnt);
            } else {
                volume.privateCnt = (Integer) private_cnt;
            }
        }
        if (JsonUtils.isEmpty(volumeObj, "funs_cnt")) {
            Object fansCnt = volumeObj.get("funs_cnt");
            if (fansCnt instanceof String) {
                volume.fansCnt = Integer.valueOf((String) fansCnt);
            } else {
                volume.fansCnt = (Integer) fansCnt;
            }
        }
        if (JsonUtils.isEmpty(volumeObj, "isfollowed")) {
            volume.isfollowed = volumeObj.getInt("isfollowed");
        }

        return volume;
    }
}
