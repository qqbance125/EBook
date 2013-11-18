/**
 *
 */

package com.qihoo360.reader.ui.offline;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.qihoo360.reader.R;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.data.Tables;
import com.qihoo360.reader.image.BitmapFactoryBase;
import com.qihoo360.reader.image.ChannelBitmapFacotry;
import com.qihoo360.reader.offline.OfflineNotificationBar;
import com.qihoo360.reader.offline.OfflineQueue;
import com.qihoo360.reader.offline.OfflineTask;
import com.qihoo360.reader.subscription.Channel;
import com.qihoo360.reader.subscription.SubscribedChannel;
import com.qihoo360.reader.subscription.reader.RssSubscribedChannel;
import com.qihoo360.reader.support.AsyncTask;
import com.qihoo360.reader.support.NetUtils;
import com.qihoo360.reader.support.UiUtils;
import com.qihoo360.reader.support.Utils;
import com.qihoo360.reader.ui.CustomDialog;

/**
 * 离线列表的适配器
 *
 * @author Jiongxuan Zhang
 */
public class OfflineListAdapter extends CursorAdapter {
    private final SparseBooleanArray mSelectedItemList;
    private final List<OfflineTask> mSelectedOfflineTaskList;

    private LayoutInflater mInflater;
    Context mContext;

    public OfflineListAdapter(Context context, ListView listView, Cursor c) {
        super(context, c);
        mContext = context;
        mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mSelectedItemList = new SparseBooleanArray(c.getCount());
        mSelectedOfflineTaskList = new ArrayList<OfflineTask>(c.getCount());
    }

    void initSelectedItem() {
        List<RssSubscribedChannel> list = RssSubscribedChannel
                .getWithoutFixed(mContext.getContentResolver());
        for (int i = 0; i < list.size(); i++) {
            SubscribedChannel subscribedChannel = list.get(i);
            if (subscribedChannel.offline) {
                Channel channel = subscribedChannel.getChannel();
                if (channel != null) {
                    selectItem(i, channel.offline());
                }
            }
        }
    }

    void saveSelectedItem() {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                List<RssSubscribedChannel> list = RssSubscribedChannel
                        .getWithoutFixed(mContext.getContentResolver());
                List<Integer> yes = new ArrayList<Integer>();
                List<Integer> no = new ArrayList<Integer>();
                for (int i = 0; i < list.size(); i++) {
                    SubscribedChannel subscribedChannel = list.get(i);
                    boolean selected = mSelectedItemList.get(i);
                    if (!subscribedChannel.offline && selected) {
                        yes.add(subscribedChannel._id);
                    } else if (subscribedChannel.offline && !selected) {
                        no.add(subscribedChannel._id);
                    }
                }

                ContentValues values = new ContentValues();
                if (!yes.isEmpty()) {
                    values.put(Tables.Subscriptions.OFFLINE, 1);
                    mContext.getContentResolver().update(
                            Tables.Subscriptions.CONTENT_URI, values,
                            getWhereString(yes), null);
                }

                if (!no.isEmpty()) {
                    values.put(Tables.Subscriptions.OFFLINE, 0);
                    mContext.getContentResolver().update(
                            Tables.Subscriptions.CONTENT_URI, values,
                            getWhereString(no), null);
                }

                return null;
            }

            private String getWhereString(List<Integer> ids) {
                StringBuilder sb = new StringBuilder();
                sb.append("_id in (");
                for (Integer id : ids) {
                    sb.append(id);
                    sb.append(',');
                }
                sb.deleteCharAt(sb.length() - 1);
                sb.append(')');

                return sb.toString();
            }
        }.execute();
    }

    void resetStatus() {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                if (!OfflineQueue.isRunning()) {
                    for (OfflineTask offlineTask : mSelectedOfflineTaskList) {

                        switch (offlineTask.getStatus()) {
                        case COMPLETED:
                        case ERROR:
                        case USER_CANCEL:
                            offlineTask.resetStatus();
                            break;
                        }
                    }
                }
                return null;
            }
        };
        task.execute();
    }

    public Context getContext() {
        return mContext;
    }

    /*
     * (non-Javadoc)
     *
     * @see android.widget.CursorAdapter#newView(android.content.Context,
     * android.database.Cursor, android.view.ViewGroup)
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = mInflater.inflate(R.layout.rd_offline_list_item, null);

        ViewHolder viewHolder = new ViewHolder();
        viewHolder.channelImage = (ImageView) view
                .findViewById(R.id.channel_icon);
        viewHolder.channelTitle = (TextView) view
                .findViewById(R.id.channel_title);
        viewHolder.offlineTime = (TextView) view
                .findViewById(R.id.offline_time);
        viewHolder.offlineStatus = (TextView) view
                .findViewById(R.id.offline_status_text);
        viewHolder.offlineProgress = (ProgressBar) view
                .findViewById(R.id.offline_progress);

        viewHolder.selectToOffline = (CheckBox) view
                .findViewById(R.id.select_to_offline);
        viewHolder.selectToOffline.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                ViewHolder viewHolder = (ViewHolder) v.getTag();
                if (isItemChecked(viewHolder.postition)) {
                    deselectItem(viewHolder.postition, viewHolder.offlineTask);
                } else {
                    selectItem(viewHolder.postition, viewHolder.offlineTask);
                }
            }
        });

        view.setTag(viewHolder);

        return view;
    }

    class ViewHolder {
        OfflineTask offlineTask;
        int postition;
        SubscribedChannel subscribedChannel;
        ImageView channelImage;
        TextView channelTitle;
        TextView offlineTime;
        TextView offlineStatus;
        ProgressBar offlineProgress;
        CheckBox selectToOffline;
    }

    /*
     * (non-Javadoc)
     *
     * @see android.widget.CursorAdapter#bindView(android.view.View,
     * android.content.Context, android.database.Cursor)
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        SubscribedChannel subscribedChannel = RssSubscribedChannel
                .inject(cursor);

        if (subscribedChannel == null) {
            Utils.error(getClass(), "bindView -> subscribedChannel is null!");
            return;
        }

        Channel channel = subscribedChannel.getChannel();

        if (channel == null) {
            Utils.error(getClass(),
                    "bindView -> channel is null! subscribedChannel name = %s",
                    subscribedChannel.channel);
            return;
        }

        viewHolder.subscribedChannel = subscribedChannel;

        // 频道图
        viewHolder.channelImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        viewHolder.channelImage.setImageBitmap(null);
        viewHolder.channelImage.setTag(BitmapFactoryBase.IMAGE_STATE_DEFAULT);
        loadChannelImage(channel, viewHolder.channelImage);

        viewHolder.channelTitle.setText(channel.title);

        // 判断当前状态
        viewHolder.offlineTask = channel.offline();
        viewHolder.selectToOffline.setChecked(mSelectedItemList.get(cursor
                .getPosition()));

        refreshItem(viewHolder);

        viewHolder.selectToOffline.setTag(viewHolder);
        viewHolder.postition = cursor.getPosition();
    }

    void selectItem(int position, OfflineTask task) {
        if (!mSelectedItemList.get(position)) {
            mSelectedItemList.put(position, true);
            mSelectedOfflineTaskList.add(task);

            ((OfflineActivity) mContext).refreshDownloadButton();
            ((OfflineActivity) mContext).refreshSelectAllText();
        }
    }

    void deselectItem(int position, OfflineTask task) {
        if (mSelectedItemList.get(position)) {
            mSelectedItemList.delete(position);
            mSelectedOfflineTaskList.remove(task);

            ((OfflineActivity) mContext).refreshDownloadButton();
            ((OfflineActivity) mContext).refreshSelectAllText();
        }
    }

    boolean isItemChecked(int position) {
        return mSelectedItemList.get(position);
    }

    int getCheckedItemCount() {
        return mSelectedItemList.size();
    }

    boolean hasSelected() {
        return mSelectedItemList.size() != 0;
    }

    boolean isSelectedAll() {
        return mSelectedItemList.size() >= getCursor().getCount();
    }

    void selectAll(ContentResolver resolver) {
        // 把可见的对勾都勾选上
        ListView listView = ((OfflineActivity) mContext).mListView;
        for (int i = 0; i < listView.getChildCount(); i++) {
            View view = listView.getChildAt(i);
            if (view != null) {
                ViewHolder viewHolder = (ViewHolder) view.getTag();
                viewHolder.selectToOffline.setChecked(true);
            }
        }

        List<RssSubscribedChannel> list = RssSubscribedChannel
                .getWithoutFixed(resolver);
        for (int i = 0; i < list.size(); i++) {
            Channel channel = list.get(i).getChannel();
            OfflineTask task = channel.offline();
            selectItem(i, task);
        }
    }

    void deselectAll(ContentResolver resolver) {
        // 把可见的对勾都取消了
        ListView listView = ((OfflineActivity) mContext).mListView;
        for (int i = 0; i < listView.getChildCount(); i++) {
            View view = listView.getChildAt(i);
            if (view != null) {
                ViewHolder viewHolder = (ViewHolder) view.getTag();
                viewHolder.selectToOffline.setChecked(false);
            }
        }

        List<RssSubscribedChannel> list = RssSubscribedChannel
                .getWithoutFixed(resolver);
        for (int i = 0; i < list.size(); i++) {
            Channel channel = list.get(i).getChannel();
            OfflineTask task = channel.offline();
            deselectItem(i, task);
        }
    }

    void refreshItem(ViewHolder viewHolder) {
        long offlineTime = viewHolder.subscribedChannel.getOfflineTime();

        if (offlineTime <= 0) {
            viewHolder.offlineTime.setVisibility(View.INVISIBLE);
        } else {
            viewHolder.offlineTime.setVisibility(View.VISIBLE);
            viewHolder.offlineTime.setText(mContext
                    .getString(R.string.rd_offline_lastest_offline_time)
                    + UiUtils.getRoughlyRelativeDateString(offlineTime));
        }

        switch (viewHolder.offlineTask.getStatus()) {
        case COMPLETED:
            viewHolder.offlineStatus.setVisibility(View.GONE);
            viewHolder.offlineProgress.setVisibility(View.INVISIBLE);
            break;

        case NOT_RUNNING:
        case USER_CANCEL:
            viewHolder.offlineStatus.setVisibility(View.GONE);
            viewHolder.offlineProgress.setVisibility(View.INVISIBLE);
            break;

        case ERROR:
            viewHolder.offlineStatus.setVisibility(View.VISIBLE);
            viewHolder.offlineStatus
                    .setText(R.string.rd_offline_status_failure);
            viewHolder.offlineStatus.setTextColor(mContext.getResources()
                    .getColor(R.color.rd_red));

            viewHolder.offlineProgress.setVisibility(View.INVISIBLE);
            break;

        case WAITING:
            viewHolder.offlineStatus.setVisibility(View.VISIBLE);
            viewHolder.offlineStatus
                    .setText(R.string.rd_offline_status_waiting);
            viewHolder.offlineStatus.setTextColor(mContext.getResources()
                    .getColor(R.color.rd_white));

            viewHolder.offlineProgress.setVisibility(View.INVISIBLE);
            break;

        case RUNNING:
            viewHolder.offlineStatus.setVisibility(View.VISIBLE);
            String currentProgress = String.format("%d%%",
                    viewHolder.offlineTask.getCurrentProgress());
            viewHolder.offlineStatus.setText(currentProgress);
            viewHolder.offlineStatus.setTextColor(mContext.getResources()
                    .getColor(R.color.rd_white));

            viewHolder.offlineTime.setVisibility(View.INVISIBLE);
            viewHolder.offlineProgress.setVisibility(View.VISIBLE);
            viewHolder.offlineProgress.setProgress((int) viewHolder.offlineTask
                    .getCurrentProgress());
            break;
        }

        View progressContainer = (View) viewHolder.offlineProgress.getParent();
        if (viewHolder.offlineProgress.getVisibility() != View.VISIBLE
                && viewHolder.offlineTime.getVisibility() != View.VISIBLE) {
            progressContainer.setVisibility(View.GONE);
        } else {
            progressContainer.setVisibility(View.VISIBLE);
        }

        if (OfflineQueue.isRunning()) {
            viewHolder.selectToOffline.setEnabled(false);
        } else {
            viewHolder.selectToOffline.setEnabled(true);
        }
    }

    void start() {
        if (!NetUtils.isNetworkAvailable()) {
            // 如果当前连网络都没连接，那就直接提示不让用了
            Toast.makeText(mContext, R.string.rd_article_network_expception,
                    Toast.LENGTH_LONG).show();
            return;
        } else if (!NetUtils.isWifiConnected()) {
            CustomDialog dialog = new CustomDialog(mContext);

            dialog.setTitle(R.string.rd_dialog_title);
            dialog.setMessage(R.string.rd_offline_dialog_tips_for_3g);

            dialog.setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startImmediately();
                            Settings.setEnableOfflineWithoutWifi(true);
                            dialog.dismiss();
                        }
                    });

            dialog.setNegativeButton(android.R.string.cancel,
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

            dialog.show();
        } else {
            startImmediately();
        }
    }
    /**
     *  立即下载 
     */
    void startImmediately() {
        OfflineNotificationBar.get(mContext).cancelComplete();

        for (OfflineTask offlineTask : mSelectedOfflineTaskList) {
            offlineTask.setListener(OfflineListRefreshController.getListener());
            offlineTask.start();
        }

        OfflineListRefreshController.setScrolling(false);
    }

    void stop() {
        OfflineQueue.stopAll();

        OfflineListRefreshController.setScrolling(false);
    }

    void setAllCheckboxEnable(boolean isEnable) {
        ListView listView = ((OfflineActivity) mContext).mListView;
        for (int i = 0; i < listView.getChildCount(); i++) {
            View view = listView.getChildAt(i);
            if (view != null) {
                ViewHolder viewHolder = (ViewHolder) view.getTag();
                viewHolder.selectToOffline.setEnabled(isEnable);
            }
        }
    }

    private void loadChannelImage(Channel channel, ImageView imageView) {
        if (imageView.getTag() == null
                || !BitmapFactoryBase.IMAGE_STATE_DEFAULT
                        .equals((String) imageView.getTag())) {
            return;
        }

        int imageVersion = 0;
        if (!TextUtils.isEmpty(channel.imageversion)) {
            try {
                imageVersion = Integer.parseInt(channel.imageversion);
            } catch (Exception e) {
                Utils.error(getClass(), Utils.getStackTrace(e));
            }
        }

        String url = channel.image;
        if (imageVersion > 0) {
            url += ChannelBitmapFacotry.IMAGE_VERSION_PREFIX + imageVersion;
        }

        Bitmap bitmap = ChannelBitmapFacotry.getInstance(mContext)
                .getBitmapByUrlFromCache(url);
        if (bitmap == null) {
            boolean needDownload = ChannelBitmapFacotry.getInstance(mContext)
                    .setDefaultChannelCover(mContext, channel, imageView);
            if (needDownload) {
                imageView.setTag(url);
                ChannelBitmapFacotry.getInstance(mContext).requestLoading(url,
                        imageView);
            } else {
                imageView.setTag(BitmapFactoryBase.IMAGE_STATE_LOADED);
            }
        } else {
            imageView.setImageBitmap(bitmap);
            imageView.setTag(BitmapFactoryBase.IMAGE_STATE_LOADED);
        }
    }

}
