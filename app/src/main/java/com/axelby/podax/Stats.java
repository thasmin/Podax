package com.axelby.podax;

import android.content.Context;
import android.content.SharedPreferences;

import org.joda.time.Days;
import org.joda.time.LocalDate;

public class Stats {
	private Stats() { }

	public static void addListenTime(Context context, float seconds) {
		SharedPreferences statsPrefs = context.getSharedPreferences("stats", Context.MODE_PRIVATE);
		float listenTime = statsPrefs.getFloat("listenTime", 0f) + seconds;
		statsPrefs.edit().putFloat("listenTime", listenTime).apply();

		Days daysSince112015 = Days.daysBetween(LocalDate.now(), new LocalDate(2015, 1, 1));
		String todayKey = "day" + daysSince112015.getDays() + "listenTime";
		float todayListenTime = statsPrefs.getFloat(todayKey, 0f) + seconds;
		statsPrefs.edit().putFloat(todayKey, todayListenTime).apply();
	}

	// returns seconds
	public static float getListenTime(Context context) {
		SharedPreferences statsPrefs = context.getSharedPreferences("stats", Context.MODE_PRIVATE);
		return statsPrefs.getFloat("listenTime", 0f);
	}

	public static String getListenTimeString(Context context) {
        return Helper.getVerboseTimeString(context, Stats.getListenTime(context), true);
	}

	// returns seconds
	public static float getListenTime(Context context, LocalDate date) {
		SharedPreferences statsPrefs = context.getSharedPreferences("stats", Context.MODE_PRIVATE);
		Days daysSince112015 = Days.daysBetween(date, new LocalDate(2015, 1, 1));
		String todayKey = "day" + daysSince112015.getDays() + "listenTime";
		return statsPrefs.getFloat(todayKey, 0f);
	}

    public static void addCompletion(Context context) {
		SharedPreferences statsPrefs = context.getSharedPreferences("stats", Context.MODE_PRIVATE);
		int completions = statsPrefs.getInt("completions", 0) + 1;
		statsPrefs.edit().putInt("completions", completions).apply();
	}

	public static int getCompletions(Context context) {
		SharedPreferences statsPrefs = context.getSharedPreferences("stats", Context.MODE_PRIVATE);
		return statsPrefs.getInt("completions", 0);
	}
}
