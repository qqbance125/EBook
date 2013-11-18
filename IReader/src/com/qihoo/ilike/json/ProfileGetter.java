/**
 *
 */
package com.qihoo.ilike.json;

import org.json.JSONException;
import org.json.JSONObject;

import com.qihoo.ilike.http.core.IHttpRequest.HttpRequestStatus;
import com.qihoo.ilike.http.core.RequestParams;
import com.qihoo.ilike.http.wrapper.AsyncJsonGetHttpRequest;
import com.qihoo.ilike.json.listener.OnProfileResultListener;
import com.qihoo.ilike.util.Constants;
import com.qihoo.ilike.util.PreferenceUtils;
import com.qihoo.ilike.util.ReqParamsFactory;
import com.qihoo.ilike.util.Utils;
import com.qihoo.ilike.util.Valuable;
import com.qihoo.ilike.vo.ErrorInfo;
import com.qihoo.ilike.vo.Profile;

/**
 * 负责“获取用户信息”与服务器的交互
 *
 * @author Jiongxuan Zhang
 *
 */
public class ProfileGetter {

    public static void get(final OnProfileResultListener listener, String qid) {
        RequestParams reqParams = genProfileReqParams(qid);
        AsyncJsonGetHttpRequest getHttpReq = new AsyncJsonGetHttpRequest(
                reqParams) {

            @Override
            public void exceptionCaught(HttpRequestStatus errorStatus) {
                listener.onRequestFailure(errorStatus);
            }

            @Override
            public void dataArrival(JSONObject data) {
                loadProfileComplete(listener, data);
            }
        };
        getHttpReq.execute();

    }

    private static RequestParams genProfileReqParams(String qid) {
        String[] params = new String[4];
        params[0] = ReqParamsFactory.Rest_index;
        params[1] = qid;
        params[2] = Utils.DEBUG_MODE;
        params[3] = ReqParamsFactory.ofmt_php;
        RequestParams requestParams = ReqParamsFactory.genReqParams(params);

        requestParams.setCookie("cur_qid", Constants.getQid());
        return ReqParamsFactory.genReqParams(params);
    }

    private static void loadProfileComplete(OnProfileResultListener listener,
            JSONObject data) {
        try {
            if (data == null) {
                dataError(listener);
                clearLoginState();
                return;
            }
            ErrorInfo error = Utils.checkError(data);
            if (error == null) {
                dataError(listener);
                clearLoginState();
                return;
            }
            if (error.isErr) {
                if (error.errmsg != null) {
                    listener.onResponseError(error);
                } else {
                    dataError(listener);
                }
                clearLoginState();
                return;
            } else {
                Profile profile = new Profile();
                profile.builder(data);
                if ((profile != null) && (profile.user != null)) {
                    // if (Utils.isStrValidable(profile.user.name)) {
                    // // loginComplete();
                    // Valuable.setUser(profile.user);
                    // } else {
                    // // startPerfectionActivity();
                    // }
                    //
                    // VolumeList volumeList = new VolumeList();
                    // volumeList.volumeList = profile.volumes;
                    // Valuable.setVolumeList(volumeList);
                    listener.onComplete(profile);
                } else {
                    dataError(listener);
                    clearLoginState();
                }
            }
        } catch (JSONException e) {
            // error process
            com.qihoo360.reader.support.Utils.error(ProfileGetter.class,
                    com.qihoo360.reader.support.Utils.getStackTrace(e));
            listener.onRequestFailure(HttpRequestStatus.DecodeException);
            clearLoginState();
        }
    }

    private static void dataError(OnProfileResultListener listener) {
        listener.onRequestFailure(HttpRequestStatus.ResponseException);
    }

    private static void clearLoginState() {
        Valuable.clear();
        PreferenceUtils.putString(PreferenceUtils.QID, null);
    }
}
