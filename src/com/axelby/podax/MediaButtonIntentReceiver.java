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

        int action = event.getAction();
        
        if (action == KeyEvent.ACTION_DOWN) {
        	if (Helper.isPlaying(context)) {
        		if (event.isLongPress()) {
        			Log.d("Podax", "Headset button long-press: Skip forward");
        			PlayerService.skipForward(context);
        		} else {
        			Log.d("Podax", "Headset button: Pause");
        			PlayerService.pause(context);
        		}
        	} else {
        		Log.d("Podax", "Headset button: Resume");
        		PlayerService.playpause(context);
        	}
        }
        abortBroadcast();
    }

}
