package com.axelby.podax;

import android.content.Context;
import android.content.SharedPreferences;

public class Stats {
	private Stats() { }

	public static void addTime(Context context, float seconds) {
		SharedPreferences statsPrefs = context.getSharedPreferences("stats", Context.MODE_PRIVATE);
		float listenTime = statsPrefs.getFloat("listenTime", 0f) + seconds;
		statsPrefs.edit().putFloat("listenTime", listenTime).commit();
	}

	public static float getTime(Context context) {
		SharedPreferences statsPrefs = context.getSharedPreferences("stats", Context.MODE_PRIVATE);
		return statsPrefs.getFloat("listenTime", 0f);
	}

	public static String getTimeString(Context context) {
		float listenSeconds = Stats.getTime(context);
		final int secondsPerMinute = 60;
		final int secondsPerHour = secondsPerMinute * 60;
		final int secondsPerDay = secondsPerHour * 24;

		StringBuilder listenText = new StringBuilder();
		if (listenSeconds > secondsPerDay) {
			int days = (int) Math.floor(listenSeconds / secondsPerDay);
			listenText.append(days);
			listenText.append(" ");
			listenText.append(context.getResources().getQuantityString(R.plurals.days, days));
			listenSeconds = listenSeconds % secondsPerDay;
		}
		if (listenSeconds > secondsPerHour) {
			int hours = (int) Math.floor(listenSeconds / secondsPerHour);
			listenText.append(hours);
			listenText.append(" ");
			listenText.append(context.getResources().getQuantityString(R.plurals.hours, hours));
			listenSeconds = listenSeconds % secondsPerHour;
		}
		if (listenSeconds > secondsPerMinute) {
			int minutes = (int) Math.floor(listenSeconds / secondsPerMinute);
			listenText.append(minutes);
			listenText.append(" ");
			listenText.append(context.getResources().getQuantityString(R.plurals.minutes, minutes));
		}
		if (listenText.length() == 0)
			listenText.append(context.getString(R.string.none));
		return listenText.toString();
	}

	public static void addCompletion(Context context) {
		SharedPreferences statsPrefs = context.getSharedPreferences("stats", Context.MODE_PRIVATE);
		int completions = statsPrefs.getInt("completions", 0) + 1;
		statsPrefs.edit().putInt("completions", completions).commit();
	}

	public static int getCompletions(Context context) {
		SharedPreferences statsPrefs = context.getSharedPreferences("stats", Context.MODE_PRIVATE);
		return statsPrefs.getInt("completions", 0);
	}
}
