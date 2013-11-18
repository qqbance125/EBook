/**
 *
 */
package com.qihoo360.reader.subscription;

import com.qihoo360.reader.data.DataEntryManager;

import android.content.ContentResolver;
import android.database.Cursor;

/**
 * 表示已经是星标的文章
 *
 * @author Jiongxuan Zhang
 *
 */
public class StaredArticle {

	/**
	 * 获取标星文章的Cursor，不分频道
	 *
	 * @param resolver
	 * @return
	 */
	public static Cursor getCursor(ContentResolver resolver) {
		return DataEntryManager.ArticleHelper.Star.getCursor(resolver);
	}

	/**
	 * 清除收藏列表中的内容
	 *
	 * @param resolver
	 * @return
	 */
	public static int clear(ContentResolver resolver) {
		return DataEntryManager.ArticleHelper.Star.deleteAll(resolver);
	}
}
