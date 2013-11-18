package com.qihoo.ilike.http.core;

import com.qihoo.ilike.http.core.IHttpRequest.HttpRequestStatus;

import org.apache.http.HttpStatus;

public class HttpRequestException extends Exception {
	private static final long serialVersionUID = 7698630310529292336L;

	private HttpRequestStatus errorStatus;

	private int responseStatusCode;

	public HttpRequestException(HttpRequestStatus errorStatus) {
		this(errorStatus, HttpStatus.SC_OK);
	}

	public HttpRequestException(HttpRequestStatus errorStatus,
			int responseStatusCode) {
		super(errorStatus.getMessage());
		this.errorStatus = errorStatus;
		this.responseStatusCode = responseStatusCode;
	}

	public HttpRequestStatus getErrorStatus() {
		return errorStatus;
	}

	public int getResponseStatusCode() {
		return responseStatusCode;
	}
}
