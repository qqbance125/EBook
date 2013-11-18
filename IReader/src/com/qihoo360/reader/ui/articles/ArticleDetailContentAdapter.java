package com.qihoo360.reader.ui.articles;

import com.qihoo360.reader.R;
import com.qihoo360.reader.ReaderApplication;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.image.ArticleListBitmapFactory;
import com.qihoo360.reader.image.BitmapFactoryBase;
import com.qihoo360.reader.subscription.Article;
import com.qihoo360.reader.support.BaseOnLinkClickListener;
import com.qihoo360.reader.support.Utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
/**
 * 文章显示内容
 */
public class ArticleDetailContentAdapter extends BaseAdapter {
    public static final int TYPE_TEXT = 0;
    public static final int TYPE_IMAGE = 1;

    protected static class ListItem {
        int type = TYPE_TEXT;
        String text = "";
        String imageUrl = "";
    }

    public static final String IMAGE_TAG = "<img>";

    private Article mArticle = null;
    private Context mContext = null;
    ArrayList<ListItem> mItems = new ArrayList<ListItem>();
    private boolean mBusy = true; // 默认不显示图片

    public ArticleDetailContentAdapter(Context context, Article article) {
        super();

        mArticle = article;
        mContext = context;
        if (article == null) {
            return;
        }

        analyzeContent(article);
    }

    public void setBusy(boolean value) {
        mBusy = value;
    }

    public boolean analyzeContent(Article article) {
        mArticle = article;
        String content = article.description;
        if (TextUtils.isEmpty(content))
            return false;

        if (Settings.isNoPicMode() == true) {
            ListItem li = new ListItem();
            li.text = content.replace(IMAGE_TAG, "");
            mItems.add(li);
        } else {
            String[] imageUrls = (!TextUtils.isEmpty(article.images360) ? article.images360 : ""/*article.images*/)
                            .split(";");

            String[] parts = content.split("\\<img\\>");
            for (int i = 0; i < parts.length; ++i) {
                if (!TextUtils.isEmpty(parts[i].replace("\n", "").trim())) {
                    ListItem item = new ListItem();
                    item.text = parts[i];
                    mItems.add(item);
                }

                if (i < imageUrls.length) {
                    String url = ArticleUtils.splitUrls(imageUrls[i]);
                    if (!TextUtils.isEmpty(url) && !url.equals("*")) {
                        ListItem item = new ListItem();
                        item.imageUrl = url;
                        item.type = TYPE_IMAGE;
                        mItems.add(item);
                    }
                }

            }

            for(int i = parts.length; i < imageUrls.length; i++) {
                String url = ArticleUtils.splitUrls(imageUrls[i]);
                if (!TextUtils.isEmpty(url) && !url.equals("*")) {
                    ListItem item = new ListItem();
                    item.imageUrl = url;
                    item.type = TYPE_IMAGE;
                    mItems.add(item);
                }
            }
        }

        return true;
    }

    public Article getArticle() {
        return mArticle;
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Object getItem(int paramInt) {
        if (paramInt < 0 || paramInt > mItems.size() - 1)
            return null;
        return mItems.get(paramInt);
    }

    @Override
    public long getItemId(int paramInt) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ListItem item = mItems.get(position);
        if (convertView == null
                        || (item.type == TYPE_TEXT && !(convertView instanceof TextView) || (item.type == TYPE_IMAGE && !(convertView instanceof ImageView)))) {
            convertView = newView(item.type);
        }

        bindView(item, convertView);
        return convertView;
    }

    @Override
    public int getItemViewType(int position) {
        if (mItems != null) {
            return mItems.get(position).type;
        } else
            return super.getItemViewType(position);
    }

    public int getViewTypeCount() {
        if (mItems != null)
            return 2;
        else
            return super.getViewTypeCount();
    }

    public View newView(int type) {
        if (type == TYPE_TEXT)
            return LayoutInflater.from(mContext).inflate(R.layout.rd_article_detail_text_parts, null);
        else
            return LayoutInflater.from(mContext).inflate(R.layout.rd_article_detail_image_parts, null);
    }

    public void bindView(ListItem data, View view) {
        if (view == null || data == null)
            return;
        if (data.type == TYPE_TEXT) {
            TextView tv_content = (TextView) view;
            Spanned content = Html.fromHtml(toHtml(data.text));
            //tv_content.setText(content);
            tv_content.setText(Utils.applyAutolink(mContext, content, 0, content.length(), new BaseOnLinkClickListener(
                            mContext), tv_content));
            tv_content.setTextSize(TypedValue.COMPLEX_UNIT_SP, ArticleDetailAdapter.mTextSize);

            if (Settings.isNightMode() == true) {
                ((TextView) view).setTextColor(ReaderApplication.getContext().getResources()
                                .getColor(R.color.rd_night_text));
            } else {
                ((TextView) view).setTextColor(ReaderApplication.getContext().getResources()
                                .getColor(R.color.rd_article_content_text));
            }
        } else if (Settings.isNoPicMode() == false) {
            ImageView imageView = (ImageView) view;
            Bitmap bitmap = ArticleListBitmapFactory.getInstance(mContext).getBitmapByUrlFromCache(data.imageUrl);
            Utils.debug("..........", data.imageUrl);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                imageView.setTag(BitmapFactoryBase.IMAGE_STATE_LOADED);
            } else {
                imageView.setTag(data.imageUrl);
                imageView.setImageResource(R.drawable.rd_article_detail_loading);

                if (!mBusy) {
                    ArticleUtils.asycSetImage(mContext, ArticleListBitmapFactory.getInstance(mContext), imageView);
                }
            }
        }
    }

    public void updateImages(AbsListView view) {
        int count = view.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = view.getChildAt(i);
            if (child != null && child instanceof ImageView) {
                if (child.getTag() == null || BitmapFactoryBase.IMAGE_STATE_LOADED.equals((String) child.getTag())) {
                    continue;
                }
                ArticleUtils.asycSetImage(mContext, ArticleListBitmapFactory.getInstance(mContext), (ImageView) child);
            }
        }
    }

    public static void changeTextSize(ViewGroup list, int size) {
        int count = list.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = list.getChildAt(i);
            if (child != null && child instanceof TextView) {
                ((TextView) child).setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
            }
        }
    }

    public static void changeIfNightMode(ListView list) {
        Resources rs = ReaderApplication.getContext().getResources();
        if (Settings.isNightMode() == true) {
            list.setBackgroundColor(rs.getColor(R.color.rd_night_bg));
        } else {
            list.setBackgroundColor(rs.getColor(R.color.rd_article_detail_bg));
        }
        rs = null;
        int count = list.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = list.getChildAt(i);
            if (child != null && child instanceof TextView) {

                if (Settings.isNightMode() == true) {
                    ((TextView) child).setTextColor(ReaderApplication.getContext().getResources()
                                    .getColor(R.color.rd_night_text));
                } else {
                    ((TextView) child).setTextColor(ReaderApplication.getContext().getResources()
                                    .getColor(R.color.rd_article_content_text));
                }
            }
        }
    }

    public static String toHtml(String content) {
        StringBuilder sb = new StringBuilder();
        String[] tmp = content.replaceAll("[\n]", "<br/>").split("\\<br\\/\\>");
        for (int i = 0; i < tmp.length; ++i) {
            if (!TextUtils.isEmpty(tmp[i].trim())) {
                sb.append(tmp[i] + (i + 1 < tmp.length ? "<br/><br/>　　" : ""));
            }
        }
        tmp = null;
        return "　　" + sb.toString();
    }
}
