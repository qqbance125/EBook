/**
 *
 */
package com.qihoo.ilike.json;

import com.qihoo.ilike.http.wrapper.AsyncJsonGetHttpRequest;

/**
 * @author zhangjiongxuan
 *
 */
public abstract class IlikeJsonGetter {

    public static final String UPDATE_NUM = "30";

    protected AsyncJsonGetHttpRequest mGetHttpRequest;

    public void stop() {
        if (mGetHttpRequest != null) {
            mGetHttpRequest.stop();
        }
    }
}
