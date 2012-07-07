package com.axelby.podax;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

public class MediaButtonIntentReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

		if (event.getAction() != KeyEvent.ACTION_DOWN)
			return;

		switch(event.getKeyCode()) {
		// Simple headsets only send KEYCODE_HEADSETHOOK
		case KeyEvent.KEYCODE_HEADSETHOOK:
		case KeyEvent.KEYCODE_MEDIA_PLAY:
		case KeyEvent.KEYCODE_MEDIA_PAUSE:
		case KeyEvent.KEYCODE_MEDIA_STOP:
		case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
			if  (event.getRepeatCount() == 0)
				PlayerService.playpause(context);
			else if (event.getRepeatCount() == 2) {
				PlayerService.skipForward(context);
				PlayerService.play(context);
			}
			break;
		case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
		case KeyEvent.KEYCODE_MEDIA_NEXT:
			PlayerService.skipForward(context);
			break;
		case KeyEvent.KEYCODE_MEDIA_REWIND:
		case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
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