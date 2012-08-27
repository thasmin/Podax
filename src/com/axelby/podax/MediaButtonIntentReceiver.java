package com.axelby.podax;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;

public class MediaButtonIntentReceiver extends BroadcastReceiver {

	@TargetApi(9)
	@Override
	public void onReceive(Context context, Intent intent) {
		KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

		if (event.getAction() != KeyEvent.ACTION_DOWN)
			return;

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		boolean canResume = prefs.getBoolean("resumeOnBluetoothPref", true);

		PodaxLog.log(context, "got media button down event");
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD && event.getDevice() != null)
			PodaxLog.log(context, "  from %s", event.getDevice().getName());

		switch(event.getKeyCode()) {
		// Simple headsets only send KEYCODE_HEADSETHOOK
		case KeyEvent.KEYCODE_HEADSETHOOK:
		case KeyEvent.KEYCODE_MEDIA_PLAY:
		case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:

			// use the pref before we start playing
			if (PlayerStatus.isStopped() && !canResume)
				break;

			if  (event.getRepeatCount() == 0) {
				PodaxLog.log(context, "playpausing because of media button intent");
				PlayerService.playpause(context);
			} else if (event.getRepeatCount() == 2) {
				PlayerService.skipForward(context);
				PlayerService.play(context);
			}
			break;
		case KeyEvent.KEYCODE_MEDIA_PAUSE:
			PodaxLog.log(context, "pausing because of media button intent");
			PlayerService.pause(context);
			break;
		case KeyEvent.KEYCODE_MEDIA_STOP:
			PodaxLog.log(context, "stopping because of media button intent");
			PlayerService.stop(context);
			break;
		case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
		case KeyEvent.KEYCODE_MEDIA_NEXT:
			PodaxLog.log(context, "skipping forard because of media button intent");
			PlayerService.skipForward(context);
			break;
		case KeyEvent.KEYCODE_MEDIA_REWIND:
		case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
			PodaxLog.log(context, "skipping back because of media button intent");
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