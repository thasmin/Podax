package com.axelby.podax;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

// catch all of our Headset events
public class HeadsetConnectionReceiver extends BroadcastReceiver {
	private boolean headsetConnected = false;

	public void onReceive(Context context, Intent intent) {
		if (!intent.hasExtra("state"))
			return;

		boolean justConnected = intent.getIntExtra("state", 0) == 1;
		Log.d("Podax", justConnected ? "Headset connected" : "Headset disconnected");

		if (headsetConnected && !justConnected) {
			// if we're playing, pause
			if (PlayerStatus.getCurrentState(context).isPlaying())
					PlayerService.stop(context);
		}

		headsetConnected = justConnected;
	}
}

