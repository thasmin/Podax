package com.axelby.podax;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.util.TypedValue;
import android.view.View;

import org.joda.time.Duration;
import org.joda.time.Period;
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

	private static final PeriodFormatter _shortFormatter;
	private static PeriodFormatter _verboseFormatter;

	static {
		_shortFormatter = new PeriodFormatterBuilder()
			.appendHours().appendSeparator(":")
			.printZeroAlways().minimumPrintedDigits(2)
			.appendMinutes().appendSeparator(":")
			.appendSeconds().toFormatter();
	}

	// TODO: change milliseconds argument to seconds
	public static String getTimeString(int milliseconds) {
		if (milliseconds / 1000 == 0)
			return "00:00";
		Period period = Duration.millis(milliseconds).minus(milliseconds % 1000).toPeriod();
		return _shortFormatter.print(period);
	}

    public static String getVerboseTimeString(Context context, float seconds, boolean fullDetail) {
		if (_verboseFormatter == null) {
			String hour = " " + context.getString(R.string.hour);
			String hours = " " + context.getString(R.string.hours);
			String minute = " " + context.getString(R.string.minute);
			String minutes = " " + context.getString(R.string.minutes);
			String second = " " + context.getString(R.string.second);
			String secondsWord = " " + context.getString(R.string.seconds);
			_verboseFormatter = new PeriodFormatterBuilder()
				.appendHours().appendSuffix(hour, hours).appendSeparator(", ")
				.appendMinutes().appendSuffix(minute, minutes).appendSeparator(", ")
				.appendSeconds().appendSuffix(second, secondsWord)
				.toFormatter();
		}

		if (seconds > 60 && !fullDetail)
			seconds -= seconds % 60;
		Period period = Duration.millis((long)(seconds) * 1000).toPeriod();
		return _verboseFormatter.print(period);
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

	public static @ColorInt int getAttributeColor(Context context, @AttrRes int attr) {
		TypedValue typedValue = new TypedValue();
		Resources.Theme theme = context.getTheme();
		theme.resolveAttribute(attr, typedValue, true);
		return typedValue.data;
	}
}
