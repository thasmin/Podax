package com.axelby.podax;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

// catch all of our Headset events
public class HeadsetConnectionReceiver extends BroadcastReceiver {
	private boolean headsetConnected = false;

	public void onReceive(Context context, Intent intent) {
		if (!intent.hasExtra("state"))
			return;

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if (!prefs.getBoolean("stopOnHeadphonePref", true))
			return;

		boolean justConnected = intent.getIntExtra("state", 0) == 1;
		Log.d("Podax", justConnected ? "Headset connected" : "Headset disconnected");

		if (headsetConnected && !justConnected) {
			// if we're playing, pause
			if (Helper.isPlaying(context)) {
					PlayerService.pause(context);
			} else if (prefs.getBoolean("resumeOnBluetoothPref", true)) {
				Log.d("Podax", "Headset button: Resume");
				PlayerService.playpause(context);
			}
		}

		headsetConnected = justConnected;
	}
}

