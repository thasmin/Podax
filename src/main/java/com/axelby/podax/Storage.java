package com.axelby.podax;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import java.io.File;

public class Storage {
	public static String getExternalStorageDirectory(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String storageCard = prefs.getString("storageCard", Environment.getExternalStorageDirectory().getPath());
		if (new File(storageCard).exists())
			return storageCard;
		return Environment.getExternalStorageDirectory().getAbsolutePath();
	}

	public static String getStorageDir(Context context) {
		return getExternalStorageDirectory(context) + "/Android/data/com.axelby.podax/files/";
	}
}
