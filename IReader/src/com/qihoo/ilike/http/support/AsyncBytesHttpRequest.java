package com.qihoo.ilike.http.support;

import com.qihoo.ilike.http.core.AbstractAsyncHttpRequest;
import com.qihoo.ilike.http.core.AbstractHttpRequest;
import com.qihoo.ilike.http.core.IHttpRequest;
/**
 * 处理字节的异步task
 */
public abstract class AsyncBytesHttpRequest extends
		AbstractAsyncHttpRequest<byte[]> {
	@Override
	public AbstractHttpRequest<byte[]> getHttpRequest() {
		return new BytesHttpRequest() {
			@Override
			public IHttpRequest getHttpService() {
				return AsyncBytesHttpRequest.this.getHttpService();
			}
		};
	}
}
