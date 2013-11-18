/**
 *
 */
package com.qihoo.ilike.json;

import com.qihoo.ilike.http.core.IHttpRequest.HttpRequestStatus;
import com.qihoo.ilike.http.core.RequestParams;
import com.qihoo.ilike.http.wrapper.AsyncJsonGetHttpRequest;
import com.qihoo.ilike.json.listener.OnILikeItPostResultListener;
import com.qihoo.ilike.util.ReqParamsFactory;
import com.qihoo.ilike.util.Utils;
import com.qihoo.ilike.util.Valuable;
import com.qihoo.ilike.vo.ErrorInfo;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * @author Jiongxuan Zhang
 *
 */
public class ILikeItPoster extends IlikeJsonGetter {

    private RequestParams genIlikeReqParams(String qid, String bookmarkId,
            String albumId, String title) {
        RequestParams requestParams = null;
        String[] params = new String[9];
        params[0] = ReqParamsFactory.Rest_turn_bookmark;
        params[1] = qid;
        params[2] = bookmarkId;
        params[3] = albumId;
        params[4] = ReqParamsFactory.whoview;
        try {
            params[5] = URLEncoder.encode(title, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            com.qihoo360.reader.support.Utils.error(getClass(), com.qihoo360.reader.support.Utils.getStackTrace(e));
        }
        params[6] = Valuable.getQid();
        params[7] = Utils.DEBUG_MODE;
        params[8] = ReqParamsFactory.ofmt_php;
        requestParams = ReqParamsFactory.genReqParams(params);
        return requestParams;
    }

    public void post(final OnILikeItPostResultListener listener, String qid,
            String bookmarkId, String albumId, String title) {
        RequestParams reqParams = genIlikeReqParams(qid, bookmarkId, albumId,
                title);
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
