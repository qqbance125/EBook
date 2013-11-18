/**
 *
 */
package com.qihoo.ilike.json;

import org.json.JSONException;
import org.json.JSONObject;

import com.qihoo.ilike.http.core.IHttpRequest.HttpRequestStatus;
import com.qihoo.ilike.http.core.RequestParams;
import com.qihoo.ilike.http.wrapper.AsyncJsonGetHttpRequest;
import com.qihoo.ilike.json.listener.OnCollectionResultListener;
import com.qihoo.ilike.util.ReqParamsFactory;
import com.qihoo.ilike.util.Utils;
import com.qihoo.ilike.vo.ErrorInfo;
import com.qihoo.ilike.vo.Favorit;

/**
 * 负责“我的收藏”与服务器的交互
 *
 * @author Jiongxuan Zhang
 *
 */
public class MyCollectionGetter extends IlikeJsonGetter {
    private RequestParams genGetMoreFavoritReqParams(String qid,
            int hasLoadPos, String newestId, String oldestId, int rangeCount) {
        String[] params = new String[9];
        params[0] = ReqParamsFactory.Rest_favorit;
        params[1] = qid;
        params[2] = String.valueOf(hasLoadPos);
        params[3] = UPDATE_NUM;
        params[4] = newestId;
        params[5] = oldestId;
        params[6] = String.valueOf(rangeCount);
        params[7] = Utils.DEBUG_MODE;
        params[8] = ReqParamsFactory.ofmt_php;
        return ReqParamsFactory.genReqParams(params);
    }

    public void parse(final OnCollectionResultListener listener, String qid,
            int hasLoadPos, String newestId, String oldestId, int rangeCount) {
        RequestParams reqParams = genGetMoreFavoritReqParams(qid, hasLoadPos,
                newestId, oldestId, rangeCount);
        mGetHttpRequest = new AsyncJsonGetHttpRequest(reqParams) {

            @Override
            public void exceptionCaught(HttpRequestStatus errorStatus) {
                listener.onRequestFailure(errorStatus);
            }

            @Override
            public void dataArrival(JSONObject data) {
                getMoreFavoritComplete(listener, data);
            }
        };
        mGetHttpRequest.execute();
    }

    private void getMoreFavoritComplete(OnCollectionResultListener listener,
            JSONObject data) {
        try {
            ErrorInfo error = Utils.checkError(data);
            if ((error == null) || error.isErr) {
                listener.onResponseError(error);

            } else {
                int total = 0;
                Favorit favorit = new Favorit();
                favorit.builder(data);

                if (favorit != null) {
                    total = favorit.total;
                }

                listener.onComplete(favorit, total);
            }
        } catch (JSONException e) {
            // error process
            com.qihoo360.reader.support.Utils.error(getClass(), com.qihoo360.reader.support.Utils.getStackTrace(e));
        }
    }
}
