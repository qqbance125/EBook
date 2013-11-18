package com.qihoo.ilike.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class IOUtils {

    public static File creatFile(String fileName) throws IOException {
        File file = new File(fileName);
        file.createNewFile();
        return file;
    }

    public static boolean checkDirectory(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            return file.mkdirs();
        }
        return true;
    }

    public static File creatDir(String dirName) {
        File dir = new File(dirName);
        dir.mkdirs();
        return dir;
    }

    public static Bitmap loadBitmapFromFile(String dirName) {
        try {
            InputStream is = new FileInputStream(dirName);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            is.close();
            return bitmap;
        } catch (OutOfMemoryError e) {
            com.qihoo360.reader.support.Utils.error(IOUtils.class, com.qihoo360.reader.support.Utils.getStackTrace(e));
        } catch (FileNotFoundException e) {
            com.qihoo360.reader.support.Utils.error(IOUtils.class, com.qihoo360.reader.support.Utils.getStackTrace(e));
        } catch (IOException e) {
            com.qihoo360.reader.support.Utils.error(IOUtils.class, com.qihoo360.reader.support.Utils.getStackTrace(e));
        }
        return null;
    }

    public static boolean isExists(String fileName) {
        File file = new File(fileName);
        return file.exists();
    }

    public static boolean deleteFile(String fileName, FilenameFilter filter)
    {
        File file = new File(fileName);
        if (file.exists())
        {
            return file.isDirectory() ? deleteFile(file, filter) : file.delete();
        }
        return true;
    }

    private static boolean deleteFile(File dir, FilenameFilter filter) {
        for (File file : dir.listFiles(filter)) {
            if (file.isDirectory()) {
                if (!deleteFile(file, filter))
                    return false;
            } else {
                if (!file.delete())
                    return false;
            }
        }
        dir.delete();
        return true;
    }
}
