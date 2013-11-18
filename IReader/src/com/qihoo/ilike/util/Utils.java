package com.qihoo.ilike.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.Toast;

import com.qihoo.ilike.manager.LoginManager;
import com.qihoo.ilike.ui.ILikeIntroActivity;
import com.qihoo.ilike.vo.ErrorInfo;
import com.qihoo.ilike.vo.Response;
import com.qihoo360.reader.R;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.support.NetUtils;

public class Utils {
    public static final String IMAGE_QUALITY_LIST = "70";

    public static final String IMAGE_QUALITY_WATERFALL = "90";

    public static final String IMAGE_QUALITY_DETAIL = "90";

    public static final String DEBUG_MODE = ReqParamsFactory.debug;

    public static final String HTML_SPACE = "&nbsp;";

    public static final float IMAGE_HEIGHT_SCALE = 0.382f;

    public static boolean isStrValidable(String str) {
        if ((str == null) || (str.equalsIgnoreCase(""))
                || (str.equalsIgnoreCase(HTML_SPACE))) {
            return false;
        }
        return true;
    }

    public static String addColorForText(String src, String color) {
        if (src != null) {
            return "<font color=\"" + color + "\">" + src + "</font>";
        }
        return null;
    }

    public static String addColorAndSizeForText(String src, String color,
            String size) {
        if (src != null) {
            return "<font size=\"" + size + "\" color=\"" + color + "\">" + src
                    + "</font>";
        }
        return null;
    }

    public static String addImageForText(int rid) {
        return "<img src='" + rid + "'/>";
    }

    public static int calculateImageWidth(int maxWidth, int maxHeight,
            int orgWidht, int orgHeight) {
        if (orgWidht <= 0) {
            return maxWidth;
        }
        int width = orgWidht;
        int height = orgHeight;
        if (orgWidht > maxWidth) {
            width = maxWidth;
            height = (int) (((float) (orgHeight * maxWidth)) / orgWidht);
        }

        if ((height > 0) && (height > maxHeight)) {
            width = (int) ((float) (width * maxHeight) / height);
        }

        return width;
    }

    public static void setTabIndicatorTranslateAnimation(View view,
            int parentWidth, float fromXValue, float toXValue, float offsetX,
            int duration) {
        int currentY = view.getTop();
        int viewWidth = view.getRight() - view.getLeft();
        float offset = (float) viewWidth / 2;
        if (offset > 0.0f) {
            offset = offset / parentWidth;
        }

        int transXType = TranslateAnimation.RELATIVE_TO_PARENT;
        int transYType = TranslateAnimation.ABSOLUTE;
        TranslateAnimation transAnima = new TranslateAnimation(transXType,
                fromXValue - offset + offsetX, transXType, toXValue - offset
                        + offsetX, transYType, currentY, transYType, currentY);

        transAnima.setDuration(duration);
        transAnima.setFillAfter(true);
        view.setAnimation(transAnima);

    }

    public static InputStream getAssetsFileIS(Context context, String fileName)
            throws IOException {
        AssetManager am = context.getAssets();
        InputStream is = null;
        is = am.open(fileName);
        return is;
    }

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public static ErrorInfo checkError(Response res) {
        ErrorInfo ret = new ErrorInfo();
        if (res != null) {
            if (res.errno != 0) {
                ret.isErr = true;
                ret.errno = res.errno;
                ret.errmsg = res.errmsg;
            }
        }
        return ret;
    }

    public static ErrorInfo checkError(JSONObject res) throws JSONException {
        Response response = new Response();
        response.builder(res);
        return checkError(response);
    }

    public static String genFileNamePrefix(String width, String quality) {
        return width + "_" + quality + "_";
    }

    public static String getAbsoluteFileNameFromUrl(String url, String prefix) {
        String fileName = getFileNameFromUrl(url);
        if ((prefix != null) && !(prefix.equalsIgnoreCase(""))) {
            fileName = prefix + fileName;
        }
        String absFileName = Constants.getAndCreateRootPath() + fileName;
        return absFileName;
    }

    public static String getFileNameFromUrl(String url) {
        String ret = null;
        if (url != null) {
            int pos = url.lastIndexOf("/");
            if (pos < url.length() - 1) {
                ret = url.substring(pos + 1);
            }
        }
        return ret;
    }

    public static String howLong(String date) {
        return date;
    }

    public static String htmlFormatB(String src) {
        return "<b>" + src + "</b>";
    }

    public static String htmlFormatFont(String src, int size, String color) {
        return "<font size=\"" + size + "\" color=\"" + color + "\">" + src
                + "</font>";
    }

    public static String getVersionName(Context context) {
        String versionName = "";
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            versionName = pi.versionName;
            if (versionName == null || versionName.length() <= 0) {
                return "";
            }
        } catch (Exception e) {
            com.qihoo360.reader.support.Utils.error(Utils.class,
                    com.qihoo360.reader.support.Utils.getStackTrace(e));
        }
        return versionName;
    }

    public static void setScrollViewThumb(Context context, ListView listView,
            int drawableRid) {
        try {
            Field f = AbsListView.class.getDeclaredField("mFastScroller");
            f.setAccessible(true);
            Object o = f.get(listView);
            f = f.getType().getDeclaredField("mThumbDrawable");
            f.setAccessible(true);
            Drawable drawable = (Drawable) f.get(o);
            drawable = context.getResources().getDrawable(drawableRid);
            f.set(o, drawable);
        } catch (Exception e) {
            com.qihoo360.reader.support.Utils.error(Utils.class,
                    com.qihoo360.reader.support.Utils.getStackTrace(e));
        }

    }

    public static boolean isSelf(String qid) {
        String globalQid = Valuable.getQid();
        if ((qid != null) && (globalQid != null)) {
            return qid.equalsIgnoreCase(globalQid);
        }
        return false;
    }

    public static void gc() {
        // VMRuntime.getRuntime().gcSoftReferences();
        System.gc();
    }

    public static void adjustViewToWrapContentHeight(View view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params == null) {
            params = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.FILL_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        } else {
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        }

        view.setLayoutParams(params);
    }

    public final static String PREF_ILIKE_INTRO_SCREEN_SHOWN = "ilike_intro_screen_shown";

    public static boolean accountConfigured(Context context) {
        SharedPreferences sp = Settings.getSharedPreferences();
        if (!sp.getBoolean(PREF_ILIKE_INTRO_SCREEN_SHOWN, false)) {
            context.startActivity(new Intent(context, ILikeIntroActivity.class));
            return false;
        }

        return checkLoginStatus(context, null, null);
    }

    public final static String INTENT_EXTRA_LAUNCH_ILIKE_MAIN_PAGE = "launch_ilike_main_page";
    public final static String INTENT_EXTRA_DATA_URL = "path_url_to_collect";
    public final static String INTENT_EXTRA_DATA_TITLE = "path_url_title_to_collect";

    public static boolean showWelcomeScreenBeforeEnterIlikeMainPage(Context context) {
        SharedPreferences sp = Settings.getSharedPreferences();
        if (!sp.getBoolean(PREF_ILIKE_INTRO_SCREEN_SHOWN, false)) {
            Intent intent = new Intent(context, ILikeIntroActivity.class);
            intent.putExtra(INTENT_EXTRA_LAUNCH_ILIKE_MAIN_PAGE, true);
            context.startActivity(intent);
            return true;
        } else {
            return false;
        }
    }

    public static boolean accountConfigured(Context context, String url,
            String title) {
        SharedPreferences sp = Settings.getSharedPreferences();
        if (!sp.getBoolean(PREF_ILIKE_INTRO_SCREEN_SHOWN, false)) {
            Intent intent = new Intent(context, ILikeIntroActivity.class);
            intent.putExtra(INTENT_EXTRA_DATA_URL, url);
            intent.putExtra(INTENT_EXTRA_DATA_TITLE, title);
            context.startActivity(intent);
            return false;
        }

        return checkLoginStatus(context, url, title);
    }

    public static boolean checkLoginStatus(Context context, String url,
            String title) {
        if (LoginManager.isLogin()) {
            return true;
        } else {
            Intent intent = new Intent();
            if (!TextUtils.isEmpty(url)) {
                intent.putExtra(INTENT_EXTRA_DATA_URL, url);
            }
            if (!TextUtils.isEmpty(title)) {
                intent.putExtra(INTENT_EXTRA_DATA_TITLE, title);
            }
            intent.setClassName(context.getPackageName(),
                    "com.qihoo360.browser.activity.LoginActivity");
            context.startActivity(intent);
            return false;
        }
    }

    public static IntentFilter getAccountConfigResultFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.BROADCAST_ACTION_LOGIN_SUCCESS);
        filter.addAction(ILikeIntroActivity.BROADCAST_ILIKE_INTRO_ACTIVITY_FINISHED);
        return filter;
    }

    public static boolean accountConfigSucceed(Intent intent) {
        String action = intent.getAction();
        return !TextUtils.isEmpty(action)
                && ((action
                        .equals(com.qihoo.ilike.util.Constants.BROADCAST_ACTION_LOGIN_SUCCESS) && intent
                        .getBooleanExtra(
                                com.qihoo.ilike.util.Constants.BROADCAST_VALUE_LOGIN_RESULT,
                                true)) || (action
                        .equals(ILikeIntroActivity.BROADCAST_ILIKE_INTRO_ACTIVITY_FINISHED) && !intent
                        .getBooleanExtra("cancelled", true)));
    }

    /**
     * Share from the top level page of I like
     */
    public static void shareMainPage(Context context) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT,
                context.getString(R.string.i_like_share_main_page));
        try {
            context.startActivity(Intent.createChooser(send,
                    context.getString(R.string.i_like_share_title)));
        } catch (android.content.ActivityNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Share from one of the I like articles
     */
    public static void shareILikeArticle(Context context, String title,
            String url) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT,
                title + " " + url + context.getString(R.string.rd_share_tips));
        send.putExtra(Intent.EXTRA_SUBJECT, title);
        try {
            context.startActivity(Intent.createChooser(send,
                    context.getString(R.string.i_like_share_title)));
        } catch (android.content.ActivityNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    public static boolean checkNetworkStatusBeforeLike(Context context) {
        boolean networkAvailable = NetUtils.isNetworkAvailable();
        if (!networkAvailable) {
            Toast.makeText(context, R.string.msg_network_exception,
                    Toast.LENGTH_SHORT).show();
        }

        return networkAvailable;

    }
}
