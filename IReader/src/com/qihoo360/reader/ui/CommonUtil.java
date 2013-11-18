package com.qihoo360.reader.ui;

import com.qihoo360.reader.R;
import com.qihoo360.reader.ReaderApplication;
import com.qihoo360.reader.image.BitmapHelper;
import com.qihoo360.reader.support.Utils;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.util.Date;

public class CommonUtil {
	/**
	 * 分享图片
	 * 
	 * @param c
	 * @param title
	 */
	public static final void shareChannel(Context c, String title) {
		Intent send = new Intent(Intent.ACTION_SEND);
		send.setType("text/plain");
		send.putExtra(Intent.EXTRA_TEXT, "我正在通过360安全浏览器阅读\"" + title
				+ "\"，内容很有趣啊，你快来试一试，http://mse.360.cn");
		send.putExtra(Intent.EXTRA_SUBJECT, title);
		try {
			c.startActivity(Intent.createChooser(send,
					c.getString(R.string.rd_channel_share)));
		} catch (android.content.ActivityNotFoundException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * 分享内容
	 * 
	 * @param c
	 * @param title
	 * @param url
	 */
	public static final void sharePage(Context c, String title, String url) {
		Intent send = new Intent(Intent.ACTION_SEND);
		send.setType("text/plain");
		send.putExtra(Intent.EXTRA_TEXT,
				title + " " + url + c.getString(R.string.rd_share_tips));
		send.putExtra(Intent.EXTRA_SUBJECT, title);
		try {
			c.startActivity(Intent.createChooser(send,
					c.getString(R.string.rd_news_share)));
		} catch (android.content.ActivityNotFoundException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * 分享图片
	 * 
	 * @param c
	 * @param url
	 * @param category
	 */
	public static final void shareImage(Context c, String url, String category) {
		Intent send = new Intent(Intent.ACTION_SEND);
		send.setType("image/jpeg");
		send.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + url));
		send.putExtra(Intent.EXTRA_TEXT, "分享自360安全浏览器" + category
				+ " http://mse.360.cn");
		send.putExtra(Intent.EXTRA_SUBJECT, "360图片分享");
		try {
			c.startActivity(Intent.createChooser(send,
					c.getString(R.string.rd_image_share)));
		} catch (android.content.ActivityNotFoundException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * @param 上一次更新访问次数的时间
	 * @return 返回是否更新主页订阅页面访问次数
	 */
	public static final boolean shouldUpdateVisit(long lastVisitTime) {
		long last = lastVisitTime;
		Date lastDate = new Date(last);
		Date toDay = new Date();
		boolean shoudlUpdate = false;
		if (lastDate.getYear() != toDay.getYear()) {
			shoudlUpdate = true;
		} else {
			if (lastDate.getMonth() != toDay.getMonth()) {
				shoudlUpdate = true;
			} else {
				if (lastDate.getDay() != toDay.getDay()) {
					shoudlUpdate = true;
				}
			}
		}

		return shoudlUpdate;
	}

	/***
	 * 获取手机是否处于锁屏状态
	 * 
	 * @return boolean 是否处于锁屏状态
	 */
	public static boolean isInLock() {
		KeyguardManager manager = (KeyguardManager) ReaderApplication
				.getContext().getSystemService(Context.KEYGUARD_SERVICE);
		return manager.inKeyguardRestrictedInputMode();
	}

	private static Toast sToast = null;

	public static void showToast(int resId) {
		if (sToast == null) {
			sToast = Toast.makeText(ReaderApplication.getContext(), "",
					Toast.LENGTH_SHORT);
		}
		sToast.setText(resId);
		sToast.show();
	}

	public static void showToast(String str) {
		if (sToast == null) {
			sToast = Toast.makeText(ReaderApplication.getContext(), "",
					Toast.LENGTH_SHORT);
		}
		sToast.setText(str);
		sToast.show();
	}

	private static Bitmap bg;
	private static int padding = 0;
	private static final String SNAPSHOT = "snapshot";

	/**
	 * 2张图片叠加
	 * 
	 * @param context
	 * @param bitmap
	 * @param paddingValue
	 * @return
	 */
	public static Bitmap combineBmp(Context context, Bitmap bitmap,
			int paddingValue) {
		try {
			if (bg == null) {
				// 背景图片
				bg = BitmapHelper.decodeResource(context.getResources(),
						R.drawable.rd_ninegrid);
				// 间距
				padding = context.getResources().getDimensionPixelSize(
						R.dimen.rd_gridview_snap_padding);
			}

			if (bitmap == null)
				return bg;

			Bitmap composed = bg.copy(Config.ARGB_8888, true);
			
			if (composed == null) {
				return bg;
			}
			Canvas canvas = new Canvas(composed);

			Paint paint = new Paint();
			paint.setAntiAlias(true);

			int width = bitmap.getWidth();
			int height = bitmap.getHeight();
			int bgWidth = composed.getWidth();
			int bgHeight = composed.getHeight();

			if (paddingValue == 0) {
				int offsetX = (bgWidth - width) / 2;
				int offsetY = (bgHeight - height) / 2;
				canvas.drawBitmap(bitmap, new Rect(0, 0, width, height),
						new Rect(offsetX, offsetY, width + offsetX, height
								+ offsetY), paint);
			} else {
				canvas.drawBitmap(bitmap, new Rect(0, 0, width, height),
						new Rect(paddingValue, paddingValue, bgWidth
								- paddingValue, bgHeight - paddingValue), paint);
			}
			return composed;

		} catch (OutOfMemoryError ie) {
			Utils.debug("combineBmp",
					"decodeByteArray: still no memory after gc...");
			return bg;
		}
	}

	public static Bitmap combineBmpForBrowser(Context context, Bitmap bitmap,
			int paddingValue) {
		try {
			if (bg == null) {
				bg = BitmapHelper.decodeResource(context.getResources(),
						R.drawable.rd_ninegrid);
				padding = context.getResources().getDimensionPixelSize(
						R.dimen.rd_gridview_snap_padding);
			}

			if (bitmap == null)
				return bg;

			Bitmap composed = bg.copy(Config.ARGB_8888, true);
			Canvas canvas = new Canvas(composed);

			Paint paint = new Paint();
			paint.setAntiAlias(true);
			canvas.drawBitmap(
					bitmap,
					new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()),
					new Rect(paddingValue, paddingValue, composed.getWidth()
							- paddingValue, composed.getHeight() - paddingValue),
					paint);
			return composed;

		} catch (OutOfMemoryError ie) {
			Utils.debug("combineBmp",
					"decodeByteArray: still no memory after gc...");
			return bg;
		}
	}

	public static Bitmap combineBmp(Context context, Bitmap bitmap) {
		return combineBmp(context, bitmap, 0);
	}

	public static Bitmap combineBmpForBrowser(Context context, Bitmap bitmap) {
		return combineBmpForBrowser(context, bitmap, 0);
	}

	public static Bitmap combineBmp(Context context, Bitmap bitmap, String type) {
		int paddingValue = type.endsWith(SNAPSHOT) ? padding : 0;
		return combineBmp(context, bitmap, paddingValue);
	}

	public static Bitmap combineBmpForBrowser(Context context, Bitmap bitmap,
			String type) {
		int paddingValue = type.endsWith(SNAPSHOT) ? padding : 0;
		return combineBmpForBrowser(context, bitmap, paddingValue);
	}

	public static Bitmap combineBmp(Context context, int drawable) {
		Bitmap drawableBitmap = BitmapHelper.decodeResource(
				context.getResources(), drawable);
		if (drawableBitmap != null) {
			Bitmap bitmap = combineBmp(context, drawableBitmap);
			drawableBitmap.recycle();
			return bitmap;
		} else {
			return null;
		}
	}

	/**
	 * 返回一些设备 的id
	 * 
	 * @param context
	 * @return
	 */
	public static String getFeedbackUrlString(Context context) {
		final String NullString = "null";
		final String parameterDelimiter = "&";
		final String BaseUrl = "http://s.mse.360.cn/feedback/index?ua=aphone";

		final String versionParameter = "appversionname=";
		final String deviceNameParameter = "deviceName=";
		final String deviceIdParameter = "deviceId=";
		final String androidOSVersionParameter = "os=";

		String versionName = versionParameter + NullString;
		try {
			versionName = versionParameter
					+ String.valueOf(context.getPackageManager()
							.getPackageInfo(context.getPackageName(), 0).versionName);
		} catch (Exception e) {
			Log.e("Exception",
					"NavigationPageView.getFeedbackUrlString throw exception, message: "
							+ e.getMessage());
		}

		String deviceName = deviceNameParameter + Build.MODEL;
		String androidOSVersion = androidOSVersionParameter
				+ Build.VERSION.RELEASE;

		TelephonyManager telephonyManager = (TelephonyManager) context
				.getSystemService(Activity.TELEPHONY_SERVICE);
		String deviceId = deviceIdParameter + telephonyManager.getDeviceId();
		return BaseUrl + parameterDelimiter + versionName + parameterDelimiter
				+ androidOSVersion + parameterDelimiter + deviceName;
	}

}
