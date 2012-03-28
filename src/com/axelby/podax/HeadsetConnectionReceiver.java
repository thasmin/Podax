package com.axelby.podax;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

// catch all of our Headset events
public class HeadsetConnectionReceiver extends BroadcastReceiver {
	private boolean headsetConnected = false;
	public void onReceive(Context context, Intent intent) {
		// if we have a state
		if (intent.hasExtra("state")){
			// if we're disconnecting
			if (headsetConnected && intent.getIntExtra("state", 0) == 0){
				Log.d("Podax", "Headset Disconnected");
				headsetConnected = false;
				// if we're playing, pause
				if (PlayerService.isPlaying()) {
						PlayerService.pause(context);
				}
			}
			// know when we're connecting so we can catch the disconnect later
			else if (!headsetConnected && intent.getIntExtra("state", 0) == 1){
				headsetConnected = true;
				Log.d("Podax", "Headset Connected");
			}
		}
	}
}

