package com.qihoo.ilike.http.wrapper;

import java.util.List;

import com.qihoo.ilike.http.core.HttpGetRequest;
import com.qihoo.ilike.http.core.IHttpRequest;
import com.qihoo.ilike.http.core.RequestParams;
import com.qihoo.ilike.http.support.AsyncBytesHttpRequest;

public abstract class AsyncBytesGetHttpRequest extends AsyncBytesHttpRequest {

	private HttpGetRequest httpGetRequest;

	private RequestParams requestParams;

	public AsyncBytesGetHttpRequest(RequestParams requestParams) {
		httpGetRequest = new HttpGetRequest();
		this.requestParams = requestParams;
		initialize();
	}

	@Override
	public IHttpRequest getHttpService() {
		return httpGetRequest;
	}

	private void initialize() {
		httpGetRequest.setUri(requestParams.getUri());
		List<String> responseHeaders = requestParams.getRegisterRespHeaders();
		if (responseHeaders != null)
			for (String name : responseHeaders)
				httpGetRequest.registerHttpResponseHeader(name);
	}
}
