package com.qihoo.ilike.http.wrapper;

import com.qihoo.ilike.http.core.HttpGetRequest;
import com.qihoo.ilike.http.core.IHttpRequest;
import com.qihoo.ilike.http.core.RequestParams;
import com.qihoo.ilike.http.support.AsyncJsonHttpRequest;

import java.util.Map;

public abstract class AsyncJsonGetHttpRequest extends AsyncJsonHttpRequest {
	private HttpGetRequest httpGetRequest;

	private RequestParams requestParams;

	public AsyncJsonGetHttpRequest(RequestParams requestParams) {
		httpGetRequest = new HttpGetRequest();
		this.requestParams = requestParams;
		initialize();
	}

	@Override
	public IHttpRequest getHttpService() {
		return httpGetRequest;
	}
	/**
	 *  初始化
	 */
	private void initialize() {
		httpGetRequest.setUri(requestParams.getUri());
		Map<String, String> cookie = requestParams.getCookie();
		if (cookie != null && cookie.size() > 0)
		{
			httpGetRequest.addHttpHeader("Cookie", HttpUtils.getCookie(cookie));
		}
	}
}
