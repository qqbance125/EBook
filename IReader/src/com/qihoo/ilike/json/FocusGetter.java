/**
 *
 */
package com.qihoo.ilike.json;

import com.qihoo.ilike.http.core.IHttpRequest.HttpRequestStatus;
import com.qihoo.ilike.http.core.RequestParams;
import com.qihoo.ilike.http.wrapper.AsyncJsonGetHttpRequest;
import com.qihoo.ilike.json.listener.OnFocusResultListener;
import com.qihoo.ilike.util.ReqParamsFactory;
import com.qihoo.ilike.util.Utils;
import com.qihoo.ilike.util.Valuable;
import com.qihoo.ilike.vo.CollectList;
import com.qihoo.ilike.vo.ErrorInfo;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 负责“我的关注”与服务器的交互
 *
 * @author Jiongxuan Zhang
 *
 */
public class FocusGetter extends IlikeJsonGetter {
    private RequestParams genGetMoreReqParams(int hasLoadPos, String newestId,
            String oldestId, int rangeCount) {
        String[] params = new String[9];
        params[0] = ReqParamsFactory.Rest_concerns;
        params[1] = Valuable.getQid();
        params[2] = String.valueOf(hasLoadPos);
        params[3] = UPDATE_NUM;
        params[4] = newestId;
        params[5] = oldestId;
        params[6] = String.valueOf(rangeCount);
        params[7] = Utils.DEBUG_MODE;
        params[8] = ReqParamsFactory.ofmt_php;
        return ReqParamsFactory.genReqParams(params);
    }

    public void parse(final OnFocusResultListener listener,
            int hasLoadPos, String newestId, String oldestId,
            int rangeCount) {
        RequestParams reqParams = genGetMoreReqParams(hasLoadPos, newestId,
                oldestId, rangeCount);
        mGetHttpRequest = new AsyncJsonGetHttpRequest(reqParams) {

            @Override
            public void exceptionCaught(HttpRequestStatus errorStatus) {
                listener.onRequestFailure(errorStatus);
            }

            @Override
            public void dataArrival(JSONObject data) {
                getMoreCollectionComplete(listener, data);
            }
        };
        mGetHttpRequest.execute();
    }

    private void getMoreCollectionComplete(OnFocusResultListener listener,
            JSONObject data) {
        try {
            ErrorInfo error = Utils.checkError(data);
            if ((error == null) || error.isErr) {
                listener.onResponseError(error);
            } else {
                CollectList collectList = new CollectList();
                int total = 0;
                collectList.builder(data);
                if (collectList != null) {
                    if (collectList.total <= 0) {
                    } else {
                        total = collectList.total;
                    }
                }

                listener.onComplete(collectList, total);
            }
        } catch (JSONException e) {
            // error process
            com.qihoo360.reader.support.Utils.error(getClass(), com.qihoo360.reader.support.Utils.getStackTrace(e));
        }
    }
}
