/**
 *
 */

package com.qihoo360.reader.offline;

import com.qihoo360.reader.Constants;
import com.qihoo360.reader.R;
import com.qihoo360.reader.ReaderApplication;
import com.qihoo360.reader.data.DataEntryManager;
import com.qihoo360.reader.data.Tables.Articles;
import com.qihoo360.reader.image.ImageDownloadStrategy;
import com.qihoo360.reader.image.ImageUtils;
import com.qihoo360.reader.listener.OnGetArticlesResultListener;
import com.qihoo360.reader.listener.OnOfflineTaskListener;
import com.qihoo360.reader.subscription.Article;
import com.qihoo360.reader.subscription.Channel;
import com.qihoo360.reader.subscription.SubscribedChannel;
import com.qihoo360.reader.subscription.reader.RssChannel;
import com.qihoo360.reader.support.ArithmeticUtils;
import com.qihoo360.reader.support.AsyncTask;
import com.qihoo360.reader.support.FakeProgress;
import com.qihoo360.reader.support.FileUtils;
import com.qihoo360.reader.support.NetUtils;
import com.qihoo360.reader.support.Utils;
import com.qihoo360.reader.ui.articles.ArticleUtils;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.AbstractHttpParams;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask.Status;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 离线下载某个频道的任务类
 *
 * @author Jiongxuan Zhang
 */
public class OfflineTask {
    private static final int DOWNLOAD_ARTICLE_COUNT = 50;
    private static final int DOWNLOAD_PHOTO_ALBUM_COUNT_NOT_WOXIHUAN = 30;
    private static final int DOWNLOAD_PHOTO_ALBUM_COUNT_WOXIHUAN = 48;
    private static final int ATTEMPT_COUNT = 3;
    private static final int ATTEMPT_TIME = 5000;
    private static final int OFFLINE_IMAGE_DOWNLOAD_TIME_OUT = 10000;

    /*
     * 通过多次尝试，我们发现文章在45%、图集在12%之前下载Json是比较好的
     *
     * 在网络环境良好的大多数情况下，我们的统计数据为：
     *
     * 文章：totalJsonTime: 66829 ms ; totalImageTime: 87559 ms ; totalImageCount: 189
     * 图集：totalTJsonTime: 26892 ms ; totalTImageTime: 183302 ms ; totalImageCount: 631 （将画质和分辨率都调整后）
     *
     */
    private static final int ARTICLE_PERCENT_IN_JSON = 45;
    private static final int IMAGE_PERCENT_IN_JSON = 12;

    private static final int MAX_PROGRESS = 100;

    private FakeProgress mFakeProgress;

    private OnOfflineTaskListener mListener;

    public static final String ACTION_CHANNEL = "channel";
    public static final String ACTION_RESULT = "result";
    public static final String ACTION_COUNT = "count";
    public static final String ACTION_IS_DELETED = "is_deleted";

    private Context mContext;
    private Channel mChannel;

    OfflineDownloadTask mTask;

    private OfflineTaskStatus mStatusType = OfflineTaskStatus.NOT_RUNNING;
    float mCurrent = 0;
    private OnOfflineTaskListener mQueueListener;
    private OfflineProgressStatus mProgressStatus = OfflineProgressStatus.NOT_RUNNING;

    /**
     * 当前任务的状态
     *
     * @author Jiongxuan Zhang
     */
    public enum OfflineTaskStatus {
        NOT_RUNNING, RUNNING, WAITING, COMPLETED, ERROR, USER_CANCEL
    }

    public enum OfflineProgressStatus {
        NOT_RUNNING, ARTICLE, IMAGE
    }

    /**
     * 初始化Task
     *
     * @param context
     * @param channel
     */
    public OfflineTask(Channel channel) {
        if (channel == null) {
            throw new IllegalArgumentException("channel is null");
        }
        mContext = ReaderApplication.getContext();
        mChannel = channel;
    }

    /**
     * 设置当某个频道下载完成以后的Listener。
     *
     * @param listener
     */
    public void setListener(OnOfflineTaskListener listener) {
        mListener = listener;
    }

    void setQueueListener(OnOfflineTaskListener listener) {
        mQueueListener = listener;
    }

    /**
     * 开始对此频道进行离线下载
     */
    public void start() {
        Utils.debug(getClass(), "starting start method");

        // 如果用户正在看这个频道，则离线下载时，直接跳过
        if (ArticleUtils.isDisplaying(mChannel)) {
            return;
        }

        OfflineQueue.add(this);

        Utils.debug(getClass(), "end start method");
    }

    /**
     * 停止该频道的离线下载
     */
    public void cancel() {
        Utils.debug(getClass(), "starting stop method");

        OfflineQueue.remove(this);

        Utils.debug(getClass(), "end stop method");
    }

    /**
     * 获取当前的进度。
     *
     * @return
     */
    public int getCurrentProgress() {
        return (int) mCurrent;
    }

    /**
     * 获取当前任务的状态
     *
     * @return
     */
    public OfflineTaskStatus getStatus() {
        return mStatusType;
    }

    /**
     * 重置状态为未下载
     */
    public void resetStatus() {
        mStatusType = OfflineTaskStatus.NOT_RUNNING;
    }

    /**
     * 离线下载是否正在进行中
     *
     * @return
     */
    public boolean isRunning() {
        return mStatusType == OfflineTaskStatus.RUNNING;
    }

    /**
     * 是否正离线下载文章的文字部分（用于UI）
     *
     * @return
     */
    public boolean isRunningInArticle() {
        return isRunning() && mProgressStatus == OfflineProgressStatus.ARTICLE;
    }

    /**
     * 是否已经失败
     *
     * @return
     */
    public boolean isFailure() {
        return mStatusType == OfflineTaskStatus.ERROR;
    }

    /**
     * 是否已经完成
     *
     * @return
     */
    public boolean isCompleted() {
        return mStatusType == OfflineTaskStatus.COMPLETED;
    }

    /**
     * 是否被用户取消
     *
     * @return
     */
    public boolean isCanceled() {
        return mStatusType == OfflineTaskStatus.USER_CANCEL;
    }

    /**
     * 还没有开始？
     *
     * @return
     */
    public boolean isNotRunning() {
        return mStatusType == OfflineTaskStatus.NOT_RUNNING;
    }

    /**
     * 该离线任务是否已经在队列中？
     *
     * @return
     */
    public boolean isWaiting() {
        return mStatusType == OfflineTaskStatus.WAITING;
    }

    /**
     * 获取该Task对应的Channel
     *
     * @return
     */
    public Channel getChannel() {
        return mChannel;
    }

    /**
     * 看是否已离线下载过，并且在两个小时以内
     *
     * @return
     */
    public boolean isOfflinedAndIn2Hours(ContentResolver resolver) {
        SubscribedChannel subscribedChannel = getChannel()
                .getSubscribedChannel(resolver);

        if (subscribedChannel != null) {
            long offlineTime = subscribedChannel.getOfflineTime();
            if (offlineTime == 0) {
                return false;
            }

            return System.currentTimeMillis() - offlineTime <= Constants.TWO_HOURS;
        }
        return false;
    }

    void updateStatus(OfflineTaskStatus status) {
        mStatusType = status;

        if (mListener != null) {
            mListener.OnUpdateStatus(this);
        }

        if (mQueueListener != null) {
            mQueueListener.OnUpdateStatus(OfflineTask.this);
        }
    }

    void startTask() {
        Utils.debug(getClass(), "starting startTask method");

        if (mTask == null) {
            mTask = new OfflineDownloadTask();
        }

        if (mTask.getStatus() == Status.PENDING) { // pending
            mTask.execute();
        }

        Utils.debug(getClass(), "end startTask method");
    }

    void stopTask() {
        Utils.debug(getClass(), "starting stopTask method");

        if (mFakeProgress != null) {
            mFakeProgress.finish();
            mFakeProgress = null;
        }

        if (mTask != null) {
            mTask.cancel();
        }

        mTask = null;

        Utils.debug(getClass(), "end stopTask method");
    }

    // DEBUG
    private static long totalJsonTime = 0;
    private static long totalImageTime = 0;
    private static int totalImageCount = 0;

    private static long totalTJsonTime = 0;
    private static long totalTImageTime = 0;
    private static int totalTImageCount = 0;

    class OfflineDownloadTask extends
            AsyncTask<OfflineTaskStatus, OfflineTaskStatus, OfflineTaskStatus> {
        private Handler mHandler = new Handler(Looper.getMainLooper());
        private DefaultHttpClient mCurrentImageHttpClient;
        private int mOfflineCount = 0;
        private int mOfflineAlbumCount = 0;
        private int mNewCount = 0;
        private boolean mIsDeleted = false;

        private long mGetFrom = 0;
        private long mGetTo = 0;

        /*
         * (non-Javadoc)
         *
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            Utils.debug(getClass(), "starting onPreExecute method");

            super.onPreExecute();

            updateStatus(OfflineTaskStatus.RUNNING);
            mCurrent = 0;

            Utils.debug(getClass(), "end onPreExecute method");
        }

        @Override
        protected OfflineTaskStatus doInBackground(OfflineTaskStatus... params) {
            Utils.debug(getClass(), "starting doInBackground method");

            // 现在开始下载文章文字
            mProgressStatus = OfflineProgressStatus.ARTICLE;

            int percentInJson = ARTICLE_PERCENT_IN_JSON;

            int downloadCount = DOWNLOAD_ARTICLE_COUNT;

            if (mChannel.type == RssChannel.TYPE_PHOTO_ALBUM) {
                if (mChannel.src.equals(Constants.IMAGE_CHANNEL_SRC_WOXIHUAN)) {
                    downloadCount = DOWNLOAD_PHOTO_ALBUM_COUNT_WOXIHUAN;
                } else {
                    downloadCount = DOWNLOAD_PHOTO_ALBUM_COUNT_NOT_WOXIHUAN;
                }
                percentInJson = IMAGE_PERCENT_IN_JSON;
            }

            if (mFakeProgress == null) {
                mFakeProgress = OfflineFakeProgress.create(OfflineTask.this);
            }
            int init = ArithmeticUtils.getRandom(1,
                    percentInJson / 4);
            mFakeProgress.setInit(init);
            percentInJson = ArithmeticUtils.getRandom(
                    (int) (percentInJson * 0.1 + 0.5 + init), percentInJson);
            mFakeProgress.setDivide(ArithmeticUtils.getRandom(3, 9));
            mFakeProgress.setMax(percentInJson);
            mFakeProgress.start();

            Utils.debug(
                    getClass(),
                    "doInBackground -> downloading %s for offline, download count = %d",
                    mChannel.channel, downloadCount);

            final long newestId = mChannel.getNewestContentId(mContext
                    .getContentResolver());

            Utils.debug(getClass(),
                    "doInBackground(OfflineTask.java): [TIMESTAMP] Starting "
                            + mChannel.channel);
            long startTime = System.currentTimeMillis();

            // 1.下载50篇最近的文章的详情（如果是图集则30份），如果下载失败，或者被用户暂停，则直接取消
            OfflineTaskStatus downloadArticlesResult = downloadArticles(
                    downloadCount, new OnGetArticlesResultListener() {

                        @Override
                        public void onNotExists(boolean isDeleted) {
                        }

                        @Override
                        public void onFailure(int error) {
                        }

                        @Override
                        public void onCompletion(long getFrom, long getTo,
                                int getCount, boolean isDeleted) {
                            mOfflineCount = getCount;
                            mGetFrom = getFrom;
                            mGetTo = getTo;
                            mNewCount = mChannel.getCount(
                                    mContext.getContentResolver(), getFrom,
                                    newestId);
                            mIsDeleted = isDeleted;

                            if (mChannel.type == RssChannel.TYPE_ARTICLE) {
                                OfflineQueue.addArticleCount(mOfflineCount);
                            }
                        }
                    });
            long endArticle = System.currentTimeMillis() - startTime;
            Utils.debug(getClass(),
                    "doInBackground(OfflineTask.java): [TIMESTAMP] Snock "
                            + mChannel.channel + " Article: After a time:"
                            + endArticle + " ms");

            if (mChannel.type == RssChannel.TYPE_PHOTO_ALBUM) {
                totalTJsonTime += endArticle;
            } else {
                totalJsonTime += endArticle;
            }

            if (downloadArticlesResult != OfflineTaskStatus.COMPLETED) {
                sendBroadcast(downloadArticlesResult);
                return downloadArticlesResult;
            }

            // 成功了，也要发Broadcast给UI
            sendBroadcast(downloadArticlesResult);

            // 现在开始下载图片
            mProgressStatus = OfflineProgressStatus.IMAGE;

            // 2.把要下载的图片放到map里
            List<List<String[]>> albumImageList = new ArrayList<List<String[]>>();
            Cursor cursor = mChannel.getFullCursor(mContext
                    .getContentResolver());
            int index = 0;
            int imageCount = 0;
            while (cursor != null && cursor.moveToNext()
                    && index < downloadCount) {
                if (isCancelled()) {
                    cursor.close();
                    return OfflineTaskStatus.USER_CANCEL;
                }

                List<String[]> imageList = new ArrayList<String[]>();

                Article article = Article.inject(cursor);

                if (!TextUtils.isEmpty(article.images360)) {
                    String[] images360 = article.images360.split(";");

                    for (String image : images360) {

                        if (image.equalsIgnoreCase("*")) {
                            continue;
                        }

                        String url = ArticleUtils.splitUrls(image);

                        if (!TextUtils.isEmpty(url)) {
                            // 先检测是否已经下载过
                            String filePath = getFilePath(url);
                            File file = new File(filePath);

                            if (file.exists()) {
                                continue;
                            }

                            imageList.add(new String[] { url, filePath });
                            imageCount++;
                        }
                    }
                }
                index++;

                if (imageList.size() != 0) {
                    albumImageList.add(imageList);
                }
            }
            cursor.close();
            if (mFakeProgress != null) {
                mFakeProgress.finish();
                mFakeProgress = null;
            }

            mCurrent = percentInJson;
            publishProgress(true);

            // 2. 下载文章的图片
            if (isCancelled()) { // 被取消
                return OfflineTaskStatus.USER_CANCEL;
            }

            // 如果是图集，而且当前用户正在看着，则为防止重复下载图片，我们必须跳过它
            if (mChannel.type == RssChannel.TYPE_PHOTO_ALBUM
                    && ArticleUtils.isDisplaying(mChannel)) {
                onComplete();
                return OfflineTaskStatus.COMPLETED;
            }

            // 有图片就下
            Utils.debug(getClass(), "doInBackground -> "
                    + "channel %s has %d images, we need to download.",
                    mChannel.channel, imageCount);
            if (imageCount != 0) {
                float imageStep = (float) (100 - percentInJson) / imageCount;

                FileUtils.ensureImageDir();

                for (List<String[]> imageList : albumImageList) {
                    for (String[] strings : imageList) {
                        if (isCancelled()) {
                            return OfflineTaskStatus.USER_CANCEL;
                        }

                        if (!FileUtils.isExternalStorageAvail()) {
                            Toast.makeText(mContext,
                                    R.string.rd_offline_sdcard_not_available,
                                    Toast.LENGTH_LONG).show();
                            return OfflineTaskStatus.ERROR;
                        }

                        if (!NetUtils.isNetworkAvailable()) {
                            return OfflineTaskStatus.ERROR;
                        }

                        OfflineTaskStatus downloadImageResult = downloadImage(
                                strings[0], strings[1]);

                        if (downloadImageResult != OfflineTaskStatus.COMPLETED) {
                            break;
                        }

                        mCurrent += imageStep;

                        if (mChannel.type == RssChannel.TYPE_PHOTO_ALBUM) {
                            OfflineQueue.addImageCount(1);
                        }

                        publishProgress(true);
                    }

                    if (!isCanceled()) {
                        mOfflineAlbumCount++;
                    }
                }
            }
            long endAll = System.currentTimeMillis() - startTime;
            Utils.debug(getClass(),
                    "doInBackground(OfflineTask.java): [TIMESTAMP] Snock "
                            + mChannel.channel + " Image: After a time:"
                            + endAll + " ms");
            Utils.debug(getClass(), "[TIMESTAMP] image size: " + imageCount);

            if (isCancelled()) {
                return OfflineTaskStatus.USER_CANCEL;
            }

            // 当下载完成后，把文章都置为“已离线下载”的
            if (mChannel.type == RssChannel.TYPE_ARTICLE) {
                ContentValues values = new ContentValues();
                values.put(Articles.ISOFFLINED, 1);

                DataEntryManager.ArticleHelper.update(
                        mContext.getContentResolver(), mChannel.channel,
                        mGetFrom, mGetTo, values);
            }

            Utils.debug(getClass(),
                    "doInBackground(OfflineTask.java): [TIMESTAMP] "
                            + mChannel.channel + ": Image: "
                            + (endAll - endArticle));

            if (mChannel.type == RssChannel.TYPE_PHOTO_ALBUM) {
                totalTImageTime += (endAll - endArticle);
                totalTImageCount += imageCount;
            } else {
                totalImageTime += (endAll - endArticle);
                totalImageCount += imageCount;
            }

            Utils.debug(getClass(),
                    "doInBackground(OfflineTask.java): [ALLTIME] "
                            + mChannel.channel + ": totalJsonTime: "
                            + totalJsonTime + " ms ; totalImageTime: "
                            + totalImageTime + " ms ; totalImageCount: "
                            + totalImageCount);

            Utils.debug(getClass(),
                    "doInBackground(OfflineTask.java): [ALLTIME TU] "
                            + mChannel.channel + ": totalTJsonTime: "
                            + totalTJsonTime + " ms ; totalTImageTime: "
                            + totalTImageTime + " ms ; totalImageCount: "
                            + totalTImageCount);

            Utils.debug(getClass(), "end doInBackground method");

            onComplete();

            return OfflineTaskStatus.COMPLETED;
        }

        private static final int REFRESH = 600;
        private long mLastRefreshTime;

        public void publishProgress(boolean needInterval) {
            // 不能老Post，需要有个时间间隔
            if (needInterval) {
                long now = System.currentTimeMillis();
                if (now - mLastRefreshTime > REFRESH) {
                    super.publishProgress();

                    mLastRefreshTime = now;
                }
            } else {
                super.publishProgress();
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see android.os.AsyncTask#onProgressUpdate(Progress[])
         */
        @Override
        protected void onProgressUpdate(OfflineTaskStatus... values) {
            super.onProgressUpdate(values);

            Utils.debug(
                    OfflineTask.this.getClass(),
                    "notifyProgressToListener -> "
                            + "notify progress to listener: channel: %s, current: %d",
                    mChannel.channel, getCurrentProgress());

            if (mListener != null) {
                mListener.onProgress(OfflineTask.this);
            }

            if (mQueueListener != null) {
                mQueueListener.onProgress(OfflineTask.this);
            }
        }

        private void onComplete() {
            mCurrent = MAX_PROGRESS;
            publishProgress(true);

            // 到这一步是COMPLETE，那么我们就把离线下载的文章数和时间都存起来
            SubscribedChannel subscribedChannel = mChannel
                    .getSubscribedChannel(mContext.getContentResolver());
            if (subscribedChannel != null) {
                subscribedChannel.setOfflineTime(mContext.getContentResolver(),
                        System.currentTimeMillis());

                if (mChannel.type == RssChannel.TYPE_ARTICLE) {
                    subscribedChannel.setOfflineCount(
                            mContext.getContentResolver(), mOfflineCount);
                } else if (mChannel.type == RssChannel.TYPE_PHOTO_ALBUM) {
                    subscribedChannel.setOfflineCount(
                            mContext.getContentResolver(), mOfflineAlbumCount);
                }
            }
        }

        private void sendBroadcast(final OfflineTaskStatus status) {
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    // 如果在离线下载的过程中，用户进了频道，则我们会让他一直在“小转轮”中等着，效果和直接刷新文章列表一样
                    // 等到请求完成后，我们会发送一个Broadcast给UI，解开小转轮，并刷新列表。
                    int result = 0;

                    switch (status) {
                    case COMPLETED:
                        result = RssChannel.RESULT_OK;
                        break;

                    case ERROR:
                        result = RssChannel.RESULT_FAILURE;
                        break;

                    case USER_CANCEL:
                        result = RssChannel.RESULT_USER_CANCELLED;
                        break;
                    }

                    if (ArticleUtils.isDisplaying(mChannel)) {
                        Intent intent = new Intent(
                                Constants.READER_BROADCAST_NEED_REFRESH_ARTICLE_LIST);
                        intent.putExtra(ACTION_CHANNEL, mChannel.channel);
                        intent.putExtra(ACTION_RESULT, result);
                        intent.putExtra(ACTION_IS_DELETED, mIsDeleted);

                        if (result == RssChannel.RESULT_OK) {
                            intent.putExtra(ACTION_COUNT, mNewCount);
                        }

                        mContext.sendBroadcast(intent);
                    }
                }
            });
        }

        private void cancel() {
            Utils.debug(getClass(), "starting cancel method");

            mChannel.stopGet();

            if (mCurrentImageHttpClient != null) {
                ClientConnectionManager manager = mCurrentImageHttpClient
                        .getConnectionManager();
                if (manager != null) {
                    manager.shutdown();
                }
            }

            cancel(true);

            Utils.debug(getClass(), "end cancel method");
        }

        private OfflineTaskStatus downloadArticles(int count,
                OnGetArticlesResultListener listener) {
            Utils.debug(getClass(), "starting downloadArticles method");

            for (int i = 0; i < ATTEMPT_COUNT; i++) {
                if (!NetUtils.isNetworkAvailable()) {
                    return OfflineTaskStatus.ERROR;
                }

                int result = mChannel.getNewArticlesSync(
                        mContext.getContentResolver(), listener, -count, false);
                Utils.debug(getClass(),
                        "downloadArticles -> channel = %s, result = %s",
                        mChannel.channel, result);
                if (result == RssChannel.RESULT_OK) {
                    return OfflineTaskStatus.COMPLETED;
                } else if (result == RssChannel.RESULT_USER_CANCELLED) {
                    return OfflineTaskStatus.USER_CANCEL;
                } else {
                    Utils.error(
                            getClass(),
                            "downloadArticles -> offline download error: channel = %s",
                            mChannel.channel);
                    try {
                        Thread.sleep(ATTEMPT_TIME);
                    } catch (InterruptedException e) {
                        Utils.error(getClass(), Utils.getStackTrace(e));
                    }
                }
            }

            Utils.debug(getClass(), "end downloadArticles method");

            return OfflineTaskStatus.ERROR;
        }

        private OfflineTaskStatus downloadImage(String url, String filePath) {
            Utils.debug(getClass(), "starting downloadImage method");

            if (!FileUtils.isExternalStorageAvail()) {
                mCurrentImageHttpClient = null;
                return OfflineTaskStatus.ERROR;
            }

            if (!NetUtils.isNetworkAvailable()) {
                mCurrentImageHttpClient = null;
                return OfflineTaskStatus.ERROR;
            }

            int pos = url.lastIndexOf("/");
            if (pos > 0) {
                int widthLimit = ImageDownloadStrategy.getInstance(mContext)
                        .getScreenWidth();

                url = url.substring(0, pos) + "/dr/" + widthLimit + "_"
                        + ImageDownloadStrategy.IMAGE_CONFIG_MAX_HEIGHT + "_"
                        + ImageDownloadStrategy.IMAGE_CONFIG_SMALL_QUALITY
                        + url.substring(pos);
            }

            String noSpaceFullUrl = url.replace(" ", "%20");
            mCurrentImageHttpClient = new DefaultHttpClient();
            HttpGet httpRequest = new HttpGet(noSpaceFullUrl);
            AbstractHttpParams httpParameters = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParameters,
                    OFFLINE_IMAGE_DOWNLOAD_TIME_OUT);
            HttpConnectionParams.setSoTimeout(httpParameters,
                    OFFLINE_IMAGE_DOWNLOAD_TIME_OUT);
            httpRequest.setParams(httpParameters);

            try {
                HttpResponse response = mCurrentImageHttpClient
                        .execute(httpRequest);

                if (response.getStatusLine().getStatusCode() == 200) {
                    byte[] bytes = EntityUtils
                            .toByteArray(response.getEntity());

                    if (bytes != null && bytes.length > 0) {
                        ImageUtils.writeImageDataToFile(
                                mContext.getApplicationContext(), filePath,
                                bytes);
                        mCurrentImageHttpClient = null;

                        Utils.debug(getClass(), "end downloadImage method");
                        return OfflineTaskStatus.COMPLETED;
                    }
                }

                Utils.debug(getClass(),
                        "downloadedImage -> url = %s, filePath = %s", url,
                        filePath);
            } catch (Exception e) {
                Utils.error(getClass(), Utils.getStackTrace(e));
            }
            Utils.error(
                    getClass(),
                    "downloadedImage -> offline download image error: url = %s; filePath = %s",
                    url, filePath);

            mCurrentImageHttpClient = null;

            Utils.debug(getClass(), "end downloadImage method");

            if (isCanceled()) {
                return OfflineTaskStatus.USER_CANCEL;
            }

            // 假如服务器给我们的图片就是坏的，我们就不应该停止下载，否则这张图片会变成“拦路虎”，短期内后面的图片就不能下载了
            return OfflineTaskStatus.COMPLETED;
        }

        private String getFilePath(String url) {
            return Constants.LOCAL_PATH_IMAGES + "full_size_images/"
                    + ArithmeticUtils.getMD5(url) + ".jpg";
        }

        /*
         * (non-Javadoc)
         *
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(OfflineTaskStatus result) {
            Utils.debug(getClass(), "starting onPostExecute method");
            super.onPostExecute(result);

            updateStatus(result);
            mProgressStatus = OfflineProgressStatus.NOT_RUNNING;

            OfflineTask.this.cancel(); // 直接完成的，需要从正在离线下载的列表中删除
            mCurrent = 0;

            Utils.debug(getClass(), "end onPostExecute method");
        }

        /*
         * (non-Javadoc)
         *
         * @see android.os.AsyncTask#onCancelled()
         */
        @Override
        protected void onCancelled() {
            super.onCancelled();

            updateStatus(OfflineTaskStatus.USER_CANCEL);
            mProgressStatus = OfflineProgressStatus.NOT_RUNNING;
            mCurrent = 0;

            mTask = null;
        }
    }
}
