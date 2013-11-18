package com.qihoo360.reader.ui.articles;

import com.qihoo.ilike.subscription.IlikeChannel;
import com.qihoo360.browser.hip.UXHelper;
import com.qihoo360.browser.hip.UXHelperConfig;
import com.qihoo360.reader.R;
import com.qihoo360.reader.listener.OnFillArticleResultListener;
import com.qihoo360.reader.listener.OnGetArticlesResultListener;
import com.qihoo360.reader.subscription.Article;
import com.qihoo360.reader.subscription.Channel;
import com.qihoo360.reader.support.Utils;
import com.qihoo360.reader.ui.ContextDialog;
import com.qihoo360.reader.ui.ContextDialogListener;
import com.qihoo360.reader.ui.GallaryActivity;
import com.qihoo360.reader.ui.articles.ArticleDetailContentAdapter.ListItem;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CursorAdapter;
import android.widget.HeaderViewListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

public class ArticleDetailAdapter extends CursorAdapter {
    public interface ArticleDownloadListener {
        public void articleDownloaded(int position, Article article);
    }

    private ArticleDownloadListener mArticleDownloadListener = null;

    public static final String TAG = "ArticleDetailAdapter";
    protected Context mContext;
    public static final int TEXT_SIZE_SMALLER = 12;
    public static final int TEXT_SIZE_SMALL = 14;
    public static final int TEXT_SIZE_MIDDLE = 17;
    public static final int TEXT_SIZE_LARGE = 20;
    public static final int TEXT_SIZE_LARGER = 32;
    protected static int mTextSize = TEXT_SIZE_MIDDLE;
    private OnClickRunnable mOnClickRunnable;
    private Handler mMainHandler;
    // values{0,1,2,3}
    private int mInitializedCount = 0;
    private boolean mReachedMaxFontSize = false;
    private boolean mReachedMinFontSize = false;
    private LinkedList<Runnable> mAsynLoadRunnable = null;
    /**
     * mFirstCurrentIsLoaded 变量仅限 <strong>首次 bindView 时</strong>，<br/>此后值为 ture 不变
     */
    private boolean mFirstCurrentIsLoaded = false;
    //private boolean mIsMyCollection = false;
    protected Channel mChannel = null;
    private Handler mHandler = null;

    // ignore the single tap once double tap is detected!!!
    private boolean mIgnoreItemClick = false;
    public ConcurrentHashMap<View, View> mHeaderViewMap = new ConcurrentHashMap<View, View>();

    public ArticleDetailAdapter(Context context, Cursor c) {
        super(context, c, false);
        this.mContext = context;
    }

    public void reference(Channel channel, Handler handler, boolean isCollection) {
        mChannel = channel;
        mHandler = handler;
        //mIsMyCollection = isCollection;
    }

    // detail-view new order: current -> next -> previous
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        mInitializedCount++;
        View view = LayoutInflater.from(context).inflate(R.layout.rd_article_detail_view, null);
        View headerView = inflateListHeaderView(context);
        ListView contentList = (ListView) view.findViewById(R.id.rd_article_detail_content);
        contentList.addHeaderView(headerView);

        View footer = inflateListFooterView(context);
        /*final Context fContext = context;
        footer.findViewById(R.id.rd_article_detail_list_footer_src).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getTag() != null) {
                    String link = (String) v.getTag();
                    if (!TextUtils.isEmpty(link)) {
                        ReaderPlugin.openLinkWithBrowser(fContext, link);
                    } else {
                        Toast.makeText(fContext, "无效链接", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });*/
        contentList.addFooterView(footer);
        mHeaderViewMap.put(view, headerView);
        return view;
    }

    protected View inflateListHeaderView (Context context) {
        return LayoutInflater.from(context).inflate(R.layout.rd_article_detail_list_header, null);
    }

    protected View inflateListFooterView (Context context) {
        View footer = LayoutInflater.from(context).inflate(R.layout.rd_article_detail_list_footer, null);
        int padding = (int) context.getResources().getDimension(R.dimen.rd_article_detaile_footer_padding);
        footer.setPadding(padding, padding, padding,
                        (int) context.getResources().getDimension(R.dimen.rd_main_padding_bottom));
        return footer;
    }

    @Override
    public void bindView(final View view, final Context context, final Cursor cursor) {
        final int pos = cursor.getPosition();
        final Article article = Article.inject(cursor);

        if (view.getTag() != null && view.getTag() instanceof Article) {
            ((Article) view.getTag()).stopFill();
            view.setTag(null);
        }

        View listHeader = mHeaderViewMap.get(view);
        if (listHeader != null) {
            loadHeaderViewContext(listHeader, article, pos);
        }

        ListView listContent = (ListView) view.findViewById(R.id.rd_article_detail_content);
        listContent.setTag(article.contentid);
        final ArticleDetailContentAdapter adapter = new ArticleDetailContentAdapter(context,
                        article.isDownloaded ? article : null);
        final View processView = view.findViewById(R.id.rd_article_detail_loading_bar);
        listContent.setAdapter(adapter);
        listContent.setOnScrollListener(new OnScrollListener() {
            private int mLatestState = 0;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == OnScrollListener.SCROLL_STATE_FLING) {
                    adapter.setBusy(true);
                } else {
                    adapter.setBusy(false);
                    if ((scrollState == OnScrollListener.SCROLL_STATE_IDLE && mLatestState != OnScrollListener.SCROLL_STATE_TOUCH_SCROLL)
                                    || (scrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL && mLatestState == OnScrollListener.SCROLL_STATE_FLING)) {
                        adapter.updateImages(view);
                    }
                }

                mLatestState = scrollState;
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });
        processView.setVisibility(View.GONE);
        if (!article.isDownloaded) {
            asynReloadContent(article, view, listContent, processView, adapter, pos, cursor.isLast(), false);
        } else {
            loadFooterViewContent (listContent, article, false);

            mFirstCurrentIsLoaded = true;
            Utils.debug(TAG, "Title: " + article.title + " | => Had Download....");
        }

        listContent.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long paramLong) {
                if (mIgnoreItemClick) {
                    return;
                }

                final ListItem li = (ListItem) adapter.getItem(position - 1);
                if (position > 0 && li != null) {
                    // plus 1 for the header view
                    if (li != null && li.type == ArticleDetailContentAdapter.TYPE_IMAGE) {
                        if (mMainHandler == null)
                            mMainHandler = view.getHandler();
                        if (mOnClickRunnable == null)
                            mOnClickRunnable = new OnClickRunnable();
                        else
                            mMainHandler.removeCallbacks(mOnClickRunnable);
                        mOnClickRunnable.setPosAndUrl(pos, li.imageUrl);
                        mMainHandler.postDelayed(mOnClickRunnable,
                                ViewConfiguration.getDoubleTapTimeout());
                        UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Article_Detail_Image_OnClick, 1);
                    }
                }
            }

        });

        listContent.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                if (position <= 0)
                    return true;
                ListView list = (ListView) parent;
                ArticleDetailContentAdapter madapter = (ArticleDetailContentAdapter) (((HeaderViewListAdapter) list
                                .getAdapter()).getWrappedAdapter());

                ListItem li = (ListItem) madapter.getItem(position - 1);
                if (li == null || li.type != ArticleDetailContentAdapter.TYPE_IMAGE)
                    return false;
                ContextDialog dialogHelper = new ContextDialog(context);
                dialogHelper.setOnItemSelectListener(new ContextDialogListener() {

                    @Override
                    public boolean onDialogItemClicked(int tag, Object tagNote) {
                        ArticleDetailContentAdapter adapter = (ArticleDetailContentAdapter) tagNote;
                        /*if (tag == R.string.rd_select_text) {
                            Intent intent = new Intent(context, SelectTextActivity.class);
                            intent.putExtra(SelectTextActivity.INTENT_PARAM_TEXT_CONTENT,
                                            adapter.getArticle().description);
                            context.startActivity(intent);
                            UXHelper.getInstance().addActionRecord(
                                            UXHelperConfig.Reader_Article_Detail_Long_Copy_Article, 1);
                        } else */if (tag == R.string.rd_view_image) {
                            ListItem li = (ListItem) adapter.getItem(position - 1);
                            if (li != null) {
                                ViewImage(pos, li.imageUrl);
                                UXHelper.getInstance().addActionRecord(
                                                UXHelperConfig.Reader_Article_Detail_Image_OnClick, 1);
                            }
                        }

                        return true;
                    }
                });
                dialogHelper.setTitle("查看");
                /*if (li.type == ArticleDetailContentAdapter.TYPE_TEXT) {
                    dialogHelper.addDialogEntry(R.string.rd_select_text, madapter);
                    dialogHelper.show();
                } else if (li.type == ArticleDetailContentAdapter.TYPE_IMAGE) {*/
                    dialogHelper.addDialogEntry(R.string.rd_view_image, madapter);
                    dialogHelper.show();
                /*}*/
                return false;
            }
        });
    }

    protected void loadHeaderViewContext(View header, Article article, int pos) {
        final TextView titleView = (TextView) header.findViewById(R.id.rd_article_title);
        final TextView timeView = (TextView) header.findViewById(R.id.rd_article_time);
        // View titleBar = listHeader.findViewById(R.id.rd_article_detail_title_bar);
        if (article == null) {
            titleView.setText("");
        } else {
            header.findViewById(R.id.rd_article_offline_tag).setVisibility(
                    article.isOfflined ? View.VISIBLE : View.GONE);

            titleView.setText(Html.fromHtml(article.title));
            String time = ArticleUtils.formatTime(article.pubdate * 1000);
            timeView.setText(time);
        }
    }

    protected void loadFooterViewContent (ListView list, Article article, boolean newDownload) {
      //list.findViewById(R.id.rd_article_detail_list_footer_src).setTag(article.link);
        ((TextView) list.findViewById(R.id.rd_article_detail_list_footer_author)).setText(TextUtils
                        .isEmpty(article.author) ? "" : ("来源：" + article.author.toLowerCase()));
    }

    /**
     * 异步加载文章正文
     * @param article
     * @param view
     * @param listContent
     * @param processView
     * @param adapter
     * @param cursor
     * @param isManualRefresh //是否是用户手动请求刷新？
     */
    private void asynReloadContent(final Article article, // current article object
                    final View view, // current detail-view
                    final ListView listContent, // article content list-view
                    final View processView, // content loading process-bar
                    final ArticleDetailContentAdapter adapter, // content list-view adapter
                    final int pos,
                    final boolean isLast,
                    final boolean isManualRefresh) {
        processView.setVisibility(View.VISIBLE);
        processView.findViewById(R.id.rd_article_detail_progressBar).setVisibility(View.VISIBLE);
        processView.findViewById(R.id.rd_article_detail_load_failure_view).setVisibility(View.GONE);
        listContent.findViewById(R.id.rd_article_detail_list_footer).setVisibility(View.GONE);
        if (mInitializedCount == 1) {
            /*
             * 第一次bindView的时候执行
             */
            view.setTag(article);
            article.fillAsync(mContext.getContentResolver(), new OnFillArticleResultListener() {

                @Override
                public void onFailure(int error) {
                    mFirstCurrentIsLoaded = true;
                    if (error == Article.ERROR_NETWORK_ERROR) {
                        processView.findViewById(R.id.rd_article_detail_progressBar).setVisibility(View.GONE);
                        processView.findViewById(R.id.rd_article_detail_load_failure_view).setVisibility(View.VISIBLE);
                        processView.findViewById(R.id.rd_article_detail_load_refresh).setOnClickListener(
                                        new OnClickListener() {

                                            @Override
                                            public void onClick(View v) {
                                                Utils.debug(TAG, "Manual Refresh | => Title: " + article.title);
                                                asynReloadContent(article, view, listContent, processView, adapter,
                                                        pos, isLast, true);
                                            }
                                        });

                    }

                    if (mAsynLoadRunnable != null){
                        for (Runnable runnable : mAsynLoadRunnable){
                            view.post(runnable);
                            if(error == Article.ERROR_USER_CANCELLED)
                                break;
                        }

                        mAsynLoadRunnable.clear();
                        mAsynLoadRunnable = null;
                    }

                    view.setTag(null);
                    Utils.debug(TAG, "Title: " + article.title + " | => Loading Failure...");
                }

                @Override
                public void onCompletion() {
                    mFirstCurrentIsLoaded = true;

                    if (mChannel != null)
                        changeCursor(mChannel.getFullCursor(mContext.getContentResolver()));

                    processView.setVisibility(View.GONE);
                    loadFooterViewContent(listContent, article, true);
                    if (adapter.analyzeContent(article)) {
                        listContent.findViewById(R.id.rd_article_detail_list_footer).setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }

                    if(mArticleDownloadListener != null) {
                        mArticleDownloadListener.articleDownloaded(pos, article);
                    }

                    if (mAsynLoadRunnable != null){
                        for (Runnable runnable : mAsynLoadRunnable) {
                            view.post(runnable);
                        }
                        mAsynLoadRunnable.clear();
                        mAsynLoadRunnable = null;
                    }

                    view.setTag(null);
                    Utils.debug(TAG, "Title: " + article.title + " | => Loading Success...");
                }
            });
        } else if (!mFirstCurrentIsLoaded && mInitializedCount < 4) {
            /*
             * 第二、三次bindView且第一次bindView中的异步请求未完成的时候执行
             */
            if (mAsynLoadRunnable == null)
                mAsynLoadRunnable = new LinkedList<Runnable>();
            mAsynLoadRunnable.add(new Runnable() {
                @Override
                public void run() {
                    view.setTag(article);
                    article.fillAsync(mContext.getContentResolver(),
                                    articleResultListenerFactory(article, view, listContent, processView, adapter,
                                            pos, isLast, isManualRefresh));
                }
            });
        } else {
            view.setTag(article);
            article.fillAsync(mContext.getContentResolver(),
                            articleResultListenerFactory(article, view, listContent, processView, adapter,
                                    pos, isLast, isManualRefresh));
        }
    }

    /**
     *
     * @param article
     * @param view
     * @param listContent
     * @param processView
     * @param adapter
     * @param isLast
     * @param cursor
     * @param isManualRefresh //是否是用户手动请求刷新？
     * @return
     */
    private OnFillArticleResultListener articleResultListenerFactory(final Article article, // current article object
                    final View view, // current detail view
                    final ListView listContent, // article content list-view
                    final View processView, // content loading process-bar
                    final ArticleDetailContentAdapter adapter, // content list-view adapter
                    final int pos,
                    final boolean isLast, // current is the adapter's last item
                    final boolean isManualRefresh) {
        return new OnFillArticleResultListener() {

            @Override
            public void onFailure(int error) {
                if (error == Article.ERROR_NETWORK_ERROR) {
                    processView.findViewById(R.id.rd_article_detail_progressBar).setVisibility(View.GONE);
                    processView.findViewById(R.id.rd_article_detail_load_failure_view).setVisibility(View.VISIBLE);
                    processView.findViewById(R.id.rd_article_detail_load_refresh).setOnClickListener(
                                    new OnClickListener() {

                                        @Override
                                        public void onClick(View v) {
                                            Utils.debug(TAG, "Manual Refresh | => Title: "
                                                    + article.title);
                                            asynReloadContent(article, view, listContent,
                                                    processView, adapter, pos, isLast, true);
                                        }
                                    });
                    Utils.debug(TAG, "Title: " + article.title + " | => Loading Failure...");

                    /*if (mHandler != null && isManualRefresh)
                        mHandler.sendEmptyMessage(ArticleUtils.MSG_ARTICLE_DETAIL_VIEW_SHOULD_RELOAD);*/
                }
                view.setTag(null);
         }

            @Override
            public void onCompletion() {
                if (mChannel != null) {
                    changeCursor(mChannel.getFullCursor(mContext.getContentResolver()));
                }

                processView.setVisibility(View.GONE);
                loadFooterViewContent(listContent, article, true);
                if (adapter.analyzeContent(article)) {
                    listContent.findViewById(R.id.rd_article_detail_list_footer).setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();
                }

                if (isLast && mContext instanceof OnGetArticlesResultListener) {
                    Utils.debug(TAG, "Channel Name:" + mChannel.channel + "=>Pre-Loading more...");
                    if(mContext instanceof ArticleDetailActivity)
                        ((ArticleDetailActivity)mContext).onStartLoad();
                } else if (isLast) {
                    Utils.debug(TAG, "Error[IsLast: " + isLast + "| Class Convert: "
                                    + (mContext instanceof OnGetArticlesResultListener));
                }

                if (mHandler != null && isManualRefresh) {
                    mHandler.sendEmptyMessage(ArticleUtils.MSG_ARTICLE_DETAIL_VIEW_SHOULD_RELOAD);
                }

                if(mArticleDownloadListener != null) {
                    mArticleDownloadListener.articleDownloaded(pos, article);
                }

                view.setTag(null);
                Utils.debug(TAG, "Title: " + article.title + " | => Loading Success...");
            }
        };
    }

    @Override
    public Object getItem(int position) {
        Object obj = super.getItem(position);
        if (obj != null && obj instanceof Cursor)
            return Article.inject((Cursor) obj);
        return null;
    }

    public boolean changeTextSize(ListView list, boolean increase) {
        if (list == null) {
            return false;
        }

        if (increase) {
            mReachedMinFontSize = false;

            if (mTextSize == TEXT_SIZE_LARGER) {
                if (!mReachedMaxFontSize) {
                    Toast.makeText(mContext, "已经是最大字体", Toast.LENGTH_SHORT).show();
                    mReachedMaxFontSize = true;
                }
                return false;
            }
        } else {
            mReachedMaxFontSize = false;

            if (mTextSize == TEXT_SIZE_SMALLER) {
                if (!mReachedMinFontSize) {
                    Toast.makeText(mContext, "已经是最小字体", Toast.LENGTH_SHORT).show();
                    mReachedMinFontSize = true;
                }
                return false;
            }
        }

        int targetTextSize = mTextSize;
        if (increase) {
            targetTextSize++;
            Utils.debug("info", "-----------large");
        } else {
            targetTextSize--;
            Utils.debug("info", "-----------small");
        }

        ArticleDetailContentAdapter.changeTextSize(list, targetTextSize);

        mTextSize = targetTextSize;
        return true;
    }

    public void changeTextSize(ListView list, int targetFont) {
        if (list == null) {
            return;
        }

        int targetTextSize = targetFont;
        ArticleDetailContentAdapter.changeTextSize(list, targetTextSize);
        mTextSize = targetTextSize;
    }

    private class OnClickRunnable implements Runnable {
        private int position;
        private String url;

        public void setPosAndUrl(int pos, String url) {
            this.position = pos;
            this.url = url;
        }

        @Override
        public void run() {
            ViewImage(position, url);
        }
    }

    private void ViewImage(int position, String url) {
        Article article = (Article) getItem(position);
        Intent intent = new Intent(mContext, GallaryActivity.class);
        intent.putExtra("title", article.title);
        intent.putExtra("images360", article.images360);
        intent.putExtra("cur_url", url);

        String shareCategory = mChannel instanceof IlikeChannel ? mContext
                .getString(R.string.i_like_share_category) : "“" + mChannel.title + "”频道";
        intent.putExtra("category", shareCategory);
        intent.putExtra("liked_article", article.star == Article.STAR_APPEARANCE);
        mContext.startActivity(intent);
    }

    public void disableSingalTap() {
        mIgnoreItemClick = true;
        if (mMainHandler != null && mOnClickRunnable != null) {
            mMainHandler.removeCallbacks(mOnClickRunnable);
        }
    }

    public void enbaleSingleTap() {
        mIgnoreItemClick = false;
    }

    public void changeIfNightMode(ListView list) {
        ArticleDetailContentAdapter.changeIfNightMode(list);
    }

    public void setArticleDownloadListener(ArticleDownloadListener listener) {
        mArticleDownloadListener = listener;
    }

}
