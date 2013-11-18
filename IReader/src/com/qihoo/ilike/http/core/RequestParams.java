package com.qihoo.ilike.http.core;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import android.util.Log;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestParams {
	private static final String TAG = "RequestParams";

	private static String ENCODING = "UTF-8";

	private String protocol = HttpConstants.PROTOCOL;

	private String host = HttpConstants.APP_URL;

	private String content = HttpConstants.CONTENT;

	private String url;

	private List<NameValuePair> queryString;
	
	private Map<String, String> cookie;
	
	private List<String> registerRespHeaders;

	public void add(String paramName, String paramValue) {
		if (queryString == null)
			queryString  = new ArrayList<NameValuePair>();
		queryString.add(new BasicNameValuePair(paramName, paramValue));
	}

	public List<NameValuePair> getRequestParams() {
		return queryString;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getHost() {
		return host;
	}

	public String getContent() {
		return content;
	}

	public String getProtocol() {
		return protocol;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public URI getUri() {
		URI uri = null;

		try {
			uri = (url == null ? URIUtils.createURI(protocol, getHost(), -1,
					getContent(), (queryString == null || queryString.size() == 0) ? null
							: URLEncodedUtils.format(queryString, ENCODING),
					null) : new URI(url));
			Log.d(TAG, "url = " + uri.toString());
		} catch (Exception e) {
			Log.e(TAG, "", e);
		}
		return uri;
	}
	
	public void setCookie(String key, String value)
	{
		if (cookie == null)
			cookie = new HashMap<String, String>();
		cookie.put(key, value);
	}
	
	public void registerResponseHeader(String name)
	{
		if (registerRespHeaders == null)
			registerRespHeaders = new ArrayList<String>();
		
		registerRespHeaders.add(name);
	}
	
	public Map<String, String> getCookie()
	{
		return cookie;
	}
	
	public List<String> getRegisterRespHeaders()
	{
		return registerRespHeaders;
	}
}
