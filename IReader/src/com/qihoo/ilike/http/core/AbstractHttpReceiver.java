package com.qihoo.ilike.http.core;

import org.apache.http.HttpEntity;

import java.io.IOException;

public abstract class AbstractHttpReceiver<T> {
	protected T response = null;

	public T getResponse() {
		return response;
	}

	public abstract void onReceive(HttpEntity entity) throws IOException;
}
