package com.axelby.podax;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

public class PodaxApp extends Application {
	static private PodaxApp _instance;

	@Override
	public void onCreate() {
		super.onCreate();

		_instance = this;

		Intent intent = new Intent(this, UpdateService.class);
		intent.setAction(Constants.ACTION_STARTUP);
		startService(intent);
		
		PodaxApp.updateWidgets(this);
	}

	public static PodaxApp getApp() {
		return _instance;
	}

	public void play() {
		sendPlayerCommand(Constants.PLAYER_COMMAND_PLAY);
	}

	public void pause() {
		sendPlayerCommand(Constants.PLAYER_COMMAND_PAUSE);
	}

	public void playpause() {
		sendPlayerCommand(Constants.PLAYER_COMMAND_PLAYPAUSE);
	}

	public void skipForward() {
		sendPlayerCommand(Constants.PLAYER_COMMAND_SKIPFORWARD);
	}

	public void skipBack() {
		sendPlayerCommand(Constants.PLAYER_COMMAND_SKIPBACK);
	}

	public void restart() {
		sendPlayerCommand(Constants.PLAYER_COMMAND_RESTART);
	}
	
	public void skipToEnd() {
		sendPlayerCommand(Constants.PLAYER_COMMAND_SKIPTOEND);
	}
	
	public void skipTo(int secs) {
		sendPlayerCommand(Constants.PLAYER_COMMAND_SKIPTO, secs);
	}

	private void sendPlayerCommand(int command, int arg) {
		Intent intent = new Intent(this, PlayerService.class);
		intent.putExtra(Constants.EXTRA_PLAYER_COMMAND, command);
		intent.putExtra(Constants.EXTRA_PLAYER_COMMAND_ARG, arg);
		startService(intent);
	}

	private void sendPlayerCommand(int command) {
		Intent intent = new Intent(this, PlayerService.class);
		intent.putExtra(Constants.EXTRA_PLAYER_COMMAND, command);
		startService(intent);
	}

	public void play(Podcast podcast) {
		if (podcast == null)
			return;
		sendPlayerCommand(Constants.PLAYER_COMMAND_PLAY_SPECIFIC_PODCAST, podcast.getId());
	}

	static String getTimeString(int milliseconds) {
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

	public static void updateWidgets(Context context) {
		SmallWidgetProvider.updateWidget(context);
		LargeWidgetProvider.updateWidget(context);
	}

	public static boolean ensureWifi(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo == null || !netInfo.isConnectedOrConnecting())
			return false;
		if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("wifiPref", false) &&
				netInfo.getType() != ConnectivityManager.TYPE_WIFI)
		{
			Log.d("Podax", "Not downloading because Wifi is required and not connected");
			return false;
		}
		
		return true;
	}
}
