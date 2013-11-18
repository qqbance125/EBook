package com.qihoo.ilike.util;

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.view.View;
import android.widget.ImageView;

public class ImageStateSwitcher {
	public final static float[] BT_PRESSED = new float[] { 1, 0, 0, 0, 11, 0,
			1, 0, 0, -42, 0, 0, 1, 0, -202, 0, 0, 0, 4, 0 };

	public final static float[] BT_DEFAULT = new float[] { 1, 0, 0, 0, 0, 0, 1,
			0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0 };

	public final static float[] BT_SELECTED = new float[] { 1, 0, 0, 0, -106,
			0, 1, 0, 0, 6, 0, 0, 1, 0, -205, 0, 0, 0, 1, 0 };

	public static void pressedImage(ImageView v) {
		v.setColorFilter(new ColorMatrixColorFilter(BT_PRESSED));
	}

	public static void releasedImage(ImageView v) {
		v.setColorFilter(new ColorMatrixColorFilter(BT_DEFAULT));
	}

	public static void selectedImage(ImageView v) {
		v.setColorFilter(new ColorMatrixColorFilter(BT_SELECTED));
	}

	public static void pressedBackground(View v) {
		ColorMatrix cm = new ColorMatrix();
		cm.setSaturation(0);
		ColorMatrixColorFilter cf = new ColorMatrixColorFilter(cm);
		v.getBackground().setColorFilter(cf);
		v.setBackgroundDrawable(v.getBackground());
	}

	public static void releasedBackground(View v) {
		v.getBackground().clearColorFilter();
		v.setBackgroundDrawable(v.getBackground());
	}
}
