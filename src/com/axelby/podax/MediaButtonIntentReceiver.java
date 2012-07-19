package com.axelby.podax;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;

public class MediaButtonIntentReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

		if (event.getAction() != KeyEvent.ACTION_DOWN)
			return;

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		boolean canResume = prefs.getBoolean("resumeOnBluetoothPref", true);

		switch(event.getKeyCode()) {
		// Simple headsets only send KEYCODE_HEADSETHOOK
		case KeyEvent.KEYCODE_HEADSETHOOK:
		case KeyEvent.KEYCODE_MEDIA_PLAY:
		case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
			PodaxLog.log(context, "media button from {0}: MEDIA_PLAY_PAUSE", event.getDevice().getName());
			// use the pref before we start playing
			if (PlayerStatus.isStopped() && !canResume)
				break;

			if  (event.getRepeatCount() == 0)
				PlayerService.playpause(context);
			else if (event.getRepeatCount() == 2) {
				PlayerService.skipForward(context);
				PlayerService.play(context);
			}
			break;
		case KeyEvent.KEYCODE_MEDIA_PAUSE:
			PodaxLog.log(context, "media button from {0}: MEDIA_PAUSE", event.getDevice().getName());
			PlayerService.pause(context);
			break;
		case KeyEvent.KEYCODE_MEDIA_STOP:
			PodaxLog.log(context, "media button from {0}: MEDIA_STOP", event.getDevice().getName());
			PlayerService.stop(context);
			break;
		case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
		case KeyEvent.KEYCODE_MEDIA_NEXT:
			PodaxLog.log(context, "media button from {0}: MEDIA_NEXT", event.getDevice().getName());
			PlayerService.skipForward(context);
			break;
		case KeyEvent.KEYCODE_MEDIA_REWIND:
		case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
			PodaxLog.log(context, "media button from {0}: MEDIA_PREVIOUS", event.getDevice().getName());
			PlayerService.skipBack(context);
			break;
		default:
			Log.d("Podax", "No matched event: " + event.getKeyCode());
		}

		if (this.isOrderedBroadcast()) {
			abortBroadcast();
		}
	}

}