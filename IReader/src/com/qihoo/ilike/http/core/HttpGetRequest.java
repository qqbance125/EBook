
package com.qihoo.ilike.http.core;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;

import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * 请求响应的类 get方式
 */
public class HttpGetRequest implements IHttpRequest {
    private static final String TAG = "HttpGetRequest";

    private int connTimeout = 1000 * 30;

    private int soTimeout = 1000 * 30;

    private String encoding = ENCODING;

    private URI uri;

    private Map<String, String> responseCookie;

    private Map<String, String> responseHeaders;

    private AbstractHttpReceiver<?> receiver;

    private Map<String, String> httpHeaders;

    private List<String> paramHeaders = null;

    public HttpGetRequest() {
    }

    public HttpGetRequest(List<String> paramHeaders) {
        this.paramHeaders = paramHeaders;
    }

    private HttpClient mHttpClient;

    public void execute() throws IOException, HttpRequestException {
        try {
            mHttpClient = new DefaultHttpClient();
            // 连接超过3秒失败
            mHttpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,
                    connTimeout);
            mHttpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, soTimeout);

            HttpGet request = new HttpGet(uri);
            request.addHeader("Accept-Encoding", "gzip, deflate");

            setHttpHeaders(request);

            try {
                HttpResponse response = mHttpClient.execute(request);

                setResponseHeaders(response);

                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == HttpStatus.SC_OK)
                {
                    setResponseCookie(mHttpClient);
                    try {
                        receiver.onReceive(response.getEntity());
                    } catch (IOException e) {
                        Log.e(TAG, "", e);
                        throw new HttpRequestException(HttpRequestStatus.DecodeException);
                    }
                } else {
                    throw new HttpRequestException(HttpRequestStatus.ResponseException, statusCode);
                }
            } catch (ClientProtocolException e) {
                throw new HttpRequestException(HttpRequestStatus.ProtocolException);
            }
        } finally {
            stop();
        }
    }

    private void setHttpHeaders(HttpGet request) {
        if (httpHeaders == null)
            return;
        Set<Entry<String, String>> entrySet = httpHeaders.entrySet();
        for (Entry<String, String> entry : entrySet) {
            request.addHeader(entry.getKey(), entry.getValue());
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
    public String getEncoding() {
        return encoding;
    }

    @Override
    public void setReceiver(AbstractHttpReceiver<?> receiver) {
        this.receiver = receiver;
    }

    /**
     * 响应的cookie
     */
    private void setResponseCookie(HttpClient httpClient) {
        CookieStore cookieStore = ((DefaultHttpClient) httpClient).getCookieStore();

        if (cookieStore == null)
            return;

        List<Cookie> cookies = cookieStore.getCookies();

        if (cookies != null) {
            if (responseCookie == null)
                responseCookie = new HashMap<String, String>();

            for (Cookie cookie : cookies) {
                responseCookie.put(cookie.getName(), cookie.getValue());
            }
        }
    }

    public Map<String, String> getResponseCookie() {
        return responseCookie;
    }

    /**
     * 设置响应的消息头
     */
    private void setResponseHeaders(HttpResponse response) {
        if (responseHeaders == null)
            responseHeaders = new HashMap<String, String>();

        if (paramHeaders != null) {
            for (String key : paramHeaders) {
                Header header = response.getFirstHeader(key);
                if (header != null)
                    responseHeaders.put(header.getName(), header.getValue());
            }
        } else {
            Header header = response.getFirstHeader("errno");
            if (header != null)
                responseHeaders.put(header.getName(), header.getValue());
        }
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public void registerHttpResponseHeader(String name) {
        if (paramHeaders == null)
            paramHeaders = new ArrayList<String>();

        paramHeaders.add(name);
    }

    @Override
    public void stop() {
        if (mHttpClient != null) {
            mHttpClient.getConnectionManager().shutdown();
            mHttpClient = null;
        }
    }

}
