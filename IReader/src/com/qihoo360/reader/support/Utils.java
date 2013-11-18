/**
 *
 */

package com.qihoo360.reader.support;

import com.qihoo360.reader.Constants;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.data.Provider;

import org.json.JSONObject;

import android.content.Context;
import android.os.Environment;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;


/**
 * 打Log的工具类。
 *
 * @author Jiongxuan Zhang
 */
public class Utils {

    /**
     * 打印正常的Debug信息
     *
     * @param cls
     * @param log
     */
    public static void debug(Class<?> cls, String log) {
        debug(cls.getSimpleName(), log);
    }

    /**
     * 打印正常的Debug信息，其后参数类似于String.format
     *
     * @param cls
     * @param log
     * @param objs
     */
    public static void debug(Class<?> cls, String log, Object... objs) {
        debug(cls.getSimpleName(), String.format(log, objs));
    }

    /**
     * 打印正常的Debug信息
     *
     * @param cls
     * @param log
     */
    public static void debug(String tag, String log) {
        if (Constants.DEBUG) {
            Log.d(tag, log);
        }
    }

    /**
     * 打印正常的Debug信息，其后参数类似于String.format
     *
     * @param cls
     * @param log
     * @param objs
     */
    public static void debug(String tag, String log, Object... objs) {
        debug(tag, String.format(log, objs));
    }

    /**
     * 打印错误信息
     *
     * @param cls
     * @param log
     */
    public static void error(Class<?> cls, String log) {
        error(cls.getSimpleName(), log);
    }

    /**
     * 打印错误信息，其后参数类似于String.format
     *
     * @param cls
     * @param log
     * @param objs
     */
    public static void error(Class<?> cls, String log, Object... objs) {
        error(cls.getSimpleName(), String.format(log, objs));
    }

    /**
     * 打印错误信息
     *
     * @param cls
     * @param log
     */
    public static void error(String tag, String log) {
        if (Constants.DEBUG) {
            Log.e(tag, log);
        }
    }

    /**
     * 打印错误信息，其后参数类似于String.format
     *
     * @param cls
     * @param log
     * @param objs
     */
    public static void error(String tag, String log, Object... objs) {
        error(tag, String.format(log, objs));
    }

    /**
     * 打印错误信息
     *
     * @param cls
     * @param log
     */
    public static void warning(Class<?> cls, String log) {
        warning(cls.getSimpleName(), log);
    }

    /**
     * 打印错误信息，其后参数类似于String.format
     *
     * @param cls
     * @param log
     * @param objs
     */
    public static void warning(Class<?> cls, String log, Object... objs) {
        warning(cls.getSimpleName(), String.format(log, objs));
    }

    /**
     * 打印错误信息
     *
     * @param cls
     * @param log
     */
    public static void warning(String tag, String log) {
        if (Constants.DEBUG) {
            Log.w(tag, log);
        }
    }

    /**
     * 打印错误信息，其后参数类似于String.format
     * @param cls
     * @param log
     * @param objs
     */
    public static void warning(String tag, String log, Object... objs) {
        warning(tag, String.format(log, objs));
    }

    /**
     * 获取堆栈信息
     *
     * @param t
     * @return
     */
    public static String getStackTrace(Throwable t)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        t.printStackTrace(pw);
        pw.flush();
        sw.flush();
        return sw.toString();
    }

    /**
     * 检查JSON的可用性
     * @param strJsonText
     * @return
     */
    public static boolean checkJsonValidation(String strJsonText) {
        try {
            new JSONObject(strJsonText);
        } catch (Exception e) {
            Utils.error(Utils.class, Utils.getStackTrace(e));
            return false;
        }

        return true;
    }

    /**
     * 获取客户端是否被更新的开关
     * @param context
     * @return
     */
    public static boolean isClientUpdated(Context context) {
        int currentVersion = Settings.getAppVersionInSp();
        int clientVersion = Settings.getCurrentAppVersion();

        return clientVersion != currentVersion;
    }

    public static CharSequence applyAutolink(Context context, CharSequence text, int start,
            int end, LinkifyUtil.OnLinkClickListener listener, TextView v) {
        CharSequence result = LinkifyUtil.matchLinks(listener, text);
        if (result instanceof Spannable) {
            v.setMovementMethod(LinkMovementMethod.getInstance());
        }
        return result;
    }
    public static boolean backupDatabase(Context context) {
        boolean writeable = FileUtils.isExternalStorageAvail();
        if (writeable) {
            String dataFilePath = Environment.getDataDirectory() + "/data/"
                    + context.getPackageName() + "/databases/" + Provider.DATABASE_NAME;
            File file = new File(dataFilePath);

            String fileBackupPath = Constants.LOCAL_PATH_READER + "/backup/";
            File fileBackupDir = new File(fileBackupPath);
            if (!fileBackupDir.exists()) {
                fileBackupDir.mkdirs();
            }

            if (file.exists()) {
                return copyFile(dataFilePath, fileBackupPath + Provider.DATABASE_NAME);
            }
        }

        return false;
    }

    public static boolean backupSharedPreferences(Context context) {
        boolean writeable = FileUtils.isExternalStorageAvail();
        if (writeable) {
            String dirPath = Environment.getDataDirectory() + "/data/"
                    + context.getPackageName() + "/shared_prefs/";
            File dir = new File(dirPath);

            if (dir.exists()) {
                String fileBackupPath = Constants.LOCAL_PATH_READER + "/backup/";
                for(String fileName : dir.list()) {
                    if(fileName.endsWith(".xml")) {
                        copyFile(dirPath + fileName, fileBackupPath + fileName);
                    }
                }
                return true;
            }
        }

        return false;
    }

    public static boolean copyFile(String src, String des) {
        try {
            if (new File(src).exists()) {
                InputStream is = new FileInputStream(src);
                OutputStream os = new FileOutputStream(des, false);
                byte[] b = new byte[4098];
                int length;
                while ((length = is.read(b)) > 0) {
                    os.write(b, 0, length);
                }
                is.close();
                os.close();
                return true;
            }
        } catch (Exception e) {
            Utils.error(Utils.class, Utils.getStackTrace(e));
        }
        return false;
    }

    public static boolean restoreDataBase(Context context) {
        boolean writeable = FileUtils.isExternalStorageAvail();
        if (writeable) {
            File databaseDir = new File(Environment.getDataDirectory() + "/data/"
                    + context.getPackageName() + "/databases/");
            if(!databaseDir.exists()) {
                databaseDir.mkdirs();
            }

            String backup = Constants.LOCAL_PATH_READER + "/backup/" + Provider.DATABASE_NAME;
            File backupFile = new File(backup);
            if (backupFile.exists()) {
                return copyFile(backup, databaseDir.getAbsolutePath() + "/" + Provider.DATABASE_NAME);
            }
        }

        return false;
    }
}
