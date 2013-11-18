/**
 *
 */
package com.qihoo360.reader.support;

import com.qihoo360.reader.Constants;
import com.qihoo360.reader.ReaderApplication;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.subscription.reader.RssManager;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 有关文件操作的工具。
 *
 * @author Jiongxuan Zhang
 *
 */
public class FileUtils {

    /**
     * 创建.NoMedia文件夹，如果路径存在的话则创建，否则不创建。
     * 写自2011-10-28
     *
     * @param path
     * @return
     * @throws IOException
     */
    public static boolean createNoMediaFileIfPathExists(String path) throws IOException {
        File file = new File(path);
        if (file.exists() && file.isDirectory()) {
            File noMediaFile = new File(path + ".nomedia");
            noMediaFile.mkdir();
            return true;
        }
        return false;
    }

    /**
     * 确保Image的目录都存在
     */
    public static void ensureImageDir() {
        String path = Constants.LOCAL_PATH_IMAGES + "articles/";
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
            createNoMediaFileIfPathExists(path);
        } catch (IOException e) {
            Utils.error(FileUtils.class, Utils.getStackTrace(e));
        }

        path = Constants.LOCAL_PATH_IMAGES + "full_size_images/";
        dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
            createNoMediaFileIfPathExists(path);
        } catch (IOException e) {
            Utils.error(FileUtils.class, Utils.getStackTrace(e));
        }
    }

    /**
     * 判断外部存储（SD卡）是否可用。
     *
     * @return
     */
    public static boolean isExternalStorageAvail() {
        // 解决“M9检查SD卡会失败”的问题 -Jiongxuan
        return android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);
    }

    /**
     * 获取存在本地的文件路径
     * @param context
     * @param fileName
     * @return
     */
    public static String getFilePathToSave(Context context, String fileName) {
        String fileDir = Settings.getReaderDir();
        if (TextUtils.isEmpty(fileDir)) {
            fileDir = context.getApplicationInfo().dataDir + "/reader";
            File dir = new File(fileDir);
            boolean isCreated = false;
            if (!dir.exists()) {
                try {
                    isCreated = dir.mkdir();
                } catch (Exception e) {
                    Utils.error(FileUtils.class, Utils.getStackTrace(e));
                    isCreated = false;
                }

                if (!isCreated) {
                    fileDir = context.getCacheDir().getPath() + "/readerCache";
                    dir = new File(fileDir);
                    if (!dir.exists()) {
                        try {
                            isCreated = dir.mkdir();
                        } catch (Exception e) {
                            Utils.error(FileUtils.class, Utils.getStackTrace(e));
                            isCreated = false;
                        }
                        if (!isCreated)
                            return null;
                    }

                }
            }
            Settings.setReaderDir(fileDir);
        }

        return fileDir + "/" + fileName;
    }

    public static void copyRawToCache(int rawId, String fileName) {
        try {
            // 要把自带的json文件复制到Cache目录下，然后读取它
            InputStream in = ReaderApplication.getContext().getResources()
                    .openRawResource(rawId);
            String filePath = getFilePathToSave(
                    ReaderApplication.getContext(), fileName);
            if (TextUtils.isEmpty(filePath)) {
                return;
            }

            OutputStream out = new FileOutputStream(filePath);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }

            in.close();
            out.close();
        } catch (Exception e) {
            Utils.error(FileUtils.class, Utils.getStackTrace(e));
        }
    }
 

    public static String getTextFromReaderFile(int rawId, String fileName) {
        String jsonString = "";
        try {
            File file = new File(getFilePathToSave(
                    ReaderApplication.getContext(), fileName));
            if (file.exists() == false) {
                copyRawToCache(rawId, fileName);
            }

            if (!file.exists()) {
                Utils.debug(RssManager.class, "cache\\%s is not exists!", fileName);
                // TODO RAW里面取
            }
            BufferedInputStream bufferedInputStream = new BufferedInputStream(
                    new FileInputStream(file));
            long contentLength = file.length();
            ByteArrayOutputStream outstream = new ByteArrayOutputStream(
                    contentLength > 0 ? (int) contentLength : 1024);
            byte[] buffer = new byte[4096];
            int len;
            while ((len = bufferedInputStream.read(buffer)) > 0) {
                outstream.write(buffer, 0, len);
            }
            outstream.close();
            jsonString = outstream.toString();
        } catch (FileNotFoundException e) {
            Utils.error(FileUtils.class, Utils.getStackTrace(e));
            jsonString = null;
        } catch (IOException e) {
            Utils.error(FileUtils.class, Utils.getStackTrace(e));
            jsonString = null;
        }

        return jsonString;
    }
}
