package com.qihoo360.reader.ui.channels;

import com.qihoo360.browser.hip.UXHelper;
import com.qihoo360.browser.hip.UXHelperConfig;
import com.qihoo360.reader.Constants;
import com.qihoo360.reader.R;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.listener.CommonListener;
import com.qihoo360.reader.subscription.Article;
import com.qihoo360.reader.subscription.StaredArticle;
import com.qihoo360.reader.support.AnimationFactory;
import com.qihoo360.reader.support.FileUtils;
import com.qihoo360.reader.ui.ActivityBase;
import com.qihoo360.reader.ui.CommonUtil;
import com.qihoo360.reader.ui.ContextDialog;
import com.qihoo360.reader.ui.ContextDialogListener;
import com.qihoo360.reader.ui.CustomDialog;
import com.qihoo360.reader.ui.ReaderMenuContainer;
import com.qihoo360.reader.ui.ReaderPlugin;
import com.qihoo360.reader.ui.articles.ArticleDetailActivity;
import com.qihoo360.reader.ui.articles.ArticleListAdapter;
import com.qihoo360.reader.ui.articles.ArticleUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class ArticleCollectionActivity extends ActivityBase implements OnItemClickListener {
    private ListView mListView;
    private TextView mTvEmpty;

    private ArticleListAdapter mListerAdapter;
    private int mListCurrentClickPositon = -1;
    private ReaderMenuContainer mMenu;

    private Cursor mCursor;

    //private ImageView mSwitchPicModeView;

    private BroadcastReceiver mImageableReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO
            if (mListerAdapter != null) {
                mListerAdapter.notifyDataSetChanged();
            }
            //updateSwitchPicModeView();
        }
    };

    private OnScrollListener mScrollListener = new OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (scrollState != OnScrollListener.SCROLL_STATE_IDLE) {
                mListerAdapter.setBusy(true);
            } else {
                mListerAdapter.setBusy(false);
                mListerAdapter.updateImages(view);
            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.rd_article_collection);

        IntentFilter imageableFilter = new IntentFilter(Constants.READER_BROADCAST_SWITCH_WHETHER_THE_IMAGE_MODE);
        registerReceiver(mImageableReceiver, imageableFilter);

        inflateLayout();
        buildButtom();
        initialize();

        ReaderPlugin.mIsRunning = true;

        /*mSwitchPicModeView = (ImageView) findViewById(R.id.top_switch_pic_mode);
        mSwitchPicModeView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Settings.setNoPicMode(ArticleCollectionActivity.this, !Settings.isNoPicMode());
                updateSwitchPicModeView();
            }
        });*/
    }

    @Override
    public void finish() {
        if (getIntent().getBooleanExtra("back2browser", true)) {
            ReaderPlugin.mIsRunning = false;
            if(!ReaderPlugin.getBrowserActivityRunning()) {
                ReaderPlugin.bringBrowserForeground(this);
            }
        }
        super.finish();
    }

    private void inflateLayout() {
        mMenu = (ReaderMenuContainer) findViewById(R.id.menu_layout);
        mListView = (ListView) findViewById(R.id.rd_article_collection_lister);
        mListView.setOnItemClickListener(this);
        mListView.setOnScrollListener(mScrollListener);
        mListView.setLayoutAnimation(AnimationFactory.getListLayoutAnimation());
        mListView.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> paramAdapterView, View paramView, final int position,
                            long paramLong) {
                ContextDialog dialogHelper = new ContextDialog(ArticleCollectionActivity.this);
                dialogHelper.setOnItemSelectListener(new ContextDialogListener() {

                    @Override
                    public boolean onDialogItemClicked(int tag, Object tagNote) {
                        Cursor cursor = mListerAdapter.getCursor();
                        cursor.moveToPosition(position);
                        Article article = Article.inject(cursor);
                        if (article != null) {
                            article.unmarkStar(getContentResolver());
                        }

                        update();

                        return false;
                    }
                });
                dialogHelper.setTitle("收藏的文章");
                dialogHelper.addDialogEntry(R.string.rd_remove_collection, null);
                dialogHelper.show();
                return false;
            }
        });
        mTvEmpty = (TextView) findViewById(android.R.id.empty);
        mTvEmpty.setText(getString(R.string.rd_article_no_collection));
        mTvEmpty.setTextSize(18);
        mTvEmpty.setPadding(30, 0, 30, 0);
        mListView.setEmptyView(mTvEmpty);
        findViewById(R.id.menu_share_btn).setEnabled(false);
    }

    private void initialize() {
        mCursor = StaredArticle.getCursor(getContentResolver());
        if (mCursor != null && mCursor.getCount() > 0) {
            if (mListerAdapter == null) {
                mListerAdapter = new ArticleListAdapter(this, mCursor);
                View view = new View(this);
                view.setBackgroundResource(R.drawable.rd_bg_menu_level_1);
                view.setVisibility(View.INVISIBLE);
                mListView.addFooterView(view);
                mListView.setAdapter(mListerAdapter);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMenu.saveBottomBarState();
    }

    private void buildButtom() {
        mMenu.setBottomBarListener(new CommonListener() {

            @Override
            public void actionPerform(int resId) {
                if (resId == R.id.menu_back) {
                    UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Collection_Back_with_Action_Bar, 1);
                    onBackPressed();
                } else if (resId == R.id.menu_delete) {
                    UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Collection_Clear_All_Collection, 1);
                    mMenu.hideMenuOrBottomBar();
                    CustomDialog dialog = new CustomDialog(ArticleCollectionActivity.this);
                    dialog.setTitle(R.string.rd_star_clear_warning_title);
                    dialog.setMessage(R.string.rd_star_clear_warning);
                    dialog.setPositiveButton(android.R.string.ok, new OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            StaredArticle.clear(getContentResolver());
                            if (mListerAdapter != null) {
                                mListerAdapter.notifyDataSetChanged();
                            }
                            mMenu.findViewById(R.id.menu_delete).setEnabled(false);
                            dialog.dismiss();
                        }
                    });
                    dialog.setNegativeButton(android.R.string.cancel, new OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    dialog.show();
                } else if (resId == R.id.menu_share_btn) {
                    CommonUtil.shareChannel(ArticleCollectionActivity.this,
                                    getString(R.string.rd_article_my_collection));
                } else if (resId == R.id.menu_menu) {
                    onKeyUp(KeyEvent.KEYCODE_MENU, null);
                }
            }
        });
    }

    private void update() {
        mListView.post(new Runnable() {
            @Override
            public void run() {
                if (mListerAdapter != null && mListerAdapter.getCount() > 0) {
                    mMenu.findViewById(R.id.menu_delete).setEnabled(true);
                } else {
                    mMenu.findViewById(R.id.menu_delete).setEnabled(false);
                }
            }
        });
    }

    public boolean onKeyUp(int keyCode, android.view.KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_MENU) {
            mMenu.onMenuClick();
            return true;
        }

        return super.onKeyUp(keyCode, event);
    };

    public void backToList(final int position) {
        mListerAdapter.setBusy(false);
        mListView.post(new Runnable() {

            @Override
            public void run() {
                mListView.setSelection(position);
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(ArticleCollectionActivity.this, ArticleDetailActivity.class);
        intent.putExtra("channel", ArticleUtils.COLLECTION_LIST);
        intent.putExtra(ArticleUtils.LIST_POSITION, position);
        startActivityForResult(intent, this.hashCode());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == this.hashCode() && data != null) {
            mListCurrentClickPositon = data.getIntExtra(ArticleUtils.DETAIL_POSITION, -1);

            long[] unstarredArticleIndices = data.getLongArrayExtra(ArticleUtils.UNSTARRED_ARTICLE_INDICES);
            if (unstarredArticleIndices != null && unstarredArticleIndices.length > 0) {
                ArrayList<Article> list = new ArrayList<Article>(unstarredArticleIndices.length);
                Cursor cursor = mListerAdapter.getCursor();
                if (cursor.moveToFirst()) {
                    Article article = Article.inject(cursor);
                    while (article != null) {
                        for (long content_id : unstarredArticleIndices) {
                            if (content_id == article.contentid) {
                                list.add(article);
                                break;
                            }
                        }

                        if (list.size() == unstarredArticleIndices.length) {
                            break;
                        } else {
                            if (cursor.moveToNext()) {
                                article = Article.inject(cursor);
                            }
                        }
                    }
                }

                for (Article article : list) {
                    article.unmarkStar(getContentResolver());
                }
            }
        } else
            super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!FileUtils.isExternalStorageAvail()) {
            Toast.makeText(this, R.string.rd_sdcard_not_available, Toast.LENGTH_LONG).show();
        }

        if (mListCurrentClickPositon >= 0) {
            backToList(mListCurrentClickPositon);
            mListCurrentClickPositon = -1;
        }

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        update();
        //updateSwitchPicModeView();

        mMenu.onResume();
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onDestroy()
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mListerAdapter != null) {
            mListerAdapter.reset();
        }

        if (mCursor != null) {
            mCursor.close();
        }
        unregisterReceiver(mImageableReceiver);
    }

    @Override
    public void onBackPressed() {
        UXHelper.getInstance().addActionRecord(UXHelperConfig.Reader_Collection_Back_Key_OnClick, 1);
        if (mMenu.onBackBtnPressed() == true) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onUpdateNightMode() {
        super.onUpdateNightMode();
        setNightMask();

        if (Settings.isNightMode() == true) {
            findViewById(R.id.rd_list_channel_name).setBackgroundResource(
                            R.drawable.rd_article_list_container_bg_drawable_nightly);
            findViewById(R.id.rd_article_collection_list_container).setBackgroundResource(
                            R.drawable.rd_article_list_container_bg_drawable_nightly);
            mListView.setBackgroundColor(getResources().getColor(R.color.rd_night_bg));
            ((TextView) findViewById(R.id.rd_list_channel_name)).setTextColor(getResources().getColor(
                            R.color.rd_night_text));
            if (mTvEmpty != null) {
                mTvEmpty.setBackgroundResource(R.drawable.rd_article_list_container_bg_drawable_nightly);
                mTvEmpty.setTextColor(getResources().getColor(R.color.rd_night_text));
            }

            findViewById(R.id.rd_title_bottom_line).setBackgroundResource(R.drawable.rd_title_bottom_cover_nightly);
        } else {
            findViewById(R.id.rd_list_channel_name).setBackgroundResource(R.drawable.rd_article_list_container_bg_drawable);
            findViewById(R.id.rd_article_collection_list_container).setBackgroundResource(
                            R.drawable.rd_article_list_container_bg_drawable);
            mListView.setBackgroundColor(getResources().getColor(R.color.rd_white));
            ((TextView) findViewById(R.id.rd_list_channel_name))
                            .setTextColor(getResources().getColor(R.color.rd_black));
            if (mTvEmpty != null) {
                mTvEmpty.setBackgroundResource(R.drawable.rd_article_list_container_bg_drawable);
                mTvEmpty.setTextColor(getResources().getColor(R.color.rd_black));
            }
            findViewById(R.id.rd_title_bottom_line).setBackgroundResource(R.drawable.rd_title_bottom_cover_list);
        }

        if (mListerAdapter != null) {
            mListerAdapter.notifyDataSetChanged();
        }
    }

    /*private void updateSwitchPicModeView() {
        if (Settings.isNoPicMode()) {
            mSwitchPicModeView.setImageResource(R.drawable.rd_article_top_pic);
        } else {
            mSwitchPicModeView.setImageResource(R.drawable.rd_article_top_no_pic);
        }
    }*/
}
