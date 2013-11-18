package com.qihoo360.reader.image;

import com.qihoo360.reader.Constants;
import com.qihoo360.reader.support.AsyncTaskEx;
import com.qihoo360.reader.support.FileUtils;
import com.qihoo360.reader.support.Utils;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;


public class BitmapFactoryBase {
    public final static String TAG = "BitmapFactoryBase";

    public final static String IMAGE_STATE_DEFAULT = "DEFAULT";
    public final static String IMAGE_STATE_LOADED = "LOADED";

    protected HashMap<String, SoftReference<Bitmap>> mBitmapCache = new HashMap<String, SoftReference<Bitmap>>();

    Context mContext = null;
    LoaderThread mLoaderThread = null;
    public Handler mLoaderThreadHandler;
    public Handler mHandler = null;
    HashMap<String, RunningRequest> mRunningRequestMap = new HashMap<String, RunningRequest>();

    public static class PendingRequest {
        String url = "";
        ImageView targetView = null;
    }

    public static class RunningRequest {
        String url = "";
        ArrayList<ImageView> mTargetViews = new ArrayList<ImageView>();
    }

    public class LoaderThread extends HandlerThread implements Callback {
        private ArrayList<PendingRequest> mPendingRequest = new ArrayList<PendingRequest>();

        public LoaderThread() {
            super(TAG);
        }

        @Override
        protected void onLooperPrepared() {
            super.onLooperPrepared();
            if (mLoaderThreadHandler == null) {
                mLoaderThreadHandler = new Handler(getLooper(), this);

                if(mPendingRequest != null && mPendingRequest.size() > 0) {
                    for(PendingRequest pr : mPendingRequest) {
                        sendRequestLoadingMsg(pr.url, pr.targetView);
                    }

                    mPendingRequest.clear();
                }
            }
        }

        private void sendRequestLoadingMsg(String url, ImageView targetView) {
            Message msg = mLoaderThreadHandler.obtainMessage();
            msg.obj = targetView;
            Bundle data = new Bundle();
            data.putString("url", url);
            msg.setData(data);

            msg.sendToTarget();
        }

        public void requestLoading(String url, ImageView targetView) {
            Utils.debug(TAG, "requesting: " + url + ", targetView: " + targetView);
            if(TextUtils.isEmpty(url) || targetView == null) {
                return;
            }

            if(mLoaderThreadHandler != null) {
                sendRequestLoadingMsg(url, targetView);
            } else {
                Utils.debug(TAG, "looper not ready: " + url);
                PendingRequest pr= new PendingRequest();
                pr.url = url;
                pr.targetView = targetView;
                mPendingRequest.add(pr);
            }
        }

        @Override
        public boolean handleMessage(final Message paramMessage) {
            final String url = paramMessage.getData().getString("url");
            ImageView targetView = (ImageView) paramMessage.obj;
            Utils.debug(TAG, "handle request: " + url);

            if(TextUtils.isEmpty(url)) {
                return false;
            }

            synchronized (mRunningRequestMap) {
                if(mRunningRequestMap.containsKey(url)) {
                    Utils.debug(TAG, "request already exists...");
                    ArrayList<ImageView> targetViews = mRunningRequestMap.get(url).mTargetViews;
                    if(!targetViews.contains(targetView)) {
                        Utils.debug(TAG, "adding target view to existing request...");
                        targetViews.add(targetView);
                    } else {
                        Utils.debug(TAG, "target view already exists ...");
                    }

                    return true;
                } else {
                    Utils.debug(TAG, "adding new request...");

                    RunningRequest rr = new RunningRequest();
                    rr.url = url;
                    rr.mTargetViews.add(targetView);
                    mRunningRequestMap.put(url, rr);
                }
            }

            Bitmap bitmap = null;

            try {
                bitmap = loadBitmapFromSDCard(url);
            } catch (LoadBitmapFromSDCardFailedException e) {
                synchronized (mRunningRequestMap) {
                    mRunningRequestMap.remove(url);
                }
                return false;
            }
            // 加载图片
            if(bitmap != null) {
                Utils.debug(TAG, "load bitmap succeed: " + url);
                setBitmap(url, bitmap);
                return false;
            } else {
                if(shouldSkipDownload()) {
                    synchronized (mRunningRequestMap) {
                        Utils.debug(TAG, "skip download for : " + url);
                        mRunningRequestMap.remove(url);
                    }
                } else {
                    Utils.debug(TAG, "load from net: " + url);
                    try {
                        new AsyncTaskEx<Void, Void, Bitmap>() {
                            @Override
                            protected Bitmap doInBackground(Void... params) {
                                String filePath = getFilePath(url);
                                final String fullUrl = processUrl(url);
                                HttpClient httpClient = new DefaultHttpClient();
                                HttpUriRequest httpRequest = ImageUtils.getHttpRequestForImageDownload(fullUrl);
                                try {
                                    HttpResponse response = httpClient.execute(httpRequest);
                                    if (response.getStatusLine().getStatusCode() == 200) {
                                        byte[] bytes = EntityUtils.toByteArray(response.getEntity());
                                        if (bytes != null && bytes.length > 0) {
                                            Bitmap downloadedBitmap = decodeByteArray(bytes);

                                            if(FileUtils.isExternalStorageAvail()) {
                                                if(fullUrl.endsWith(".png")) {
                                                    Bitmap newBitmap = ImageUtils.convertPngToJpeg(bytes);
                                                    if(newBitmap != null) {
                                                        downloadedBitmap.recycle();
                                                        downloadedBitmap = newBitmap;

                                                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                                        newBitmap.compress(CompressFormat.JPEG, 60, baos);
                                                        bytes = baos.toByteArray();
                                                    }
                                                }

                                                ImageUtils.writeImageDataToFile(mContext, filePath, bytes);
                                            }

                                            return downloadedBitmap;
                                        }
                                    }

                                    Utils.debug(TAG, "load from net end:  " + url);
                                } catch(Exception e) {
                                    Utils.error(getClass(), Utils.getStackTrace(e));
                                } catch (OutOfMemoryError e) {
                                    System.gc();
                                    Utils.debug(TAG, "out of memory, causing gc...");
                                }
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Bitmap result) {
                                if(result != null) {
                                    setBitmap(url, result);
                                } else {
                                    Utils.debug(TAG, "download bitmap failed: " + url);

                                    synchronized (mRunningRequestMap) {
                                        Utils.debug(TAG, "remove request from map: " + url);
                                        mRunningRequestMap.remove(url);
                                    }
                                }
                            }

                        }.execute();
                    } catch (Exception e) {
                        Utils.debug(TAG, "executing async task failed!!!");
                        Utils.error(getClass(), Utils.getStackTrace(e));
                        synchronized (mRunningRequestMap) {
                            mRunningRequestMap.remove(url);
                        }
                    }
                }
            }

            return true;
        }
    }

    protected void setBitmap(String url, Bitmap bitmap) {
        mBitmapCache.put(url, new SoftReference<Bitmap>(bitmap));
        Message message = mHandler.obtainMessage();
        message.obj = bitmap;
        Bundle data = new Bundle();
        data.putString("url", url);
        message.setData(data);
        message.sendToTarget();
    }

    protected BitmapFactoryBase(Context context) {
        ensureImageDir();
        mHandler = new Handler(context.getMainLooper()) {
            public void handleMessage(Message msg) {
                try {
                    if(msg != null && msg.obj != null) {
                        String url = msg.getData().getString("url");
                        Bitmap bitmap = (Bitmap) msg.obj;

                        if(!TextUtils.isEmpty(url) && bitmap != null) {
                            synchronized (mRunningRequestMap) {
                                if(mRunningRequestMap.containsKey(url)) {
                                    RunningRequest rr = mRunningRequestMap.get(url);
                                    if(rr.mTargetViews != null && rr.mTargetViews.size() > 0) {
                                        for(ImageView iv : rr.mTargetViews) {
                                            if(iv.getTag() != null && url.equals((String)iv.getTag())) {
                                                Utils.debug(TAG, "setting bitmap back to the image view: " + iv + ", " + url);
                                                setImageBackToView(bitmap, iv);
                                                iv.setTag(IMAGE_STATE_LOADED);
                                            }
                                        }
                                    } else {
                                        Utils.debug(TAG, "could not find target view for url: " + url);
                                    }

                                    mRunningRequestMap.remove(url);
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };

        mContext = context.getApplicationContext();
    }

    public Bitmap getBitmapByUrlFromCache(String url) {
        if(mBitmapCache.get(url) != null && mBitmapCache.get(url).get() != null) {
            return mBitmapCache.get(url).get();
        }

        return null;
    }

    public void requestLoading(String url, ImageView targetView) {
        if(mLoaderThread == null) {
            mLoaderThread = new LoaderThread();
            mLoaderThread.start();
        }

        mLoaderThread.requestLoading(url, targetView);
    }

    public void clearCache() {
        mBitmapCache.clear();
    }

    protected void ensureImageDir() {
        File dir = new File(Constants.LOCAL_PATH_IMAGES);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    protected String getFilePath(String url) {
        return Constants.LOCAL_PATH_IMAGES + url;
    }

    protected String processUrl(String url) {
        return url;
    }

    public static class LoadBitmapFromSDCardFailedException extends Exception {
        public LoadBitmapFromSDCardFailedException() {
            super();
        }
    }

    protected Bitmap loadBitmapFromSDCard(String url)
            throws LoadBitmapFromSDCardFailedException {
        Bitmap bitmap = null;
        final String filePath = getFilePath(url);
        File pf = new File(filePath);
        if (pf.exists()) {// 读取本地图片
            Utils.debug(TAG, "load from sdcard: " + url);
            bitmap = decodeFile(filePath);
            if (bitmap == null) {
                throw new LoadBitmapFromSDCardFailedException();
            }
        }
        return bitmap;
    }

    protected Bitmap decodeFile(String filePath) {
        return BitmapHelper.decodeFile(filePath);
    }

    protected Bitmap decodeByteArray(byte[] bytes) {
        return BitmapHelper.decodeByteArray(bytes, 0, bytes.length);
    }

    protected void setImageBackToView (Bitmap bitmap, ImageView iv) {
        iv.setImageBitmap(bitmap);
    }

    protected boolean shouldSkipDownload() {
        return false;
    }
}
