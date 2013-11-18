package com.qihoo.ilike.util;

import com.qihoo.ilike.http.core.RequestParams;
import com.qihoo.ilike.manager.LoginManager;

import org.apache.http.NameValuePair;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReqParamsFactory {
    public static final String Rest_index = "Rest.index";
    /** 喜欢的 */
    public static final String Rest_favorit = "Rest.favorit";
    public static final String Rest_concerns = "Rest.concerns";
    public static final String Rest_detail = "Rest.detail";
    public static final String Rest_volume = "Rest.volume";
    public static final String Rest_volume_list = "Rest.volume_list";
    public static final String Rest_category = "Rest.category";
    public static final String Rest_category_list = "Rest.category_list";
    public static final String Rest_turn_bookmark = "Rest.turn_bookmark";
    public static final String Rest_add_bookmark = "Rest.add_bookmark";
    public static final String Rest_concerns_up = "Rest.concerns_up";
    public static final String Rest_share = "Rest.share";
    public static final String Rest_area = "Rest.area";
    public static final String Rest_update_user = "Rest.update_user";
    // 关注与取消关注相关定义
    public static final String Rest_follow_user = "Rest.follow_user";
    public static final String Rest_unfollow_user = "Rest.unfollow_user";
    public static final String Rest_follow_volume = "Rest.follow_volume";
    public static final String Rest_unfollow_volume = "Rest.unfollow_volume";

    public static final String debug = "1";

    public static final String ofmt_php = "php";

    public static final String whoview = "3";

    static final String[] indexKeys = new String[] { "method", "qid", "debug",
            "ofmt" };
    static final String[] favoritKeys = new String[] { "method", "qid",
            "offset", "count", "newest_id", "oldest_id", "range_count",
            "debug", "ofmt" };
    static final String[] concernsKeys = new String[] { "method", "qid",
            "offset", "count", "newest_id", "oldest_id", "range_count",
            "debug", "ofmt" };
    static final String[] detailKeys = new String[] { "method", "qid",
            "bookmark_id", "like_count", "debug", "ofmt" };
    static final String[] volumeKeys = new String[] { "method", "qid", "token",
            "volume_id", "start", "count", "debug", "ofmt" };
    static final String[] volume_listKeys = new String[] { "method", "qid",
            "debug", "ofmt" };
    static final String[] categoryKeys = new String[] { "method", "cate_name",
            "offset", "count", "newest_id", "oldest_id", "range_count",
            "debug", "ofmt" };
    static final String[] category_listKeys = new String[] { "method", "ver", "debug",
            "ofmt" };

    static final String[] iLikeKeys = new String[] { "method", "from_qid",
            "bookmark_id", "volume_id", "whoview", "title", "qid", "debug",
            "ofmt" };

    static final String[] iLikeUrlKeys = new String[] { "method", "qid",
            "debug", "ofmt" };

    static final String[] concernsUpKeys = new String[] { "method", "qid",
            "bookmark_id", "debug", "ofmt" };

    static final String[] shareKeys = new String[] { "method", "qid",
            "share_to", "bookmark_id", "debug", "ofmt" };

    static final String[] areaKeys = new String[] { "method", "debug", "ofmt" };

    static final String[] update_userKeys = new String[] { "method", "qid",
            "debug", "ofmt" };

    // 关注与取消关注相关定义
    static final String[] follow_userKeys = new String[] { "method", "qid",
            "follow_qid", "debug", "ofmt" };
    static final String[] unfollow_userKeys = new String[] { "method", "qid",
            "follow_qid", "debug", "ofmt" };
    // 关注与取消关注相关定义
    static final String[] follow_volumeKeys = new String[] { "method", "qid",
            "volume_id", "volume_qid", "debug", "ofmt" };
    static final String[] unfollow_volumeKeys = new String[] { "method", "qid",
            "volume_id", "volume_qid", "debug", "ofmt" };

    public static String genLoginParams() {
        return null;
    }
    /**
     * 获取各自类型的数组
     * 
     */
    private static String[] getTypeArray(String typeName) {
        if (typeName.equals(Rest_index)) {
            return indexKeys;
        } else if (typeName.equals(Rest_favorit)) {
            return favoritKeys;
        } else if (typeName.equals(Rest_concerns)) {
            return concernsKeys;
        } else if (typeName.equals(Rest_detail)) {
            return detailKeys;
        } else if (typeName.equals(Rest_volume)) {
            return volumeKeys;
        } else if (typeName.equals(Rest_volume_list)) {
            return volume_listKeys;
        } else if (typeName.equals(Rest_category)) {
            return categoryKeys;
        } else if (typeName.equals(Rest_category_list)) {
            return category_listKeys;
        } else if (typeName.equals(Rest_turn_bookmark)) {
            return iLikeKeys;
        } else if (Rest_add_bookmark.equals(typeName)) {
            return iLikeUrlKeys;
        } else if (Rest_concerns_up.equalsIgnoreCase(typeName)) {
            return concernsUpKeys;
        } else if (Rest_share.equalsIgnoreCase(typeName)) {
            return shareKeys;
        } else if (Rest_area.equalsIgnoreCase(typeName)) {
            return areaKeys;
        } else if (Rest_update_user.equalsIgnoreCase(typeName)) {
            return update_userKeys;
        } else if (Rest_follow_user.equalsIgnoreCase(typeName)) {
            return follow_userKeys;
        } else if (Rest_unfollow_user.equalsIgnoreCase(typeName)) {
            return unfollow_userKeys;
        } else if (Rest_follow_volume.equalsIgnoreCase(typeName)) {
            return follow_volumeKeys;
        } else if (Rest_unfollow_volume.equalsIgnoreCase(typeName)) {
            return unfollow_volumeKeys;
        } else {
            return new String[0];
        }
    }

    private static int typeParamsCount(String type) {
        if (type.equals(Rest_index)) {
            return indexKeys.length;
        } else if (type.equals(Rest_favorit)) {
            return favoritKeys.length;
        } else if (type.equals(Rest_concerns)) {
            return concernsKeys.length;
        } else if (type.equals(Rest_detail)) {
            return detailKeys.length;
        } else if (type.equals(Rest_volume)) {
            return volumeKeys.length;
        } else if (type.equals(Rest_volume_list)) {
            return volume_listKeys.length;
        } else if (type.equals(Rest_category)) {
            return categoryKeys.length;
        } else if (type.equals(Rest_category_list)) {
            return category_listKeys.length;
        } else if (type.equals(Rest_turn_bookmark)) {
            return iLikeKeys.length;
        } else if (Rest_add_bookmark.equals(type)) {
            return iLikeUrlKeys.length;
        } else if (Rest_concerns_up.equalsIgnoreCase(type)) {
            return concernsUpKeys.length;
        } else if (Rest_share.equalsIgnoreCase(type)) {
            return shareKeys.length;
        } else if (Rest_area.equalsIgnoreCase(type)) {
            return areaKeys.length;
        } else if (Rest_update_user.equalsIgnoreCase(type)) {
            return update_userKeys.length;
        } else if (Rest_follow_user.equalsIgnoreCase(type)) {
            return follow_userKeys.length;
        } else if (Rest_unfollow_user.equalsIgnoreCase(type)) {
            return unfollow_userKeys.length;
        } else if (Rest_follow_volume.equalsIgnoreCase(type)) {
            return follow_volumeKeys.length;
        } else if (Rest_unfollow_volume.equalsIgnoreCase(type)) {
            return unfollow_volumeKeys.length;
        } else {
            return 0;
        }
    }

    public static String getImagePath(String oldURl, String width,
            String quality) {
        if (oldURl == null) {
            return null;
        }
        return oldURl.replaceFirst("\\/[0-9]*\\_[0-9]*[_]{0,1}\\/", "/" + width
                + "__" + quality + "/");
    }

    public static String getImageAddParam(String oldURl, String Param) {
        if (oldURl == null) {
            return null;
        }
        String newUrl = oldURl + "?f=ilike";
        return newUrl;
    }
    /**
     * 解析请求的参数
     */
    private static RequestParams parseReqParams(String[] key, String[] pars) {
        RequestParams ret = new RequestParams();
        String method = null;
        for (int i = 0; i < pars.length; i++) {
            if (key[i].equals("debug") || key[i].equals("token")
                    || key[i].equals("ofmt"))
                continue;

            if ("method".equals(key[i]))
                method = pars[i];

            ret.add(key[i], pars[i]);
        }

        // token
        if (!Rest_category.equals(method)
                && !(Rest_category_list.equals(method))) {
            ret.add("token",
                    MD5Util.getMD5code(MD5Util.getMD5code(Constants.getQid())
                            + MD5Util.getMD5code("www.woxihuan.com:wap")));
        }

        // Q/T(Cookie)
        if (LoginManager.isLogin()) {
            ret.setCookie("Q", LoginManager.getQ());
            ret.setCookie("T", LoginManager.getT());
        }

        // appname
        ret.add("appname", "app_phone");

        // sign
        ret.add("sign",
                getSign(ret.getRequestParams(), "www.woxihuan.com:app_phone"));
        return ret;
    }

    public static RequestParams genReqParams(String[] pars) {
        if (null == pars) {
            return null;
        } else {
            if (pars.length != typeParamsCount(pars[0])) {
                return null;
            } else {
                return parseReqParams(getTypeArray(pars[0]), pars);
            }
        }
    }

    private static String getSign(List<NameValuePair> params, String secretKey) {
        List<String> sortList = new ArrayList<String>();

        for (NameValuePair valuePair : params) {
            try {
                sortList.add(valuePair.getName() + "="
                        + URLEncoder.encode(valuePair.getValue(), "UTF-8"));
            } catch (Exception e) {
            }
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
