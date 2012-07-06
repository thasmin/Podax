package com.axelby.podax;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

public class Helper {

	public static boolean ensureWifi(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo == null || !netInfo.isConnectedOrConnecting())
			return false;
		// always OK if we're on wifi
		if (netInfo.getType() == ConnectivityManager.TYPE_WIFI)
			return true;
		// check for wifi only pref
		if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("wifiPref", true))
		{
			Log.d("Podax", "Not downloading because Wifi is required and not connected");
			return false;
		}
		// check for 3g data turned off
		if (!netInfo.isConnected()) {
			Log.d("Podax", "Not downloading because background data is turned off");
			return false;
		}
		
		return true;
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


	public static boolean isPlaying(Context context) {
		ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
			if ("com.axelby.podax.PlayerService".equals(service.service.getClassName()) &&
					PlayerService.isPlaying())
				return true;
		return false;
	}

	public static boolean isGPodderInstalled(Context context) {
		List<ProviderInfo> providerList = context.getPackageManager().queryContentProviders(null, 0, 0);
		for (ProviderInfo provider : providerList)
			if (provider.authority.equals("com.axelby.gpodder.podcasts"))
				return true;
		return false;
	}

	public static void registerMediaButtons(Context context) {
		AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
		audioManager.registerMediaButtonEventReceiver(new ComponentName(context, MediaButtonIntentReceiver.class));
	}

	public static void unregisterMediaButtons(Context context) {
		AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
		audioManager.unregisterMediaButtonEventReceiver(new ComponentName(context, MediaButtonIntentReceiver.class));
	}
}
