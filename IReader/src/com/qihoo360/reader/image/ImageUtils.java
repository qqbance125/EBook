
package com.qihoo360.reader.image;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.params.AbstractHttpParams;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.TextView;
import android.widget.Toast;

import com.qihoo360.reader.Constants;
import com.qihoo360.reader.R;
import com.qihoo360.reader.support.ArithmeticUtils;
import com.qihoo360.reader.support.NetUtils;
import com.qihoo360.reader.support.Utils;
import com.qihoo360.reader.ui.view.ImageAlbumCoverView.LocalFileInfo;

public class ImageUtils {
    public static final String TAG = "ImageUtils";
    public static final int FOLDER_SIZE_LIMIT_ARTICLES = 32 * 1024 * 1024;
    public static final int FOLDER_SIZE_REDUCE_TO_ARTICLES = 8 * 1024 * 1024;

    public static final int FOLDER_SIZE_LIMIT_FULL_SIZE_IMAGES = 128 * 1024 * 1024;
    public static final int FOLDER_SIZE_REDUCE_TO_FULL_SIZE_IMAGES = 32 * 1024 * 1024;

    public static final int CHECK_IMAGE_CACHE_DURATION = 24 * 60 * 60 * 1000;

    public static void cleanImageCache() {
        String[] dirsToClean = {
                Constants.LOCAL_PATH_IMAGES + "channel",
                Constants.LOCAL_PATH_IMAGES + "articles",
                Constants.LOCAL_PATH_IMAGES + "full_size_images",
        };

        for (String dir : dirsToClean) {
            File f = new File(dir);
            if (f.exists() && f.isDirectory()) {
                File imageFiles[] = f.listFiles();
                for (File imageFile : imageFiles) {
                    if (!imageFile.isDirectory()) {
                        imageFile.delete();
                    }
                }
            }
        }
    }

    public static void ensureImageCacheSize() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... arg0) {
                ensureImageCacheSize(Constants.LOCAL_PATH_IMAGES + "articles",
                        FOLDER_SIZE_LIMIT_ARTICLES,
                        FOLDER_SIZE_REDUCE_TO_ARTICLES);

                ensureImageCacheSize(Constants.LOCAL_PATH_IMAGES + "full_size_images",
                        FOLDER_SIZE_LIMIT_FULL_SIZE_IMAGES, FOLDER_SIZE_REDUCE_TO_FULL_SIZE_IMAGES);
                return null;
            }
        }.execute();
    }

    public static void ensureImageCacheSize(String folderName, int sizeLimit, int reduceTo) {
        if (TextUtils.isEmpty(folderName)) {
            return;
        }

        File folder = new File(folderName);
        if (folder.exists() && folder.isDirectory()) {
            File imageFiles[] = folder.listFiles();
            if (imageFiles == null || imageFiles.length <= 0) {
                return;
            }

            ArrayList<File> fileList = new ArrayList<File>(imageFiles.length);
            long totalSize = 0;
            for (File imageFile : imageFiles) {
                if (imageFile.isFile()) {
                    totalSize += imageFile.length();
                    fileList.add(imageFile);
                }
            }

            if (totalSize > sizeLimit) {
                // exceeds the limit
                Collections.sort(fileList, new Comparator<File>() {
                    public final int compare(File a, File b) {
                        return (int) (a.lastModified() - b.lastModified()); //   修改时间的差值
                    }
                });

                for (File file : fileList) {
                    long size = file.length();
                    if (file.delete()) {
                        totalSize -= size;
                        if (totalSize <= reduceTo) {
                            break;
                        }
                    }
                }
            } else {
                return;
            }
        }
    }

    public static HttpUriRequest getHttpRequestForImageDownload(Context context, String url,
            int quality, LocalFileInfo lfi) {
        int pos = url.lastIndexOf("/");
        if (pos > 0) {
            int widthLimit = ImageDownloadStrategy.getInstance(context).getScreenWidth();
            if (lfi != null && lfi.oriHeight > lfi.oriWidth && !NetUtils.isWifiConnected()) {
                widthLimit = widthLimit * 2 / 3;
            }

            int heightLimit = ImageDownloadStrategy.IMAGE_CONFIG_MAX_HEIGHT;

            if (lfi != null && (lfi.oriWidth > widthLimit || lfi.oriHeight > heightLimit)) {
                // 记录将下载图片的尺寸
                if(widthLimit * lfi.oriHeight > heightLimit * lfi.oriWidth) {
                    lfi.oriWidth = (lfi.oriWidth * heightLimit / lfi.oriHeight);
                    lfi.oriHeight = heightLimit;
                } else {
                    lfi.oriWidth = widthLimit;
                    lfi.oriHeight = (lfi.oriHeight * widthLimit / lfi.oriWidth);
                }
            }

            url = url.substring(0, pos) + "/dr/" + widthLimit + "_"
                    + ImageDownloadStrategy.IMAGE_CONFIG_MAX_HEIGHT + "_"
                    + ((quality > 0 && quality <= 100) ? quality : "") + url.substring(pos);
        }

        return getHttpRequestForImageDownload(url);
    }

    public static HttpUriRequest getHttpRequestForImageDownload(String url) {
        String noSpaceFullUrl = url.replace(" ", "%20");
        HttpGet httpRequest = new HttpGet(noSpaceFullUrl);
        AbstractHttpParams httpParameters = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, 20000);
        HttpConnectionParams.setSoTimeout(httpParameters, 20000);
        httpRequest.setParams(httpParameters);

        return httpRequest;
    }

    private static final int SDCARD_ERROR_TOAST_INTERVAL = 60 * 1000;
    private static long mLastShowSDCardErrorToastTime = -1;

    public static boolean writeImageDataToFile(final Context context, String path, byte[] data) {
        if (context == null || TextUtils.isEmpty(path) || data == null || data.length <= 0) {
            return false;
        }
        Log.i(TAG, "path = "+ data.length);
        try {
            BufferedOutputStream bos = new BufferedOutputStream(
                    new FileOutputStream(path, false), data.length);
            bos.write(data);
            bos.flush();
            bos.close();
        } catch (Exception e) {
            Utils.error(ImageUtils.class, Utils.getStackTrace(e));

            long now = System.currentTimeMillis();
            if (mLastShowSDCardErrorToastTime <= 0
                    || now - mLastShowSDCardErrorToastTime > SDCARD_ERROR_TOAST_INTERVAL) {
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context.getApplicationContext(),
                                context.getString(R.string.rd_write_file_to_sdcard_error),
                                Toast.LENGTH_SHORT).show();
                    }
                };
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(r);
                mLastShowSDCardErrorToastTime = now;
            }
            return false;
        }
        return true;
    }

    /**
     * png 转换为 Jpeg
     * @param bytes
     * @return
     */
    public static Bitmap convertPngToJpeg(byte[] bytes) {
        if (bytes == null || bytes.length <= 0) {
            return null;
        }

        Bitmap oldBitmap = BitmapHelper.decodeByteArray(bytes, 0, bytes.length);
        int width = oldBitmap.getWidth();
        int height = oldBitmap.getHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }

        try {
            Bitmap newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            Canvas canvas = new Canvas(newBitmap);
            canvas.drawColor(0xffffffff, android.graphics.PorterDuff.Mode.SRC);

            Paint paint = new Paint();
            paint.setAntiAlias(true);
            canvas.drawBitmap(oldBitmap, 0, 0, paint);
            return newBitmap;
        } catch (Exception e) {
            Utils.error(ImageUtils.class, Utils.getStackTrace(e));
            return null;
        }

    }
    /**
     * 文字生成图片
     * @param context
     * @param text
     * @param width
     * @param height
     * @return
     */
    public static String generateImageFromText (Context context, String text, int width, int height) {
        String filePath = Constants.LOCAL_PATH_IMAGES + "full_size_images/"
            + ArithmeticUtils.getMD5(text + "_" + width + "_" + height) + ".jpg";

        File file = new File(filePath);
        if(file.exists()) {
            return filePath;
        } else {
            TextView tv = (TextView) LayoutInflater.from(context).inflate(R.layout.ilike_no_image_article_textview, null);
            tv.setText(text);

            int factor = width*height;
            if(factor > 40000) {
                if(factor > 80000) {
                    tv.setTextSize(24);
                } else {
                    tv.setTextSize(18);
                }
            }

            int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
            int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
            tv.measure(widthMeasureSpec, heightMeasureSpec);
            tv.layout(0, 0, tv.getMeasuredWidth(), tv.getMeasuredHeight());
            tv.setDrawingCacheEnabled(true);
            Bitmap snapShot = tv.getDrawingCache();
            try {
                FileOutputStream fOutStream;
                fOutStream = new FileOutputStream(file);
                snapShot.compress(CompressFormat.JPEG, 100, fOutStream);
            } catch (FileNotFoundException e) {
                Utils.error(ImageUtils.class, Utils.getStackTrace(e));
                return null;
            }
            tv.setDrawingCacheEnabled(false);

            return filePath;
        }
    }

    /*
     * public static void checkChannelCover(Context context) {
     * Collection<Channel> list = Manager.mChannelMap.values(); String log =
     * "missing cover for channels: " + "\n"; for(Channel channel : list) {
     * String ch = "rd_" + channel.channel; int id =
     * context.getResources().getIdentifier(ch, "drawable",
     * context.getPackageName()); if(id <= 0) { log += channel.channel + "\n"; }
     * } Utils.debug(TAG, log); } public static void
     * findUselessChannelCovers(String path) { File dir = new File(path); String
     * log = ""; if(dir.isDirectory()) { for(File cover : dir.listFiles()) {
     * String fileName = cover.getName(); String channelName =
     * fileName.substring("rd_".length(), fileName.length() - ".jpg".length());
     * if(Manager.mChannelMap.get(channelName) == null) { log += fileName +
     * "\n"; } } } Utils.debug("remove channel covers: ", log); }
     */
}
