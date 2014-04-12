package com.axelby.podax;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import java.io.File;

public class Storage {
	public static File getExternalStorageDirectory(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String storageCard = prefs.getString("storageCard", "/storage/sdcard0");
		if (new File(storageCard).exists())
			return new File(storageCard);
		return Environment.getExternalStorageDirectory();
	}
}
