package com.axelby.podax;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import java.io.File;

public class Storage {
	public static File getExternalStorageDirectory(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if (!prefs.contains("storageCard"))
			return Environment.getExternalStorageDirectory();
		return new File(prefs.getString("storageCard", "/sdcard"));
	}
}
