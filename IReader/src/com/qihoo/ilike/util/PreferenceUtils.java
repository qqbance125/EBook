package com.qihoo.ilike.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.qihoo360.reader.ReaderApplication;

public class PreferenceUtils {
	private static final String NAME = "config";

	private static final SharedPreferences SHARED = ReaderApplication
			.getContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);

	public static final String EMAIL = "e-mail";

	public static final String QID = "qid";

	public static void putString(String name, String value) {
		Editor editor = SHARED.edit();
		editor.putString(name, value);
		editor.commit();
	}

	public static String getString(String name) {
		return SHARED.getString(name, "");
	}
}
