package com.qihoo.ilike.http.core;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;

import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class HttpPostRequest implements IHttpRequest {
    private static final String TAG = "HttpPostRequest";

    private int connTimeout = 1000 * 30;

    private int soTimeout = 1000 * 30;

    private String encoding = ENCODING;

    private URI uri;
    /**
     * 请求的内容
     */
    private String httpBody;
    /**
     * 获取请求响应的内容
     */
    private HttpEntity httpEntity;
    
    private AbstractHttpReceiver<?> receiver;
    /**
     * 请求的消息头
     */
    private Map<String, String> httpHeaders;
    /**
     * 客户端
     */
    private HttpClient mHttpClient;

    public void execute() throws IOException, HttpRequestException {
        try {
            mHttpClient = new DefaultHttpClient();

            mHttpClient.getParams().setParameter(
                    CoreConnectionPNames.CONNECTION_TIMEOUT, connTimeout);
            mHttpClient.getParams().setParameter(
                    CoreConnectionPNames.SO_TIMEOUT, soTimeout);

            HttpPost request = new HttpPost(uri);

            setHttpHeaders(request);

            try {
                setHttpPostEntity(request);

                HttpResponse response = mHttpClient.execute(request);

                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode == HttpStatus.SC_OK) {
                    try {
                        // 把数据发送到相应类型的广播
                        receiver.onReceive(response.getEntity());
                    } catch (IOException e) {
                        Log.e(TAG, "", e);
                        throw new HttpRequestException(
                                HttpRequestStatus.DecodeException);
                    }
                } else {
                    throw new HttpRequestException(
                            HttpRequestStatus.ResponseException, statusCode);
                }
            } catch (ClientProtocolException e) {
                throw new HttpRequestException(
                        HttpRequestStatus.ProtocolException);
            } catch (UnsupportedEncodingException e) {
                throw new HttpRequestException(
                        HttpRequestStatus.EncodeException);
            }
        } finally {
            stop();
        }
    }

    private void setHttpHeaders(HttpPost request) {
        if (httpHeaders == null)
            return;

        Set<Entry<String, String>> entrySet = httpHeaders.entrySet();
        for (Entry<String, String> entry : entrySet) {
            request.addHeader(entry.getKey(), entry.getValue());
        }
    }
    /**
     * 设置请求的实体
     */
    private void setHttpPostEntity(HttpPost request)
            throws UnsupportedEncodingException {
        if (httpEntity != null) {
            request.setEntity(httpEntity);
        } else if (httpBody != null && !httpBody.equals("")) {
            request.setEntity(new StringEntity(httpBody, encoding));
        }
    }

    public void addHttpHeader(String name, String value) {
        if (value == null)
            return;

        if (httpHeaders == null)
            httpHeaders = new HashMap<String, String>();

        httpHeaders.put(name, value);
    }

    public void clearHttpHeader() {
        if (httpHeaders != null)
            httpHeaders.clear();
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public void setHttpEntity(HttpEntity httpEntity) {
        this.httpEntity = httpEntity;
    }

    public void setHttpBody(String httpBody) {
        this.httpBody = httpBody;
    }

    public void setConnTimeout(int connTimeout) {
        this.connTimeout = connTimeout;
    }

    public void setSoTimeout(int soTimeout) {
        this.soTimeout = soTimeout;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    @Override
    public void setReceiver(AbstractHttpReceiver<?> receiver) {
        this.receiver = receiver;
    }

    @Override
    public String getEncoding() {
        return encoding;
    }

    @Override
    public void stop() {
        if (mHttpClient != null) {
            mHttpClient.getConnectionManager().shutdown();
            mHttpClient = null;
        }
    }
}
