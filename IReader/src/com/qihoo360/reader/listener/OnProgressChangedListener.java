/**
 *
 */
package com.qihoo360.reader.listener;

/**
 * 当进度发生变化时发生。
 *
 * @author Jiongxuan Zhang
 *
 */
public interface OnProgressChangedListener {
    /**
     * 当进度发生变化时发生
     *
     * @param progress
     * @param max
     */
    void onProgressChanged(Object tag, double progress, double max);
}
