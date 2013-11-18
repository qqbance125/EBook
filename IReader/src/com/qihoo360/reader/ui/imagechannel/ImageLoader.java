
package com.qihoo360.reader.ui.imagechannel;

import com.qihoo360.reader.ReaderApplication;
import com.qihoo360.reader.image.ImageUtils;
import com.qihoo360.reader.support.FileUtils;
import com.qihoo360.reader.support.Utils;
import com.qihoo360.reader.ui.view.ImageAlbumCoverView;
import com.qihoo360.reader.ui.view.ImageAlbumCoverView.LocalFileInfo;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.widget.Scroller;

import java.io.File;
import java.util.ArrayList;

public class ImageLoader extends HandlerThread implements Callback {
    public interface ImageLoadedCallback {
        public void onImageLoaded(Bitmap bitmap, LocalFileInfo lfi);
    }

    public final static String TAG = "ImageLoader";
    public Handler mLoaderThreadHandler;
    private ArrayList<LocalFileInfo> mPendingRequest = new ArrayList<LocalFileInfo>();
    Context mContext = ReaderApplication.getContext();
    private int mImageQuality = -1;
    private ImageLoadedCallback mImageLoadedCallback = null;

    public ImageLoader() {
        super(TAG);
    }
    public void setImageLoadedCallback(ImageLoadedCallback callback) {
        mImageLoadedCallback = callback;
    }

    public void setImageQuality(int quality) {
        mImageQuality = quality;
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        if (mLoaderThreadHandler == null) {
            mLoaderThreadHandler = new Handler(getLooper(), this);

            if (mPendingRequest != null && mPendingRequest.size() > 0) {
                for (LocalFileInfo lfi : mPendingRequest) {
                    sendRequestLoadingMsg(lfi);
                }

                mPendingRequest.clear();
            }
        }
    }

    private void sendRequestLoadingMsg(LocalFileInfo lfi) {
        Message msg = mLoaderThreadHandler.obtainMessage();
        msg.obj = lfi;
        msg.sendToTarget();
    }

    @Override
    public boolean handleMessage(Message msg) {
        final LocalFileInfo lfi = (LocalFileInfo) msg.obj;
        String url = lfi.url;
        ImageAlbumCoverView targetView = lfi.target;
        Utils.debug(TAG, "handle request: " + url);

        Bitmap bitmap = null;
        if (!TextUtils.isEmpty(lfi.path) && new File(lfi.path).exists()) {
            Utils.debug(TAG, "load bitmap locally: " + url);
            bitmap = targetView.loadBitmapFromFile(lfi);
        }

        if (bitmap != null) {
            Utils.debug(TAG, "load bitmap succeed: " + url);
            if (mImageLoadedCallback != null) {
                mImageLoadedCallback.onImageLoaded(bitmap, lfi);
            }
            return true;
        } else {
            Utils.debug(TAG, "load from net: " + url);
            try {
                new AsyncTask<Void, Void, Bitmap>() {
                    @Override
                    protected Bitmap doInBackground(Void... params) {
                        Utils.debug(TAG, "load from net end:  " + lfi.url);

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
                        } catch (OutOfMemoryError e) {
                            System.gc();
                            Utils.debug(TAG, "out of memory, causing gc...");
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Bitmap result) {
                        if (mImageLoadedCallback != null) {
                            mImageLoadedCallback.onImageLoaded(result, lfi);
                        }

                        if (result != null) {
                            Utils.debug(TAG, "download bitmap failed: " + lfi.url);
                        }
                    }

                }.execute();
            } catch (Exception e) {
                Utils.debug(TAG, "executing async task failed!!!");
                Utils.error(getClass(), Utils.getStackTrace(e));

                if (mImageLoadedCallback != null) {
                    mImageLoadedCallback.onImageLoaded(null, lfi);
                }
            }
        }

        return true;
    }

    public void requestLoading(LocalFileInfo lfi) {
        Utils.debug(TAG, "requesting: " + lfi.url + ", targetView: " + lfi.target);
        if (mLoaderThreadHandler != null) {
            sendRequestLoadingMsg(lfi);
        } else {
            mPendingRequest.add(lfi);
        }
    }
}
