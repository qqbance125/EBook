package com.qihoo.ilike.json;

import com.qihoo.ilike.http.core.IHttpRequest.HttpRequestStatus;
import com.qihoo.ilike.http.core.RequestParams;
import com.qihoo.ilike.http.wrapper.AsyncJsonGetHttpRequest;
import com.qihoo.ilike.json.listener.OnHotCollectionBookmarksResultListener;
import com.qihoo.ilike.util.ReqParamsFactory;
import com.qihoo.ilike.util.Utils;
import com.qihoo.ilike.vo.CategoryVo;
import com.qihoo.ilike.vo.ErrorInfo;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 负责“热门收藏”与服务器的交互
 *
 * @author Jiongxuan Zhang
 *
 */
public class HotCollectionBookmarksGetter extends IlikeJsonGetter {
    private RequestParams genUpdateReqParams(String categoryName, int offset,
            String newestId, String oldestId, int rangeCount) {
        String[] params = new String[9];
        params[0] = ReqParamsFactory.Rest_category;
        params[1] = categoryName;
        params[2] = String.valueOf(offset);
        params[3] = UPDATE_NUM;
        params[4] = newestId;
        params[5] = oldestId;
        params[6] = String.valueOf(rangeCount);
        params[7] = Utils.DEBUG_MODE;
        params[8] = ReqParamsFactory.ofmt_php;
        return ReqParamsFactory.genReqParams(params);
    }

    public void get(final OnHotCollectionBookmarksResultListener listener,
            String categoryName, final int offset, String newestId, String oldestId, int rangeCount) {
        RequestParams reqParams = genUpdateReqParams(categoryName, offset,
                newestId, oldestId, rangeCount);
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

    private void getMoreCollectionComplete(
            OnHotCollectionBookmarksResultListener listener, JSONObject data) {
        try {
            ErrorInfo error = Utils.checkError(data);
            if ((error == null) || error.isErr) {
                listener.onResponseError(error);
            } else {
                int total = 0;
                CategoryVo category = new CategoryVo();
                category.builder(data);
                if ((category != null) && (category.collectList != null)
                        && (category.collectList.length > 0)) {
                    total = category.total;
                }
                listener.onComplete(category, total);
            }
        } catch (JSONException e) {
            com.qihoo360.reader.support.Utils.error(getClass(), com.qihoo360.reader.support.Utils.getStackTrace(e));
        }
    }
}
