package com.axelby.podax;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PodaxLog {

	public static void ensureRemoved(Context context) {
		if (isDebuggable(context))
			return;
		File file = new File(context.getExternalFilesDir(null), "podax.log");
		if (file.exists())
			file.delete();
	}

	public static void log(Context context, String format, Object... args) {
		try {
			if (!isDebuggable(context))
				return;

			String message = String.format(format, args);
			message = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()) + " " + message;
			message += "\n";
			File file = new File(context.getExternalFilesDir(null), "podax.log");
			FileWriter out = new FileWriter(file, true);
			out.write(message);
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static boolean isDebuggable(Context context) {
		// make sure debuggable flag is set before writing to the log
		PackageInfo packageInfo;
		try {
			packageInfo = context.getPackageManager().getPackageInfo(
					context.getApplicationInfo().packageName,
					PackageManager.GET_CONFIGURATIONS);
		} catch (NameNotFoundException e) {
			return false;
		}
		if (packageInfo == null)
			return false;

		int flags = packageInfo.applicationInfo.flags;
        return (flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;

    }

}
