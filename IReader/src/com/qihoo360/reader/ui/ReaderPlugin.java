package com.qihoo360.reader.ui;

import com.qihoo.ilike.data.DataEntryManager;
import com.qihoo.ilike.http.core.IHttpRequest.HttpRequestStatus;
import com.qihoo.ilike.json.listener.OnILikeItPostResultListener;
import com.qihoo.ilike.manager.IlikeManager;
import com.qihoo.ilike.subscription.IlikeChannel;
import com.qihoo.ilike.subscription.OnDownloadCategoryListResultListener;
import com.qihoo.ilike.ui.ILikeImageChannelActivity;
import com.qihoo.ilike.vo.ErrorInfo;
import com.qihoo360.browser.hip.CollectSubScribe;
import com.qihoo360.reader.Constants;
import com.qihoo360.reader.R;
import com.qihoo360.reader.ReaderApplication;
import com.qihoo360.reader.ServerUris;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.listener.OnDownloadIndexResultListener;
import com.qihoo360.reader.offline.OfflineQueue;
import com.qihoo360.reader.push.PushManager;
import com.qihoo360.reader.subscription.reader.RssManager;
import com.qihoo360.reader.subscription.reader.RssSortedChannel;
import com.qihoo360.reader.subscription.reader.RssSubscribedChannel;
import com.qihoo360.reader.support.Utils;
import com.qihoo360.reader.ui.articles.ArticleUtils;
import com.qihoo360.reader.ui.view.ReaderMainNineGridView;
import com.qihoo360.reader.ui.view.ReaderMainNineGridView.OnNineGridDataChangeListener;
import com.qihoo360.reader.ui.view.ReaderMainView;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.Toast;

public class ReaderPlugin {

    public static boolean sIsSubscribedChanged = false;

    /**
     * 当异步初始化完成后发送的消息
     */
    public static final int READER_INIT_FINISHED = 10001;

    public static boolean mIsRunning = false;

    private static boolean sIsBrowserExit = false;

    private Cursor mHomeCursor;
    private ReaderMainView mView;
    private static ReaderPlugin mInstance;

    private boolean isInit = true;

    private ReaderPlugin() {

    }

    public static ReaderPlugin getInstance() {
        if (mInstance == null) {
            mInstance = new ReaderPlugin();
        }
        return mInstance;
    }

    /**
     * 是否正在通过离线模式下载文章？
     *
     * @return
     */
    public static boolean isDownloadingWithOffline() {
        return OfflineQueue.isRunning();
    }

    /**
     * 退出阅读
     */
    public static void exitReader() {
        ActivityBase.closeAll();
    }

    /**
     * 获取阅读首页View
     *
     * @param context
     */
    public ReaderMainView getReaderMain(Context context) {
        customHomePageChannel(context);
        mHomeCursor = RssSubscribedChannel.getCursor((Activity) context,
                context.getContentResolver());
        mView = new ReaderMainView(context, mHomeCursor);

        // 此时应启动Push服务
        PushManager.start(context);
        return mView;
    }

    /***
     * 获取当前页的阅读view
     *
     * @param context
     * @param curScreen
     *            当前为第几页，从1开始
     * @return
     */
    public ReaderMainNineGridView getReaderMainView(Context context,
            int curScreen) {
        /**
         * 打点，记录用户在每个已定阅频道的阅读时间
         */
        CollectSubScribe.getInstance().initializeSubScribeKey(context);

        customHomePageChannel(context);
        ReaderMainNineGridView view = new ReaderMainNineGridView(context,
                curScreen);
        if (mListener == null) {
            throw new IllegalAccessError(
                    "please first call setOnDataChangeListener method");
        }
        view.setOnNineGridDataChangeListener(mListener);
        return view;
    }

    /**
     * 是否需要异步处理初始化的过程
     *
     * @return
     */
    public boolean shouldWaitForInit() {
        return Settings.getAppVersionInSp() < Constants.APP_VERSION
                || Settings.getIlikeVersionInSp() < Constants.ILIKE_VERSION
                || !Settings.isDatabaseInit()
                || RssSubscribedChannel.isNeedRefreshSortFloat();
    }

    /**
     * 当用户升级或者安装时要做的操作
     *
     * @param context
     * @param handler
     */
    public void initAfterInstallationOrUpgrade(final Context context) {
        AsyncTask<Integer, Integer, Integer> task = new AsyncTask<Integer, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Integer... params) {
                // 更新软件，并修改版本号
                RssManager.initalizeWhenInstalledOrUpgrade(context
                        .getContentResolver());
                IlikeManager.initalizeWhenInstalledOrUpgrade(context
                        .getContentResolver());
                return null;
            }

            @Override
            protected void onPostExecute(Integer result) {
                super.onPostExecute(result);
                if (mNavHandler != null) {
                    mNavHandler.sendEmptyMessage(READER_INIT_FINISHED);
                }
            }

        };

        task.execute();
    }

    /***
     * 获取阅读总页数
     *
     * @param context
     * @return
     */
    public int getReaderPageCount(Context context) {
        customHomePageChannel(context);
        int count = 0;
        Cursor cursor = RssSubscribedChannel.getCursor((Activity) context,
                context.getContentResolver());
        int sum = cursor.getCount();
        if (cursor != null) {
            cursor.close();
        }
        count = sum % 9 == 0 ? sum / 9 : sum / 9 + 1;
        return count;
    }

    /**
     * 设置Reader的目录，这主要用于1.74迁移到1.8时所需要的操作
     */
    public void setReaderDirToZero() {
        Settings.setReaderDir("");
    }

    /**
     * 关闭主页cursor
     */

    private void closeCursor() {
        if (mHomeCursor != null) {
            mHomeCursor.close();
        }
    }

    /**
     * activity destory 调用
     */
    public void onDestory() {
        closeCursor();
    }

    /**
     * 同步更新数据
     */
    private void asyData(final Context context) {
        // sync data
        RssManager.checkIfNeedUpdateAsync(new OnDownloadIndexResultListener() {

            @Override
            public void onUpdated() {
                Utils.debug(getClass(), "Channel: Updated Complete!");
                RssManager.getIndex();
            }

            @Override
            public void onFailure() {
                Utils.debug(getClass(), "Channel: Updated Error!");
            }

            @Override
            public void onAlreadyLastestVersion() {
                Utils.debug(getClass(),
                        "SortedChannel: Updated- Already Lastest Version!");
            }
        });

        IlikeChannel.checkIfNeedUpdateAsync(new OnDownloadCategoryListResultListener() {

            @Override
            public void onUpdated() {
                Utils.debug(getClass(), "Channel: Updated Complete!");
                RssManager.getIndex();
            }

            @Override
            public void onFailure() {
                Utils.debug(getClass(), "Channel: Updated Error!");
            }

            @Override
            public void onAlreadyLastestVersion() {
                Utils.debug(getClass(),
                        "SortedChannel: Updated- Already Lastest Version!");
            }
        });

        RssSortedChannel
                .checkIfNeedUpdateAsync(new OnDownloadIndexResultListener() {

                    @Override
                    public void onUpdated() {
                        Utils.debug(getClass(),
                                "SortedChannel: Updated Complete!");
                        RssManager.getIndex();
                    }

                    @Override
                    public void onFailure() {
                        Utils.debug(getClass(), "SortedChannel: Updated Error!");
                    }

                    @Override
                    public void onAlreadyLastestVersion() {
                        Utils.debug(getClass(),
                                "SortedChannel: Updated- Already Lastest Version!");
                    }
                });
    }

    /**
     * 初始化定制频道,只执行一次
     */
    private void customHomePageChannel(Context context) {
        if (isInit) {
            asyData(context);
            isInit = false;
        }
    }

    /*
     * public boolean cancelDeleteConditon() { return
     * mView.cancelDeleteCondition(); }
     */

    public static void openLinkWithBrowser(Context context, String link) {

        try {
            if (!(link.startsWith("http://", 0) || link.startsWith("https://",
                    0))) {
                link = "http://" + link;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setPackage(context.getPackageName())
                    .addCategory(Intent.CATEGORY_BROWSABLE)
                    .setData(Uri.parse(link));

            intent.putExtra("com.android.browser.application_id", "reader");
            context.startActivity(intent);
        } catch (Exception e) {
            Utils.error(ReaderPlugin.class, Utils.getStackTrace(e));
        }

    }

    /*
     * public static void openReaderMain(Context context) { Intent intent = new
     * Intent(); intent.setAction(Intent.ACTION_VIEW);
     * intent.addCategory(Intent.CATEGORY_LAUNCHER); ComponentName cmp = new
     * ComponentName(context.getPackageName(),
     * "com.qihoo360.browser.BrowserActivity"); intent.setComponent(cmp);
     * context.startActivity(intent); }
     */

    public static void bringBrowserForeground(Activity context) {
        Intent intent = getBringBrowserForegroundIntent(context);
        try {
            // 增加判断，判断是否为浏览器退出,浏览器退出，不执行start activity
            if (sIsBrowserExit == false) {
                context.startActivity(intent);
            }
        } catch (Exception e) {
            Utils.error(ReaderPlugin.class, Utils.getStackTrace(e));
        }
    }

    public static Intent getBringBrowserForegroundIntent(Context context) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setPackage(context.getPackageName());
        intent.addCategory(Intent.CATEGORY_DEFAULT);

        String component = context instanceof ILikeImageChannelActivity ? "com.qihoo.ilike"
                : "com.qihoo360.reader";
        intent.putExtra("launchedby", component);

        return intent;
    }

    private OnNineGridDataChangeListener mListener;

    public void setOnDataChangeListener(OnNineGridDataChangeListener listener) {
        mListener = listener;
    }

    /***
     * 恢复默认设置
     */
    public void resetDefaultPreference() {
        removePref(Settings.SPKEY_ENABLE_AUTOUPDATE_KEY);
        removePref(Settings.SPKEY_NEW_CHANNEL_ADD_POSTION);
        removePref(ArticleUtils.CHANNEL_SUBSCRIBE_TIPS);
        removePref(ArticleUtils.SCROLL_CHANGE_TEXT_SIZE_TIP);
        removePref(ArticleUtils.SCROLL_SWITCH_MORE_IMAGE_TIP);
        PreferenceManager.setDefaultValues(ReaderApplication.getContext(),
                Settings.READER_PREFERENCE, Context.MODE_WORLD_READABLE,
                R.xml.rd_reader_preferences, true);
    }

    private void removePref(String key) {
        if (TextUtils.isEmpty(key)) {
            try {
                throw new Exception("key shouldn't be null");
            } catch (Exception e) {
                Utils.error(ReaderPlugin.class, Utils.getStackTrace(e));
            }
        } else {

            SharedPreferences sp = Settings.getSharedPreferences();
            if (!sp.contains(key)) {
                Utils.debug("ReaderPlugin",
                        Utils.getStackTrace(new Throwable("key not exist")));
            } else {
                SharedPreferences.Editor editor = sp.edit();
                editor.remove(key);
                editor.commit();
            }
        }

    }

    /***
     * 退出浏览器，send intent finish reader activity
     */
    public void onBrowserExit() {
        sIsBrowserExit = true;
        ActivityBase.closeAll();
    }

    private Handler mNavHandler = null;

    public void setNavigationHandler(Handler handler) {
        mNavHandler = handler;

    }

    public void setServerAddress(String serverAddress) {
        ServerUris.setHttpServer(serverAddress);
    }

    public void clearCache(Context context) {
        RssManager.clearCacheSync(context);
    }

    private Bitmap bitmap;

    public void setBackGroundCommon(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public Bitmap getBackGroundCommon() {
        return bitmap;
    }

    public void launchILike(Context context) {
        if (shouldWaitForInit()) {
            Toast.makeText(context, "正在为您优化“我喜欢”功能，请稍后几秒再试", Toast.LENGTH_LONG)
                    .show();
            return;
        }

        if(!com.qihoo.ilike.util.Utils.showWelcomeScreenBeforeEnterIlikeMainPage(context)) {
            // The welcome screen has previously shown
            enterILikeMainPage(context);
        }
    }

    public static void enterILikeMainPage(Context context) {
        Intent intent = new Intent(context, ILikeImageChannelActivity.class);
        intent.putExtra("channel",
                IlikeChannel.get(IlikeChannel.ILIKE_HOT_COLLECTION).channel);
        context.startActivity(intent);
    }

    private BroadcastReceiver mBroadcastReceiver = null;
    private OnILikeItPostResultListener mILikeResultListener = new OnILikeItPostResultListener() {
        @Override
        public void onResponseError(ErrorInfo errorInfo) {
            Toast.makeText(ReaderApplication.getContext(),
                    R.string.ilike_collect_url_failure,
                    Toast.LENGTH_LONG).show();
        }

        @Override
        public void onRequestFailure(HttpRequestStatus errorStatus) {
            Toast.makeText(ReaderApplication.getContext(),
                    R.string.ilike_collect_url_failure,
                    Toast.LENGTH_LONG).show();
        }

        @Override
        public void onComplete() {
            Toast.makeText(ReaderApplication.getContext(),
                    R.string.ilike_collect_url_successful,
                    Toast.LENGTH_SHORT).show();
        }
    };

    public void likeUrl(Context context, String url, String title) {
        if (DataEntryManager.urlLiked(context.getContentResolver(), url)) {
            Toast.makeText(context, R.string.i_like_url_already_collected, Toast.LENGTH_SHORT)
                    .show();
            return;
        } else if (!com.qihoo.ilike.util.Utils.checkNetworkStatusBeforeLike(context)) {
            return;
        }

        if (!com.qihoo.ilike.util.Utils.accountConfigured(context, url, title)) {
            if (mBroadcastReceiver == null) {
                mBroadcastReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (com.qihoo.ilike.util.Utils.accountConfigSucceed(intent)) {
                            String url = intent
                                    .getStringExtra(com.qihoo.ilike.util.Utils.INTENT_EXTRA_DATA_URL);
                            String title = intent
                                    .getStringExtra(com.qihoo.ilike.util.Utils.INTENT_EXTRA_DATA_TITLE);
                            if (!TextUtils.isEmpty(url)) {
                                IlikeManager.likeUrl(context.getContentResolver(), url, title,
                                        IlikeManager.LikeType.Webpage, mILikeResultListener);
                            }
                        }
                        context.unregisterReceiver(mBroadcastReceiver);
                        mBroadcastReceiver = null;
                    }
                };

                IntentFilter filter = com.qihoo.ilike.util.Utils.getAccountConfigResultFilter();
                context.registerReceiver(mBroadcastReceiver, filter);
            }
        } else {
            IlikeManager.likeUrl(context.getContentResolver(), url, title,
                    IlikeManager.LikeType.Webpage, mILikeResultListener);
        }
    }

    private static boolean mIsBrowserActivityRunning = false;

    public static void setBrowserActivityRunning(boolean value) {
        mIsBrowserActivityRunning = value;
    }

    public static boolean getBrowserActivityRunning() {
        return mIsBrowserActivityRunning;
    }
}
