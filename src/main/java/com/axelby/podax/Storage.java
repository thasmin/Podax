package com.axelby.podax;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import java.io.File;

public class Storage {
	private static String getExternalStorageDirectory(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String storageCard = prefs.getString("storageCard", Environment.getExternalStorageDirectory().getPath());
		if (new File(storageCard).exists())
			return storageCard;
		return Environment.getExternalStorageDirectory().getAbsolutePath();
	}

	public static String getStoragePath(Context context) {
		return getExternalStorageDirectory(context) + "/Android/data/com.axelby.podax/files/";
	}

	public static void moveFilesTo(Context context, String newPath) {
		String oldPath = Storage.getExternalStorageDirectory(context);
		if (oldPath.equals(newPath))
			return;

		String addition = "/Android/data/com.axelby.podax/files";
		File oldStorageDir = new File(oldPath, addition);
		if (!oldStorageDir.exists())
			return;

		File newPathDir = new File(newPath, addition);
		if (!newPathDir.exists())
			if (!newPathDir.mkdirs())
				return;

		File[] filesToMove = oldStorageDir.listFiles();
		if (filesToMove == null)
			return;

		for (File from : filesToMove)
			from.renameTo(new File(newPathDir, from.getName()));
	}
}
