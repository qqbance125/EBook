
package com.qihoo360.reader.ui.imagechannel;

import com.qihoo360.reader.Constants;
import com.qihoo360.reader.R;
import com.qihoo360.reader.data.Tables.Articles;
import com.qihoo360.reader.image.BitmapFactoryBase;
import com.qihoo360.reader.image.BitmapHelper;
import com.qihoo360.reader.image.ImageUtils;
import com.qihoo360.reader.support.ArithmeticUtils;
import com.qihoo360.reader.support.FileUtils;
import com.qihoo360.reader.support.MemoryHelper;
import com.qihoo360.reader.support.Utils;
import com.qihoo360.reader.ui.imagechannel.ImageLoader.ImageLoadedCallback;
import com.qihoo360.reader.ui.view.ImageAlbumCoverView;
import com.qihoo360.reader.ui.view.ImageAlbumCoverView.LocalFileInfo;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewParent;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.LayoutParams;
import android.widget.BaseAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class ImageChannelVerticleListAdapter extends BaseAdapter
        implements ImageLoadedCallback {
    public interface OnAlbumClickListener {
        public void albumClicked(int albumIndex);
    }

    public static final int ALBUM_COUNT_FOR_EACH_PAGE = 6;
    public static final int PRELOAD_IMAGE_STEP = 10;
    public static final boolean PRELOAD_PAGES = MemoryHelper.getMaxHeapSize() > 24*1024*1024;
    //布局的切换
    private static final int[] PAGE_TEMPLAT = {
        R.layout.rd_image_channel_page_template_1,
        R.layout.rd_image_channel_page_template_2,
        R.layout.rd_image_channel_page_template_3,
    };

    private static final int[] IMAGEVIEW_IDS = {
            R.id.rd_image_view_1,
            R.id.rd_image_view_2,
            R.id.rd_image_view_3,
            R.id.rd_image_view_4,
            R.id.rd_image_view_5,
            R.id.rd_image_view_6,
    };

    public static final String TAG = "ImageChannelVerticleListAdapter";
    public static final int PRELOAD_STRATEGY_FORWARD = 1;
    public static final int PRELOAD_STRATEGY_BACKWARD = 2;
    public static final int PRELOAD_STRATEGY_STAY_STILL = 3;

    private Activity mContext = null;
    private Cursor mCursor = null;
    private int mAlbumCount = -1;
    private int mTemplateCount = PAGE_TEMPLAT.length;
    private View[] mTemplatePool = new View[mTemplateCount];
    private int mPageCount = -1;
    private boolean mBusy = false;
    private boolean mLoadAsFling = false;
    private static final int IMAGE_INFO_CACHE_SIZE_LIMIT = PRELOAD_PAGES ? 600 : 300;
    private HashMap<Integer, LocalFileInfo> mAlbumCoverInfoMap
            = new HashMap<Integer, LocalFileInfo>();
    private AsyncTask<Void, Void, Void> mLoadPageTask = null;

    public static final int CACHED_BITMAP_PAGE_COUNT = 3;
    private int mImageQuality = -1;
    private OnAlbumClickListener mAlbumClickListener = null;
    private Rect mPageRectInWindow = null;

    private ArrayList<String> mDownloadingImageUrls = new ArrayList<String>();
    private ImageLoader mImageLoader = null;
    private AlbumCoverLoader mCoverLoader = null;
    private AsyncTask<Void, Void, Void> mPreoadImageTask = null;
    private int mLastTouchMoveOffsetY = 0;

    private Bitmap mDefaultCoverBitmap = null;
    private String mAlbumCoverDataFiledName = "";

    public ImageChannelVerticleListAdapter(Activity context, Cursor cursor,
            String albumCoverDataFiledName, OnAlbumClickListener albumClickListener) {
        super();

        mImageLoader = new ImageLoader();
        mImageLoader.setImageLoadedCallback(this);
        mImageLoader.start();

        mCoverLoader = new AlbumCoverLoader();
        mCoverLoader.start();

        mContext = context;
        updateCursor(cursor, true);
        mAlbumCoverDataFiledName = albumCoverDataFiledName;
        mAlbumClickListener = albumClickListener;
        mDefaultCoverBitmap = BitmapHelper.decodeResource(context.getResources(),
                R.drawable.rd_article_detail_loading);
    }

    public void updateCursor(Cursor cursor, boolean initializing) {
        mCursor = cursor;
        mAlbumCount = mCursor.getCount();
        mPageCount = -1;
        if (!initializing) {
            notifyDataSetChanged();
        }
    }
    

    public void setTouchMoveOffsetY(int offset) {
        mLastTouchMoveOffsetY = offset;
    }

    public void resetPages() {
        for (View page : mTemplatePool) {
            if(page != null) {
                page.setTag(null);
            }
        }

        mAlbumCoverInfoMap.clear();
    }

    public void setPageRectInWindow(Rect rect) {
        mPageRectInWindow = rect;
    }

    public void setPageHeight(int width, int height) {
        for (int i = 0; i < mTemplateCount; i++) {
            View view = LayoutInflater.from(mContext).inflate(PAGE_TEMPLAT[i], null);
            if (view != null) {
                LayoutParams lp = new LayoutParams(width, height);
                view.setLayoutParams(lp);
                mTemplatePool[i] = view;
                int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
                int widthHeightSpec = View.MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);

                view.measure(widthMeasureSpec, widthHeightSpec);
                view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
            } else {
                mTemplatePool[i] = null;
            }
        }
    }

    @Override
    public int getCount() {
        for(View page : mTemplatePool) {
            if(page == null) {
                return 0;
            }
        }

        if (mPageCount < 0 && mCursor != null) {
            int albumCount = mCursor.getCount();
            if (albumCount % ALBUM_COUNT_FOR_EACH_PAGE == 0) {
                mPageCount = albumCount / ALBUM_COUNT_FOR_EACH_PAGE;
            } else {
                mPageCount = albumCount / ALBUM_COUNT_FOR_EACH_PAGE + 1;
            }
        }
        return mPageCount;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = mTemplatePool[position % mTemplateCount];
        if (view != null) {
            /**
             * position 其实很简单的页数
             * 移动的距离大于0
             */
            loadPage(view, position, mLastTouchMoveOffsetY > 0);
        }
        return view;
    }

    public void setBusy(boolean value) {
        mBusy = value;
        if (value) {
            stopPendingTasks();
        }
    }

    private void stopPendingTasks() {
        if (mLoadPageTask != null && !mLoadPageTask.isCancelled()) {
            mLoadPageTask.cancel(true);
        }

        stopLoadingImageInfo();
        removePendingMessages();
    }

    public void setLoadAsFling(boolean value) {
        mLoadAsFling = value;
    }

    public void removePendingMessages() {
        mUIHandler.removeMessages(MSG_SET_ALBUM_COVER);
        mUIHandler.removeMessages(MSG_SET_DEFAULT_IMAGE);
        mUIHandler.removeMessages(MSG_SET_DEFAULT_IMAGE_AND_HIDE);
        mUIHandler.removeMessages(MSG_SET_DEFAULT_IMAGE_AND_DOWNLOAD);
    }

    private void loadPage(View page, int pageIndex, boolean reversely) {
        if (page == null || mCursor == null || pageIndex < 0 || pageIndex > mPageCount - 1) {
            return;
        }

        int oldPageNumber = page.getTag() == null ? -1 : (Integer) page.getTag();
        final boolean pageIndexChanged = oldPageNumber != pageIndex;
        page.setTag(pageIndex);

        int count = IMAGEVIEW_IDS.length;
        for (int i = 0; i < count; i++) {
            int viewIndex = reversely ? count - 1 - i : i;
            final ImageAlbumCoverView imageView = (ImageAlbumCoverView) page
                    .findViewById(IMAGEVIEW_IDS[viewIndex]);
            if (imageView == null
                        || (!pageIndexChanged && !TextUtils.isEmpty((String) imageView.getTag())
                                /*&& (PRELOAD_PAGES || mBusy)*/)) {
                continue;
            }

            imageView.setTag("");

            final int albumIndex = pageIndex * ALBUM_COUNT_FOR_EACH_PAGE + viewIndex;
            boolean validPosition = albumIndex < mAlbumCount;
            boolean needSetDefaultCover = true;
            boolean isMainLooper = (Looper.myLooper() == Looper.getMainLooper());
            if (!mBusy && validPosition/* && (PRELOAD_PAGES || isImageViewInWindow(imageView))*/) {
                needSetDefaultCover = false;
                setImageContent(imageView, albumIndex, pageIndexChanged);
            } else if (mBusy && mLoadAsFling && validPosition && isMainLooper // 同步更新
                    && imageView.isLayoutValid()/* && (PRELOAD_PAGES || isImageViewInWindow(imageView))*/) {
                needSetDefaultCover = false;
                asyncSetImageContent(imageView, albumIndex, pageIndex);
            }
            //加载默认的图片
            if (needSetDefaultCover) {
                if (!isMainLooper) {
                    Message msg = mUIHandler
                                .obtainMessage(validPosition ? MSG_SET_DEFAULT_IMAGE
                                        : MSG_SET_DEFAULT_IMAGE_AND_HIDE);
                    msg.obj = imageView;
                    msg.sendToTarget();
                } else {
                    setDefaultCover(imageView, validPosition ? View.VISIBLE : View.INVISIBLE, "");
                }
            }
        }
    }

    private boolean isImageViewInWindow(ImageAlbumCoverView imageView) {
        if(mPageRectInWindow == null || imageView == null) {
            return true;
        }

        int width = imageView.getWidth();
        int height = imageView.getHeight();
        if(width <= 0 || height <= 0) {
            return true;
        }

        int[] loc = new int[2];
        imageView.getLocationInWindow(loc);
        Rect viewRect = new Rect(loc[0], loc[1], width, loc[1] + height);

        return viewRect.intersect(mPageRectInWindow);
    }

    public void updateImages(final AbsListView list, final int preloadStrategy) {
        if (mLoadPageTask != null && !mLoadPageTask.isCancelled()) {
            mLoadPageTask.cancel(true);
        }

        mCoverLoader.removePendingRequests();

        mLoadPageTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                if (!PRELOAD_PAGES) {
                    for (View page : mTemplatePool) {
                        if (page != null && page.getParent() != list) {
                            clearPageContent(page);
                        }
                    }
                }
            };

            @Override
            protected Void doInBackground(Void... params) {
                int count = list.getChildCount();

                // must consider the header
                int pageShift = Math.max(list.getFirstVisiblePosition() - 1, 0);
                boolean headerShown = count > 0
                        && list.getChildAt(0).getId() == R.id.pull_to_refresh_header;
                boolean footerShown = count > 1
                        && list.getChildAt(count - 1).getId() == R.id.rd_article_loading_processer;
                if(footerShown) {
                    count--;
                }
                int start = headerShown ? 1 : 0;
                for (int i = 0; i < count - start && !isCancelled(); i++) {
                    View child = list.getChildAt(i + start);
                    loadPage(child, i + pageShift, preloadStrategy == PRELOAD_STRATEGY_BACKWARD);
                }

                if (preloadStrategy != PRELOAD_STRATEGY_STAY_STILL) {
                    /*
                     * View firstVisiblePage = list.getChildAt(headerShown ? 1 :
                     * 0); boolean loadNext2Pages = pageShift <= 0 ||
                     * (firstVisiblePage != null && (firstVisiblePage.getTop() <
                     * -firstVisiblePage.getHeight() / 3));
                     */
                    boolean forward = pageShift <= 0
                            || (preloadStrategy == PRELOAD_STRATEGY_FORWARD && pageShift < mPageCount - 1);

                    int reachPageIndex = -1;

                    if (!isCancelled() && (count - start) == 1) {
                        int pageIndex;
                        if (forward) {
                            // 加载下一页
                            pageIndex = pageShift + 1;
                        } else {
                            // 加载上一页
                            pageIndex = pageShift - 1;
                        }

                        int viewIndex = pageIndex % mTemplateCount;
                        loadPage(mTemplatePool[viewIndex], pageIndex, !forward);
                    }

                    if (!isCancelled() && PRELOAD_PAGES) {
                        int pageIndex;
                        if (forward) {
                            // 加载第三页
                            pageIndex = pageShift + 2;
                        } else {
                            if((count - start) == 1) {
                                // 如果当前只显示一页，则加载上二页
                                pageIndex = pageShift - 2;
                            } else {
                                // 如果当前显示两页，则加载上一页
                                pageIndex = pageShift - 1;
                            }
                        }

                        int viewIndex = pageIndex % mTemplateCount;
                        if (viewIndex >= 0) {
                            loadPage(mTemplatePool[viewIndex], pageIndex, !forward);
                        }

                        reachPageIndex = pageIndex;
                    }

                    if (!isCancelled()) {
                        if(forward) {
                            preloadImageInfo(reachPageIndex + 1, PRELOAD_STRATEGY_FORWARD, PRELOAD_IMAGE_STEP);
                        } else {
                            preloadImageInfo(reachPageIndex - 1, PRELOAD_STRATEGY_BACKWARD, PRELOAD_IMAGE_STEP);
                        }
                    }
                }

                if (!isCancelled()) {
                    System.gc();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                mLoadPageTask = null;
            };

            @Override
            protected void onCancelled() {
                mLoadPageTask = null;
            }
        };

        mLoadPageTask.execute();
    }

    public void preloadImageInfoOnLoadOldFinished(AbsListView list) {
        int start = list.getLastVisiblePosition();
        preloadImageInfo(start, PRELOAD_STRATEGY_FORWARD, PRELOAD_IMAGE_STEP);
    }

    private void setImageContent(ImageAlbumCoverView imageView, int albumIndex,
            boolean pageIndexChanged) {
        if (!pageIndexChanged && imageView.getVisibility() == View.VISIBLE
                && !TextUtils.isEmpty((String) imageView.getTag())) {
            return;
        }

        LocalFileInfo lfi = mAlbumCoverInfoMap.get(albumIndex);
        if (lfi == null) {
            mMissCacheCount++;
            if(!mCursor.isClosed()) {
                mCursor.moveToPosition(albumIndex);
                lfi = getLocalFileInfo(mCursor, imageView);
                mAlbumCoverInfoMap.put(albumIndex, lfi);
            }
        } else {
            mHitCacheCount++;
        }

        boolean isMainLooper = (Looper.myLooper() == Looper.getMainLooper());
        if (lfi != null) {
            lfi.target = imageView;
            //本地缓存
            if (!TextUtils.isEmpty(lfi.path) && new File(lfi.path).exists()) {
                if (isMainLooper) {
                    imageView.setVisibility(View.VISIBLE);
                    Utils.debug(TAG, "setImageContent - set loaded tag on: " + imageView);
                    imageView.setTag(BitmapFactoryBase.IMAGE_STATE_LOADED);
                    imageView.setLocalFile(lfi);
                } else {
                    Message msg = mUIHandler.obtainMessage(MSG_SET_ALBUM_COVER);
                    msg.obj = lfi;
                    msg.sendToTarget();
                }
            } // 从服务器获取
            else if (!TextUtils.isEmpty(lfi.url) && !mDownloadingImageUrls.contains(lfi.url)) {
                if (isMainLooper) {
                    setDefaultCover(imageView, View.VISIBLE, lfi.url);
                    downloadImage(lfi);
                } else {
                    Message msg = mUIHandler.obtainMessage(MSG_SET_DEFAULT_IMAGE_AND_DOWNLOAD);
                    msg.obj = lfi;
                    msg.sendToTarget();
                }
            }
        } else {
            if (isMainLooper) {
                setDefaultCover(imageView, View.VISIBLE, "");
            } else {
                Message msg = mUIHandler.obtainMessage(MSG_SET_DEFAULT_IMAGE);
                msg.obj = imageView;
                msg.sendToTarget();
            }
        }
    }

    int mHitCacheCount = 0;
    int mMissCacheCount = 0;
    /**
     * This method must be called in UI thread
     * @param imageView
     * @param albumIndex
     * @param pageIndex
     */
    private void asyncSetImageContent(ImageAlbumCoverView imageView, int albumIndex,
            int pageIndex) {
        String tag = "";
        LocalFileInfo lfi = mAlbumCoverInfoMap.get(albumIndex);
        if (lfi != null && !TextUtils.isEmpty(lfi.url)) {
            mHitCacheCount++;
            tag = lfi.url;
            lfi.target = imageView;
            mImageLoader.requestLoading(lfi);
        } else if (lfi == null){
            mMissCacheCount++;
            LoadCoverRequest lcr = new LoadCoverRequest();
            lcr.pageIndex = pageIndex;
            lcr.albumIndex = albumIndex;
            lcr.targetView = imageView;
            mCoverLoader.requestLoading(lcr);
        }

        setDefaultCover(imageView, View.VISIBLE, tag);
    }

    void clearPageContent(View page) {
        for (int i = 0; i < IMAGEVIEW_IDS.length; i++) {
            ImageAlbumCoverView imageView = (ImageAlbumCoverView) page
                    .findViewById(IMAGEVIEW_IDS[i]);
            if (imageView != null) {
                setDefaultCover(imageView, imageView.getVisibility(), "");
            }
        }
    }

    private LocalFileInfo getLocalFileInfo(Cursor cursor, ImageAlbumCoverView imageView) {
        int fieldIdx = mCursor.getColumnIndex(mAlbumCoverDataFiledName);
        if (fieldIdx >= 0) {
            String imageUrls = mCursor.getString(fieldIdx);
            if (!TextUtils.isEmpty(imageUrls)) {
                return getLocalFileInfo(imageUrls);
            }
        }

        fieldIdx = mCursor.getColumnIndex(Articles.TITLE);
        if (fieldIdx >= 0) {
            String title = mCursor.getString(fieldIdx);
            return getLocalFileInfo(title, imageView);
        }

        return null;
    }

    private LocalFileInfo getLocalFileInfo(String images360) {
        String[] strArray = images360.split(";");
        if (strArray == null || strArray.length == 0) {
            return null;
        }

        LocalFileInfo lfi = new LocalFileInfo();
        for (String validUrl : strArray) {
            if (TextUtils.isEmpty(validUrl) || validUrl.equals("*")) {
                continue;
            }

            String[] strArrayTmp = validUrl.split("\\|\\|");
            if (strArrayTmp == null || strArrayTmp.length < 2) {
                continue;
            } else {
                lfi.url = strArrayTmp[0];
                lfi.path = getFilePath(strArrayTmp[0]);
                if(new File(lfi.path).exists()) {
                    // 鑾峰彇鏂囦欢鐪熷疄灏哄
                    getBitmapDimension(lfi);
                    break;
                } else {
                    // 鑾峰彇鍥剧墖鍘熷昂瀵革紝涔嬪悗浼氭牴鎹綉缁滅姸鍐佃皟鏁翠笅杞藉昂瀵�
                    try {
                        String[] dimenArray = strArrayTmp[1].split("\\*");
                        if (dimenArray == null || dimenArray.length < 2) {
                            continue;
                        }
                        lfi.oriWidth = Integer.valueOf(dimenArray[0].substring("size:".length()));

                        if (dimenArray[1].charAt(dimenArray[1].length() - 1) == '\r') {
                            dimenArray[1] = dimenArray[1].substring(0, dimenArray[1].length() - 1);
                        }
                        lfi.oriHeight = Integer.valueOf(dimenArray[1]);
                        break;
                    } catch (Exception e) {
                        Utils.error(getClass(), Utils.getStackTrace(e));
                        break;
                    }
                }
            }
        }

        return lfi;
    }

    private LocalFileInfo getLocalFileInfo(String title, ImageAlbumCoverView imageView) {
        if (imageView.isLayoutValid()) {
            int width = imageView.getWidth();
            int height = imageView.getHeight();
            String filePath = ImageUtils.generateImageFromText(mContext, title,
                    width, height);

            if (!TextUtils.isEmpty(filePath)) {
                LocalFileInfo lfi = new LocalFileInfo();
                lfi.path = filePath;
                lfi.oriWidth = width;
                lfi.oriHeight = height;
                return lfi;
            }
        }

        return null;
    }

    private String getFilePath(String url) {
        return Constants.LOCAL_PATH_IMAGES + "full_size_images/"
                + ArithmeticUtils.getMD5(url) + ".jpg";
    }

    private void getBitmapDimension(LocalFileInfo lfi) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(lfi.path, options);
        lfi.oriWidth = options.outWidth;
        lfi.oriHeight = options.outHeight;
    }

    public void setImageQuality(int quality) {
        mImageQuality = quality;
        mImageLoader.setImageQuality(quality);
    }


    /**
     * 下载图片
     * @param lfi
     */
    private void downloadImage(final LocalFileInfo lfi) {
        try {
            new AsyncTask<Void, Void, Bitmap>() {
                @Override
                protected void onPreExecute() {
                    mDownloadingImageUrls.add(lfi.url);
                };

                @Override
                protected Bitmap doInBackground(Void... params) {
                    HttpClient httpClient = new DefaultHttpClient();
                    HttpUriRequest httpRequest = ImageUtils.getHttpRequestForImageDownload(
                            mContext,
                            lfi.url, mImageQuality, lfi);
                    try {
                        HttpResponse response = httpClient.execute(httpRequest);
                        if (response.getStatusLine().getStatusCode() == 200) {
                            byte[] bytes = EntityUtils.toByteArray(response.getEntity());
                            if (bytes != null && bytes.length > 0) {
                                if (FileUtils.isExternalStorageAvail()) {
                                    ImageUtils.writeImageDataToFile(mContext, lfi.path, bytes);
                                }
                                return lfi.target.loadBitmapFromByteArray(lfi, bytes);
                            }
                        }

                        httpClient = null;
                        httpRequest = null;
                    } catch (Exception e) {
                        Utils.error(getClass(), Utils.getStackTrace(e));
                        return null;
                    } catch (OutOfMemoryError e) {
                        System.gc();
                        return null;
                    }
                    return null;
                }

                @Override
                protected void onCancelled() {
                    mDownloadingImageUrls.remove(lfi.url);
                };

                @Override
                protected void onPostExecute(Bitmap result) {
                    ImageAlbumCoverView imageView = lfi.target;
                    if (lfi.url.equals((String) imageView.getTag())) {
                        if (result != null && (PRELOAD_PAGES || isImageViewInWindow(imageView))) {
//                            Utils.debug(TAG, "downloadImage - set loaded tag on: " + imageView);
                            imageView.setTag(BitmapFactoryBase.IMAGE_STATE_LOADED);
                            imageView.setImageBitmap(result);
                        } else {
//                            Utils.debug(TAG, "downloadImage - set empty tag on: " + imageView);
                            lfi.target.setTag("");
                        }
                    }
                    mDownloadingImageUrls.remove(lfi.url);
                }
            }.execute();
        } catch (Exception e) {
            Utils.debug(TAG, e.toString());
        }
    }
    
    /**
     * 重置数据
     */
    public void doFinialize() {
        mAlbumCoverInfoMap.clear();
        mDownloadingImageUrls.clear();

        mImageLoader.quit();
        mCoverLoader.quit(); 

        stopPendingTasks();
        mLoadPageTask = null;
        mPreoadImageTask = null;

        for(int i = 0; i < mTemplateCount; i++) {
            View page = mTemplatePool[i];
            if(page.getParent() == null) {
                clearPageContent(page);
            }
        }
        System.gc();

        Utils.debug(TAG, "ImageInfo cache hit: " + mHitCacheCount + ", miss: " + mMissCacheCount);
    }

    public static final int MSG_SET_ALBUM_COVER = 1;
    public static final int MSG_SET_DEFAULT_IMAGE = 2;
    public static final int MSG_SET_DEFAULT_IMAGE_AND_HIDE = 3;
    public static final int MSG_SET_DEFAULT_IMAGE_AND_DOWNLOAD = 4;
    private Handler mUIHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            LocalFileInfo lfi = null;
            ImageAlbumCoverView imageView = null;
            switch (msg.what) {
                case MSG_SET_ALBUM_COVER:
                    lfi = (LocalFileInfo) msg.obj;
                    imageView = lfi.target;
                    if (lfi != null && imageView != null) {
                        imageView.setVisibility(View.VISIBLE);
//                        Utils.debug(TAG, "mUIHandler - set loaded tag on: " + imageView);
                        imageView.setTag(BitmapFactoryBase.IMAGE_STATE_LOADED);
                        imageView.setLocalFile(lfi);
                    }
                    break;

                case MSG_SET_DEFAULT_IMAGE_AND_HIDE:
                    imageView = (ImageAlbumCoverView) msg.obj;
                    setDefaultCover(imageView, View.INVISIBLE, "");
                    break;

                case MSG_SET_DEFAULT_IMAGE:
                        imageView = (ImageAlbumCoverView) msg.obj;
                        setDefaultCover(imageView, View.VISIBLE, "");
                        break;

                    case MSG_SET_DEFAULT_IMAGE_AND_DOWNLOAD:
                    lfi = (LocalFileInfo) msg.obj;
                    imageView = lfi.target;
                    if (lfi != null && lfi.target != null) {
                        setDefaultCover(imageView, View.VISIBLE, lfi.url);
                        downloadImage(lfi);
                    }
                    break;
            }
        }
    };

    /**
     * 设置默认的图片效果图
     * @param imageView
     * @param visibility
     * @param tag
     */
    private void setDefaultCover(ImageAlbumCoverView imageView, int visibility, Object tag) {
        imageView.setVisibility(visibility);
//        Utils.debug(TAG, "setDefaultCover - set empty tag on: " + imageView);
        imageView.setTag(tag);

        if(mDefaultCoverBitmap != null) {
            imageView.setDefaultCover(mDefaultCoverBitmap);
        } else {
            imageView.setImageResource(R.drawable.rd_article_detail_loading);
        }
    }

    public void handlePageClicked(View page, Point point) {
        if(page.getId() != R.id.rd_image_container) {
            return;
        }

        for (int i = 0; i < IMAGEVIEW_IDS.length; i++) {
            ImageAlbumCoverView imageView = (ImageAlbumCoverView) page
                    .findViewById(IMAGEVIEW_IDS[i]);
            int[] loc = new int[2];
            getOffsetInList(imageView, loc);

            Rect r = new Rect(loc[0], loc[1], loc[0] + imageView.getWidth(), loc[1]
                    + imageView.getHeight());
            if (r.contains(point.x, point.y)) {
                if (mAlbumClickListener != null) {
                    int pageIndex = (Integer) page.getTag();
                    mAlbumClickListener.albumClicked(pageIndex * ALBUM_COUNT_FOR_EACH_PAGE + i);
                }
                break;
            }
        }
    }

    private void getOffsetInList(View view, int[] param) {
        param[0] = view.getLeft();
        param[1] = view.getTop();

        ViewParent parent = view.getParent();
        while(parent != null && parent instanceof View) {
            View parentView = (View) parent;
            if(parentView.getId() == R.id.rd_image_channel_list) {
                break;
            }
            param[0] += parentView.getLeft() - parentView.getScrollX();
            param[1] += parentView.getTop() - parentView.getScrollY();
            parent = parentView.getParent();
        }
    }

    @Override
    public void onImageLoaded(final Bitmap bitmap, final LocalFileInfo lfi) {
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageAlbumCoverView imageView = lfi.target;
//                Utils.debug(TAG, "onImageLoaded - tag: " + (String)imageView.getTag()
//                        + ", bitmap: " + bitmap + ", busy ? " + mBusy + "mLoadAsFling ? " + mLoadAsFling);
                if (lfi != null && imageView != null && !TextUtils.isEmpty(lfi.url)
                        && lfi.url.equals((String) imageView.getTag())) {
                    if (bitmap != null && (PRELOAD_PAGES || isImageViewInWindow(imageView))) {
//                        Utils.debug(TAG, "onImageLoaded - set loaded tag on: " + imageView);
                        imageView.setTag(BitmapFactoryBase.IMAGE_STATE_LOADED);
                        imageView.setImageBitmap(bitmap);
                        return;
                    } else {
//                        Utils.debug(TAG, "onImageLoaded - set empty tag on: " + imageView);
                        imageView.setTag("");
                    }
                }
                Utils.debug(TAG, "failed on: " + imageView);
            }
        });
    }

    private void preloadImageInfo(final int startPage, final int preloadStrategy, final int step) {
        if(mPreoadImageTask != null && !mPreoadImageTask.isCancelled()) {
            mPreoadImageTask.cancel(true);
        }

        if(startPage < 0 || startPage >= mPageCount) {
            return;
        }

        mPreoadImageTask = new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... arg0) {
                if(mAlbumCoverInfoMap.size() >= IMAGE_INFO_CACHE_SIZE_LIMIT) {
                    mAlbumCoverInfoMap.clear();
                }

                if (preloadStrategy == PRELOAD_STRATEGY_FORWARD) {
                    for (int i = startPage; i < startPage + step && i < mPageCount
                            && !isCancelled(); i++) {
                        preloadImageInfoForPage(i);
                    }
                } else if (preloadStrategy == PRELOAD_STRATEGY_BACKWARD) {
                    for (int i = startPage; i > startPage - step && i >= 0 && !isCancelled(); i--) {
                        preloadImageInfoForPage(i);
                    }
                }
                return null;
            }

            protected void onCancelled() {
                mPreoadImageTask = null;
            };

            protected void onPostExecute(Void result) {
                mPreoadImageTask = null;
            };
        };
        mPreoadImageTask.execute();
    }

    private void stopLoadingImageInfo() {
        if(mPreoadImageTask != null && !mPreoadImageTask.isCancelled()) {
            mPreoadImageTask.cancel(true);
        }
    }

    private boolean preloadImageInfoForPage(int pageIndex) {
        try {
            for (int i = 0; i < ALBUM_COUNT_FOR_EACH_PAGE; i++) {
                int idx = pageIndex * ALBUM_COUNT_FOR_EACH_PAGE + i;
                if (idx >= mAlbumCount) {
                    return false;
                } else {
                    LocalFileInfo lfi = mAlbumCoverInfoMap.get(idx);
                    if (lfi == null) {
                        mCursor.moveToPosition(idx);
                        int fieldIdx = mCursor.getColumnIndex(mAlbumCoverDataFiledName);
                        if (fieldIdx >= 0) {
                            lfi = getLocalFileInfo(mCursor.getString(fieldIdx));
                            mAlbumCoverInfoMap.put(idx, lfi);
                            // Utils.debug(TAG, "Loaded ImageInfo for album: " + idx);
                        }

                    }
                }
            }
            // Utils.debug(TAG, "Loaded ImageInfo for page: " + pageIndex);
        } catch (Exception e) {
            Utils.error(getClass(), Utils.getStackTrace(e));
            return false;
        }

        return true;
    }


    public static class LoadCoverRequest {
        int pageIndex = -1;
        int albumIndex = -1;
        ImageAlbumCoverView targetView = null;

        @Override
        public String toString() {
            return "page index: " + pageIndex + ", album index: " + albumIndex + ", target view" + targetView;
        }
    }

    public class AlbumCoverLoader extends HandlerThread implements Callback {

        private final static String TAG = "AlbumCoverLoader";
        public static final int MSG_LOAD_COVER = 1;
        private Handler mLoaderThreadHandler;

        public AlbumCoverLoader() {
            super(TAG);
        }

        @Override
        protected void onLooperPrepared() {
            super.onLooperPrepared();
            if (mLoaderThreadHandler == null) {
                mLoaderThreadHandler = new Handler(getLooper(), this);
            }
        }

        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == MSG_LOAD_COVER) {
                LoadCoverRequest lcr = (LoadCoverRequest) msg.obj;
//                Utils.debug(TAG, "requesting cover: " + lcr);
                ImageAlbumCoverView coverView = lcr.targetView;
                View page = getParentById(coverView, R.id.rd_image_container);
                boolean pageChanged = (page == null || page.getTag() == null || (Integer) page
                        .getTag() != lcr.pageIndex);
                if (!pageChanged && isCorrectCoverIndex(lcr) && TextUtils.isEmpty((String) coverView.getTag())) {
                    LocalFileInfo lfi = mAlbumCoverInfoMap.get(lcr.albumIndex);
                    if (lfi == null && !mCursor.isClosed()) {
                        mCursor.moveToPosition(lcr.albumIndex);
                        lfi = getLocalFileInfo(mCursor, coverView);
                        mAlbumCoverInfoMap.put(lcr.albumIndex, lfi);
                    }

                    if (lfi != null && !TextUtils.isEmpty(lfi.url)
                            && TextUtils.isEmpty((String) coverView.getTag())) {
                        lfi.target = coverView;
                        if (coverView.isLayoutValid()) {
                            final LocalFileInfo flfi = lfi;
//                            Utils.debug(TAG, "handleMessage - set url tag on: " + coverView + ", " + flfi.url);
                            coverView.setTag(flfi.url);
                            mImageLoader.requestLoading(flfi);
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        public void requestLoading(LoadCoverRequest lcr) {
            if(mLoaderThreadHandler != null && lcr != null) {
                Message msg = mLoaderThreadHandler.obtainMessage(MSG_LOAD_COVER);
                msg.obj = lcr;
                msg.sendToTarget();
            }
        }

        private boolean isCorrectCoverIndex(LoadCoverRequest lcr) {
            int viewId = lcr.targetView.getId();
            for(int i = 0; i < IMAGEVIEW_IDS.length; i++) {
                if(IMAGEVIEW_IDS[i] == viewId) {
                    return lcr.pageIndex * ALBUM_COUNT_FOR_EACH_PAGE + i == lcr.albumIndex;
                }
            }

            return false;
        }

        public void removePendingRequests() {
            if(mLoaderThreadHandler != null) {
                mLoaderThreadHandler.removeMessages(MSG_LOAD_COVER);
            }
        }

        private View getParentById(View child, int id) {
            try {
                View view = child;
                while (view != null && view.getId() != id) {
                    view = (View) view.getParent();
                }

                return view;
            } catch (Exception e) {
                Utils.error(getClass(), Utils.getStackTrace(e));
                return null;
            }
        }
    }
}
