package com.qihoo.ilike.http.core;

import com.qihoo.ilike.http.core.AbstractAsyncHttpRequest.Result;
import com.qihoo.ilike.http.core.IHttpRequest.HttpRequestStatus;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
/**
 * 异步执行请求的task
 * @param <T>
 */
public abstract class AbstractAsyncHttpRequest<T> extends
        AsyncTask<Void, Integer, Result<T>> {
    private static final String TAG = "AbstractAsyncHttpRequest";

    @Override
    protected Result<T> doInBackground(Void... params) {
        Log.i(TAG, "doInBackground");
        Result<T> result = new Result<T>();
        try {
            result.data = getHttpRequest().request();
        } catch (IOException e) {
            result.status = HttpRequestStatus.NetworkException;
        } catch (HttpRequestException e) {
            result.status = e.getErrorStatus();
        } catch (Exception e) {
            result.status = HttpRequestStatus.UnknowException;
        }

        if (isCancelled()) {
            result.status = HttpRequestStatus.UserCancelled;
        }

        return result;
    }

    @Override
    protected void onPostExecute(Result<T> result) {
        Log.i(TAG, "onPostExecute");
        super.onPostExecute(result);
        switch (result.status) {
        case RequestSuccess:
            dataArrival(result.data);
            break;

        case UserCancelled:
            break;

        default:
            exceptionCaught(result.status);
        }
    }

    public static class Result<T> {
        public HttpRequestStatus status = HttpRequestStatus.RequestSuccess;

        public T data;
    }
    /**
     * 获取成功返回的数据
     *  @param data   返回的数据 
     */
    public abstract void dataArrival(T data);
    /**
     * 抓取异常
     *  @param errorStatus    状态的异常
     */
    public abstract void exceptionCaught(HttpRequestStatus errorStatus);
    
    public abstract IHttpRequest getHttpService();

    public abstract AbstractHttpRequest<T> getHttpRequest();

    public void stop() {
        cancel(true);

        if (getHttpRequest() != null) {
            getHttpRequest().stop();
        }
    }
}
