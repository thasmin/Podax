package com.axelby.podax;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.joda.time.LocalDateTime;
import org.joda.time.Seconds;

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		setupAlarms(context);
	}

	public static void setupAlarms(Context context) {
		// intent will always be BOOT_COMPLETED
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		// refresh the feeds
		Intent refreshIntent = new Intent(context, UpdateService.class);
		refreshIntent.setAction(Constants.ACTION_REFRESH_ALL_SUBSCRIPTIONS);
		PendingIntent pendingRefreshIntent = PendingIntent.getService(context, 0, refreshIntent, 0);
		alarmManager.setInexactRepeating(AlarmManager.RTC,
				System.currentTimeMillis(), AlarmManager.INTERVAL_HOUR * 6, pendingRefreshIntent);

		// cache the itunes toplists
		Intent toplistIntent = new Intent(context, ToplistService.class);
		PendingIntent toplistPI = PendingIntent.getService(context, 0, toplistIntent, 0);
		// figure out if this should run today or tomorrow
		LocalDateTime now = new LocalDateTime();
		LocalDateTime twoAMToday = now.withMillisOfDay(0).plusHours(2);
		if (now.isBefore(twoAMToday))
			alarmManager.setRepeating(AlarmManager.RTC, twoAMToday.getMillisOfDay(), AlarmManager.INTERVAL_DAY, toplistPI);
		else {
			int fromNow = Seconds.secondsBetween(now, twoAMToday.plusDays(1)).getSeconds() * 1000;
			alarmManager.setRepeating(AlarmManager.RTC,
				System.currentTimeMillis() + fromNow, AlarmManager.INTERVAL_DAY, toplistPI);
		}
	}

}
