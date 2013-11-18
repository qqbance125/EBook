/**
 *
 */
package com.qihoo360.reader.ui;

/**
 * 表示该Activity或者View支持夜间模式
 *
 * @author Jiongxuan Zhang
 *
 */
public interface Nightable {
    /**
     * 针对夜间模式做的操作
     */
    public void onUpdateNightMode();
}
