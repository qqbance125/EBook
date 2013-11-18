package com.qihoo.ilike.http.support;

import org.json.JSONObject;

import com.qihoo.ilike.http.core.AbstractAsyncHttpRequest;
import com.qihoo.ilike.http.core.AbstractHttpRequest;
import com.qihoo.ilike.http.core.IHttpRequest;

public abstract class AsyncJsonHttpRequest extends
        AbstractAsyncHttpRequest<JSONObject> {

    private AbstractHttpRequest<JSONObject> mAbstractHttpRequest;

    @Override
    public AbstractHttpRequest<JSONObject> getHttpRequest() {
        if (mAbstractHttpRequest == null) {
            mAbstractHttpRequest = new JsonHttpRequest() {
                @Override
                public IHttpRequest getHttpService() {
                    return AsyncJsonHttpRequest.this.getHttpService();
                }
            };
        }

        return mAbstractHttpRequest;
    }
}
