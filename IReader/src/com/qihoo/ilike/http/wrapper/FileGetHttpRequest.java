package com.qihoo.ilike.http.wrapper;

import com.qihoo.ilike.http.core.HttpGetRequest;
import com.qihoo.ilike.http.core.IHttpRequest;
import com.qihoo.ilike.http.core.RequestParams;
import com.qihoo.ilike.http.support.FileHttpRequest;

public abstract class FileGetHttpRequest extends FileHttpRequest {
	private HttpGetRequest httpGetRequest;

	private RequestParams requestParams;

	public FileGetHttpRequest(RequestParams requestParams) {
		httpGetRequest = new HttpGetRequest();
		this.requestParams = requestParams;
		initialize();
	}

	private void initialize() {
		httpGetRequest.setUri(requestParams.getUri());
	}

	@Override
	public IHttpRequest getHttpService() {
		return httpGetRequest;
	}
}
