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

import org.joda.time.Duration;
import org.joda.time.Hours;
import org.joda.time.Minutes;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

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

    public static boolean isTablet(Context context) {
		return (context.getResources().getConfiguration().screenLayout
				& Configuration.SCREENLAYOUT_SIZE_MASK)
				>= Configuration.SCREENLAYOUT_SIZE_LARGE;
	}

	public static String getTimeString(int milliseconds) {
		Period period = Duration.millis(milliseconds).minus(milliseconds % 1000).toPeriod();
		if (Hours.standardHoursIn(period).getHours() > 0) {
			PeriodFormatter formatter = new PeriodFormatterBuilder()
				.appendHours().appendSeparator(":")
				.appendMinutes().minimumPrintedDigits(2).appendSeparator(":")
				.appendSeconds().minimumPrintedDigits(2).toFormatter();
			return formatter.print(period);
		}

		PeriodFormatter formatter = new PeriodFormatterBuilder()
			.appendMinutes().appendSeparator(":")
			.appendSeconds().minimumPrintedDigits(2).toFormatter();
		return formatter.print(period);
	}

    public static String getVerboseTimeString(Context context, float seconds, boolean fullDetail) {
		Period period = Duration.millis((long)(seconds) * 1000).toPeriod();
		if (fullDetail)
			return PeriodFormat.getDefault().print(period);

		if (Hours.standardHoursIn(period).getHours() > 0) {
			String hour = " " + context.getString(R.string.hour);
			String hours = " " + context.getString(R.string.hours);
			PeriodFormatter hoursFormatter = new PeriodFormatterBuilder()
				.appendHours().appendSuffix(hour, hours)
				.toFormatter();
			return hoursFormatter.print(period);
		}

		if (Minutes.standardMinutesIn(period).getMinutes() > 0) {
			String minute = " " + context.getString(R.string.minute);
			String minutes = " " + context.getString(R.string.minutes);
			PeriodFormatter minutesFormatter = new PeriodFormatterBuilder()
				.appendMinutes().appendSuffix(minute, minutes)
				.toFormatter();
			return minutesFormatter.print(period);
		}

		String second = context.getString(R.string.second);
		String secondsWord = context.getString(R.string.seconds);
		PeriodFormatter secondsFormatter = new PeriodFormatterBuilder().appendSeconds().appendSuffix(second, secondsWord).toFormatter();
		return secondsFormatter.print(period);
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
