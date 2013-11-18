/**
 *
 */

package com.qihoo360.reader.ui.offline;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.qihoo360.reader.R;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.listener.OnOfflineQueueListener;
import com.qihoo360.reader.offline.OfflineQueue;
import com.qihoo360.reader.push.PushManager;
import com.qihoo360.reader.subscription.reader.RssSubscribedChannel;
import com.qihoo360.reader.support.FileUtils;
import com.qihoo360.reader.support.NetUtils;
import com.qihoo360.reader.ui.ActivityBase;
import com.qihoo360.reader.ui.ReaderPlugin;
import com.qihoo360.reader.ui.offline.OfflineListAdapter.ViewHolder;

/**
 * 离线下载页面
 *
 * @author Jiongxuan Zhang
 */
public class OfflineActivity extends ActivityBase {

    private Button mDownloadNowButton;
    private Button mStopNowButton;

    private Button mSelectAllButton;
    private Button mBackButton;
    ListView mListView;
    private OfflineListAdapter mAdapter;
    private Cursor mCursor;

    private OnOfflineQueueListener sQueueListener = new OnOfflineQueueListener() {

        @Override
        public void onQueueStart() {
            refreshDownloadButton();
            refreshSelectAllButton();
            mAdapter.setAllCheckboxEnable(false);
        }

        @Override
        public void onQueueComplete() {
            refreshDownloadButton();
            refreshSelectAllButton();
            mAdapter.setAllCheckboxEnable(true);

            if (Settings.isEnableOfflineWithoutWifi()) {
                // 相当于如果下次用户仍处于3G环境下，则还是需要确认
                Settings.setEnableOfflineWithoutWifi(false);
            }
        }
    };
    private boolean mBackToDesktop;

    /**
     * 打开Offline对话框
     *
     * @param context
     */
    public static void startActivity(final Context context,
            boolean backToDesktop) {
        if (context == null) {
            throw new IllegalArgumentException("context is null");
        }

        if (!ensureOkay(context)) {
            return;
        }

        startActivityFor(context, backToDesktop);
    }

    private static boolean ensureOkay(final Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context is null");
        }

        if (!FileUtils.isExternalStorageAvail()) {
            Toast.makeText(context, R.string.rd_offline_sdcard_not_available,
                    Toast.LENGTH_LONG).show();
            return false;
        }

        if (!NetUtils.isNetworkAvailable()) {
            Toast.makeText(context, R.string.rd_article_network_expception,
                    Toast.LENGTH_LONG).show();
            return false;
        }

        if (RssSubscribedChannel.getSubscribedCount() == 0) {
            Toast.makeText(context, R.string.rd_offline_no_subscribe_channel,
                    Toast.LENGTH_LONG).show();
            return false;
        }

        // 如果此时正在Push，为防止冲突，我们应禁止Push，然后再开始离线下载
        if (PushManager.isPushing()) {
            PushManager.stop(context);
        }

        return true;
    }

    private static void startActivityFor(Context context, boolean backToDesktop) {
        Intent intent = new Intent(context, OfflineActivity.class);
        intent.putExtra(BACK_TO_DESKTOP, backToDesktop);
        context.startActivity(intent);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.qihoo360.reader.ui.ActivityBase#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rd_offline_page);

        mListView = (ListView) findViewById(R.id.offline_list);

        mCursor = RssSubscribedChannel
                .getCursorWithoutFixed(getContentResolver());
        mAdapter = new OfflineListAdapter(this, mListView, mCursor);

        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                if (!OfflineQueue.isRunning()) {
                    ViewHolder viewHolder = (ViewHolder) view.getTag();
                    if (mAdapter.isItemChecked(viewHolder.postition)) {
                        mAdapter.deselectItem(viewHolder.postition,
                                viewHolder.offlineTask);
                        viewHolder.selectToOffline.setChecked(false);
                    } else {
                        mAdapter.selectItem(viewHolder.postition,
                                viewHolder.offlineTask);
                        viewHolder.selectToOffline.setChecked(true);
                    }
                }
            }
        });

        mListView.setOnScrollListener(new OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                OfflineListRefreshController
                        .setScrolling(scrollState != OnScrollListener.SCROLL_STATE_IDLE);
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                    int visibleItemCount, int totalItemCount) {

            }
        });

        mSelectAllButton = (Button) findViewById(R.id.select_all);
        mSelectAllButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!mAdapter.isSelectedAll()) {
                    mAdapter.selectAll(getContentResolver());
                } else {
                    mAdapter.deselectAll(getContentResolver());
                }
            }
        });

        mDownloadNowButton = (Button) findViewById(R.id.download_now);
        mDownloadNowButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mAdapter.start();
            }
        });

        mStopNowButton = (Button) findViewById(R.id.stop_now);
        mStopNowButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mAdapter.stop();
            }
        });

        mBackButton = (Button) findViewById(R.id.back);
        mBackButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ReaderPlugin.mIsRunning = true;

        Intent intent = getIntent();
        if (intent != null) {
            mBackToDesktop = getIntent()
                    .getBooleanExtra(BACK_TO_DESKTOP, false);
        }
    }

    void refreshDownloadButton() {
        if (OfflineQueue.isRunning()) {
            mDownloadNowButton.setVisibility(View.GONE);
            mStopNowButton.setVisibility(View.VISIBLE);
        } else {
            mDownloadNowButton.setVisibility(View.VISIBLE);
            mStopNowButton.setVisibility(View.GONE);

            if (mAdapter.hasSelected()) {
                mDownloadNowButton.setTextColor(getResources().getColor(
                        R.color.rd_white));
                mDownloadNowButton.setEnabled(true);
            } else {
                mDownloadNowButton.setTextColor(getResources().getColor(
                        R.color.rd_gray));
                mDownloadNowButton.setEnabled(false);
            }
        }
    }

    void refreshSelectAllButton() {
        mSelectAllButton.setEnabled(!OfflineQueue.isRunning());
    }

    void refreshSelectAllText() {
        if (mAdapter.isSelectedAll()) {
            // 此时应为“取消全选”
            mSelectAllButton.setText(R.string.rd_deselect_all);
        } else {
            // 此时应为“全选”
            mSelectAllButton.setText(R.string.rd_select_all);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause() {
        super.onPause();

        // 因为Activity此时已失去焦点，故不再通过Listener刷新Adapter
        OfflineListRefreshController.setAdapter(null);
        OfflineQueue.setQueueListener(null);

        mAdapter.saveSelectedItem();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.qihoo360.reader.ui.ActivityBase#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();

        OfflineListRefreshController.setAdapter(mAdapter);
        OfflineQueue.setQueueListener(sQueueListener);
        mAdapter.initSelectedItem();

        refreshSelectAllText();
        refreshSelectAllButton();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.qihoo360.reader.ui.ActivityBase#onDestroy()
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mCursor != null) {
            mCursor.close();
        }

        mAdapter.resetStatus();

        ReaderPlugin.mIsRunning = false;

        if (mBackToDesktop && !ReaderPlugin.getBrowserActivityRunning()) {
            ReaderPlugin.bringBrowserForeground(this);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.qihoo360.reader.ui.ActivityBase#onUpdateNightMode()
     */
    @Override
    public void onUpdateNightMode() {
        super.onUpdateNightMode();

        setBackground(R.id.main);
        refreshDownloadButton();
        refreshSelectAllButton();
        refreshSelectAllText();
    }
}
