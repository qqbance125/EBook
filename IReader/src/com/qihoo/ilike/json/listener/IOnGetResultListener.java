/**
 *
 */
package com.qihoo.ilike.json.listener;

import com.qihoo.ilike.http.core.IHttpRequest.HttpRequestStatus;
import com.qihoo.ilike.vo.ErrorInfo;

/**
 * @author zhangjiongxuan
 *
 */
public interface IOnGetResultListener {
    void onRequestFailure(HttpRequestStatus errorStatus);
    void onResponseError(ErrorInfo errorInfo);
}
