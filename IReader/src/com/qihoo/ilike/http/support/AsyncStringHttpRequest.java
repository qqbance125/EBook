package com.qihoo.ilike.http.support;

import com.qihoo.ilike.http.core.AbstractAsyncHttpRequest;
import com.qihoo.ilike.http.core.AbstractHttpRequest;
import com.qihoo.ilike.http.core.IHttpRequest;

public abstract class AsyncStringHttpRequest extends
		AbstractAsyncHttpRequest<String> {
	@Override
	public AbstractHttpRequest<String> getHttpRequest() {
		return new StringHttpRequest() {
			@Override
			public IHttpRequest getHttpService() {
				return AsyncStringHttpRequest.this.getHttpService();
			}
		};
	}
}
