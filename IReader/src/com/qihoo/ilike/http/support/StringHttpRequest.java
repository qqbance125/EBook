package com.qihoo.ilike.http.support;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;

import com.qihoo.ilike.http.core.AbstractHttpReceiver;
import com.qihoo.ilike.http.core.AbstractHttpRequest;
/**
 * 处理字符串的接受广播
 * 
 */
public abstract class StringHttpRequest extends AbstractHttpRequest<String> {
	@Override
	public AbstractHttpReceiver<String> getHttpResponseReceiver() {
		return new AbstractHttpReceiver<String>() {
			@Override
			public void onReceive(HttpEntity entity) throws IOException {
				response = EntityUtils.toString(entity, getHttpService()
						.getEncoding());
			}
		};
	}
}
