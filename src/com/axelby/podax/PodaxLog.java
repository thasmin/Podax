package com.axelby.podax;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class PodaxLog {

	public static void log(Context context, String format, Object... args) {
		try {
			// make sure debuggable flag is set before writing to the log
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
						context.getApplicationInfo().packageName,
						PackageManager.GET_CONFIGURATIONS);
			if (packageInfo == null)
				return;
			
			int flags = packageInfo.applicationInfo.flags;
			if ((flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0)
				return;
	
			String message = String.format(format, args);
			message = new SimpleDateFormat().format(new Date()) + " " + message;
			message += "\n";
			File file = new File(context.getExternalFilesDir(null), "podax.log");
			FileWriter out = new FileWriter(file, true);
			out.write(message);
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
