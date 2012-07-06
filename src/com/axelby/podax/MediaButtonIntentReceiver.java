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

        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return;
        }
        switch(event.getKeyCode()) {
            // Simple headsets only send KEYCODE_HEADSETHOOK
            case KeyEvent.KEYCODE_HEADSETHOOK:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if  (event.getRepeatCount() == 0) {
                    Log.d("Podax", "Headset button: Play/pause");
                    PlayerService.playpause(context);
                } else if (event.getRepeatCount() == 2) {
                    Log.d("Podax", "Headset button: Long press");
                    PlayerService.skipForward(context);
                    PlayerService.play(context);
                }
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                Log.d("Podax", "Headset button: Play");
                PlayerService.play(context);
                break;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_STOP:
                Log.d("Podax", "Headset button: Pause");
                PlayerService.pause(context);
                break;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                Log.d("Podax", "Headset button fast-forward: Skip forward");
                PlayerService.skipForward(context);
                break;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                Log.d("Podax", "Headset button rewind: Skip back");
                PlayerService.skipBack(context);
                break;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                Log.d("Podax", "Headset button: next podcast");
                // skipToEnd removes the current podcast from the queue,
                // which seems extreme; skip forward instead for now
                // TODO: Make this a preference?
                PlayerService.skipForward(context);
                //PlayerService.skipToEnd(context);
                break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                Log.d("Podax", "Headset button: restart podcast");
                PlayerService.restart(context);
                break;
            default:
                Log.d("Podax", "No matched event: " + event.getKeyCode());
        }
       
        if (this.isOrderedBroadcast()) {
            abortBroadcast();
        }
    }
}
