package com.qihoo.ilike.http.support;

import com.qihoo.ilike.http.core.AbstractHttpReceiver;
import com.qihoo.ilike.http.core.AbstractHttpRequest;
import com.qihoo360.reader.support.NetUtils;

import org.apache.http.HttpEntity;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
/**
 * 处理JSON串的接受广播
 */
public abstract class JsonHttpRequest extends AbstractHttpRequest<JSONObject> {
    @Override
    public AbstractHttpReceiver<JSONObject> getHttpResponseReceiver() {
        return new AbstractHttpReceiver<JSONObject>() {
            @Override
            public void onReceive(HttpEntity entity) throws IOException {
                try {
                    response = (JSONObject) (new JSONTokener(
                            NetUtils.toString(entity)).nextValue());
                } catch (JSONException e) {
                    throw new IOException("json parse exception");
                }
            }
        };
    }
}
