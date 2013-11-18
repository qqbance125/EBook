/**
 *
 */
package com.qihoo.ilike.json;

import org.json.JSONException;
import org.json.JSONObject;

import com.qihoo.ilike.http.core.IHttpRequest.HttpRequestStatus;
import com.qihoo.ilike.http.core.RequestParams;
import com.qihoo.ilike.http.wrapper.AsyncJsonGetHttpRequest;
import com.qihoo.ilike.json.listener.OnCategorysResultListener;
import com.qihoo.ilike.util.ReqParamsFactory;
import com.qihoo.ilike.util.Utils;
import com.qihoo.ilike.vo.CategoryList;
import com.qihoo.ilike.vo.ErrorInfo;

/**
 * 负责“大家喜欢”与服务器的交互
 *
 * @author Jiongxuan Zhang
 *
 */
public class CategorysGetter extends IlikeJsonGetter {

    private RequestParams genUpdateReqParams(String ver) {
        String[] params = new String[4];
        params[0] = ReqParamsFactory.Rest_category_list;
        params[1] = ver;
        params[2] = Utils.DEBUG_MODE;
        params[3] = ReqParamsFactory.ofmt_php;
        return ReqParamsFactory.genReqParams(params);
    }

    public void get(final OnCategorysResultListener listener, String ver) {
        RequestParams reqParams = genUpdateReqParams(ver);
        mGetHttpRequest = new AsyncJsonGetHttpRequest(
                reqParams) {

            @Override
            public void exceptionCaught(HttpRequestStatus errorStatus) {
                listener.onRequestFailure(errorStatus);
            }

            @Override
            public void dataArrival(JSONObject data) {
                getCategoryComplete(listener, data);
            }
        };
        mGetHttpRequest.execute();
    }

    private void getCategoryComplete(
            OnCategorysResultListener listener, JSONObject data) {
        try {
            ErrorInfo error = Utils.checkError(data);
            if ((error == null) || error.isErr) {
                // error process
                listener.onResponseError(error);
            } else {
                CategoryList categoryList = new CategoryList();
                categoryList.builder(data);
                listener.onComplete(data.toString(), categoryList);
            }
        } catch (JSONException e) {
            // error process
            com.qihoo360.reader.support.Utils.error(getClass(), com.qihoo360.reader.support.Utils.getStackTrace(e));
        }

    }
}
