/**
 *
 */

package com.qihoo360.reader.push;

import com.qihoo360.reader.R;
import com.qihoo360.reader.ServerUris;
import com.qihoo360.reader.Settings;
import com.qihoo360.reader.json.JsonGetterBase;
import com.qihoo360.reader.offline.OfflineQueue;
import com.qihoo360.reader.subscription.Article;
import com.qihoo360.reader.subscription.Channel;
import com.qihoo360.reader.subscription.reader.RssChannel;
import com.qihoo360.reader.support.AsyncTask;
import com.qihoo360.reader.support.Utils;
import com.qihoo360.reader.ui.articles.ArticleUtils;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.Context;
import android.text.TextUtils;

/**
 * 真正需要Push操作的类
 *
 * @author Jiongxuan Zhang
 */
public class PushTask extends AsyncTask<Integer, Integer, Integer> {

	private Context mContext;
	private ContentResolver mContentResolver;
	private Channel mCurrentChannel;

	private static final int RESULT_OK = 1;

	private JsonGetterBase mJsonGetter;

	class PushObject {
		int response;
		String channel;
		long contentId;
	}

	class PushJson extends JsonGetterBase {

		@Override
		public Object parse(String jsonString) {
			PushObject pushObject = new PushObject();

			try {
				JSONObject jsonObject = new JSONObject(jsonString);
				pushObject.response = jsonObject.getInt("response");
				pushObject.channel = jsonObject.getString("channel");
				pushObject.contentId = jsonObject.getLong("contentid");
			} catch (JSONException e) {
				Utils.error(getClass(), Utils.getStackTrace(e));
			}
			return pushObject;
		}
	}

	JsonGetterBase getJsonGetter() {
		if (mJsonGetter == null) {
			mJsonGetter = new PushJson();
		}

		return mJsonGetter;
	}

	/**
	 * 初始化
	 *
	 * @param context
	 */
	PushTask(Context context) {
		if (context == null) {
			throw new IllegalArgumentException("context is null");
		}

		mContext = context;
		mContentResolver = mContext.getContentResolver();
	}

	/**
	 * 取消正在进行的Push
	 */
	public void cancel() {
		if (mCurrentChannel != null) {
			mCurrentChannel.stopGet();
		}

		cancel(true);
	}

	/**
	 * 获取当前正在请求Push的频道，为Null表示还没有开始
	 *
	 * @return
	 */
	public Channel getCurrentChannel() {
		return mCurrentChannel;
	}

	@Override
	protected Integer doInBackground(Integer... params) {
		// 获取要Push的频道
		PushObject pushObject = getPushContent();

		if (pushObject == null) {
			PushManager.sPushTask = null;
			return null;
		}

		int result = RssChannel.RESULT_NOT_EXISTS;
		Channel channel = RssChannel.get(pushObject.channel);

		if (channel != null) {

			// 如果当前正在阅览该频道，则不需要Push
			if (ArticleUtils.isDisplaying(mCurrentChannel)) {
				PushManager.sPushTask = null;
				return null;
			}

			// 如果当前正离线下载该频道，则也不需要Push
			if (OfflineQueue.isRunning(mCurrentChannel)) {
				PushManager.sPushTask = null;
				return null;
			}

			mCurrentChannel = channel;
			long newestIdInDb = mCurrentChannel
					.getNewestContentId(mContentResolver);

			// 只有服务器推给我们的content ID比数据库里的新，才向服务器请求
			if (pushObject.contentId > newestIdInDb) {
				result = mCurrentChannel.getArticlesRangeSync(mContentResolver,
						null, pushObject.contentId, newestIdInDb, 0, true);

				if (result == RssChannel.RESULT_OK) {
					Article article = mCurrentChannel.getArticle(
							mContentResolver, pushObject.contentId);

					if (article != null && !isCancelled()) {
						// 我们需要下载它的详细内容，不管是否成功。这样做可以方便用户在点按通知后，能直接阅读
						article.fillSync(mContentResolver);

						PushNotificationBar pushNotificationBar = PushNotificationBar
								.get(mContext);
						pushNotificationBar.cancel();
						pushNotificationBar.push(
								mContext.getString(R.string.rd_push_title),
								article.title, mCurrentChannel.channel, 0);
						Settings.setPushedTime();
						Settings.setPushedChannel(pushObject.channel);
						Settings.setPushedContentId(pushObject.contentId);

					} else {
						Utils.error(getClass(),
								"The %s does not have a newest article",
								pushObject.channel);
					}
				}
			}

		}

		PushManager.sPushTask = null;
		mCurrentChannel = null;
		return null;
	}
	/**
	 * 获取推送的内容
	 */
	private PushObject getPushContent() {
		String url = ServerUris.getPush(Settings.getPushedChannel(),
				Settings.getPushedContentId());
		String jsonString = getJsonGetter().get(url);

		if (!TextUtils.isEmpty(jsonString)) {
			PushObject pushObject = (PushObject) getJsonGetter().parse(
					jsonString);

			if (pushObject.response == RESULT_OK) {
				return pushObject;
			}
		}

		return null;
	}
}
