/**
 *
 */
package com.qihoo.ilike.json;

import com.qihoo.ilike.http.core.IHttpRequest.HttpRequestStatus;
import com.qihoo.ilike.http.core.RequestParams;
import com.qihoo.ilike.http.wrapper.AsyncJsonGetHttpRequest;
import com.qihoo.ilike.json.listener.OnLoginPostResultListener;
import com.qihoo.ilike.util.Constants;
import com.qihoo.ilike.util.DesUtil;
import com.qihoo.ilike.util.MD5Util;
import com.qihoo.ilike.util.Utils;
import com.qihoo.ilike.util.Valuable;
import com.qihoo.ilike.vo.ErrorInfo;

import org.apache.http.NameValuePair;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 负责“登陆”与服务器的交互
 *  用get方式交互
 * @author Jiongxuan Zhang
 *
 */
public class LoginPoster extends IlikeJsonGetter {

    public void post(final OnLoginPostResultListener listener,
            String userName, String password) {
        RequestParams params = new RequestParams();
        params.add("method", "UserIntf.login");
        params.add("des", "1");
        params.add("is_keep_alive", "0");
        params.add("from", "woxihuan");
        params.add("fields", "qid,username");
        params.add("v", "1.0");

        String param = DesUtil.encryptDES("username=" + userName + "&password="
                + MD5Util.getMD5code(password));
        params.add("param", param);
        params.add("format", "json");
        params.add("sig", getSign(params.getRequestParams(), "4e083ddc1"));
        params.setHost("login.360.cn");

        mGetHttpRequest = (AsyncJsonGetHttpRequest) new AsyncJsonGetHttpRequest(params) {
            @Override
            public void dataArrival(JSONObject data) {
                try {
                    ErrorInfo error = Utils.checkError(data);
                    if ((error == null) || error.isErr) {
                        if (error.errno == 5010 || error.errno == 5011) {
                            listener.onHandleCaptcha(true);
                        } else {
                            listener.onResponseError(error);
                        }
                    } else {
                        String qid = data.getJSONObject("user")
                                .getString("qid");
                        Valuable.setQid(qid);
                        Constants.setQid(qid);
                        listener.onComplete();
                    }
                } catch (Exception e) {
                    exceptionCaught(HttpRequestStatus.DecodeException);
                }
            }

            @Override
            public void exceptionCaught(HttpRequestStatus errorStatus) {
                listener.onRequestFailure(errorStatus);
            }
        }.execute();
    }
    /**
     * 标记
     * secretKey 密钥
     */
    private String getSign(List<NameValuePair> params, String secretKey) {
        List<String> sortList = new ArrayList<String>();

        for (NameValuePair valuePair : params) {
            sortList.add(valuePair.toString());
        }

        Collections.sort(sortList);

        StringBuilder sb = new StringBuilder();

        for (String param : sortList) {
            sb.append(param);
        }

        sb.append(secretKey);

        return MD5Util.getMD5code(sb.toString());
    }
}
