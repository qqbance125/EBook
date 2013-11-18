package com.qihoo.ilike.subscription;

public interface OnDownloadCategoryListResultListener {
    /**
     * 当更新成功时
     */
    public void onUpdated();

    /**
     * 当已经是最新版本时
     */
    public void onAlreadyLastestVersion();

    /**
     * 当失败时
     */
    public void onFailure();
}
