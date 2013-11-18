package com.qihoo.ilike.http.core;

import java.io.IOException;

public abstract class AbstractHttpRequest<T> {
    private IHttpRequest httpService;
    /**
     * 发送请求消息
     */
    public T request() throws IOException, HttpRequestException {
        httpService = getHttpService();

        AbstractHttpReceiver<T> receiver = getHttpResponseReceiver();

        httpService.setReceiver(receiver);

        httpService.execute();

        return receiver.getResponse();
    }

    public abstract IHttpRequest getHttpService();
    /**
     * 获得相应的请求消息
     */
    public abstract AbstractHttpReceiver<T> getHttpResponseReceiver();

    public void stop() {
        if (httpService != null) {
            httpService.stop();
            httpService = null;
        }
    }
}
