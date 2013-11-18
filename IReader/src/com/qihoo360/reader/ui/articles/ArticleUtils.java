package com.qihoo360.reader.ui.articles;

import com.qihoo360.reader.R;
import com.qihoo360.reader.image.BitmapFactoryBase;
import com.qihoo360.reader.subscription.Channel;
import com.qihoo360.reader.subscription.reader.RssChannel;
import com.qihoo360.reader.support.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class ArticleUtils {
    // msg_article_detail_view_should_reload
    public final static int MSG_ARTICLE_DETAIL_VIEW_SHOULD_RELOAD = 01010101;
    // detail_activity_request_code
    public final static int DETAIL_ACTIVITY_REQUEST_CODE = 0x1fed;
    // no exist
    public final static int NOEXIST = -1;
    // failure
    public final static int FAILURE = 1;
    // success
    public final static int SUCCESS = 0;
    // article readed
    public final static int ARTICLE_READED = -1;
    // article unreader
    public final static int ARTICLE_UNREAD = 1;
    public final static int TYPE_NORMAL = 0;
    // collection
    public final static int TYPE_COLLECTION = 1;
    // size string
    public final static String SIZE_STRING = "size:";
    // collection had changed
    public final static String COLLECTION_HAD_CHANGED = "result_collection_changed";
    public final static String LIST_POSITION = "list_position";
    public final static String DETAIL_POSITION = "detail_position";
    public final static String DETAIL_LOADED = "detai_loaded";
    public final static String DETAIL_LOADED_RESULT = "detail_loaded_result";
    public final static String UNSTARRED_ARTICLE_INDICES = "unstarred_article_indices";
    public final static String COLLECTION_LIST = "collection";
    public final static String CHANNEL_SUBSCRIBE_TIPS = "channel_subscribe_tips";
    public final static String SCROLL_SWITCH_MORE_IMAGE_TIP = "scroll_switch_more_image_tips";
    public final static String SCROLL_CHANGE_TEXT_SIZE_TIP = "scroll_change_text_size_tips";
    public final static String CLICK_ALBUM_TO_BROWSE_MORE_TIP = "click_album_to_browse_more_tip";
    public final static String[] TIME_SPLITE = { "秒前", "分钟前", "小时前" };
    // titlt random bgcolor
    public final static int[] TITLE_RANDOM_BGCOLOR = {
            R.color.rd_article_line_1, R.color.rd_article_line_2,
            R.color.rd_article_line_3, R.color.rd_article_line_4,
            R.color.rd_article_line_5, R.color.rd_article_line_6,
            R.color.rd_article_line_7 };
    //  标题的随机背景 夜间模式的效果
    public final static int[] TITLE_RANDOM_BGCOLOR_NIGHTLY = {
            R.color.rd_article_line_1_night, R.color.rd_article_line_2_night,
            R.color.rd_article_line_3_night, R.color.rd_article_line_4_night,
            R.color.rd_article_line_5_night, R.color.rd_article_line_6_night,
            R.color.rd_article_line_7_night };
    // abstract count
    public final static int ABSTRACT_COUNT = 100;
    //
    private static List<Channel> sDisplayings = null;

    /**
     * 是否为正在阅读的频道
     *
     * @param channel
     * @return
     */
    public static boolean isDisplaying(Channel channel) {
        return sDisplayings != null && sDisplayings.contains(channel);
    }

    /**
     * 添加阅读的频道
     *
     * @param channel
     */
    public static void addDisplaying(Channel channel) {
        if (channel == null || !(channel instanceof RssChannel)) {
            return;
        }

        if (sDisplayings == null) {
            sDisplayings = new ArrayList<Channel>();
        }

        if (!sDisplayings.contains(channel)) {
            sDisplayings.add(channel);
        }
    }

    /**
     * 删除阅读的频道
     *
     * @param channel
     */
    public static void removeDisplaying(Channel channel) {
        if (channel == null) {
            return;
        }

        if (sDisplayings != null && sDisplayings.contains(channel)) {
            sDisplayings.remove(channel);
        }
    }

    private static byte[] SUB_CONTENT_SYNC = new byte[0];
    /**
     * 同步切割 string 的内容
     * 
     */
    public static final String subContent(String string, int ref) {
        synchronized (SUB_CONTENT_SYNC) {
            if (TextUtils.isEmpty(string))
                return "";
            String content = string.replaceAll("\\<img\\>", " ");
            try {
                content = content.replaceAll("\n", " ").trim();
                if (content.length() > ref)
                    content = content.substring(0, ref);
            } catch (Exception e) {

            }
            return content.trim();
        }
    }

    private static byte[] SPLIT_URLS_SYNC = new byte[0];
    /**
     * 切割数据url
     */
    public static final String splitUrls(String urls) {
        synchronized (SPLIT_URLS_SYNC) {
            Utils.debug("debug", " splitUrls URLS: " + urls);
            if (urls == null)
                return null;
            String[] tmp1 = urls.split(";");
            if (tmp1 == null || tmp1.length == 0)
                return null;
            Utils.debug("debug", "tmp_URL: " + tmp1[0]);
            String[] tmp2 = tmp1[0].split("\\|\\|");
            if (tmp2 == null || tmp2.length == 0)
                return null;
            Utils.debug("debug", "URL: " + tmp2[0]);
            return tmp2[0];
        }

    }

    public static final String getFirstValidImageUrlForHeaderView(String urls) {
        return getFirstValidImageUrl(urls, true, 200, 2.0f, 0.5f);
    }

    public static final String getFirstValidImageUrlForThumbView(String urls) {
        return getFirstValidImageUrl(urls, false, 200, 5.0f, 0.2f);
    }

    private static byte[] GET_FIRST_VALID_IMAGE_URL_SYNC = new byte[0];

    static final String getFirstValidImageUrl(String urls, boolean limit,
            int limitPixel, float maxLimitScale, float minLimitScale) {
        synchronized (GET_FIRST_VALID_IMAGE_URL_SYNC) {
            if (TextUtils.isEmpty(urls))
                return null;
            String[] imageUrlArray = urls.split(";");
            if (imageUrlArray == null || imageUrlArray.length == 0)
                return null;

            for (String validUrl : imageUrlArray) {
                if (TextUtils.isEmpty(validUrl) || validUrl.equals("*")) {
                    continue;
                }

                String[] imageUrlAndSize = validUrl.split("\\|\\|");
                if (imageUrlAndSize == null || imageUrlAndSize.length == 0) {
                    continue;
                } else if (limit) {
                    // 找这个图片是否符合要求
                    String sizeFull = null;
                    String picUrl = imageUrlAndSize[0];
                    if (imageUrlAndSize.length >= 2) {
                        sizeFull = imageUrlAndSize[1];
                    }
                    if (!TextUtils.isEmpty(sizeFull)) {
                        String size = sizeFull.substring(SIZE_STRING.length());
                        String[] sizeArray = size.split("\\*");
                        if (sizeArray.length == 2) {
                            int width = 0;
                            int height = 0;
                            try {
                                width = Integer.parseInt(sizeArray[0]);
                                height = Integer.parseInt(sizeArray[1]);
                            } catch (Exception e) {
                                Utils.error(ArticleUtils.class, Utils.getStackTrace(e));
                                continue;
                            }

                            // 1.长宽比悬殊不能太大（长宽相比应大于或者等于12:9）；
                            // 2.图片不应太小（长宽均大于或等于200像素）
                            boolean pixel = width >= limitPixel
                                    && height >= limitPixel;
                            boolean scale = false;
                            if (maxLimitScale == 0 || minLimitScale == 0
                                    || maxLimitScale < minLimitScale) { // 没有比例限制，那就通过
                                scale = true;
                            } else {
                                float scaleFloat = (float) width / height;
                                scale = (scaleFloat < maxLimitScale)
                                        && (scaleFloat > minLimitScale);
                            }
                            if (pixel && scale) {
                                return picUrl;
                            }
                        }
                    }
                } else {
                    return imageUrlAndSize[0];
                }
            }

            return null;
        }
    }

    private static byte[] ASYC_SET_IMAGE_SYNC = new byte[0];
    // 同步设置图片
    public static final void asycSetImage(Context context,
            BitmapFactoryBase bitmapFactory, ImageView view) {
        synchronized (ASYC_SET_IMAGE_SYNC) {
            if (view.getTag() == null) {
                return;
            }

            String url = (String) view.getTag();
            if (url.equals(BitmapFactoryBase.IMAGE_STATE_LOADED)) {
                return;
            }

            Utils.debug("watch", "URL: " + url);
            Bitmap bitmap = bitmapFactory.getBitmapByUrlFromCache(url);
            if (bitmap == null) {
                bitmapFactory.requestLoading(url, view);
            } else {
                view.setImageBitmap(bitmap);
                view.setTag(BitmapFactoryBase.IMAGE_STATE_LOADED);
            }

        }
    }
    //格式日期
    public static final String formatTime(long milliseconds) {
        String str = null;
        Date pub = new Date(milliseconds);
        Date now = new Date();
        if (pub == null || now == null)
            return "";

        if (pub.getYear() != now.getYear() || pub.getMonth() != now.getMonth()) {
            str = new String(pub.getYear() + 1900 + "-" + (pub.getMonth() + 1)
                    + "-" + pub.getDate()
            /* + formatUtl("", pub.getHours(), pub.getMinutes()) */);
        } else if (pub.getDate() != now.getDate()) {
            /*
             * int dis = now.getDate() - pub.getDate(); if (dis <= 1) str =
             * formatUtl("昨天", pub.getHours(), pub.getMinutes()); else
             */str = new String(pub.getYear() + 1900 + "-"
                    + (pub.getMonth() + 1) + "-" + pub.getDate()
            /* + formatUtl("", pub.getHours(), pub.getMinutes()) */);
        } else if (pub.getHours() != now.getHours()) {
            int dis = now.getHours() - pub.getHours();
            if (dis < 0)
                dis += 24;
            /*
             * if (dis > 11) { str = formatUtl("今天", pub.getHours(),
             * pub.getMinutes()); } else
             */if (dis > 1) {
                str = new String(dis + TIME_SPLITE[2]); //  小时钱
            } else {
                dis = now.getMinutes() + 60 - pub.getMinutes();
                if (dis >= 60)
                    str = new String(1 + TIME_SPLITE[2]);
                else
                    str = new String(dis + TIME_SPLITE[1]);
            }
        } else if (pub.getMinutes() != now.getMinutes()) {
            int dis = now.getMinutes() - pub.getMinutes();
            str = new String(dis + TIME_SPLITE[1]);
        } else if (pub.getSeconds() != now.getSeconds()) {
            int dis = now.getSeconds() - pub.getSeconds();
            str = new String(dis + TIME_SPLITE[0]);
        }
        return str;
    }

//	private final static String formatUtl(String prex, int hours, int minutes) {
//		return new String(prex + "  " + (hours < 10 ? "0" : "") + hours + ":"
//				+ (minutes < 10 ? "0" : "") + minutes);
//	}

    public final static int getRandomColor(Context context, int reference,
            boolean isNightly) {
        return context.getResources().getColor(
                isNightly ? TITLE_RANDOM_BGCOLOR_NIGHTLY[reference % 5]
                        : TITLE_RANDOM_BGCOLOR[reference % 5]);
    }
}
