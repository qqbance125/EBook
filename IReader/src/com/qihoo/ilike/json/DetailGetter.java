/**
 *
 */
package com.qihoo.ilike.json;

import org.json.JSONException;
import org.json.JSONObject;

import com.qihoo.ilike.http.core.IHttpRequest.HttpRequestStatus;
import com.qihoo.ilike.http.core.RequestParams;
import com.qihoo.ilike.http.wrapper.AsyncJsonGetHttpRequest;
import com.qihoo.ilike.json.listener.OnDetailResultListener;
import com.qihoo.ilike.util.ReqParamsFactory;
import com.qihoo.ilike.util.Utils;
import com.qihoo.ilike.vo.Detail;
import com.qihoo.ilike.vo.ErrorInfo;

/**
 * 负责“获取图片正文”与服务器的交互
 *
 * @author Jiongxuan Zhang
 *
 */
public class DetailGetter extends IlikeJsonGetter {

    private RequestParams genUpdateReqDetailParams(String qid,
            String bookmarkId, String like_count) {
        String[] params = new String[6];
        params[0] = ReqParamsFactory.Rest_detail;
        params[1] = qid;
        params[2] = bookmarkId;
        params[3] = like_count;
        params[4] = Utils.DEBUG_MODE;
        params[5] = ReqParamsFactory.ofmt_php;
        return ReqParamsFactory.genReqParams(params);
    }

    public void get(final OnDetailResultListener listener, String qid,
            String bookmarkId, String like_count) {
        RequestParams reqParams = genUpdateReqDetailParams(qid, bookmarkId,
                like_count);
        mGetHttpRequest = new AsyncJsonGetHttpRequest(reqParams) {

            @Override
            public void exceptionCaught(HttpRequestStatus errorStatus) {
                listener.onRequestFailure(errorStatus);
            }

            @Override
            public void dataArrival(JSONObject data) {
                getMoreDetailComplete(listener, data);
            }
        };
        mGetHttpRequest.execute();
    }

    private void getMoreDetailComplete(OnDetailResultListener listener,
            JSONObject data) {
        try {
            ErrorInfo error = Utils.checkError(data);
            if ((error == null) || error.isErr) {
                listener.onResponseError(error);

            } else {
                Detail detail = new Detail();
                detail.builder(data);
                listener.onComplete(detail);
            }
        } catch (JSONException e) {
            // error process
            com.qihoo360.reader.support.Utils.error(getClass(), com.qihoo360.reader.support.Utils.getStackTrace(e));
        }
    }
}
