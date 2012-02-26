package com.axelby.podax;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;

public class PodaxLog {

	public static void log(Context context, String format, Object... args) {
		String message = String.format(format, args);
		message = new SimpleDateFormat().format(new Date()) + " " + message;
		message += "\n";
		File file = new File(context.getExternalFilesDir(null), "podax.log");
		try {
			FileWriter out = new FileWriter(file, true);
			out.write(message);
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
