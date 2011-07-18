package com.axelby.podax;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class UpdateService extends Service {
	public static void updateSubscription(Context context, Subscription subscription) {
		Intent intent = new Intent(context, UpdateService.class);
		intent.setAction("com.axelby.podax.REFRESH_SUBSCRIPTION");
		intent.putExtra("com.axelby.podax.SUBSCRIPTION_ID", subscription.getId());
		context.startService(intent);
	}

	public static void downloadPodcasts(Context context) {
		Intent intent = new Intent(context, UpdateService.class);
		intent.setAction("com.axelby.podax.DOWNLOAD_PODCASTS");
		context.startService(intent);
	}

	private SubscriptionUpdateBinder _binder = new SubscriptionUpdateBinder();
	private PendingIntent _hourlyRefreshIntent;
	private SubscriptionUpdater _subscriptionUpdater;
	private PendingIntent _hourlyDownloadIntent;
	private PodcastDownloader _podcastDownloader;

	public class SubscriptionUpdateBinder extends Binder {
		UpdateService getService() {
			return UpdateService.this;
		}
	}
	@Override
	public IBinder onBind(Intent arg0) {
		return _binder;
	}

	@Override
	public void onCreate() {
		_subscriptionUpdater = new SubscriptionUpdater(this);
		_podcastDownloader = new PodcastDownloader(this);
	}

	@Override
	public void onDestroy() {
	}

	@Override
	public void onStart(Intent intent, int startId) {
	    handleIntent(intent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	    handleIntent(intent);
	    return START_NOT_STICKY;
	}
	
	private void handleIntent(Intent intent) {
		String action = intent.getAction();
		if (action == null)
			return;

		if (action.equals("com.axelby.podax.STARTUP")) {
			AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);

			// refresh the feeds
			Intent refreshIntent = new Intent(this, UpdateService.class);
			refreshIntent.setAction("com.axelby.podax.REFRESH_ALL_SUBSCRIPTIONS");
			_hourlyRefreshIntent = PendingIntent.getService(this, 0, refreshIntent, 0);
			alarmManager.setInexactRepeating(AlarmManager.RTC,
					System.currentTimeMillis(), AlarmManager.INTERVAL_HOUR, _hourlyRefreshIntent);

			Intent downloadIntent = new Intent(this, UpdateService.class);
			downloadIntent.setAction("com.axelby.podax.DOWNLOAD_PODCASTS");
			_hourlyDownloadIntent = PendingIntent.getService(this, 0, downloadIntent, 0);
			alarmManager.setInexactRepeating(AlarmManager.RTC,
					System.currentTimeMillis(), AlarmManager.INTERVAL_HOUR, _hourlyDownloadIntent);
		}
		if (action.equals("com.axelby.podax.REFRESH_ALL_SUBSCRIPTIONS")) {
			_subscriptionUpdater.addAllSubscriptions();
			_subscriptionUpdater.run();
		}
		if (action.equals("com.axelby.podax.REFRESH_SUBSCRIPTION")) {
			int subscriptionId = intent.getIntExtra("com.axelby.podax.SUBSCRIPTION_ID", -1);
			if (subscriptionId != -1) {
				_subscriptionUpdater.addSubscriptionId(subscriptionId);
				_subscriptionUpdater.run();
			}
		}
		if (action.equals("com.axelby.podax.DOWNLOAD_PODCASTS")) {
			_podcastDownloader.download();
		}
	}
}
