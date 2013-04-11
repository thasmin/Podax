package com.axelby.podax;

import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import com.axelby.podax.ui.LargeWidgetProvider;
import com.axelby.podax.ui.SmallWidgetProvider;

public class PlayStateReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
			if (PlayerStatus.getCurrentState(context).isPlaying())
				PlayerService.stop(context);
		} else if (intent.getAction().equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
			if (PlayerStatus.getCurrentState(context).isPlaying())
				PlayerService.stop(context);
		} else if (intent.getAction().equals("com.axelby.podax.player.positionchanged")) {
			updateWidgets(context);
		} else if (intent.getAction().equals("com.axelby.podax.player.statechanged")) {
			updateWidgets(context);
		} else if (intent.getAction().equals("com.axelby.podax.player.activepodcastchanged")) {
			updateWidgets(context);
		}
	}

	private void updateWidgets(Context context) {
		AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
		int[] widgetIds;

		widgetIds = widgetManager.getAppWidgetIds(new ComponentName(context, LargeWidgetProvider.class));
		new LargeWidgetProvider().onUpdate(context, widgetManager, widgetIds);

		widgetIds = widgetManager.getAppWidgetIds(new ComponentName(context, SmallWidgetProvider.class));
		new SmallWidgetProvider().onUpdate(context, widgetManager, widgetIds);
	}

}
