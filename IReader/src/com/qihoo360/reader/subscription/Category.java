/**
 *
 */
package com.qihoo360.reader.subscription;

import com.qihoo360.reader.subscription.reader.RssChannel;

import java.util.ArrayList;
import java.util.List;

/**
 * 表示一个“Rss分类”
 *
 * @author Jiongxuan Zhang
 */
public class Category {
	/**
	 * 分类名
	 */
	public String name;

	/**
	 * 分类的类型，是RSS？还是微博？
	 */
	public int type;

	public List<RssChannel> entry;

	public Category() {
	}

	/**
	 * 获取频道列表
	 *
	 * @return
	 */
	public List<RssChannel> getChannels() {
		if (entry != null) {
			return entry;
		} else {
			return new ArrayList<RssChannel>();
		}
	}
}
