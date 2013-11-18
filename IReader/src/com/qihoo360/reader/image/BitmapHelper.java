
package com.qihoo360.reader.image;

import com.qihoo360.reader.support.Utils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;

public class BitmapHelper {
    public final static String TAG = "BitmapHelper";

    public static Bitmap decodeFile(String filePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        // 使用BitmapFactory创建的Bitmap 用于存储Pixel的内存空间在系统内存不足时可以被回收
        options.inPurgeable = true;
        // 意味着有三个参数，R，G，B，三个参数分别占5bit，6bit，5bit.
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        return decodeFile(filePath, options);
    }

    public static Bitmap decodeFile(String filePath, int sizeLimit) {
        if (!new File(filePath).exists()) {
            return null;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options); // 此时返回为空

        options.inSampleSize = getCompressRatio(options.outWidth, options.outHeight, sizeLimit);
        options.inJustDecodeBounds = false;
        options.inPurgeable = true;
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        Utils.debug(TAG, "decodeFile - width: " + options.outWidth + ", height: " + options.outHeight + ", compress ratio: " + options.inSampleSize);

        return decodeFile(filePath, options);
    }

    public static Bitmap decodeFile(String filePath, BitmapFactory.Options options) {
        Bitmap bm = null;
        try {
            bm = BitmapFactory.decodeFile(filePath, options);
        } catch (OutOfMemoryError e) {
            System.gc();
            Utils.debug(TAG, "decodeFile: out of memory, causing gc...");

            try {
                bm = BitmapFactory.decodeFile(filePath, options);
            } catch (OutOfMemoryError ie) {
                Utils.debug(TAG, "decodeFile: still no memory after gc...");
                bm = null;
            }
        }
        return bm;
    }

    public static Bitmap decodeSampleFromFile(String filePath, int compressedRatio) {
        if (!new File(filePath).exists()) {
            return null;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = compressedRatio; //  缩小几倍
        options.inPurgeable = true;
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        return decodeFile(filePath, options);
    }

    public static Bitmap decodeByteArray(byte[] data, int offset, int length) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPurgeable = true;
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        return decodeByteArray(data, offset, length, options);
    }

    public static Bitmap decodeByteArrayWithCompressedRatio(byte[] data, int offset, int length,
            int compressRatio) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = compressRatio;
        options.inPurgeable = true;
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        return decodeByteArray(data, offset, length, options);
    }

    public static Bitmap decodeByteArray(byte[] data, int offset, int length, int sizeLimit) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, offset, length, options); // 此时返回为空

        options.inSampleSize = getCompressRatio(options.outWidth, options.outHeight, sizeLimit);
        options.inJustDecodeBounds = false;
        options.inPurgeable = true;
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        Utils.debug(TAG, "decodeFile - width: " + options.outWidth + ", height: " + options.outHeight + ", compress ratio: " + options.inSampleSize);

        return decodeByteArray(data, offset, length, options);
    }

    public static Bitmap decodeByteArray(byte[] data, int offset, int length, BitmapFactory.Options options) {
        Bitmap bm = null;
        try {
            bm = BitmapFactory.decodeByteArray(data, offset, length, options);
        } catch (OutOfMemoryError e) {
            System.gc();
            Utils.debug(TAG, "decodeByteArray: out of memory, causing gc...");

            try {
                bm = BitmapFactory.decodeByteArray(data, offset, length, options);
            } catch (OutOfMemoryError ie) {
                Utils.debug(TAG, "decodeByteArray: still no memory after gc...");
                bm = null;
            }
        }

        return bm;
    }
    /**
     *   获取压缩的比例
     *  @param bWidth
     *  @param bHeight
     *  @param sizeLimit
     *  @return    设定文件 
     */
    public static int getCompressRatio(int bWidth, int bHeight, int sizeLimit) {
        int compressedRatio = 1;
        if (bWidth < bHeight) {
            if (bWidth > sizeLimit) {
                compressedRatio = bWidth / sizeLimit;
            }
        } else {
            if (bHeight > sizeLimit) {
                compressedRatio = bHeight / sizeLimit;
            }
        }

        if (compressedRatio > 1) {
            if (compressedRatio % 2 != 0) {
                compressedRatio -= 1;
            }
        }

        return compressedRatio;
    }

    public static Bitmap getCompressedBitmap(String filePath, int widthLimit) {
        return getCompressedBitmap(decodeFile(filePath), widthLimit);
    }

    public static Bitmap getCompressedBitmap(byte[] bytes, int widthLimit) {
        return getCompressedBitmap(decodeByteArray(bytes, 0, bytes.length), widthLimit);
    }

    public static Bitmap getCompressedBitmap(Bitmap origin, int widthLimit) {
        if(origin == null) {
            return null;
        }

        int width = origin.getWidth();
        int height = origin.getHeight();
        if (width > widthLimit) {
            height = (int) ((float)height * widthLimit / width + 0.5f);
            width = widthLimit;
        } else {
            return origin;
        }

        Bitmap ret = null;
        try {
            ret = Bitmap.createScaledBitmap(origin, width, height, false);
        } catch (OutOfMemoryError e) {
            System.gc();
            Utils.debug(TAG, "getCompressedBitmap: out of memory, causing gc...");

            try {
                ret = Bitmap.createScaledBitmap(origin, width, height, false);
            } catch (OutOfMemoryError ie) {
                Utils.debug(TAG, "getCompressedBitmap: still not enough memory after gc, returning null ...");
                ret = null;
            }
        }
        return ret;
    }
    /**
     *  根据资源id获取图片
     *  @param res
     *  @param id
     *  @return    设定文件 
     */
    public static Bitmap decodeResource(Resources res, int id) {
        Bitmap bm = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        try {
            bm = BitmapFactory.decodeResource(res, id, options);
        } catch (OutOfMemoryError e) {
            System.gc();
            Utils.debug(TAG, "decodeResource: out of memory, causing gc...");

            try {
                bm = BitmapFactory.decodeResource(res, id, options);
            } catch (OutOfMemoryError ie) {
                Utils.debug(TAG, "decodeResource: still no memory after gc...");
                bm = null;
            }
        }
        return bm;
    }
}
