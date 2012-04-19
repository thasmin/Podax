package com.axelby.podax;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothClass;

// catch all of our Bluetooth events
public class BluetoothConnectionReceiver extends BroadcastReceiver {

	public void onReceive(Context context, Intent intent) {
		Log.d("Podax", "Bluetooth something");

		// If we're not stopping when headphones disconnect, don't worry about it
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if (!prefs.getBoolean("stopOnBluetoothPref", true))
			return;

		// Figure out what kind of device it is
		BluetoothDevice btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

		// if it's a headphones thingy

		Log.d("Podax", "Device class is " + btDevice.getBluetoothClass().getMajorDeviceClass());
		Log.d("Podax", "We only care about " + BluetoothClass.Device.Major.AUDIO_VIDEO);
		if (btDevice.getBluetoothClass().getMajorDeviceClass() == BluetoothClass.Device.Major.AUDIO_VIDEO)
			// if we're playing, pause
			if (PlayerService.isPlaying())
				PlayerService.pause(context);
	}
}

