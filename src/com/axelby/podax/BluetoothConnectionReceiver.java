package com.axelby.podax;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

// catch all of our Bluetooth events
public class BluetoothConnectionReceiver extends BroadcastReceiver {

	public void onReceive(Context context, Intent intent) {
		// If we're not stopping when headphones disconnect, don't worry about it
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if (!prefs.getBoolean("stopOnBluetoothPref", true))
			return;

		// Figure out what kind of device it is
		BluetoothDevice btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

		// pause if it's headphones
		if (btDevice.getBluetoothClass().getMajorDeviceClass() == BluetoothClass.Device.Major.AUDIO_VIDEO)
			if (PlayerStatus.isPlaying())
				PlayerService.stop(context);
	}
}

