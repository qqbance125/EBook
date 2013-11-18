/**
 *
 */
package com.qihoo.ilike.json;

import org.json.JSONObject;

import com.qihoo.ilike.http.core.IHttpRequest.HttpRequestStatus;
import com.qihoo.ilike.http.core.RequestParams;
import com.qihoo.ilike.http.wrapper.AsyncJsonGetHttpRequest;
import com.qihoo.ilike.json.listener.OnILikeItPostResultListener;
import com.qihoo.ilike.util.ReqParamsFactory;
import com.qihoo.ilike.util.Utils;
import com.qihoo.ilike.util.Valuable;
import com.qihoo.ilike.vo.ErrorInfo;

/**
 *
 *
 * @author Jiongxuan Zhang
 *
 */
public class ILikeItUrlPoster extends IlikeJsonGetter {

    private RequestParams genIlikeReqParams(String title, String url,
            String img_list) {
        RequestParams requestParams = null;
        String[] params = new String[4];
        params[0] = ReqParamsFactory.Rest_add_bookmark;
        params[1] = Valuable.getQid();
        params[2] = Utils.DEBUG_MODE;
        params[3] = ReqParamsFactory.ofmt_php;
        requestParams = ReqParamsFactory.genReqParams(params);
        requestParams.setCookie("title", title);
        requestParams.setCookie("url", url);
        requestParams.setCookie("img_list", img_list);
        return requestParams;
    }

    public void post(final OnILikeItPostResultListener listener, String title,
            String url, String img_list) {
        RequestParams reqParams = genIlikeReqParams(title, url, img_list);
        mGetHttpRequest = (AsyncJsonGetHttpRequest) new AsyncJsonGetHttpRequest(
                reqParams) {

            @Override
            public void exceptionCaught(HttpRequestStatus errorStatus) {
                listener.onRequestFailure(errorStatus);
            }

            @Override
            public void dataArrival(JSONObject data) {
                try {
                    ErrorInfo error = Utils.checkError(data);
                    if ((error == null) || error.isErr) {
                        listener.onResponseError(error);
                        return;
                    }
                } catch (Exception e) {
                    listener.onResponseError(null);
                }

                listener.onComplete();
            }
        }.execute();
    }
}
