package com.axelby.podax;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		setupAlarms(context);
	}

	public static void setupAlarms(Context context) {
		// intent will always be BOOT_COMPLETED
		AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

		// refresh the feeds
		Intent refreshIntent = new Intent(context, UpdateService.class);
		refreshIntent.setAction(Constants.ACTION_REFRESH_ALL_SUBSCRIPTIONS);
		PendingIntent pendingRefreshIntent = PendingIntent.getService(context, 0, refreshIntent, 0);
		alarmManager.cancel(pendingRefreshIntent);
		alarmManager.setInexactRepeating(AlarmManager.RTC,
				System.currentTimeMillis(), AlarmManager.INTERVAL_HOUR, pendingRefreshIntent);
	}

}
