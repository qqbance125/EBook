/**
 *
 */
package com.qihoo360.reader.subscription;

import java.util.ArrayList;
import java.util.List;

import com.qihoo360.reader.support.AnimationHelper;

/**
 * 表示“主页”
 *
 * @author Jiongxuan Zhang
 */
public class Index {
	public int response;

	public String version;

	public List<Category> category;

	public Index() {

	}

	/**
	 * 获取主页下的“RSS分类”列表
	 *
	 * @return
	 */
	public List<Category> getCategories() {
		if (category != null) {
			return category;
		} else {
			return new ArrayList<Category>();
		}
	}
}
