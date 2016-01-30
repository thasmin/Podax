package com.axelby.podax;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.view.View;

public class Helper {

	public static boolean isInvalidNetworkState(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo == null || !netInfo.isConnectedOrConnecting())
			return true;
		// always OK if we're on wifi
		if (netInfo.getType() == ConnectivityManager.TYPE_WIFI)
			return false;
		// check for wifi only pref
		if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("wifiPref", true))
			return true;
		// check for 3g data turned off
		return !netInfo.isConnected();
	}

	public static String getTimeString(int milliseconds) {
		int seconds = milliseconds / 1000;
		final int SECONDSPERHOUR = 60 * 60;
		final int SECONDSPERMINUTE = 60;
		int hours = seconds / SECONDSPERHOUR;
		int minutes = seconds % SECONDSPERHOUR / SECONDSPERMINUTE;
		seconds = seconds % SECONDSPERMINUTE;

		StringBuilder builder = new StringBuilder();
		if (hours > 0) {
			builder.append(hours);
			builder.append(":");
			if (minutes < 10)
				builder.append("0");
		}
		builder.append(minutes);
		builder.append(":");
		if (seconds < 10)
			builder.append("0");
		builder.append(seconds);
		return builder.toString();
	}

    public static boolean isTablet(Context context) {
		return (context.getResources().getConfiguration().screenLayout
				& Configuration.SCREENLAYOUT_SIZE_MASK)
				>= Configuration.SCREENLAYOUT_SIZE_LARGE;
	}

    public static String getVerboseTimeString(Context context, float seconds, boolean fullDetail) {
        final int secondsPerMinute = 60;
        final int secondsPerHour = secondsPerMinute * 60;
        final int secondsPerDay = secondsPerHour * 24;
		final int secondsPerMonth = secondsPerDay * 30;
		final int secondsPerYear = secondsPerDay * 365;

        StringBuilder listenText = new StringBuilder();

        if (seconds > secondsPerYear) {
            int years = (int) Math.floor(seconds / secondsPerYear);
            listenText.append(years);
            listenText.append(" ");
            listenText.append(context.getResources().getQuantityString(R.plurals.years, years));
            seconds = seconds % secondsPerYear;
			if (!fullDetail)
				return listenText.toString();
        }

        if (seconds > secondsPerMonth) {
            int months = (int) Math.floor(seconds / secondsPerMonth);
			if (listenText.length() > 0)
				listenText.append(" ");
            listenText.append(months);
            listenText.append(" ");
            listenText.append(context.getResources().getQuantityString(R.plurals.months, months));
            seconds = seconds % secondsPerMonth;
			if (!fullDetail)
				return listenText.toString();
        }

        if (seconds > secondsPerDay) {
            int days = (int) Math.floor(seconds / secondsPerDay);
			if (listenText.length() > 0)
				listenText.append(" ");
            listenText.append(days);
            listenText.append(" ");
            listenText.append(context.getResources().getQuantityString(R.plurals.days, days));
            seconds = seconds % secondsPerDay;
			if (!fullDetail)
				return listenText.toString();
        }

        if (seconds > secondsPerHour) {
            int hours = (int) Math.floor(seconds / secondsPerHour);
            if (listenText.length() > 0)
                listenText.append(" ");
            listenText.append(hours);
            listenText.append(" ");
            listenText.append(context.getResources().getQuantityString(R.plurals.hours, hours));
            seconds = seconds % secondsPerHour;
			if (!fullDetail)
				return listenText.toString();
        }

        if (seconds > secondsPerMinute) {
            int minutes = (int) Math.floor(seconds / secondsPerMinute);
            if (listenText.length() > 0)
                listenText.append(" ");
            listenText.append(minutes);
            listenText.append(" ");
            listenText.append(context.getResources().getQuantityString(R.plurals.minutes, minutes));
			if (!fullDetail)
				return listenText.toString();
        }
        if (listenText.length() == 0)
            listenText.append(context.getString(R.string.none));
        return listenText.toString();
    }

	public static int getVersionCode(Context context) {
		PackageManager packageManager = context.getPackageManager();
		if (packageManager == null)
			return -1;

		try {
			return packageManager.getPackageInfo(context.getPackageName(), 0).versionCode;
		} catch (PackageManager.NameNotFoundException ignored) {
			return -1;
		}
	}

	public static Activity getActivityFromView(View view) {
		Context context = view.getContext();
		while (context instanceof ContextWrapper) {
			if (context instanceof Activity) {
				return (Activity)context;
			}
			context = ((ContextWrapper)context).getBaseContext();
		}
		return null;
	}
}
