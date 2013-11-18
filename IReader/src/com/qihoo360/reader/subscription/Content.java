/**
 *
 */

package com.qihoo360.reader.subscription;

import com.qihoo360.reader.subscription.reader.RssArticle;

import java.util.ArrayList;
import java.util.List;

/**
 * 表示“Rss内容”，里面包含所有要读取的信息
 *
 * @author Jiongxuan Zhang
 */
public class Content {
	public int response; // 0 1....

	public String channel;

	public int number;

	public List<RssArticle> entry;

	public Content() {

	}

	/**
	 * 获取所有的Rss的内容。
	 *
	 * @return
	 */
	public List<RssArticle> getItems() {
		if (entry == null) {
			return new ArrayList<RssArticle>();
		} else {
			return entry;
		}
	}

	/**
	 * 判断是否是有效的Content
	 *
	 * @return
	 */
	public boolean isOk() {
		return response == 1 && getItems().size() != 0;
	}

	/**
	 * 是否是不存在的
	 *
	 * @return
	 */
	public boolean isNotExists() {
		return response == 2 || (response == 1 && getItems().size() == 0);
	}
}
