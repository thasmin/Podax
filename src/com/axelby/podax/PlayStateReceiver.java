package com.axelby.podax;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

public class PlayStateReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
			if (PlayerStatus.getCurrentState(context).isPlaying())
				PlayerService.stop(context);
		} else if (intent.getAction().equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
			if (PlayerStatus.getCurrentState(context).isPlaying())
				PlayerService.stop(context);
		}
	}
}
