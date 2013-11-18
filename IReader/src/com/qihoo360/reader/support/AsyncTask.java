/**
 *
 */
package com.qihoo360.reader.support;


/**
 * @author zhangjiongxuan
 *
 */
public abstract class AsyncTask<Params, Progress, Result> extends android.os.AsyncTask<Params, Progress, Result> {
    private volatile Status mStatus = Status.PENDING; // 表示尚未执行的任务。

    /**
     * 同步执行这个AsyncTask
     * @param params
     */
    public Result doSync(Params... params) {
        mStatus = Status.RUNNING;
        this.onPreExecute();
        Result result = this.doInBackground(params);
        this.onPostExecute(result);
        mStatus = Status.FINISHED;
        return result;
    }

    /**
     * 获取当前任务的Task，它同时支持异步和“同步”
     * @return
     */
    public Status getTaskStatus() {
        return mStatus;
    }
}
