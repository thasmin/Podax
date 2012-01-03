package com.axelby.podax;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;

public class UpdateService extends Service {
	public static void updateSubscriptions(Context context) {
		Intent intent = new Intent(context, UpdateService.class);
		intent.setAction(Constants.ACTION_REFRESH_ALL_SUBSCRIPTIONS);
		context.startService(intent);
	}

	public static void updateSubscription(Context context, int subscriptionId) {
		Intent intent = new Intent(context, UpdateService.class);
		intent.setAction(Constants.ACTION_REFRESH_SUBSCRIPTION);
		intent.putExtra(Constants.EXTRA_SUBSCRIPTION_ID, subscriptionId);
		context.startService(intent);
	}

	public static void updateSubscription(Context context, Uri subscriptionUri) {
		Intent intent = new Intent(context, UpdateService.class);
		intent.setAction(Constants.ACTION_REFRESH_SUBSCRIPTION);
		intent.putExtra(Constants.EXTRA_SUBSCRIPTION_ID, Integer.valueOf(subscriptionUri.getLastPathSegment()));
		context.startService(intent);
	}

	public static void downloadPodcasts(Context context) {
		Intent intent = new Intent(context, UpdateService.class);
		intent.setAction(Constants.ACTION_DOWNLOAD_PODCASTS);
		context.startService(intent);
	}

	private SubscriptionUpdateBinder _binder = new SubscriptionUpdateBinder();
	private SubscriptionUpdater _subscriptionUpdater;
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

		if (action.equals(Constants.ACTION_STARTUP)) {
			AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);

			// refresh the feeds
			Intent refreshIntent = new Intent(this, UpdateService.class);
			refreshIntent.setAction(Constants.ACTION_REFRESH_ALL_SUBSCRIPTIONS);
			PendingIntent pendingRefreshIntent = PendingIntent.getService(this, 0, refreshIntent, 0);
			alarmManager.setInexactRepeating(AlarmManager.RTC,
					System.currentTimeMillis(), AlarmManager.INTERVAL_HOUR, pendingRefreshIntent);

			Intent downloadIntent = new Intent(this, UpdateService.class);
			downloadIntent.setAction(Constants.ACTION_DOWNLOAD_PODCASTS);
			PendingIntent pendingDownloadIntent = PendingIntent.getService(this, 0, downloadIntent, 0);
			alarmManager.setInexactRepeating(AlarmManager.RTC,
					System.currentTimeMillis(), AlarmManager.INTERVAL_HOUR, pendingDownloadIntent);
			
			_subscriptionUpdater.addAllSubscriptions();
			_subscriptionUpdater.run();

			_podcastDownloader.download();
		}
		else if (action.equals(Constants.ACTION_REFRESH_ALL_SUBSCRIPTIONS)) {
			_subscriptionUpdater.addAllSubscriptions();
			_subscriptionUpdater.run();
		}
		else if (action.equals(Constants.ACTION_REFRESH_SUBSCRIPTION)) {
			int subscriptionId = intent.getIntExtra(Constants.EXTRA_SUBSCRIPTION_ID, -1);
			if (subscriptionId != -1) {
				_subscriptionUpdater.addSubscriptionId(subscriptionId);
				_subscriptionUpdater.run();
			}
		}
		else if (action.equals(Constants.ACTION_DOWNLOAD_PODCASTS)) {
			_podcastDownloader.download();
		}
	}
}
