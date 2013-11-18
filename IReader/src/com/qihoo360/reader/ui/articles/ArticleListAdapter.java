
package com.qihoo360.reader.ui.articles;

import com.qihoo360.reader.R;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.image.ArticleListBitmapFactory;
import com.qihoo360.reader.image.BitmapFactoryBase;
import com.qihoo360.reader.image.SizeLimitedArticleBitmapFactory;
import com.qihoo360.reader.subscription.Article;
import com.qihoo360.reader.subscription.reader.RssArticle;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;

public class ArticleListAdapter extends CursorAdapter {
    private Context context;
    private boolean mBusy = false;
    private boolean mIsInitState = true;
    private int mHeaderItemPosition = -1;
    private HashMap<Long, String> mIdUrl;
    private HashMap<Long, String> mIdTime;
    private ItemHold item;

    public ArticleListAdapter(Context context, Cursor c) {
        super(context, c);
        this.context = context;
    }

    public ArticleListAdapter(Context context, Cursor c, boolean autoRefresh) {
        super(context, c, autoRefresh);
        this.context = context;
    }

    public void setState(boolean isInitState) {
        mIsInitState = isInitState;
    }

    public boolean isInitState() {
        return mIsInitState;
    }

    public int getHeaderItemPosition() {
        return mHeaderItemPosition;
    }

    public void setBusy(boolean value) {
        mBusy = value;
    }

    public void reset() {
        mIsInitState = true;
        mHeaderItemPosition = -1;
        mBusy = false;
        if (mIdUrl != null) {
            mIdUrl.clear();
        }

        if (mIdTime != null) {
            mIdTime.clear();
        }
    }

    public void destory() {
        if (mIdUrl != null) {
            mIdUrl.clear();
            mIdUrl = null;
        }

        if (mIdTime != null) {
            mIdTime.clear();
            mIdTime = null;
        }
    }

    public void bindHeaderView(View header, String url, String title, int cursorPosition) {
        if (header != null && mIsInitState == true) {
            ImageView imageView = (ImageView) header
                    .findViewById(R.id.rd_article_list_header_iamgeview);
            ((TextView) header.findViewById(R.id.rd_article_list_header_title)).setText(title);
            imageView.setTag(url);
            ArticleUtils.asycSetImage(context, ArticleListBitmapFactory.getInstance(context),
                    imageView);
            mHeaderItemPosition = cursorPosition;
            mIsInitState = false;
        }
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        Article article = Article.injectAbstract(cursor);
        item = (ItemHold) view.getTag();

        String url = null;
        if (mIdUrl != null && mIdUrl.containsKey(article.contentid)) {
            url = mIdUrl.get(article.contentid);
        } else {
            url = ArticleUtils.getFirstValidImageUrlForThumbView(article.images360);
            if (mIdUrl == null)
                mIdUrl = new HashMap<Long, String>();
            mIdUrl.put(article.contentid, url);
        }

        if (Settings.isNoPicMode() == true || TextUtils.isEmpty(url)) {
            item.containerView.setVisibility(View.GONE);
        } else {
            ImageView imageView = item.snapshot;
            imageView.setImageBitmap(null);
            imageView.setTag(url);

            if (item.containerView.getVisibility() == View.GONE) {
                item.containerView.setVisibility(View.VISIBLE);
            }

            Bitmap bitmap = SizeLimitedArticleBitmapFactory.getInstance(context)
                    .getBitmapByUrlFromCache(url);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                imageView.setTag(BitmapFactoryBase.IMAGE_STATE_LOADED);
            } else if (!mBusy) {
                ArticleUtils.asycSetImage(context,
                        SizeLimitedArticleBitmapFactory.getInstance(context), imageView);
            }
        }

        /*
         * 兼容旧版本： 旧版本中文章摘要为description的前一百个字符，在新版本中文章摘要为单独的 “abstractContent”字段
         */
        String time = null;
        if (mIdTime != null && mIdTime.containsKey(article.contentid)) {
            time = mIdTime.get(article.contentid);
        } else {
            if (mIdTime == null)
                mIdTime = new HashMap<Long, String>();
            time = ArticleUtils.formatTime(article.pubdate * 1000);
            mIdTime.put(article.contentid, time);
        }

        TextView content = item.content;
        TextView titleView = item.title;
        item.offline.setVisibility(article.isOfflined ? View.VISIBLE : View.GONE);
        content.setText(time);
        titleView.setText(Html.fromHtml(article.title));

        // 夜间模式判断
        if (Settings.isNightMode()) {
            if (article.read == RssArticle.READ)
                titleView.setTextColor(context.getResources().getColor(R.color.rd_night_text));
            else
                titleView.setTextColor(context.getResources().getColor(R.color.rd_night_title));
            content.setTextColor(context.getResources().getColor(R.color.rd_night_text));
            view.setBackgroundResource(R.drawable.rd_article_list_item_night);
        } else {
            if (article.read == RssArticle.READ)
                titleView.setTextColor(context.getResources().getColor(R.color.rd_article_content));
            else
                titleView.setTextColor(context.getResources().getColor(R.color.rd_article_title));
            content.setTextColor(context.getResources().getColor(R.color.rd_article_content));
            view.setBackgroundResource(R.drawable.rd_article_list_item);
        }
    }

    @Override
    public Object getItem(int position) {
        Object obj = super.getItem(position);
        if (obj != null && obj instanceof Cursor) {
            return RssArticle.inject((Cursor) obj);
        } else {
            return null;
        }
    }

    public void updateImages(AbsListView view) {
        if (Settings.isNoPicMode() == true)
            return;
        int count = view.getChildCount();
        final int last = view.getLastVisiblePosition();
        for (int i = 0; i < count && i < last; i++) {
            View child = view.getChildAt(i);
            ImageView imageView = (ImageView) child.findViewById(R.id.rd_list_item_snapshot);
            if (imageView != null && imageView.getTag() != null
                    && imageView.getVisibility() == View.VISIBLE) {
                ArticleUtils.asycSetImage(context,
                        SizeLimitedArticleBitmapFactory.getInstance(context), imageView);
            }
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.rd_article_list_item, null);
        ItemHold items = new ItemHold();
        items.title = (TextView) view.findViewById(R.id.rd_list_item_article_title);
        items.content = (TextView) view.findViewById(R.id.rd_list_item_article_content);
        items.offline = view.findViewById(R.id.rd_list_item_article_offline_tag);
        items.containerView = view.findViewById(R.id.rd_list_item_snap_container);
        items.snapshot = (ImageView) view.findViewById(R.id.rd_list_item_snapshot);
        view.setTag(items);
        return view;
    }

    private class ItemHold {
        TextView title;
        TextView content;
        View offline;
        View containerView;
        ImageView snapshot;
    }
}
