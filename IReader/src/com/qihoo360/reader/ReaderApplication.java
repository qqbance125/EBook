/**
 *
 */
package com.qihoo360.reader;

import android.app.Application;
import android.content.Context;

/**
 * 和Browser配合的Application类
 *
 * @author Jiongxuan Zhang
 *
 */
public class ReaderApplication extends Application {

    static Context sContext;

    /**
     * 获取整个应用程序的Context。
     *
     * @return
     */
    public static Context getContext() {
        return sContext;
    }

    public final static void setContext(Context context){
        sContext = context;
    }

    /* (non-Javadoc)
     * @see android.app.Application#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();

        sContext = this.getApplicationContext();
    }
}
