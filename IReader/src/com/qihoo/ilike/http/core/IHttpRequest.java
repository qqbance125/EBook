
package com.qihoo.ilike.http.core;

import java.io.IOException;

/**
 * 请求的相应消息头
 * @see HttpPostRequest {@link HttpGetRequest} 的实现类
 */
public interface IHttpRequest {
    /**
     * 状态码：
     * 网络异常 ，收到响应数据异常，编码请求数据异常，request协议异常
     * 服务器响应异常 ，未知异常 ，用户取消 ，success
     */
    public enum HttpRequestStatus {
        RequestSuccess(null), NetworkException("network exception"), DecodeException(
                "receive response data exception"), EncodeException("encode request data exception"), ProtocolException(
                "request protocol exception"), ResponseException("server response exception"), UnknowException(
                "unknow exception"), UserCancelled("user cancelled");

        private String message;

        HttpRequestStatus(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public static final String ENCODING = "UTF-8";

    /**
     * 执行方法
     */
    void execute() throws IOException, HttpRequestException;

    /**
     * 广播
     */
    void setReceiver(AbstractHttpReceiver<?> receiver);

    /**
     * 获得编码
     */
    String getEncoding();

    /**
     * 设置超时的时间
     */
    void setSoTimeout(int soTimeout);

    void stop();
}
